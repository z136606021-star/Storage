package com.storage.common.exception;

import com.storage.design.exception.DesignGuideNotFoundException;
import com.storage.design.exception.DesignProductTypeNotFoundException;
import com.storage.design.exception.DesignStageNotFoundException;
import com.storage.experience.exception.ExperienceRecordNotFoundException;
import com.storage.experience.exception.ExperienceTypeNotFoundException;
import com.storage.warehouse.exception.MaterialIoNotFoundException;
import com.storage.warehouse.exception.MaterialLedgerNotFoundException;
import com.storage.warehouse.exception.SafetyStockNotFoundException;
import com.storage.warehouse.exception.WarehouseBinNotFoundException;
import com.storage.warehouse.exception.WarehouseBomNotFoundException;
import com.storage.system.customer.exception.SysCustomerNotFoundException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthenticated(UnauthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "未登录或登录已过期"));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "无权限访问"));
    }

    @ExceptionHandler(MaterialLedgerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(MaterialLedgerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(WarehouseBinNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWarehouseBinNotFound(WarehouseBinNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(WarehouseBomNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWarehouseBomNotFound(WarehouseBomNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MaterialIoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMaterialIoNotFound(MaterialIoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SafetyStockNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSafetyStockNotFound(SafetyStockNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SysCustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSysCustomerNotFound(SysCustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DesignProductTypeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDesignProductTypeNotFound(DesignProductTypeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DesignStageNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDesignStageNotFound(DesignStageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DesignGuideNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDesignGuideNotFound(DesignGuideNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ExperienceRecordNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleExperienceRecordNotFound(ExperienceRecordNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ExperienceTypeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleExperienceTypeNotFound(ExperienceTypeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ImportFormatException.class)
    public ResponseEntity<Map<String, String>> handleImportFormat(ImportFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, BusinessException.class})
    public ResponseEntity<Map<String, String>> handleBusiness(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }
}
