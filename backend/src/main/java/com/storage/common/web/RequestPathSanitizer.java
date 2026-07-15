package com.storage.common.web;

import org.springframework.util.StringUtils;

public final class RequestPathSanitizer {

    private static final int MAX_LENGTH = 512;

    private RequestPathSanitizer() {
    }

    public static String sanitize(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String path = rawPath.trim();
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) {
            path = path.substring(0, fragmentIndex);
        }
        if (path.length() > MAX_LENGTH) {
            return path.substring(0, MAX_LENGTH);
        }
        return path;
    }
}
