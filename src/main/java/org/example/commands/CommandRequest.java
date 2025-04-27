package org.example.commands;


import org.example.core.AvailableCommand;

public record CommandRequest(AvailableCommand commandName, String[] commandArgs) {


}

