package com.storage.system.exceptionlog.scheduler;

import com.storage.system.exceptionlog.service.SysExceptionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionLogCleanupScheduler {

    private final SysExceptionLogService sysExceptionLogService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        try {
            sysExceptionLogService.cleanupExpired();
        } catch (Exception ex) {
            log.warn("Scheduled exception log cleanup failed", ex);
        }
    }
}
