package com.storage.system.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.password-reset")
public class PasswordResetProperties {

    private String publicBaseUrl = "http://localhost:5173";

    private long tokenTtlMinutes = 30;
}
