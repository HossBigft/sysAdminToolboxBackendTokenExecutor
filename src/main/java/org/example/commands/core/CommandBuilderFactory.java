package org.example.commands.core;

import org.example.commands.Command;
import org.example.commands.CommandRequest;


public interface CommandBuilderFactory {

    Command<?> build(CommandRequest command);

}
