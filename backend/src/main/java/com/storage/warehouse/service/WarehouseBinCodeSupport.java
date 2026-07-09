package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;

public final class WarehouseBinCodeSupport {

    private WarehouseBinCodeSupport() {
    }

    public static String buildBinCode(Integer rowNo, Integer colNo, Integer levelNo) {
        validateCoordinateCombination(rowNo, colNo, levelNo);
        if (hasPositive(levelNo)) {
            return rowNo + "-" + colNo + "-" + levelNo;
        }
        if (hasPositive(colNo)) {
            return rowNo + "-" + colNo;
        }
        return String.valueOf(rowNo);
    }

    public static void validateCoordinateCombination(Integer rowNo, Integer colNo, Integer levelNo) {
        if (!hasPositive(rowNo)) {
            throw new BusinessException("排不能为空");
        }
        if (hasPositive(levelNo) && !hasPositive(colNo)) {
            throw new BusinessException("填写层时必须同时填写列");
        }
    }

    private static boolean hasPositive(Integer value) {
        return value != null && value >= 1;
    }
}
