package com.storage.warehouse.bin.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.bin.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.bin.entity.WarehouseBin;
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

        wrapper.orderByAsc(WarehouseBin::getRowNo, WarehouseBin::getColNo, WarehouseBin::getLevelNo);
        return wrapper;
    }
}
