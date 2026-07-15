package com.storage.system.exceptionlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.common.web.RequestContext;
import com.storage.common.web.RequestContextFilter;
import com.storage.common.web.RequestPathSanitizer;
import com.storage.system.exceptionlog.ExceptionLogLevels;
import com.storage.system.exceptionlog.ExceptionLogSources;
import com.storage.system.exceptionlog.config.ExceptionLogProperties;
import com.storage.system.exceptionlog.dto.ExceptionLogCleanupDTO;
import com.storage.system.exceptionlog.dto.FrontendExceptionReportDTO;
import com.storage.system.exceptionlog.dto.SysExceptionLogQueryDTO;
import com.storage.system.exceptionlog.entity.SysExceptionLog;
import com.storage.system.exceptionlog.exception.SysExceptionLogNotFoundException;
import com.storage.system.exceptionlog.mapper.SysExceptionLogMapper;
import com.storage.system.exceptionlog.query.SysExceptionLogQueryBuilder;
import com.storage.system.user.contract.OperatorInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysExceptionLogServiceImpl implements SysExceptionLogService {

    private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
    private static final Set<String> ALLOWED_FRONTEND_ERROR_CODES = Set.of(
            "VUE_RUNTIME_ERROR",
            "WINDOW_ERROR",
            "UNHANDLED_REJECTION",
            "ROUTER_ERROR",
            "API_SERVER_ERROR"
    );

    private final SysExceptionLogMapper sysExceptionLogMapper;
    private final ExceptionLogProperties exceptionLogProperties;

    @Override
    public PageResult<SysExceptionLog> page(SysExceptionLogQueryDTO query) {
        var result = sysExceptionLogMapper.selectPage(
                PageSupport.page(query.getPage(), query.getPageSize()),
                SysExceptionLogQueryBuilder.build(query)
        );
        return PageSupport.result(result);
    }

    @Override
    public SysExceptionLog getById(Long id) {
        SysExceptionLog record = sysExceptionLogMapper.selectById(id);
        if (record == null) {
            throw new SysExceptionLogNotFoundException(id);
        }
        return record;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordBackendException(Throwable throwable, HttpServletRequest request, int httpStatus) {
        try {
            SysExceptionLog entity = new SysExceptionLog();
            entity.setSource(ExceptionLogSources.BACKEND);
            entity.setLevel(ExceptionLogLevels.ERROR);
            entity.setOccurredAt(LocalDateTime.now());
            entity.setErrorCode(INTERNAL_ERROR_CODE);
            entity.setRequestId(resolveRequestId(request));
            entity.setHttpStatus(httpStatus);
            entity.setHttpMethod(request == null ? null : request.getMethod());
            entity.setRequestPath(request == null ? null : RequestPathSanitizer.sanitize(request.getRequestURI()));
            entity.setExceptionClass(throwable.getClass().getName());
            entity.setSummary(truncate(throwable.getMessage(), exceptionLogProperties.getMaxSummaryLength(), "未提供异常消息"));
            entity.setStackTrace(truncateStackTrace(throwable));
            entity.setCreatedAt(LocalDateTime.now());
            sysExceptionLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to persist backend exception log requestId={}", resolveRequestId(request), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFrontendReport(FrontendExceptionReportDTO dto, OperatorInfo operator) {
        if (!ALLOWED_FRONTEND_ERROR_CODES.contains(dto.getErrorCode())) {
            throw new IllegalArgumentException("不支持的前端异常类型");
        }
        try {
            SysExceptionLog entity = new SysExceptionLog();
            entity.setSource(ExceptionLogSources.FRONTEND);
            entity.setLevel(ExceptionLogLevels.ERROR);
            entity.setOccurredAt(LocalDateTime.now());
            entity.setErrorCode(dto.getErrorCode().trim());
            entity.setRequestId(trimToNull(dto.getRequestId()));
            entity.setHttpStatus(dto.getHttpStatus());
            entity.setHttpMethod(trimToNull(dto.getHttpMethod()));
            entity.setRequestPath(RequestPathSanitizer.sanitize(dto.getRequestPath()));
            entity.setExceptionClass(trimToNull(dto.getExceptionClass()));
            entity.setSummary(truncate(dto.getSummary(), exceptionLogProperties.getMaxSummaryLength(), "前端异常"));
            entity.setStackTrace(truncate(dto.getStackTrace(), exceptionLogProperties.getMaxStackTraceLength(), null));
            entity.setFrontendRoute(RequestPathSanitizer.sanitize(dto.getFrontendRoute()));
            entity.setBrowserInfo(truncate(dto.getBrowserInfo(), 512, null));
            if (operator != null) {
                entity.setOperatorId(operator.getId());
                entity.setOperatorUsername(operator.getUsername());
            }
            entity.setCreatedAt(LocalDateTime.now());
            sysExceptionLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to persist frontend exception log requestId={}", dto.getRequestId(), ex);
        }
    }

    @Override
    @Transactional
    public int cleanupBefore(ExceptionLogCleanupDTO dto) {
        LambdaQueryWrapper<SysExceptionLog> wrapper = Wrappers.<SysExceptionLog>lambdaQuery()
                .lt(SysExceptionLog::getOccurredAt, dto.getBefore());
        return sysExceptionLogMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public int cleanupExpired() {
        int retentionDays = Math.max(exceptionLogProperties.getRetentionDays(), 1);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        LambdaQueryWrapper<SysExceptionLog> wrapper = Wrappers.<SysExceptionLog>lambdaQuery()
                .lt(SysExceptionLog::getOccurredAt, cutoff);
        int deleted = sysExceptionLogMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("Cleaned up {} expired exception logs before {}", deleted, cutoff);
        }
        return deleted;
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request != null && StringUtils.hasText(request.getHeader(RequestContextFilter.REQUEST_ID_HEADER))) {
            return request.getHeader(RequestContextFilter.REQUEST_ID_HEADER).trim();
        }
        return RequestContext.getRequestId();
    }

    private String truncateStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return truncate(writer.toString(), exceptionLogProperties.getMaxStackTraceLength(), null);
    }

    private String truncate(String value, int maxLength, String fallback) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return fallback;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
