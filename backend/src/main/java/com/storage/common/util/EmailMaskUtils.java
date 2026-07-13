package com.storage.common.util;

import org.springframework.util.StringUtils;

public final class EmailMaskUtils {

    private EmailMaskUtils() {
    }

    public static String mask(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String trimmed = email.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return trimmed;
        }
        String local = trimmed.substring(0, atIndex);
        String domain = trimmed.substring(atIndex);
        if (local.length() <= 1) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }
}
