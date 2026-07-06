package com.storage.system.auth.shiro;

import com.storage.system.auth.service.JwtService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.WebSubject;

import java.io.IOException;

@RequiredArgsConstructor
public class ShiroSubjectBindingFilter implements Filter {

    private final SecurityManager securityManager;
    private final JwtService jwtService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        SecurityUtils.setSecurityManager(securityManager);
        Subject subject = new WebSubject.Builder(securityManager, httpRequest, httpResponse).buildWebSubject();
        ThreadContext.bind(subject);
        try {
            authenticateBearerToken(httpRequest, subject);
            chain.doFilter(request, response);
        } finally {
            ThreadContext.unbindSubject();
        }
    }

    private void authenticateBearerToken(HttpServletRequest request, Subject subject) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return;
        }
        try {
            subject.login(new JwtAuthenticationToken(token, jwtService.parseUserId(token)));
        } catch (AuthenticationException ex) {
            subject.logout();
        }
    }
}
