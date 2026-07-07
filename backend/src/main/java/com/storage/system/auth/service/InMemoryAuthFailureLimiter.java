package com.storage.system.auth.service;

import com.storage.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class InMemoryAuthFailureLimiter implements LoginFailureLimiter, ForgotPasswordFailureLimiter {

    private static final int FORGOT_PASSWORD_MAX_FAILURES = 5;
    private static final Duration FORGOT_PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int LOGIN_MAX_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentMap<String, Failures> forgotPasswordFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Failures> loginFailures = new ConcurrentHashMap<>();

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
    public void assertAllowedForgotPassword(String username) {
        assertAllowed(
                forgotPasswordFailures,
                normalize(username),
                FORGOT_PASSWORD_MAX_FAILURES,
                "密码找回尝试次数过多，请 15 分钟后再试"
        );
    }

    @Override
    public void recordForgotPasswordFailure(String username) {
        recordFailure(forgotPasswordFailures, normalize(username), FORGOT_PASSWORD_FAILURE_WINDOW);
    }

    @Override
    public void resetForgotPassword(String username) {
        forgotPasswordFailures.remove(normalize(username));
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
