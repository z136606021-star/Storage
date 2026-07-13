package com.storage.system.auth.shiro;

import com.storage.system.auth.dto.JwtClaims;
import org.apache.shiro.authc.AuthenticationToken;

public class JwtAuthenticationToken implements AuthenticationToken {

    private final String token;
    private final JwtClaims claims;

    public JwtAuthenticationToken(String token, JwtClaims claims) {
        this.token = token;
        this.claims = claims;
    }

    @Override
    public Object getPrincipal() {
        return claims.userId();
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    public JwtClaims getClaims() {
        return claims;
    }
}
