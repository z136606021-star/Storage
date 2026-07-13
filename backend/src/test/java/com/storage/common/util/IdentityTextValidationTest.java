package com.storage.common.util;

import com.storage.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTextValidationTest {

    @Test
    void requireNoWhitespace_allowsPlainText() {
        IdentityTextValidation.requireNoWhitespace("abc123", "账号");
    }

    @Test
    void requireNoWhitespace_rejectsInternalSpace() {
        assertThatThrownBy(() -> IdentityTextValidation.requireNoWhitespace("123 asd", "账号"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号不能包含空格或空白字符");
    }

    @Test
    void containsWhitespace_detectsTab() {
        assertThat(IdentityTextValidation.containsWhitespace("abc\tdef")).isTrue();
        assertThat(IdentityTextValidation.containsWhitespace("abcdef")).isFalse();
    }
}
