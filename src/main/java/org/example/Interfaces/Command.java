package org.example.Interfaces;

public interface Command<T> {
    T execute() throws Exception;
}
