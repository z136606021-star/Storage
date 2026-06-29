package com.storage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.dto.MaterialIoAggregateVO;
import com.storage.dto.SafetyStockQueryDTO;
import com.storage.dto.SafetyStockRecordVO;
import com.storage.dto.WarehouseStatsOverviewVO;
import com.storage.entity.MaterialLedger;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.mapper.WarehouseStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseStatsService {

    private static final int DEFAULT_RECENT_DAYS = 7;
    private static final int WARNING_PREVIEW_LIMIT = 10;

    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseStatsMapper warehouseStatsMapper;
    private final SafetyStockService safetyStockService;

    public WarehouseStatsOverviewVO overview(Integer recentDays) {
        int days = recentDays == null || recentDays < 1 ? DEFAULT_RECENT_DAYS : Math.min(recentDays, 90);
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        WarehouseStatsOverviewVO vo = new WarehouseStatsOverviewVO();
        vo.setRecentDays(days);
        vo.setTotalLedgerCount(materialLedgerMapper.selectCount(Wrappers.lambdaQuery(MaterialLedger.class)));
        vo.setTotalStockQuantity(nullToZero(warehouseStatsMapper.sumStockQuantity()));
        vo.setWarningMaterialCount(nullToZero(warehouseStatsMapper.countWarningMaterials()));

        for (MaterialIoAggregateVO row : warehouseStatsMapper.summarizeIoSince(since)) {
            if ("IN".equalsIgnoreCase(row.getIoType())) {
                vo.setInboundRecordCount(nullToZero(row.getRecordCount()));
                vo.setInboundQuantitySum(nullToZero(row.getQuantitySum()));
            } else if ("OUT".equalsIgnoreCase(row.getIoType())) {
                vo.setOutboundRecordCount(nullToZero(row.getRecordCount()));
                vo.setOutboundQuantitySum(nullToZero(row.getQuantitySum()));
            }
        }

        SafetyStockQueryDTO warningQuery = new SafetyStockQueryDTO();
        warningQuery.setWarningPeriod("是");
        warningQuery.setPage(1);
        warningQuery.setPageSize(WARNING_PREVIEW_LIMIT);
        List<SafetyStockRecordVO> warningMaterials = safetyStockService.page(warningQuery).getRecords();
        vo.setWarningMaterials(warningMaterials);
        return vo;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
