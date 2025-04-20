package org.example.Interfaces;

public interface Command <T>{
    public T  execute() throws Exception;
}
