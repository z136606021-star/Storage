package com.storage.warehouse.safety.service;

public final class SafetyStockWarningStatus {

    private SafetyStockWarningStatus() {
    }

    public static boolean inWarning(Integer stockQuantity, Integer safetyQuantity, Boolean warningEnabled) {
        if (!Boolean.TRUE.equals(warningEnabled)) {
            return false;
        }
        int stock = stockQuantity != null ? stockQuantity : 0;
        int safety = safetyQuantity != null ? safetyQuantity : 0;
        return stock < safety;
    }

    public static String formatWarningPeriod(boolean inWarning) {
        return inWarning ? "是" : "否";
    }
}
