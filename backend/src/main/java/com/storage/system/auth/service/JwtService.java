package com.storage.system.auth.service;

import com.storage.system.auth.config.JwtProperties;
import com.storage.system.user.entity.SysUser;
import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.auth.shiro.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
public interface JwtService {
    String issueToken(SysUser user);
    JwtClaims parseClaims(String token);
    Long parseUserId(String token);
}
