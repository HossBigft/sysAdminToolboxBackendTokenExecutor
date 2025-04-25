package org.example.config;

public record FileAccessPolicy(String permissions, String owner, String group) {
    public FileAccessPolicy {
        if (permissions == null || !permissions.matches("[r-][w-][x-]{1}[r-][w-][x-]{1}[r-][w-][x-]{1}")) {
            throw new IllegalArgumentException("Invalid permissions format: " + permissions);
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner must not be null or blank.");
        }
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Group must not be null or blank.");
        }
    }
}
