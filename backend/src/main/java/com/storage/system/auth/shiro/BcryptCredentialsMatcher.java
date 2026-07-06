package com.storage.system.auth.shiro;

import com.storage.system.user.entity.SysUser;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptCredentialsMatcher implements CredentialsMatcher {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        if (token instanceof JwtAuthenticationToken) {
            return true;
        }
        Object credentials = token.getCredentials();
        if (credentials == null) {
            return false;
        }
        String rawPassword = new String((char[]) credentials);
        SysUser user = (SysUser) info.getPrincipals().getPrimaryPrincipal();
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
