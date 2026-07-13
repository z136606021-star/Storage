package com.storage.common.util;

import com.storage.common.exception.BusinessException;
import org.springframework.util.StringUtils;

public final class IdentityTextValidation {

    private IdentityTextValidation() {
    }

    public static void requireNoWhitespace(String value, String fieldLabel) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (containsWhitespace(value)) {
            throw new BusinessException(fieldLabel + "不能包含空格或空白字符");
        }
    }

    public static boolean containsWhitespace(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.codePoints().anyMatch(Character::isWhitespace);
    }
}
