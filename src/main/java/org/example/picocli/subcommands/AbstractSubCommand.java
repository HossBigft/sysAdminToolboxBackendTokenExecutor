package org.example.picocli.subcommands;

import org.example.main;

import java.util.concurrent.Callable;

public abstract class AbstractSubCommand implements Callable<Integer> {
    protected final main parent;

    public AbstractSubCommand(main parent) {
        this.parent = parent;
    }


    /**
     * Print error message to standard error and return failure code.
     *
     * @param message The error message to print.
     * @return Always returns 1 (error code).
     */
    protected Integer error(String message) {
        System.err.println(message);
        return 1;
    }

    /**
     * Print success message to standard output and return success code.
     *
     * @param message The success message to print.
     * @return Always returns 0 (success code).
     */
    protected Integer success(String message) {
        System.out.println(message);
        return 0;
    }
}