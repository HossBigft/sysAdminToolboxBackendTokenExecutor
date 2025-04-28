package org.example.commands.core;

import org.example.commands.CommandRequest;
import org.example.commands.Command;


public interface CommandBuilderFactory {

    Command<?> build(CommandRequest command);

}
