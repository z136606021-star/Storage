package com.storage.system.auth.service;

import com.storage.system.auth.config.JwtProperties;
import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.auth.shiro.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;

    @Override
    public String issueToken(SysUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getTtlMinutes() * 60L);
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("tv", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    @Override
    public JwtClaims parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!StringUtils.hasText(claims.getId())) {
                throw new JwtAuthenticationException("JWT 无效或已过期", null);
            }
            Integer tokenVersion = claims.get("tv", Integer.class);
            if (tokenVersion == null) {
                tokenVersion = 0;
            }
            return new JwtClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.getId(),
                    claims.getExpiration().toInstant(),
                    tokenVersion
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("JWT 无效或已过期", ex);
        }
    }

    @Override
    public Long parseUserId(String token) {
        return parseClaims(token).userId();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
