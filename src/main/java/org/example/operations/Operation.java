package org.example.operations;

import java.util.Optional;

public interface Operation<T> {
    Optional<T> execute() throws Exception;
}
