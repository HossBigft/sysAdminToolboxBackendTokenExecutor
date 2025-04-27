package org.example.config.security;

import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;
import org.example.config.core.ConfigManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class SudoPrivilegeManager {
    static final String shellUser = ShellUtils.resolveToolBoxUser();
    private final String SUDOERS_DIR = "/etc/sudoers.d/";

    private final String TEMP_DIR = "/tmp/";
    private final String SUDOERS_PERMISSIONS = "r--r-----";

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public void setupSudoPrivileges() throws CommandFailedException, IOException, URISyntaxException {
        Path sudoersFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());

        if (Files.exists(sudoersFile) && isFileInsecure(sudoersFile)) {
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

    private boolean isFileInsecure(Path file) throws IOException {
        boolean isSecure = FileSecurityManager.hasCorrectPermissions(file, SUDOERS_PERMISSIONS)
                || !FileSecurityManager.hasOwner(file, "root")
                || !FileSecurityManager.hasOwnerGroup(file, "root");
        if (!isSecure) {
            getLogger().
                    warn("Existing sudoers file have incorrect permissions: " + file.toString());
            return false;
        }
        return true;

    }

    private void securePermissions(Path file) throws IOException {

        FileSecurityManager.setPermissions(file, SUDOERS_PERMISSIONS);
        FileSecurityManager.setOwner(file, "root");
        FileSecurityManager.setGroup(file, "root");
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
                getLogger().
                        info("Sudoers rule not present in " + sudoersFile);
            }

            return missing;

        } catch (IOException e) {
            getLogger().
                    info("Sudoers file " + sudoersFile + " is not present.");
            return true;
        }
    }

    private void createSudoersRuleFile(String sudoRule) throws IOException, CommandFailedException {
        String tempFileName = TEMP_DIR + "sudoers_" + ConfigManager.getDatabaseUser();
        Path tempFile = Paths.get(tempFileName);

        getLogger().
                debug("Creating temp sudo rule file at " + tempFile);
        Files.writeString(tempFile, sudoRule);


        Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("r--r-----"));
        try {
            getLogger().
                    debug("Validating sudoers syntax with visudo -cf");
            ShellUtils.runCommand("visudo", "-cf", tempFileName);
        } catch (CommandFailedException e) {
            getLogger().
                    error("Invalid sudoers syntax detected!", e);
            Files.delete(tempFile);
            getLogger().
                    error("Failed temp file removed without moving it to sudoers " + tempFile);
            throw new CommandFailedException("Invalid sudoers syntax detected!");
        }
        getLogger().
                debug("Soon to be sudoers file is validated " + tempFile);

        Path targetFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());
        getLogger().
                debug("Copying sudo rule to final location: " + targetFile);
        ShellUtils.runCommand("sudo", "cp", tempFileName, targetFile.toString());
        getLogger().
                debug("Applying 440 permissions to " + targetFile);
        ShellUtils.runCommand("sudo", "chmod", "440", targetFile.toString());

        Files.delete(tempFile);
        getLogger().
                debug("Temporary sudo rule file deleted: " + tempFile);
    }

    private String generateSudoRule() throws URISyntaxException {
        String rule = shellUser + " ALL=(ALL) NOPASSWD: " + ShellUtils.getExecutablePath() + " *";
        getLogger().
                debug("Generated sudo rule: " + rule);
        return rule;
    }

    private void printRelevantRules() throws CommandFailedException {
        getLogger().
                debug("Printing relevant sudo rules from /etc/sudoers");
        ShellUtils.runCommand("cat", "/etc/sudoers").stream()
                .filter(l -> l.contains(shellUser))
                .forEach(System.out::println);
    }


}

