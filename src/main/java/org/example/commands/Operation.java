package org.example.commands;

import java.util.Optional;

public interface Operation<T> {
    Optional<T> execute() throws Exception;
}
