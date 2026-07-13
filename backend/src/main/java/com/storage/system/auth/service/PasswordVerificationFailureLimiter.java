package com.storage.system.auth.service;

public interface PasswordVerificationFailureLimiter {

    void assertAllowedVerify(Long userId);

    void recordVerifyFailure(Long userId);

    void resetVerifyFailures(Long userId);
}
