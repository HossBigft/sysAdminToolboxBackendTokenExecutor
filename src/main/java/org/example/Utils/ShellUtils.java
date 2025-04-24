package org.example.Utils;

import org.example.Config.SudoersManager;
import org.example.Exceptions.CommandFailedException;
import org.example.Logging.facade.LogManager;
import org.example.Logging.implementations.DefaultCliLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ShellUtils {
    private static final DefaultCliLogger logger = LogManager.getLogger();
    private ShellUtils() {
    }

    public static String getSqlCliName() throws CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            throw new CommandFailedException(
                    "Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    private static boolean isCommandAvailable(String cmd) {
        return new File("/usr/bin/" + cmd).exists() || new File("/usr/local/bin/" + cmd).exists();
    }

    public static List<String> runCommand(String... args) throws CommandFailedException {
        try {
            logger.debugEntry().command(args).log();
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

                boolean
                        completed =
                        process.waitFor(30,
                                TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new CommandFailedException("Command execution timed out: " + String.join(" ",
                            args));
                }


                CompletableFuture.allOf(outputFuture,
                        errorFuture).join();

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorMessage = String.format("Command '%s' failed with exit code %d: %s",
                            String.join(" ", args), exitCode, String.join("\n", outputLines));
                    LogManager.error(errorMessage);
                    throw new CommandFailedException(errorMessage);
                }

                return Collections.unmodifiableList(outputLines);
            }
        } catch (IOException e) {
            String errorMessage = String.format("Failed to execute command '%s': %s",
                    String.join(" ", args), e.getMessage());
            LogManager.error(errorMessage);
            throw new CommandFailedException(errorMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMessage = String.format("Failed to execute command '%s': %s",
                    String.join(" ", args), e.getMessage());
            LogManager.error(errorMessage);
            throw new CommandFailedException(errorMessage);
        }
    }

    public static String resolveToolBoxUser() {
        return Stream.of(getSudoUser(), getSystemUser(), getUserFromPath()).flatMap(Optional::stream)
                .filter(ShellUtils::isValidUser).findFirst().orElseThrow(
                        () -> new IllegalStateException("Could not determine valid user for toolbox."));
    }

    public static Optional<String> getSudoUser() {
        return Optional.ofNullable(System.getenv("SUDO_USER"));
    }

    public static Optional<String> getSystemUser() {
        String systemUser = System.getProperty("user.name");
        return Optional.ofNullable(systemUser);
    }

    public static Optional<String> getUserFromPath() {
        String cwd = System.getProperty("user.dir");
        if (cwd == null) return Optional.empty();

        Path path = Paths.get(cwd).toAbsolutePath();
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            if ("home".equals(path.getName(i).toString())) {
                String username = path.getName(i + 1).toString();
                return Optional.of(username);
            }
        }
        return Optional.empty();
    }

    public static boolean isValidUser(String user) {
        return !"root".equals(user);
    }

    public static String resolveShellUser() {
        return Stream.of(getSudoUser(), getSystemUser())
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Could not determine valid user for running executable."));
    }

    public static Path getExecutablePath() throws URISyntaxException {
        return Paths.get(SudoersManager.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
    }
}