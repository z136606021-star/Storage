package com.storage.system.exceptionlog.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.system.exceptionlog.dto.SysExceptionLogQueryDTO;
import com.storage.system.exceptionlog.entity.SysExceptionLog;
import org.springframework.util.StringUtils;

public final class SysExceptionLogQueryBuilder {

    private SysExceptionLogQueryBuilder() {
    }

    public static LambdaQueryWrapper<SysExceptionLog> build(SysExceptionLogQueryDTO query) {
        LambdaQueryWrapper<SysExceptionLog> wrapper = Wrappers.<SysExceptionLog>lambdaQuery()
                .orderByDesc(SysExceptionLog::getOccurredAt)
                .orderByDesc(SysExceptionLog::getId);

        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(SysExceptionLog::getSource, query.getSource().trim());
        }
        if (query.getHttpStatus() != null) {
            wrapper.eq(SysExceptionLog::getHttpStatus, query.getHttpStatus());
        }
        if (StringUtils.hasText(query.getExceptionClass())) {
            wrapper.like(SysExceptionLog::getExceptionClass, query.getExceptionClass().trim());
        }
        if (StringUtils.hasText(query.getRequestId())) {
            wrapper.eq(SysExceptionLog::getRequestId, query.getRequestId().trim());
        }
        if (StringUtils.hasText(query.getRequestPath())) {
            wrapper.like(SysExceptionLog::getRequestPath, query.getRequestPath().trim());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysExceptionLog::getSummary, keyword)
                    .or()
                    .like(SysExceptionLog::getStackTrace, keyword)
                    .or()
                    .like(SysExceptionLog::getExceptionClass, keyword));
        }
        if (query.getOccurredAtStart() != null) {
            wrapper.ge(SysExceptionLog::getOccurredAt, query.getOccurredAtStart());
        }
        if (query.getOccurredAtEnd() != null) {
            wrapper.le(SysExceptionLog::getOccurredAt, query.getOccurredAtEnd());
        }
        return wrapper;
    }
}
