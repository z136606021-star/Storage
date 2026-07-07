package com.storage.system.auth.service;

public interface ForgotPasswordFailureLimiter {

    void assertAllowedForgotPassword(String username);

    void recordForgotPasswordFailure(String username);

    void resetForgotPassword(String username);
}
