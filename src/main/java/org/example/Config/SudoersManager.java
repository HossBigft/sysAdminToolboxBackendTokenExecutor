package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.ShellUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

public class SudoersManager {
    static final String shellUser = ShellUtils.resolveUser();
    private final String SUDOERS_DIR = "/etc/sudoers.d/";
    private final String TEMP_DIR = "/tmp/";
    private final String SUDOERS_PERMISSIONS = "r--r-----";

    public void ensureSudoersRuleIsPresent() throws CommandFailedException, IOException, URISyntaxException {
        Path sudoersFile = Paths.get(SUDOERS_DIR + ConfigManager.getDatabaseUser());

        if (Files.exists(sudoersFile) && isPermissionsInsecure(sudoersFile)) {
            System.out.println("Warning: Existing sudoers file with incorrect permissions detected!");
            securePermissions(sudoersFile);
        }

        if (isSudoRuleNotPresent()) {
            createSudoersRuleFile(generateSudoRule());
            securePermissions(sudoersFile);

            if (isSudoRuleNotPresent()) {
                throw new CommandFailedException("Failed to apply sudo rules correctly!");
            }
            printRelevantRules();
        }
    }

    private boolean isPermissionsInsecure(Path file) throws IOException {
        return !ShellUtils.hasCorrectPermissions(file, SUDOERS_PERMISSIONS)
                || !ShellUtils.hasCorrectOwner(file, "root")
                || !ShellUtils.hasCorrectGroup(file, "root");

    }

    private void securePermissions(Path file) throws IOException {

        ShellUtils.setPermissions(file, SUDOERS_PERMISSIONS);
        ShellUtils.setOwner(file, "root");
        ShellUtils.setGroup(file, "root");
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
        return shellUser + " ALL=(ALL) NOPASSWD: " + ShellUtils.getExecutablePath() + " *";
    }

    private void printRelevantRules() throws CommandFailedException {
        ShellUtils.runCommand("cat", "/etc/sudoers").stream().filter(l -> l.contains(shellUser))
                .forEach(System.out::println);
    }


}

