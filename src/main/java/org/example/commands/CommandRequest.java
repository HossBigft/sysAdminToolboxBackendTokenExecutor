package org.example.commands;


import org.example.commands.core.AvailableCommand;

import java.util.Arrays;

public record CommandRequest(AvailableCommand commandName, String[] commandArgs) {

    @Override
    public String toString() {
        return commandName + " args:" + Arrays.toString(commandArgs);
    }
}

