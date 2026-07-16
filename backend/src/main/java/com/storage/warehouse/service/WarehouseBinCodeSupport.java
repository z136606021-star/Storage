package com.storage.warehouse.service;

import com.storage.common.exception.BusinessException;
import org.springframework.util.StringUtils;

public final class WarehouseBinCodeSupport {

    private WarehouseBinCodeSupport() {
    }

    public static String buildBinCode(String rowNo, Integer colNo, Integer levelNo) {
        validateCoordinateCombination(rowNo, colNo, levelNo);
        String normalizedRow = rowNo.trim();
        String binCode;
        if (hasPositive(levelNo)) {
            binCode = normalizedRow + "-" + colNo + "-" + levelNo;
        } else if (hasPositive(colNo)) {
            binCode = normalizedRow + "-" + colNo;
        } else {
            binCode = normalizedRow;
        }
        if (binCode.length() > 32) {
            throw new BusinessException("Bin位编号长度不能超过32");
        }
        return binCode;
    }

    public static void validateCoordinateCombination(String rowNo, Integer colNo, Integer levelNo) {
        if (!StringUtils.hasText(rowNo)) {
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
