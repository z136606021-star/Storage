package com.storage.system.auth.service;

public interface RegistrationVerificationMailService {

    void sendVerificationCode(String email, String rawCode);
}
