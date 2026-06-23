package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.mapper.MaterialLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MaterialLedgerService {

    private static final String ALL = "全部";

    private final MaterialLedgerMapper materialLedgerMapper;

    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();

        if (isFilterValue(query.getCategory())) {
            wrapper.eq(MaterialLedger::getCategory, query.getCategory());
        }
        if (isFilterValue(query.getGenericName())) {
            wrapper.eq(MaterialLedger::getGenericName, query.getGenericName());
        }
        if (isFilterValue(query.getBrand())) {
            wrapper.eq(MaterialLedger::getBrand, query.getBrand());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(MaterialLedger::getName, query.getName().trim());
        }
        if (isFilterValue(query.getModel())) {
            wrapper.eq(MaterialLedger::getModel, query.getModel());
        }
        if (isFilterValue(query.getBinLocation())) {
            wrapper.eq(MaterialLedger::getBinLocation, query.getBinLocation());
        }

        wrapper.orderByAsc(MaterialLedger::getId);

        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<MaterialLedger> result = materialLedgerMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public FilterOptionsVO filterOptions() {
        return new FilterOptionsVO(
                materialLedgerMapper.selectDistinctCategories(),
                materialLedgerMapper.selectDistinctGenericNames(),
                materialLedgerMapper.selectDistinctBrands(),
                materialLedgerMapper.selectDistinctModels(),
                materialLedgerMapper.selectDistinctBinLocations()
        );
    }

    private boolean isFilterValue(String value) {
        return StringUtils.hasText(value) && !ALL.equals(value);
    }
}
