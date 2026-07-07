package com.storage.system.auth.dto;

import java.time.Instant;

public record JwtClaims(Long userId, String jti, Instant expiresAt) {
}
