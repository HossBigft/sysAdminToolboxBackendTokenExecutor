package org.example;

import org.example.Interfaces.Command;


public interface CommandFactory {

    Command<?> build(String... args);

}
