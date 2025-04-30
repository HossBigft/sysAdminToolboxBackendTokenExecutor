package org.example.config.security;

import org.example.config.core.ConfigProvider;
import org.example.constants.EnvironmentConstants;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class SudoPrivilegeManager {
    private static final String shellUser = ShellUtils.resolveAppUser();
    private static final String SUDOERS_DIR = "/etc/sudoers.d/";

    private static final String TEMP_DIR = "/tmp/";
    private static final String SUDOERS_PERMISSIONS = "r--r-----";
    private static final FileAccessPolicy sudoersFilePolicy = new FileAccessPolicy(SUDOERS_PERMISSIONS,
            EnvironmentConstants.SUPERADMIN_USER, EnvironmentConstants.SUPERADMIN_USER);
    private static final ConfigProvider cprovider = new ConfigProvider();

    public void setupSudoPrivileges() throws CommandFailedException, IOException, URISyntaxException {
        File sudoersFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser()).toFile();

        if (sudoersFile.isFile() && isFileInsecure(sudoersFile)) {
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

    private boolean isFileInsecure(File file) throws IOException {

        return !new FileSecurityManager().isFilePermissionsSecure(file, sudoersFilePolicy);

    }

    private void securePermissions(File file) throws IOException {
        new FileSecurityManager().enforceFileAccessPolicy(file, sudoersFilePolicy);
    }

    private boolean isSudoRuleNotPresentInFile() {
        Path sudoersFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser());
        try {
            boolean missing = Files.readAllLines(sudoersFile).stream().noneMatch(line -> {
                try {
                    return line.contains(generateSudoRule());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });

            if (missing) {
                getLogger().info("Sudoers rule not present in " + sudoersFile);
            }

            return missing;

        } catch (IOException e) {
            getLogger().info("Sudoers file " + sudoersFile + " is not present.");
            return true;
        }
    }

    private void createSudoersRuleFile(String sudoRule) throws IOException, CommandFailedException {
        String tempFileName = TEMP_DIR + "sudoers_" + cprovider.getDatabaseUser();
        Path tempFile = Paths.get(tempFileName);

        getLogger().debug("Creating temp sudo rule file at " + tempFile);
        Files.writeString(tempFile, sudoRule);

        getLogger().debug("Generated sudo rule: " + sudoRule);
        Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("r--r-----"));
        try {
            getLogger().debug("Validating sudoers syntax with visudo -cf");
            ShellUtils.runCommand("visudo", "-cf", tempFileName);
        } catch (CommandFailedException e) {
            getLogger().error("Invalid sudoers syntax detected!", e);
            Files.delete(tempFile);
            getLogger().error("Failed temp file removed without moving it to sudoers " + tempFile);
            throw new CommandFailedException("Invalid sudoers syntax detected!");
        }
        getLogger().debug("Soon to be sudoers file is validated " + tempFile);

        Path targetFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser());
        getLogger().debug("Copying sudo rule to final location: " + targetFile);
        ShellUtils.runCommand("sudo", "cp", tempFileName, targetFile.toString());
        getLogger().debug("Applying 440 permissions to " + targetFile);
        ShellUtils.runCommand("sudo", "chmod", "440", targetFile.toString());

        Files.delete(tempFile);
        getLogger().debug("Temporary sudo rule file deleted: " + tempFile);
    }

    private String generateSudoRule() throws URISyntaxException {
        String rule = shellUser + " ALL=(ALL) NOPASSWD: " + ShellUtils.getExecutablePath() + " *";
        return rule;
    }

    private void printRelevantRules() throws CommandFailedException {
        getLogger().debug("Printing relevant sudo rules from /etc/sudoers");
        ShellUtils.runCommand("cat", "/etc/sudoers").stream().filter(l -> l.contains(shellUser))
                .forEach(System.out::println);
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }


}

