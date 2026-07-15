package com.storage.common.web;

import java.util.UUID;

public final class RequestIdGenerator {

    private static final int MAX_LENGTH = 64;

    private RequestIdGenerator() {
    }

    public static String normalize(String candidate) {
        if (candidate == null) {
            return generate();
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return generate();
        }
        String sanitized = trimmed.replaceAll("[^A-Za-z0-9._-]", "");
        if (sanitized.isEmpty()) {
            return generate();
        }
        return sanitized.length() > MAX_LENGTH ? sanitized.substring(0, MAX_LENGTH) : sanitized;
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
