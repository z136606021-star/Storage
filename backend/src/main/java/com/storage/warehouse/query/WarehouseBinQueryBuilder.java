package com.storage.warehouse.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.entity.WarehouseBin;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class WarehouseBinQueryBuilder {

    private WarehouseBinQueryBuilder() {
    }

    public static LambdaQueryWrapper<WarehouseBin> build(WarehouseBinQueryDTO query) {
        LambdaQueryWrapper<WarehouseBin> wrapper = Wrappers.lambdaQuery();

        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(WarehouseBin::getId, query.getIds());
        }

        if (StringUtils.hasText(query.getBinCode())) {
            wrapper.like(WarehouseBin::getBinCode, query.getBinCode().trim());
        }

        if (query.getRowNo() != null) {
            wrapper.eq(WarehouseBin::getRowNo, query.getRowNo());
        }

        if (query.getColNo() != null) {
            wrapper.eq(WarehouseBin::getColNo, query.getColNo());
        }

        if (query.getLevelNo() != null) {
            wrapper.eq(WarehouseBin::getLevelNo, query.getLevelNo());
        }

        wrapper.orderByDesc(WarehouseBin::getUpdatedAt, WarehouseBin::getId);
        return wrapper;
    }
}
