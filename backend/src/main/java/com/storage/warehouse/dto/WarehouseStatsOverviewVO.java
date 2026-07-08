package com.storage.warehouse.dto;

import com.storage.warehouse.dto.SafetyStockRecordVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WarehouseStatsOverviewVO {

    private int recentDays = 7;

    private long totalLedgerCount;

    private long totalStockQuantity;

    private long warningMaterialCount;

    private long inboundRecordCount;

    private long outboundRecordCount;

    private long inboundQuantitySum;

    private long outboundQuantitySum;

    private List<SafetyStockRecordVO> warningMaterials = new ArrayList<>();
}
