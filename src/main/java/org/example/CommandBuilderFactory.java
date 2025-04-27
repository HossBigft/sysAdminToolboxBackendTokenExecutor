package org.example;

import org.example.Interfaces.Command;


public interface CommandBuilderFactory {

    Command<?> build(CommandRequest command);

}
