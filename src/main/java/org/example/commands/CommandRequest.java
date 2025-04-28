package org.example.commands;


import org.example.commands.core.AvailableCommand;

public record CommandRequest(AvailableCommand commandName, String[] commandArgs) {


}

