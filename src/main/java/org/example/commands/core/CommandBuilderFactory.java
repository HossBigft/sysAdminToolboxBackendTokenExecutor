package org.example.commands.core;

import org.example.commands.Operation;
import org.example.commands.CommandRequest;


public interface CommandBuilderFactory {

    Operation<?> build(CommandRequest command);

}
