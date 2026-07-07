package com.storage.system.auth.service;

import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.auth.entity.JwtRevokedToken;
import com.storage.system.auth.mapper.JwtRevokedTokenMapper;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
public interface JwtRevocationService {
    boolean isRevoked(String jti);
    void revoke(JwtClaims claims);
}
