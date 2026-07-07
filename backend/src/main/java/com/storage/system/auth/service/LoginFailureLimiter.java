package com.storage.system.auth.service;

public interface LoginFailureLimiter {

    void assertAllowed(String username);

    void recordFailure(String username);

    void reset(String username);
}
