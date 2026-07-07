package com.storage.warehouse.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.warehouse.shared.MaterialUsageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MaterialLedgerUsageQueryService implements MaterialUsageQueryService {

    private final MaterialLedgerMapper materialLedgerMapper;

    @Override
    public long countByBinCode(String binCode) {
        return materialLedgerMapper.selectCount(
                Wrappers.<MaterialLedger>lambdaQuery().eq(MaterialLedger::getBinLocation, binCode)
        );
    }

    @Override
    public long countByBomCatalogKey(String category, String genericName, String brand, String name) {
        LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.<MaterialLedger>lambdaQuery()
                .eq(MaterialLedger::getCategory, category)
                .eq(MaterialLedger::getGenericName, genericName)
                .eq(MaterialLedger::getName, name);

        if (StringUtils.hasText(brand)) {
            wrapper.eq(MaterialLedger::getBrand, brand);
        } else {
            wrapper.and(w -> w.isNull(MaterialLedger::getBrand).or().eq(MaterialLedger::getBrand, ""));
        }

        return materialLedgerMapper.selectCount(wrapper);
    }
}
