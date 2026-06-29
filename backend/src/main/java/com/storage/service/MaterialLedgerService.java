package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storage.converter.MaterialLedgerConverter;
import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.BomCatalogItemVO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.exception.BusinessException;
import com.storage.exception.MaterialLedgerNotFoundException;
import com.storage.mapper.MaterialIoRecordMapper;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.query.MaterialLedgerQueryBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialLedgerService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialIoRecordMapper materialIoRecordMapper;
    private final MaterialLedgerExportService materialLedgerExportService;
    private final MaterialLedgerImportService materialLedgerImportService;
    private final MaterialLedgerConverter materialLedgerConverter;
    private final WarehouseBinService warehouseBinService;
    private final WarehouseBomService warehouseBomService;

    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<MaterialLedger> result = materialLedgerMapper.selectPage(
                new Page<>(page, pageSize),
                MaterialLedgerQueryBuilder.build(query)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public MaterialLedger getById(Long id) {
        MaterialLedger material = materialLedgerMapper.selectById(id);
        if (material == null) {
            throw new MaterialLedgerNotFoundException(id);
        }
        return material;
    }

    public MaterialLedger findByMaterialKey(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            String binLocation
    ) {
        return materialLedgerMapper.selectOne(MaterialLedgerQueryBuilder.byNaturalKey(
                category, genericName, brand, name, model, binLocation));
    }

    public MaterialLedger create(MaterialSaveDTO dto) {
        warehouseBinService.assertBinExists(dto.getBinLocation());
        warehouseBomService.assertCatalogExists(
                dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName());
        MaterialLedger entity = materialLedgerConverter.toNewEntity(dto);
        materialLedgerMapper.insert(entity);
        return entity;
    }

    public MaterialLedger update(Long id, MaterialSaveDTO dto) {
        warehouseBinService.assertBinExists(dto.getBinLocation());
        warehouseBomService.assertCatalogExists(
                dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName());
        MaterialLedger existing = getById(id);
        materialLedgerConverter.applySaveDto(existing, dto);
        materialLedgerMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        getById(id);
        assertNoIoRecords(id);
        materialLedgerMapper.deleteById(id);
    }

    public void batchDelete(BatchDeleteDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            return;
        }
        for (Long id : dto.getIds()) {
            getById(id);
            assertNoIoRecords(id);
        }
        materialLedgerMapper.deleteByIds(dto.getIds());
    }

    private void assertNoIoRecords(Long ledgerId) {
        long count = materialIoRecordMapper.countByMaterialLedgerId(ledgerId);
        if (count > 0) {
            throw new BusinessException("该物料台账已被 " + count + " 条出入库记录引用，无法删除");
        }
    }

    public List<MaterialLedger> listByQuery(MaterialQueryDTO query) {
        return materialLedgerMapper.selectList(MaterialLedgerQueryBuilder.build(query));
    }

    public byte[] export(MaterialQueryDTO query) throws IOException {
        return materialLedgerExportService.export(listByQuery(query));
    }

    public byte[] exportTemplate() throws IOException {
        return materialLedgerExportService.exportTemplate();
    }

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return materialLedgerImportService.importExcel(file);
    }

    public List<String> listBinCodes() {
        return warehouseBinService.listAllCodes();
    }

    public List<BomCatalogItemVO> listBomCatalog() {
        return warehouseBomService.listCatalogSummaries();
    }

    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return new FilterOptionsVO(
                distinctValues(MaterialLedgerQueryBuilder.withCategory(query), MaterialLedger::getCategory),
                distinctValues(MaterialLedgerQueryBuilder.withCategoryAndGeneric(query), MaterialLedger::getGenericName),
                distinctValues(MaterialLedgerQueryBuilder.withCategoryGenericAndBrand(query), MaterialLedger::getBrand),
                distinctValues(MaterialLedgerQueryBuilder.withCategoryGenericAndBrand(query), MaterialLedger::getModel),
                warehouseBinService.listAllCodes()
        );
    }

    private List<String> distinctValues(
            LambdaQueryWrapper<MaterialLedger> wrapper,
            Function<MaterialLedger, String> extractor
    ) {
        wrapper.select(MaterialLedger::getCategory, MaterialLedger::getGenericName, MaterialLedger::getBrand,
                MaterialLedger::getModel, MaterialLedger::getBinLocation);

        return materialLedgerMapper.selectList(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
