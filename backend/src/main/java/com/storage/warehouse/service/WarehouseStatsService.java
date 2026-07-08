package com.storage.warehouse.service;

import com.storage.warehouse.dto.WarehouseStatsOverviewVO;

public interface WarehouseStatsService {

    WarehouseStatsOverviewVO overview(Integer recentDays);
}
