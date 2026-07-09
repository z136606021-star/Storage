package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import com.storage.warehouse.query.MaterialIoQueryBuilder;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MaterialIoPurpose {

    public static final String EMPLOYEE_PICKUP = "EMPLOYEE_PICKUP";
    public static final String MACHINING = "MACHINING";
    public static final String PROJECT_USE = "PROJECT_USE";
    public static final String RETURN_IN = "RETURN_IN";
    public static final String PROCUREMENT = "PROCUREMENT";
    public static final String ADJUSTMENT = "ADJUSTMENT";
    public static final String OTHER = "OTHER";

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(EMPLOYEE_PICKUP, "员工领用");
        LABELS.put(MACHINING, "机加工用");
        LABELS.put(PROJECT_USE, "项目领用");
        LABELS.put(RETURN_IN, "退库入库");
        LABELS.put(PROCUREMENT, "采购入库");
        LABELS.put(ADJUSTMENT, "盘点调整");
        LABELS.put(OTHER, "其他");
    }

    private MaterialIoPurpose() {
    }

    public static Set<String> codes() {
        return LABELS.keySet();
    }

    public static String normalizePurpose(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return null;
        }
        String value = purpose.trim();
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(value) || entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return value.toUpperCase();
    }

    public static void assertValidPurpose(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return;
        }
        String normalized = normalizePurpose(purpose);
        if (!LABELS.containsKey(normalized)) {
            throw new BusinessException("用途无效，支持: " + String.join("、", LABELS.values()));
        }
    }

    public static void validatePurposeForIoType(String ioType, String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return;
        }
        assertValidPurpose(purpose);
    }

    public static String purposeLabel(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            return "";
        }
        String normalized = normalizePurpose(purpose);
        return LABELS.getOrDefault(normalized, purpose);
    }

    public static void validateProjectRef(String purpose, String projectRef) {
        if (StringUtils.hasText(projectRef) && projectRef.trim().length() > 128) {
            throw new BusinessException("项目编号长度不能超过128字符");
        }
    }

    public static String normalizeProjectRef(String purpose, String projectRef) {
        if (!StringUtils.hasText(projectRef)) {
            return null;
        }
        String trimmed = projectRef.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
