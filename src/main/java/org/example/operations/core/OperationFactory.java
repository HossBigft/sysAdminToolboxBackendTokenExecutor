package org.example.operations.core;

import org.example.operations.Operation;
import org.example.operations.OperationRequest;


public interface OperationFactory {

    Operation<?> build(OperationRequest command);

}
