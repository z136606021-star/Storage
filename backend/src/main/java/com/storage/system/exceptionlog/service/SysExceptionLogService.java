package com.storage.system.exceptionlog.service;

import com.storage.common.dto.PageResult;
import com.storage.system.exceptionlog.dto.ExceptionLogCleanupDTO;
import com.storage.system.exceptionlog.dto.FrontendExceptionReportDTO;
import com.storage.system.exceptionlog.dto.SysExceptionLogQueryDTO;
import com.storage.system.exceptionlog.entity.SysExceptionLog;
import com.storage.system.user.contract.OperatorInfo;
import jakarta.servlet.http.HttpServletRequest;

public interface SysExceptionLogService {

    PageResult<SysExceptionLog> page(SysExceptionLogQueryDTO query);

    SysExceptionLog getById(Long id);

    void recordBackendException(Throwable throwable, HttpServletRequest request, int httpStatus);

    void recordFrontendReport(FrontendExceptionReportDTO dto, OperatorInfo operator);

    int cleanupBefore(ExceptionLogCleanupDTO dto);

    int cleanupExpired();
}
