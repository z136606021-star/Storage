package com.storage.system.exceptionlog.controller;

import com.storage.common.dto.PageResult;
import com.storage.system.exceptionlog.dto.ExceptionLogCleanupDTO;
import com.storage.system.exceptionlog.dto.FrontendExceptionReportDTO;
import com.storage.system.exceptionlog.dto.SysExceptionLogQueryDTO;
import com.storage.system.exceptionlog.entity.SysExceptionLog;
import com.storage.system.exceptionlog.service.SysExceptionLogService;
import com.storage.system.user.contract.OperatorInfo;
import com.storage.system.user.contract.OperatorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/exception-logs")
@RequiredArgsConstructor
public class SysExceptionLogController {

    private final SysExceptionLogService sysExceptionLogService;
    private final OperatorResolver operatorResolver;

    @GetMapping
    @RequiresPermissions("system:exception-log:read")
    public PageResult<SysExceptionLog> page(SysExceptionLogQueryDTO query) {
        return sysExceptionLogService.page(query);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:exception-log:read")
    public SysExceptionLog getById(@PathVariable Long id) {
        return sysExceptionLogService.getById(id);
    }

    @PostMapping("/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@Valid @RequestBody FrontendExceptionReportDTO dto) {
        OperatorInfo operator = operatorResolver.requireCurrentOperator();
        sysExceptionLogService.recordFrontendReport(dto, operator);
    }

    @DeleteMapping("/cleanup")
    @RequiresPermissions("system:exception-log:write")
    public Map<String, Integer> cleanup(@Valid @RequestBody ExceptionLogCleanupDTO dto) {
        int deleted = sysExceptionLogService.cleanupBefore(dto);
        return Map.of("deleted", deleted);
    }
}
