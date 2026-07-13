package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class InMemoryAuthFailureLimiter implements LoginFailureLimiter, ForgotPasswordFailureLimiter,
        PasswordVerificationFailureLimiter, RegistrationVerificationFailureLimiter {

    private static final int FORGOT_PASSWORD_MAX_FAILURES = 5;
    private static final Duration FORGOT_PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int LOGIN_MAX_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int VERIFY_PASSWORD_MAX_FAILURES = 5;
    private static final Duration VERIFY_PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentMap<String, Failures> forgotPasswordFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Failures> loginFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Failures> passwordVerificationFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Failures> registrationVerificationFailures = new ConcurrentHashMap<>();

    @Override
    public void assertAllowed(String username) {
        assertAllowed(loginFailures, normalize(username), LOGIN_MAX_FAILURES, "登录失败次数过多，请 15 分钟后再试");
    }

    @Override
    public void recordFailure(String username) {
        recordFailure(loginFailures, normalize(username), LOGIN_FAILURE_WINDOW);
    }

    @Override
    public void reset(String username) {
        loginFailures.remove(normalize(username));
    }

    @Override
    public void assertAllowedForgotPassword(String email) {
        assertAllowed(
                forgotPasswordFailures,
                normalize(email),
                FORGOT_PASSWORD_MAX_FAILURES,
                "密码找回尝试次数过多，请 15 分钟后再试"
        );
    }

    @Override
    public void recordForgotPasswordFailure(String email) {
        recordFailure(forgotPasswordFailures, normalize(email), FORGOT_PASSWORD_FAILURE_WINDOW);
    }

    @Override
    public void resetForgotPassword(String email) {
        forgotPasswordFailures.remove(normalize(email));
    }

    @Override
    public void assertAllowedVerify(Long userId) {
        assertAllowed(
                passwordVerificationFailures,
                String.valueOf(userId),
                VERIFY_PASSWORD_MAX_FAILURES,
                "验证码尝试次数过多，请 15 分钟后再试"
        );
    }

    @Override
    public void recordVerifyFailure(Long userId) {
        recordFailure(passwordVerificationFailures, String.valueOf(userId), VERIFY_PASSWORD_FAILURE_WINDOW);
    }

    @Override
    public void resetVerifyFailures(Long userId) {
        passwordVerificationFailures.remove(String.valueOf(userId));
    }

    @Override
    public void assertAllowedVerify(String email) {
        assertAllowed(
                registrationVerificationFailures,
                normalize(email),
                VERIFY_PASSWORD_MAX_FAILURES,
                "验证码尝试次数过多，请 15 分钟后再试"
        );
    }

    @Override
    public void recordVerifyFailure(String email) {
        recordFailure(registrationVerificationFailures, normalize(email), VERIFY_PASSWORD_FAILURE_WINDOW);
    }

    @Override
    public void resetVerifyFailures(String email) {
        registrationVerificationFailures.remove(normalize(email));
    }

    private void assertAllowed(
            ConcurrentMap<String, Failures> failuresByKey,
            String key,
            int maxFailures,
            String message
    ) {
        Failures failures = failuresByKey.get(key);
        if (failures == null || failures.isExpired()) {
            return;
        }
        if (failures.count() >= maxFailures) {
            throw new BusinessException(message);
        }
    }

    private void recordFailure(ConcurrentMap<String, Failures> failuresByKey, String key, Duration window) {
        failuresByKey.compute(key, (ignored, failures) -> {
            if (failures == null || failures.isExpired(window)) {
                return new Failures(1, Instant.now(), window);
            }
            return new Failures(failures.count() + 1, failures.firstFailedAt(), window);
        });
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private record Failures(int count, Instant firstFailedAt, Duration window) {
        boolean isExpired() {
            return isExpired(window);
        }

        boolean isExpired(Duration overrideWindow) {
            return firstFailedAt.plus(overrideWindow).isBefore(Instant.now());
        }
    }
}
