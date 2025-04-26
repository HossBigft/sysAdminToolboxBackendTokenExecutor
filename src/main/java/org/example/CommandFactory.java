package org.example;

import org.example.Interfaces.Command;


public interface CommandFactory {

    Command<?> build(CommandInput command);

}
