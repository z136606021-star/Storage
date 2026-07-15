package com.storage.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
public class ApiErrorBody {

    private String message;
    private String code;
    private String requestId;
    private String timestamp;
    private String path;

    public Map<String, String> toResponseMap() {
        Map<String, String> body = new LinkedHashMap<>();
        if (message != null) {
            body.put("message", message);
        }
        if (code != null) {
            body.put("code", code);
        }
        if (requestId != null) {
            body.put("requestId", requestId);
        }
        if (timestamp != null) {
            body.put("timestamp", timestamp);
        }
        if (path != null) {
            body.put("path", path);
        }
        return body;
    }

    public static ApiErrorBody internalError(String requestId, String path) {
        return ApiErrorBody.builder()
                .message("服务器内部错误，请联系管理员")
                .code("INTERNAL_ERROR")
                .requestId(requestId)
                .timestamp(Instant.now().toString())
                .path(path)
                .build();
    }
}
