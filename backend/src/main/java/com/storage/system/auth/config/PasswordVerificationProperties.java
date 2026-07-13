package com.storage.system.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.password-verification")
public class PasswordVerificationProperties {

    private int codeLength = 6;

    private long ttlMinutes = 10;

    private long sendCooldownSeconds = 60;

    private int maxVerifyFailures = 5;

    private long verifyFailureWindowMinutes = 15;
}
