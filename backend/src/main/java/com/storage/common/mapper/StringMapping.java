package com.storage.common.mapper;

import org.mapstruct.Named;
import org.springframework.util.StringUtils;

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
}
