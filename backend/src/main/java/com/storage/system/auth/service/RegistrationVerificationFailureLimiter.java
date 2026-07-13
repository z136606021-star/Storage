package com.storage.system.auth.service;

public interface RegistrationVerificationFailureLimiter {

    void assertAllowedVerify(String email);

    void recordVerifyFailure(String email);

    void resetVerifyFailures(String email);
}
