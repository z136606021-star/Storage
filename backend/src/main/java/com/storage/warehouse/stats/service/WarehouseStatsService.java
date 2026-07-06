package com.storage.warehouse.stats.service;

import com.storage.warehouse.stats.dto.WarehouseStatsOverviewVO;

public interface WarehouseStatsService {

    WarehouseStatsOverviewVO overview(Integer recentDays);
}
