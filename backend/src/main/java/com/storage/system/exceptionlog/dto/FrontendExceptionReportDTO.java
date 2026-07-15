package com.storage.system.exceptionlog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FrontendExceptionReportDTO {

    @NotBlank
    @Size(max = 64)
    private String errorCode;

    @Size(max = 64)
    private String requestId;

    private Integer httpStatus;

    @Size(max = 16)
    private String httpMethod;

    @Size(max = 512)
    private String requestPath;

    @Size(max = 256)
    private String exceptionClass;

    @NotBlank
    @Size(max = 500)
    private String summary;

    @Size(max = 8000)
    private String stackTrace;

    @Size(max = 512)
    private String frontendRoute;

    @Size(max = 512)
    private String browserInfo;

    @Pattern(regexp = "FRONTEND|API")
    private String reportType;
}
