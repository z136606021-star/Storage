package com.storage.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.warehouse.converter.WarehouseBinConverter;
import com.storage.warehouse.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.entity.WarehouseBin;
import com.storage.warehouse.exception.WarehouseBinNotFoundException;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.mapper.WarehouseBinMapper;
import com.storage.warehouse.query.WarehouseBinQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseBinServiceImpl extends ServiceImpl<WarehouseBinMapper, WarehouseBin>
        implements WarehouseBinService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseBinConverter warehouseBinConverter;
    private final WarehouseBinExportService warehouseBinExportService;

    @Override
    public PageResult<WarehouseBin> page(WarehouseBinQueryDTO query) {
        var result = page(
                PageSupport.page(query.getPage(), query.getPageSize()),
                WarehouseBinQueryBuilder.build(query)
        );
        return PageSupport.result(result);
    }

    @Override
    public WarehouseBin getById(Long id) {
        WarehouseBin bin = super.getById(id);
        if (bin == null) {
            throw new WarehouseBinNotFoundException(id);
        }
        return bin;
    }

    @Override
    public List<String> listAllCodes() {
        return list(Wrappers.<WarehouseBin>lambdaQuery()
                        .select(WarehouseBin::getBinCode)
                        .orderByAsc(WarehouseBin::getBinCode))
                .stream()
                .map(WarehouseBin::getBinCode)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBinCode(String binCode) {
        if (!StringUtils.hasText(binCode)) {
            return false;
        }
        return count(
                Wrappers.<WarehouseBin>lambdaQuery().eq(WarehouseBin::getBinCode, binCode.trim())
        ) > 0;
    }

    @Override
    public void assertBinExists(String binCode) {
        if (!existsByBinCode(binCode)) {
            throw new BusinessException("Bin位不存在: " + binCode);
        }
    }

    @Override
    public WarehouseBin create(WarehouseBinSaveDTO dto) {
        String binCode = buildBinCode(dto.getRowNo(), dto.getColNo(), dto.getLevelNo());
        assertNotDuplicate(binCode, dto.getRowNo(), dto.getColNo(), dto.getLevelNo(), null);
        WarehouseBin entity = warehouseBinConverter.toNewEntity(dto, binCode);
        save(entity);
        return entity;
    }

    @Override
    public WarehouseBin update(Long id, WarehouseBinSaveDTO dto) {
        WarehouseBin existing = getById(id);
        String newBinCode = buildBinCode(dto.getRowNo(), dto.getColNo(), dto.getLevelNo());

        if (!newBinCode.equals(existing.getBinCode())) {
            long usageCount = countMaterialUsage(existing.getBinCode());
            if (usageCount > 0) {
                throw new BusinessException("该 Bin 位已被 " + usageCount + " 条物料台账引用，不能修改排/列/层");
            }
        }

        assertNotDuplicate(newBinCode, dto.getRowNo(), dto.getColNo(), dto.getLevelNo(), id);
        warehouseBinConverter.applySaveDto(existing, dto, newBinCode);
        updateById(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        WarehouseBin existing = getById(id);
        assertNotInUse(existing.getBinCode());
        removeById(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<WarehouseBin> listByQuery(WarehouseBinQueryDTO query) {
        return list(WarehouseBinQueryBuilder.build(query));
    }

    @Override
    public byte[] export(WarehouseBinQueryDTO query) throws IOException {
        return warehouseBinExportService.export(listByQuery(query));
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return warehouseBinExportService.exportTemplate();
    }

    private static String buildBinCode(int rowNo, int colNo, int levelNo) {
        return rowNo + "-" + colNo + "-" + levelNo;
    }

    private void assertNotDuplicate(String binCode, int rowNo, int colNo, int levelNo, Long excludeId) {
        LambdaQueryWrapper<WarehouseBin> codeWrapper = Wrappers.<WarehouseBin>lambdaQuery()
                .eq(WarehouseBin::getBinCode, binCode);
        if (excludeId != null) {
            codeWrapper.ne(WarehouseBin::getId, excludeId);
        }
        if (count(codeWrapper) > 0) {
            throw new BusinessException("Bin位编号已存在: " + binCode);
        }

        LambdaQueryWrapper<WarehouseBin> coordWrapper = Wrappers.<WarehouseBin>lambdaQuery()
                .eq(WarehouseBin::getRowNo, rowNo)
                .eq(WarehouseBin::getColNo, colNo)
                .eq(WarehouseBin::getLevelNo, levelNo);
        if (excludeId != null) {
            coordWrapper.ne(WarehouseBin::getId, excludeId);
        }
        if (count(coordWrapper) > 0) {
            throw new BusinessException("相同排/列/层的 Bin 位已存在");
        }
    }

    private void assertNotInUse(String binCode) {
        long count = countMaterialUsage(binCode);
        if (count > 0) {
            throw new BusinessException("该 Bin 位已被 " + count + " 条物料台账引用，无法删除");
        }
    }

    private long countMaterialUsage(String binCode) {
        return materialLedgerMapper.selectCount(
                Wrappers.<MaterialLedger>lambdaQuery().eq(MaterialLedger::getBinLocation, binCode)
        );
    }
}
