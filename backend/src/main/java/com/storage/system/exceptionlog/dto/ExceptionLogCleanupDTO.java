package com.storage.system.exceptionlog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExceptionLogCleanupDTO {

    @NotNull
    private LocalDateTime before;
}
