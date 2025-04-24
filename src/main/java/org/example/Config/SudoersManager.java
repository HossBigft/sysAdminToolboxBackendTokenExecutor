package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.Logging.facade.LogManager;
import org.example.Utils.ShellUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class SudoersManager {
    static final String shellUser = ShellUtils.resolveToolBoxUser();
    private final String SUDOERS_DIR = "/etc/sudoers.d/";
    private final String TEMP_DIR = "/tmp/";
    private final String SUDOERS_PERMISSIONS = "r--r-----";

    public void ensureSudoersRuleIsPresent() throws CommandFailedException, IOException, URISyntaxException {
        Path sudoersFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());

        if (Files.exists(sudoersFile) && isPermissionsInsecure(sudoersFile)) {
            System.out.println("Warning: Existing sudoers file with incorrect permissions detected!");
            securePermissions(sudoersFile);
        }

        if (isSudoRuleNotPresentInFile()) {
            createSudoersRuleFile(generateSudoRule());
            securePermissions(sudoersFile);

            if (isSudoRuleNotPresentInFile()) {
                throw new CommandFailedException("Failed to apply sudo rules correctly!");
            }
            printRelevantRules();
        }
    }

    private boolean isPermissionsInsecure(Path file) throws IOException {
        boolean isSecure = PermissionManager.hasPermissions(file, SUDOERS_PERMISSIONS)
                || !PermissionManager.hasOwner(file, "root")
                || !PermissionManager.hasOwnerGroup(file, "root");
        if (!isSecure) {
            LogManager.warn("Existing sudoers file have incorrect permissions: " + file.toString());
            return false;
        }
        return true;

    }

    private void securePermissions(Path file) throws IOException {

        PermissionManager.setPermissions(file, SUDOERS_PERMISSIONS);
        PermissionManager.setOwner(file, "root");
        PermissionManager.setGroup(file, "root");
    }

    private boolean isSudoRuleNotPresentInFile() {
        Path sudoersFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());
        try {
            boolean missing = Files.readAllLines(sudoersFile).stream().noneMatch(line -> {
                try {
                    return line.contains(generateSudoRule());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });

            if (missing) {
                LogManager.info("Sudoers rule not present in " + sudoersFile);
            }

            return missing;

        } catch (IOException e) {
            LogManager.info("Sudoers file " + sudoersFile + " is not present.");
            return true;
        }
    }

    private void createSudoersRuleFile(String sudoRule) throws IOException, CommandFailedException {
        String tempFileName = TEMP_DIR + "sudoers_" + ConfigManager.getDatabaseUser();
        Path tempFile = Paths.get(tempFileName);

        LogManager.debug("Creating temp sudo rule file at " + tempFile);
        Files.writeString(tempFile, sudoRule);


        Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("r--r-----"));
        try {
            LogManager.debug("Validating sudoers syntax with visudo -cf");
            ShellUtils.runCommand("visudo", "-cf", tempFileName);
        } catch (CommandFailedException e) {
            LogManager.error("Invalid sudoers syntax detected!", e);
            Files.delete(tempFile);
            LogManager.error("Failed temp file removed without moving it to sudoers " + tempFile);
            throw new CommandFailedException("Invalid sudoers syntax detected!");
        }
        LogManager.debug("Soon to be sudoers file is validated " + tempFile);

        Path targetFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());
        LogManager.debug("Copying sudo rule to final location: " + targetFile);
        ShellUtils.runCommand("sudo", "cp", tempFileName, targetFile.toString());
        LogManager.debug("Applying 440 permissions to " + targetFile);
        ShellUtils.runCommand("sudo", "chmod", "440", targetFile.toString());

        Files.delete(tempFile);
        LogManager.debug("Temporary sudo rule file deleted: " + tempFile);
    }

    private String generateSudoRule() throws URISyntaxException {
        String rule = shellUser + " ALL=(ALL) NOPASSWD: " + ShellUtils.getExecutablePath() + " *";
        LogManager.debug("Generated sudo rule: " + rule);
        return rule;
    }

    private void printRelevantRules() throws CommandFailedException {
        LogManager.debug("Printing relevant sudo rules from /etc/sudoers");
        ShellUtils.runCommand("cat", "/etc/sudoers").stream()
                .filter(l -> l.contains(shellUser))
                .forEach(System.out::println);
    }


}

