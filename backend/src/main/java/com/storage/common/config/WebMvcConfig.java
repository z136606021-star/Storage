package com.storage.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.common.exception.ApiErrorBody;
import com.storage.common.web.RequestContext;
import com.storage.common.web.RequestContextFilter;
import com.storage.common.web.RequestIdGenerator;
import com.storage.common.web.RequestPathSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiAuthInterceptor(objectMapper))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/register/**",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/files/preview"
                );
    }

    private static class ApiAuthInterceptor implements HandlerInterceptor {

        private final ObjectMapper objectMapper;

        ApiAuthInterceptor(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return true;
            }
            if (SecurityUtils.getSubject().isAuthenticated()) {
                return true;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String requestId = resolveRequestId(request);
            response.setHeader(RequestContextFilter.REQUEST_ID_HEADER, requestId);
            String path = RequestPathSanitizer.sanitize(request.getRequestURI());
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiErrorBody.builder()
                            .message("未登录或登录已过期")
                            .code("UNAUTHENTICATED")
                            .requestId(requestId)
                            .path(path)
                            .build()
                            .toResponseMap()
            );
            return false;
        }

        private String resolveRequestId(HttpServletRequest request) {
            String requestId = RequestContext.getRequestId();
            if (requestId != null && !requestId.isBlank()) {
                return requestId;
            }
            return RequestIdGenerator.normalize(request.getHeader(RequestContextFilter.REQUEST_ID_HEADER));
        }
    }
}
