package com.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "storage.jwt")
public class JwtProperties {

    private String secret = "dev-only-change-this-jwt-secret-at-least-32-bytes";

    private long ttlMinutes = 120;
}
