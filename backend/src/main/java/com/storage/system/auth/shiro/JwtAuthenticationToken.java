package com.storage.system.auth.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class JwtAuthenticationToken implements AuthenticationToken {

    private final String token;
    private final Long userId;

    public JwtAuthenticationToken(String token, Long userId) {
        this.token = token;
        this.userId = userId;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
