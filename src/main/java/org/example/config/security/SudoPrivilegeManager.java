package org.example.config.security;

import org.example.config.core.EnvironmentConfig;
import org.example.config.constants.EnvironmentConstants;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.CommandFailedException;
import org.example.utils.ShellUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class SudoPrivilegeManager {
    private static final String shellUser = ShellUtils.resolveAppUser();
    private static final String SUDOERS_DIR = "/etc/sudoers.d/";

    private static final String TEMP_DIR = "/tmp/";
    private static final String SUDOERS_PERMISSIONS = "r--r-----";
    private static final EnvironmentConfig cprovider = new EnvironmentConfig();
    private static final Path SUDOERS_FILE = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser());
    private static final FileAccessPolicy
            sudoersFilePolicy =
            new FileAccessPolicy(SUDOERS_FILE).permissions(SUDOERS_PERMISSIONS)
                    .owner(EnvironmentConstants.SUPERADMIN_USER)
                    .group(EnvironmentConstants.SUPERADMIN_USER);

    public void setupSudoPrivileges() throws CommandFailedException, IOException {
        File sudoersFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser()).toFile();

        if (sudoersFile.isFile() && isFileInsecure()) {
            securePermissions();
        }

        if (isSudoRuleNotPresentInFile()) {
            createSudoersRuleFile(generateSudoRule());
            securePermissions();

            if (isSudoRuleNotPresentInFile()) {
                throw new CommandFailedException("Failed to apply sudo rules correctly!");
            }
        }
    }

    private boolean isFileInsecure() {

        return !sudoersFilePolicy.isSecured();

    }

    private void securePermissions() {
        sudoersFilePolicy.enforce();
    }

    private boolean isSudoRuleNotPresentInFile() {
        Path sudoersFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser());

        try {
            String expectedSudoRule = generateSudoRule().trim();
            String fileContent = Files.readString(sudoersFile).trim();

            boolean missing = !fileContent.equals(expectedSudoRule);
            if (missing) {
                getLogger().warn("Sudoers rule not present or out of sync in " + sudoersFile);
            }

            return missing;

        } catch (IOException e) {
            getLogger().warn("Sudoers file " + sudoersFile + " is not present or unreadable.");
            return true;
        }
    }

    private void createSudoersRuleFile(String sudoRule) throws IOException, CommandFailedException {
        String tempFileName = TEMP_DIR + "sudoers_" + cprovider.getDatabaseUser();
        Path tempFile = Paths.get(tempFileName);

        getLogger().info("Creating temp sudo rule file at " + tempFile);

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            writer.write(sudoRule);
            writer.write("\n");
        }

        getLogger().info("Generated sudo rule: " + sudoRule);
        Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("r--r-----"));
        try {
            getLogger().debug("Validating sudoers syntax with visudo -cf");
            ShellUtils.execute("visudo", "-cf", tempFileName);
        } catch (CommandFailedException e) {
            getLogger().error("Invalid sudoers syntax detected!", e);
            Files.delete(tempFile);
            getLogger().error("Failed temp file removed without moving it to sudoers " + tempFile);
            throw new CommandFailedException("Invalid sudoers syntax detected!");
        }
        getLogger().debug("Soon to be sudoers file is validated " + tempFile);

        Path targetFile = Paths.get(SUDOERS_DIR + cprovider.getDatabaseUser());
        getLogger().debug("Copying sudo rule to final location: " + targetFile);
        ShellUtils.execute("sudo", "cp", tempFileName, targetFile.toString());
        getLogger().debug("Applying 440 permissions to " + targetFile);
        ShellUtils.execute("sudo", "chmod", "440", targetFile.toString());

        Files.delete(tempFile);
        getLogger().debug("Temporary sudo rule file deleted: " + tempFile);
    }

    private String generateSudoRule() {
        String executablePath = ShellUtils.getExecutablePath();
        //For passing parameters via $SSH_ORIGINAL_COMMAND with SSH Forced command
        String defaultsLine = String.format("Defaults:%s env_keep += \"SSH_ORIGINAL_COMMAND\"", shellUser);
        String sudoRuleLine = String.format("%s ALL=(ALL) NOPASSWD: %s", shellUser, executablePath);

        return String.join("\n", defaultsLine, sudoRuleLine);
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }


}

