package com.storage.common.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringMappingTest {

    @Test
    void trimToNullLowercase_returnsNullForBlank() {
        assertThat(StringMapping.trimToNullLowercase(null)).isNull();
        assertThat(StringMapping.trimToNullLowercase("")).isNull();
        assertThat(StringMapping.trimToNullLowercase("   ")).isNull();
    }

    @Test
    void trimToNullLowercase_trimsAndLowercasesWithRootLocale() {
        assertThat(StringMapping.trimToNullLowercase("  Bo_Lv@Jabil.COM  "))
                .isEqualTo("bo_lv@jabil.com");
        assertThat(StringMapping.trimToNullLowercase("User@Example.com"))
                .isEqualTo("user@example.com");
    }
}
