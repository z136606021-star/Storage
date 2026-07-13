package com.storage.common.mapper;

import org.mapstruct.Named;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class StringMapping {

    private StringMapping() {
    }

    @Named("trim")
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("trimToNull")
    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Named("trimToEmpty")
    public static String trimToEmpty(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    @Named("trimToNullLowercase")
    public static String trimToNullLowercase(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
