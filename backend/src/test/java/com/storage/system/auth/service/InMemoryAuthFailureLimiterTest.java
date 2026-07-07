package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAuthFailureLimiterTest {

    @Test
    void loginLimiter_blocksAfterMaxFailuresAndCanReset() {
        InMemoryAuthFailureLimiter limiter = new InMemoryAuthFailureLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("Admin");
        }

        assertThatThrownBy(() -> limiter.assertAllowed("admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");

        limiter.reset("ADMIN");

        assertThatCode(() -> limiter.assertAllowed("admin")).doesNotThrowAnyException();
    }

    @Test
    void forgotPasswordLimiter_usesIndependentFailureBucket() {
        InMemoryAuthFailureLimiter limiter = new InMemoryAuthFailureLimiter();

        for (int i = 0; i < 5; i++) {
            limiter.recordForgotPasswordFailure("user");
        }

        assertThatCode(() -> limiter.assertAllowed("user")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.assertAllowedForgotPassword("USER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码找回尝试次数过多");
    }
}
