package org.example.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.util.Optional;


public record OperationResult(ExecutionStatus status, String message, Optional<JsonNode> payload) {
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static OperationResult success(Optional<JsonNode> result) {
        return new OperationResult(ExecutionStatus.OK, "Operation completed successfully", result);
    }

    public static OperationResult success() {
        return new OperationResult(ExecutionStatus.OK, "Operation completed successfully", Optional.empty());
    }

    public static OperationResult success(String message) {
        return new OperationResult(ExecutionStatus.OK, message, Optional.empty());
    }

    public static OperationResult success(String message,
                                          Optional<JsonNode> result) {
        return new OperationResult(ExecutionStatus.OK, message, result);
    }

    public static OperationResult successCreated(String message,
                                                 Optional<JsonNode> result) {
        return new OperationResult(ExecutionStatus.CREATED, message, result);
    }

    public static OperationResult failure(ExecutionStatus statusCode,
                                          String message) {
        return new OperationResult(statusCode, message, Optional.empty());
    }

    public static OperationResult internalError(String message) {
        return new OperationResult(ExecutionStatus.INTERNAL_ERROR, message, Optional.empty());
    }

    public static OperationResult internalError() {
        return new OperationResult(ExecutionStatus.INTERNAL_ERROR, "Operation execution failed.", Optional.empty());
    }

    public static OperationResult notFound(String message) {
        return new OperationResult(ExecutionStatus.NOT_FOUND, message, Optional.empty());
    }

    public static OperationResult failure(ExecutionStatus statusCode,
                                          String message,
                                          Optional<JsonNode> partialResult) {
        return new OperationResult(statusCode, message, partialResult);
    }

    public static OperationResult failure(String message) {
        return new OperationResult(ExecutionStatus.INTERNAL_ERROR, message, Optional.empty());
    }

    public int getStatusCode() {
        return status.getCode();
    }

    public String getMessage() {
        return message;
    }

    public Optional<JsonNode> getPayload() {
        return payload;
    }

    public String toPrettyJson() {
        try {
            return MAPPER.writeValueAsString(this.toJsonNode());
        } catch (JsonProcessingException e) {
            getLogger().errorEntry().message("Error serializing OperationResult to JSON").exception(e).log();

            return String.format("""
                            {
                              "status": "%s",
                              "code": %d,
                              "message": "Serialization failure: %s",
                              "payload": ""
                            }
                            """, ExecutionStatus.INTERNAL_ERROR.getText(), ExecutionStatus.INTERNAL_ERROR.getCode(),
                    e.getMessage());
        }
    }

    public JsonNode toJsonNode() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("status", status.name());
        root.put("code", status.getCode());
        root.put("message", message);

        if (payload.isPresent()) {
            root.set("payload", MAPPER.valueToTree(payload.get()));
        } else {
            root.set("payload", MAPPER.nullNode());
        }
        return root;
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public enum ExecutionStatus {
        OK(200, "OK"), CREATED(201, "Created"), BAD_REQUEST(400, "Bad Request"), UNAUTHORIZED(401,
                "Unauthorized"), UNPROCCESIBLE_ENTITY(422, "Unprocessable Entity"), NOT_FOUND(404,
                "Not Found"), INTERNAL_ERROR(500, "Internal Server Error");


        private final int code;
        private final String text;

        ExecutionStatus(int code,
                        String text) {
            this.code = code;
            this.text = text;
        }

        public int getCode() {
            return code;
        }

        public String getText() {
            return text;
        }
    }
}
