package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.ShellUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class SudoersManager {
    static final String shellUser = ShellUtils.resolveUser();
    private final String SUDOERS_DIR = "/etc/sudoers.d/";
    private final String TEMP_DIR = "/tmp/";
    private final Set<PosixFilePermission> SUDOERS_PERMISSIONS = PosixFilePermissions.fromString("r--r-----");

    public void ensureSudoersRuleIsPresent() throws CommandFailedException, IOException, URISyntaxException {
        Path targetFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());

        // Check if file exists but has incorrect permissions
        if (Files.exists(targetFile) && !validateFilePermissions(targetFile)) {
            System.out.println("Warning: Existing sudoers file with incorrect permissions detected!");
            ShellUtils.runCommand("sudo", "rm", targetFile.toString());
        }

        if (!isSudoRuleNotPresent()) {
            createSudoersRuleFile(generateSudoRule());


            if (!isSudoRuleNotPresent()) {
                throw new CommandFailedException("Failed to apply sudo rules correctly!");
            }
        }
    }

    private boolean validateFilePermissions(Path file) throws CommandFailedException {
        try {
            if (!Files.exists(file)) {
                return false;
            }


            String ownerInfo = ShellUtils.runCommand("stat", "-c", "%U:%G", file.toString()).toString();
            if (!"root:root".equals(ownerInfo.trim())) {
                return false;
            }

            String permissions = ShellUtils.runCommand("stat", "-c", "%a", file.toString()).toString();
            return "440".equals(permissions.trim());
        } catch (CommandFailedException e) {
            return false;
        }
    }

    private void createSudoersRuleFile(String sudoRule) throws IOException, CommandFailedException {
        String tempFileName = TEMP_DIR + "sudoers_" + ConfigManager.getDatabaseUser();
        Path tempFile = Paths.get(tempFileName);

        Files.writeString(tempFile, sudoRule);


        Files.setPosixFilePermissions(tempFile, PosixFilePermissions.fromString("r--r-----"));
        try {
            ShellUtils.runCommand("visudo", "-cf", tempFileName);
        } catch (CommandFailedException e) {
            Files.delete(tempFile);
            throw new CommandFailedException("Invalid sudoers syntax detected!");
        }

        Path targetFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());
        ShellUtils.runCommand("sudo", "cp", tempFileName, targetFile.toString());
        ShellUtils.runCommand("sudo", "chmod", "440", targetFile.toString());

        Files.delete(tempFile);
    }

    private String generateSudoRule() throws URISyntaxException {
        return shellUser + " ALL=(ALL) NOPASSWD: " + getExecutablePath() + " *";
    }

    private void printRelevantRules() throws CommandFailedException {
        ShellUtils.runCommand("cat", "/etc/sudoers").stream().filter(l -> l.contains(shellUser))
                .forEach(System.out::println);
    }

    private Path getExecutablePath() throws URISyntaxException {
        return Paths.get(SudoersManager.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }

    private boolean isSudoRuleNotPresent() throws CommandFailedException {

        Path sudoersFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());
        try {
            return Files.readAllLines(sudoersFile).stream().noneMatch(l -> {
                try {
                    return l.contains(generateSudoRule());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            return true;
        }
    }


}

