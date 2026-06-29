package com.storage.exception;

public class SafetyStockNotFoundException extends RuntimeException {

    public SafetyStockNotFoundException(Long materialLedgerId) {
        super("安全库存配置不存在: 物料台账 " + materialLedgerId);
    }
}
