package com.storage.system.auth.service;

import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.auth.entity.JwtRevokedToken;
import com.storage.system.auth.mapper.JwtRevokedTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class JwtRevocationServiceImpl implements JwtRevocationService {

    private final JwtRevokedTokenMapper jwtRevokedTokenMapper;

    public boolean isRevoked(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return jwtRevokedTokenMapper.countByJti(jti) > 0;
    }

    @Transactional
    public void revoke(JwtClaims claims) {
        if (claims == null || !StringUtils.hasText(claims.jti())) {
            return;
        }
        if (isRevoked(claims.jti())) {
            return;
        }
        JwtRevokedToken revoked = new JwtRevokedToken();
        revoked.setJti(claims.jti());
        revoked.setExpiresAt(LocalDateTime.ofInstant(claims.expiresAt(), ZoneId.systemDefault()));
        revoked.setRevokedAt(LocalDateTime.now());
        jwtRevokedTokenMapper.insert(revoked);
    }
}
