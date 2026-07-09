package com.storage.warehouse.service;

public final class SafetyStockWarningStatus {

    public static final String WARNING_SQL_EXPRESSION = """
            (COALESCE(ss.safety_quantity, 0) > 0 AND ml.stock_quantity <= COALESCE(ss.safety_quantity, 0))
            """;

    private SafetyStockWarningStatus() {
    }

    public static boolean inWarning(Integer stockQuantity, Integer safetyQuantity, Boolean warningEnabled) {
        int stock = stockQuantity != null ? stockQuantity : 0;
        int safety = safetyQuantity != null ? safetyQuantity : 0;
        return safety > 0 && stock <= safety;
    }

    public static boolean isAutoWarningEnabled(Integer safetyQuantity) {
        return safetyQuantity != null && safetyQuantity > 0;
    }

    public static String formatWarningPeriod(boolean inWarning) {
        return inWarning ? "是" : "否";
    }
}
