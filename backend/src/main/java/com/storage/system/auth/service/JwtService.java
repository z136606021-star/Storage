package com.storage.system.auth.service;

import com.storage.system.auth.dto.JwtClaims;
import com.storage.system.user.entity.SysUser;

public interface JwtService {
    String issueToken(SysUser user);

    JwtClaims parseClaims(String token);

    Long parseUserId(String token);
}
