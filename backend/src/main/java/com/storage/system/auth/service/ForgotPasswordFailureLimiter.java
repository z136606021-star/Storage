package com.storage.system.auth.service;

public interface ForgotPasswordFailureLimiter {

    void assertAllowedForgotPassword(String email);

    void recordForgotPasswordFailure(String email);

    void resetForgotPassword(String email);
}
