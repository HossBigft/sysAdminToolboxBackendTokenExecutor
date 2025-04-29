package org.example.commands;

import java.util.Optional;

public interface Command <T> {
     Optional<T> execute() throws Exception;
}
