package org.example.Utils;

import org.example.Exceptions.CommandFailedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ShellUtils {
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";

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
            Process process = new ProcessBuilder(args).start();


            List<String> outputLines = new ArrayList<String>();
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
                    String errorOutput = errorBuilder.toString().trim();
                    throw new CommandFailedException(
                            String.format("Command failed with exit code %d: %s\nCommand: %s",
                                    exitCode,
                                    errorOutput,
                                    String.join(" ",
                                            args))
                    );
                }

                return Collections.unmodifiableList(outputLines);
            }
        } catch (IOException e) {
            throw new CommandFailedException("Failed to execute command: " +
                    String.join(" ",
                            args),
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandFailedException("Command execution was interrupted: " +
                    String.join(" ",
                            args),
                    e);
        }
    }

    public static String resolveUser() {
        return Stream.of(getSudoUser(), getSystemUser(), getUserFromPath()).flatMap(Optional::stream)
                .filter(ShellUtils::isValidUser).findFirst().orElseThrow(
                        () -> new IllegalStateException("Could not determine valid user for running executable."));
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

    private static boolean isValidUser(String user) {
        return !"root".equals(user);
    }

    public static void setEnvPermissionsOwner(File envFile) throws IOException {
        Path filePath = envFile.toPath();
        Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString(DOTENV_PERMISSIONS));
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(DOTENV_OWNER);
        GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(DOTENV_GROUP);

        Files.setAttribute(filePath, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
        Files.setAttribute(filePath, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
    }

    public static boolean isEnvPermissionsSecure(File envFile) throws IOException {
        Path envPath = envFile.toPath();
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(envPath);

        String permString = PosixFilePermissions.toString(permissions);
        boolean permsOk = DOTENV_PERMISSIONS.equals(permString);

        UserPrincipal owner = Files.getOwner(envPath);
        boolean ownerOk = DOTENV_OWNER.equals(owner.getName());

        PosixFileAttributes attrs = Files.readAttributes(envPath, PosixFileAttributes.class);
        boolean groupOk = DOTENV_GROUP.equals(attrs.group().getName());

        return permsOk && ownerOk && groupOk;

    }
}