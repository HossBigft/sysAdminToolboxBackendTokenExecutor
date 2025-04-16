package org.example;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Utils {
    static String generatePassword(int length) {

        PasswordGenerator generator = new PasswordGenerator();

        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase, 1);
        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase, 1);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, 1);

        CharacterData safeSpecials = new CharacterData() {
            public String getErrorCode() {
                return "SHELL_QUOTE_CHARS_PROHIBITED";
            }

            public String getCharacters() {
                return "!#$%&()*+,-./:;<=>?@[\\]^_{|}~";
            }
        };
        CharacterRule specialRule = new CharacterRule(safeSpecials, 1);

        List<CharacterRule> rules = Arrays.asList(lowerCaseRule, upperCaseRule, digitRule, specialRule);

        return generator.generatePassword(length, rules);
    }

    static List<String> runCommand(String... args) throws CommandFailedException {
        try {
            Process process = new ProcessBuilder(args).start();


            List<String> outputLines = new ArrayList<>();
            StringBuilder errorBuilder = new StringBuilder();

            try (
                    BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            ) {
                CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(() ->
                        stdOutput.lines().forEach(outputLines::add));

                CompletableFuture<Void> errorFuture = CompletableFuture.runAsync(() ->
                        stdError.lines().forEach(line -> errorBuilder.append(line).append("\n")));

                boolean completed = process.waitFor(30, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new CommandFailedException("Command execution timed out: " + String.join(" ", args));
                }


                CompletableFuture.allOf(outputFuture, errorFuture).join();

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorOutput = errorBuilder.toString().trim();
                    throw new CommandFailedException(
                            String.format("Command failed with exit code %d: %s\nCommand: %s",
                                    exitCode, errorOutput, String.join(" ", args))
                    );
                }

                return Collections.unmodifiableList(outputLines);
            }
        } catch (IOException e) {
            throw new CommandFailedException("Failed to execute command: " + String.join(" ", args), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new CommandFailedException("Command execution was interrupted: " + String.join(" ", args), e);
        }
    }

    static String getSqlCliName() throws CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            throw new CommandFailedException("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    private static boolean isCommandAvailable(String command) {
        try {
            runCommand("which", command);
        } catch (CommandFailedException e) {
            return false;
        }
        return true;
    }

    static class CommandFailedException extends Exception {
        public CommandFailedException(String message) {
            super(message);
        }

        public CommandFailedException(String message,
                                      Throwable cause) {
            super(message, cause);
        }
    }
}