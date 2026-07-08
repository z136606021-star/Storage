package com.storage.warehouse.bom.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.bom.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.bom.entity.WarehouseBom;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class WarehouseBomQueryBuilder {

    private static final String ALL = "全部";

    private WarehouseBomQueryBuilder() {
    }

    public static LambdaQueryWrapper<WarehouseBom> build(WarehouseBomQueryDTO query) {
        LambdaQueryWrapper<WarehouseBom> wrapper = Wrappers.lambdaQuery();

        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(WarehouseBom::getId, query.getIds());
        }

        if (isFilterValue(query.getCategory())) {
            wrapper.eq(WarehouseBom::getCategory, query.getCategory());
        }
        if (isFilterValue(query.getGenericName())) {
            wrapper.eq(WarehouseBom::getGenericName, query.getGenericName());
        }
        if (isFilterValue(query.getBrand())) {
            wrapper.eq(WarehouseBom::getBrand, query.getBrand());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(WarehouseBom::getName, query.getName().trim());
        }
        if (StringUtils.hasText(query.getModel())) {
            wrapper.like(WarehouseBom::getModel, query.getModel().trim());
        }

        wrapper.orderByAsc(WarehouseBom::getId);
        return wrapper;
    }

    public static LambdaQueryWrapper<WarehouseBom> withCategory(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<WarehouseBom> wrapper = Wrappers.lambdaQuery();
        if (isFilterValue(query.getCategory())) {
            wrapper.eq(WarehouseBom::getCategory, query.getCategory());
        }
        return wrapper;
    }

    public static LambdaQueryWrapper<WarehouseBom> withCategoryAndGeneric(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<WarehouseBom> wrapper = withCategory(query);
        if (isFilterValue(query.getGenericName())) {
            wrapper.eq(WarehouseBom::getGenericName, query.getGenericName());
        }
        return wrapper;
    }

    public static LambdaQueryWrapper<WarehouseBom> withCategoryGenericAndBrand(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<WarehouseBom> wrapper = withCategoryAndGeneric(query);
        if (isFilterValue(query.getBrand())) {
            wrapper.eq(WarehouseBom::getBrand, query.getBrand());
        }
        return wrapper;
    }

    public static boolean isFilterValue(String value) {
        return StringUtils.hasText(value) && !ALL.equals(value);
    }
}
