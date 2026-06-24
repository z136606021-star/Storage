package com.storage.shiro;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.WebSubject;

import java.io.IOException;

@RequiredArgsConstructor
public class ShiroSubjectBindingFilter implements Filter {

    private final SecurityManager securityManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        SecurityUtils.setSecurityManager(securityManager);
        Subject subject = new WebSubject.Builder(securityManager, httpRequest, httpResponse).buildWebSubject();
        ThreadContext.bind(subject);
        try {
            chain.doFilter(request, response);
        } finally {
            ThreadContext.unbindSubject();
        }
    }
}
