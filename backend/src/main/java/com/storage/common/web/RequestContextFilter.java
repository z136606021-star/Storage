package com.storage.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = RequestIdGenerator.normalize(request.getHeader(REQUEST_ID_HEADER));
        String method = request.getMethod();
        String path = RequestPathSanitizer.sanitize(request.getRequestURI());
        long startedAt = System.currentTimeMillis();

        RequestContext.set(new RequestContext.State(requestId, method, path, startedAt));
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            if (response.getStatus() >= 500) {
                log.error("request completed status={} method={} path={} durationMs={}",
                        response.getStatus(), method, path, durationMs);
            } else if (log.isDebugEnabled()) {
                log.debug("request completed status={} method={} path={} durationMs={}",
                        response.getStatus(), method, path, durationMs);
            }
            MDC.remove(MDC_REQUEST_ID);
            RequestContext.clear();
        }
    }
}
