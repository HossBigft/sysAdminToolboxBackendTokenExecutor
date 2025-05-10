package org.example.utils;

import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ShellUtils {
    private ShellUtils() {
    }

    public static String getSqlCliName() throws CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            throw new CommandFailedException("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    private static boolean isCommandAvailable(String cmd) {
        return new File("/usr/bin/" + cmd).exists() || new File("/usr/local/bin/" + cmd).exists();
    }

    public static List<String> runCommand(String... args) throws CommandFailedException {
        try {
            getLogger().debugEntry().command(args).log();
            Process process = new ProcessBuilder(args).start();


            List<String> outputLines = new ArrayList<>();
            StringBuilder errorBuilder = new StringBuilder();

            try (BufferedReader stdOutput = new BufferedReader(
                    new InputStreamReader(process.getInputStream())); BufferedReader stdError = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(
                        () -> stdOutput.lines().forEach(outputLines::add));

                CompletableFuture<Void> errorFuture = CompletableFuture.runAsync(
                        () -> stdError.lines().forEach(line -> errorBuilder.append(line).append("\n")));

                boolean completed = process.waitFor(30, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new CommandFailedException("Command execution timed out: " + String.join(" ", args));
                }


                CompletableFuture.allOf(outputFuture, errorFuture).join();

                int exitCode = process.exitValue();
                if (exitCode != 0) {

                    String errorMessage = String.format("Command '%s' failed with exit code %d: %s\n Stderr: %s",
                            String.join(" ", args), exitCode, String.join("\n", outputLines), errorBuilder);
                    getLogger().errorEntry().message("Command failed").field("command", String.join(" ", args))
                            .field("exitCode", exitCode).field("stdout", String.join("\n", outputLines))
                            .field("stderr", errorBuilder.toString()).log();

                    throw new CommandFailedException(errorMessage);
                }

                return outputLines;
            }
        } catch (IOException e) {
            String errorMessage = String.format("Failed to execute command '%s': %s", String.join(" ", args),
                    e.getMessage());
            getLogger().error(errorMessage);
            throw new CommandFailedException(errorMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = String.format("Failed to execute command '%s': %s", String.join(" ", args),
                    e.getMessage());
            getLogger().error(errorMessage);
            throw new CommandFailedException(errorMessage);
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public static String resolveAppUser() {

        Optional<String> sudoUser = getSudoUser();
        Optional<String> systemUser = getSystemUser();
        Optional<String> pathUser = getUserFromPath();

        return Stream.of(sudoUser, systemUser, pathUser).flatMap(Optional::stream).filter(ShellUtils::isValidUser)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine valid system user for app."));
    }

    public static Optional<String> getSudoUser() {
        return Optional.ofNullable(System.getenv("SUDO_USER"));
    }

    public static Optional<String> getSystemUser() {
        String systemUser = System.getProperty("user.name");
        return Optional.ofNullable(systemUser);
    }

    public static Optional<String> getUserFromPath() {
        try {
            String path = ProcessHandle.current().info().command().orElse("");
            Pattern pattern = Pattern.compile("/home/([^/]+)/");
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean isValidUser(String user) {
        return !"root".equals(user);
    }

    public static String getExecutablePath() {
        return ProcessHandle.current().info().command().orElse("");
    }

    public static String resolveShellUser() {
        return Stream.of(getSudoUser(), getSystemUser()).flatMap(Optional::stream).findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not determine valid user for running executable."));
    }

}