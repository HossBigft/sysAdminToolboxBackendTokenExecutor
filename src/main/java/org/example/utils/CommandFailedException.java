package org.example.utils;

import java.util.List;

public class CommandFailedException extends Exception {
    private List<String> stdout = List.of();
    private List<String> stderr = List.of();
    private int returnCode = -1;

    public CommandFailedException(String message) {
        super(message);
    }

    public CommandFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandFailedException(Throwable cause, List<String> stdout, List<String> stderr, int exitCode) {
        this("Shell command completed with non-zero exit code [" +
                exitCode +
                "] with error:" +
                String.join("\n", stderr), cause, stdout, stderr, exitCode);
    }

    public CommandFailedException(String message,
                                  Throwable cause,
                                  List<String> stdout,
                                  List<String> stderr,
                                  int returnCode) {
        super(message, cause);
        this.stdout = stdout;
        this.stderr = stderr;
        this.returnCode = returnCode;
    }

    public CommandFailedException(List<String> stdout, List<String> stderr, int exitCode) {
        this("Shell command completed with non-zero exit code [" +
                exitCode +
                "] with error:" +
                String.join("\n", stderr), stdout, stderr, exitCode);
    }

    public CommandFailedException(String message, List<String> stdout, List<String> stderr, int returnCode) {
        super(message);
        this.stdout = stdout;
        this.stderr = stderr;
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public List<String> getStderr() {
        return stderr;
    }

    public List<String> getStdout() {
        return stdout;
    }


}
