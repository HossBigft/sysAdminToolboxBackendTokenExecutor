package org.example.commands;


import java.util.Arrays;

public record OperationRequest(AvailableOperation commandName, String[] commandArgs) {

    @Override
    public String toString() {
        return commandName + " args:" + Arrays.toString(commandArgs);
    }
}

