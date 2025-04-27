package org.example.commands;

public interface Command<T> {
    T execute() throws Exception;
}
