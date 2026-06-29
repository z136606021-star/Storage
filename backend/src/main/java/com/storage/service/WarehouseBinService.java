package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.converter.WarehouseBinConverter;
import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.WarehouseBinQueryDTO;
import com.storage.dto.WarehouseBinSaveDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.entity.WarehouseBin;
import com.storage.exception.BusinessException;
import com.storage.exception.WarehouseBinNotFoundException;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.mapper.WarehouseBinMapper;
import com.storage.query.WarehouseBinQueryBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseBinService {

    private final WarehouseBinMapper warehouseBinMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseBinConverter warehouseBinConverter;
    private final WarehouseBinExportService warehouseBinExportService;

    public PageResult<WarehouseBin> page(WarehouseBinQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<WarehouseBin> result = warehouseBinMapper.selectPage(
                new Page<>(page, pageSize),
                WarehouseBinQueryBuilder.build(query)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public WarehouseBin getById(Long id) {
        WarehouseBin bin = warehouseBinMapper.selectById(id);
        if (bin == null) {
            throw new WarehouseBinNotFoundException(id);
        }
        return bin;
    }

    public List<String> listAllCodes() {
        return warehouseBinMapper.selectList(
                        Wrappers.<WarehouseBin>lambdaQuery()
                                .select(WarehouseBin::getBinCode)
                                .orderByAsc(WarehouseBin::getBinCode)
                ).stream()
                .map(WarehouseBin::getBinCode)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    public boolean existsByBinCode(String binCode) {
        if (!StringUtils.hasText(binCode)) {
            return false;
        }
        return warehouseBinMapper.selectCount(
                Wrappers.<WarehouseBin>lambdaQuery().eq(WarehouseBin::getBinCode, binCode.trim())
        ) > 0;
    }

    public void assertBinExists(String binCode) {
        if (!existsByBinCode(binCode)) {
            throw new BusinessException("Bin位不存在: " + binCode);
        }
    }

    public WarehouseBin create(WarehouseBinSaveDTO dto) {
        String binCode = buildBinCode(dto.getRowNo(), dto.getColNo(), dto.getLevelNo());
        assertNotDuplicate(binCode, dto.getRowNo(), dto.getColNo(), dto.getLevelNo(), null);
        WarehouseBin entity = warehouseBinConverter.toNewEntity(dto, binCode);
        warehouseBinMapper.insert(entity);
        return entity;
    }

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
        warehouseBinMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        WarehouseBin existing = getById(id);
        assertNotInUse(existing.getBinCode());
        warehouseBinMapper.deleteById(id);
    }

    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    public List<WarehouseBin> listByQuery(WarehouseBinQueryDTO query) {
        return warehouseBinMapper.selectList(WarehouseBinQueryBuilder.build(query));
    }

    public byte[] export(WarehouseBinQueryDTO query) throws IOException {
        return warehouseBinExportService.export(listByQuery(query));
    }

    public byte[] exportTemplate() throws IOException {
        return warehouseBinExportService.exportTemplate();
    }

    public static String buildBinCode(int rowNo, int colNo, int levelNo) {
        return rowNo + "-" + colNo + "-" + levelNo;
    }

    private void assertNotDuplicate(String binCode, int rowNo, int colNo, int levelNo, Long excludeId) {
        LambdaQueryWrapper<WarehouseBin> codeWrapper = Wrappers.<WarehouseBin>lambdaQuery()
                .eq(WarehouseBin::getBinCode, binCode);
        if (excludeId != null) {
            codeWrapper.ne(WarehouseBin::getId, excludeId);
        }
        if (warehouseBinMapper.selectCount(codeWrapper) > 0) {
            throw new BusinessException("Bin位编号已存在: " + binCode);
        }

        LambdaQueryWrapper<WarehouseBin> coordWrapper = Wrappers.<WarehouseBin>lambdaQuery()
                .eq(WarehouseBin::getRowNo, rowNo)
                .eq(WarehouseBin::getColNo, colNo)
                .eq(WarehouseBin::getLevelNo, levelNo);
        if (excludeId != null) {
            coordWrapper.ne(WarehouseBin::getId, excludeId);
        }
        if (warehouseBinMapper.selectCount(coordWrapper) > 0) {
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
