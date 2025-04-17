package org.example.Utils;

import org.example.Exceptions.CommandFailedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ShellUtils {

    private ShellUtils() {
    }

    private static String getSqlCliName() throws CommandFailedException {
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

}