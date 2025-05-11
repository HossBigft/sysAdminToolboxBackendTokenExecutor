package org.example.commands.core;

import org.example.commands.Operation;
import org.example.commands.OperationRequest;


public interface CommandBuilderFactory {

    Operation<?> build(OperationRequest command);

}
