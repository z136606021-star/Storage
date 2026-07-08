package com.storage.warehouse.controller;

import com.storage.warehouse.dto.WarehouseStatsOverviewVO;
import com.storage.warehouse.service.WarehouseStatsService;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouse-stats")
@RequiredArgsConstructor
public class WarehouseStatsController {

    private final WarehouseStatsService warehouseStatsService;

    @GetMapping("/overview")
    @RequiresPermissions("warehouse:stats:read")
    public WarehouseStatsOverviewVO overview(@RequestParam(required = false) Integer recentDays) {
        return warehouseStatsService.overview(recentDays);
    }
}
