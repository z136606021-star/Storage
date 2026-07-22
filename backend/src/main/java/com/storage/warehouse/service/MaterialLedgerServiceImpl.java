package com.storage.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.query.PageSupport;
import com.storage.warehouse.converter.MaterialLedgerConverter;
import com.storage.warehouse.dto.BomCatalogItemVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.dto.MaterialQueryDTO;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.exception.MaterialLedgerNotFoundException;
import com.storage.warehouse.excel.MaterialLedgerExportRow;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.query.MaterialLedgerQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialLedgerServiceImpl extends ServiceImpl<MaterialLedgerMapper, MaterialLedger>
        implements MaterialLedgerService {

    private final MaterialLedgerConverter materialLedgerConverter;
    private final WarehouseBinService warehouseBinService;
    private final WarehouseBomService warehouseBomService;

    @Override
    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        var result = page(
                PageSupport.page(query.getPage(), query.getPageSize()),
                MaterialLedgerQueryBuilder.build(query)
        );
        return PageSupport.result(result);
    }

    @Override
    public MaterialLedger getById(Serializable id) {
        MaterialLedger material = super.getById(id);
        if (material == null) {
            Long materialId = id instanceof Number number ? number.longValue() : null;
            throw new MaterialLedgerNotFoundException(materialId);
        }
        return material;
    }

    @Override
    public MaterialLedger findByMaterialKey(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            String binLocation
    ) {
        return getOne(MaterialLedgerQueryBuilder.byNaturalKey(category, genericName, brand, name, model, binLocation));
    }

    @Override
    public MaterialLedger create(MaterialSaveDTO dto) {
        warehouseBinService.assertBinExists(dto.getBinLocation());
        warehouseBomService.assertCatalogExists(
                dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName());
        MaterialLedger entity = materialLedgerConverter.toNewEntity(dto);
        save(entity);
        return entity;
    }

    @Override
    public List<MaterialLedger> listByQuery(MaterialQueryDTO query) {
        return list(MaterialLedgerQueryBuilder.build(query));
    }

    @Override
    public byte[] export(MaterialQueryDTO query) throws IOException {
        return exportRecords(listByQuery(query));
    }

    @Override
    public List<String> listBinCodes() {
        return warehouseBinService.listAllCodes();
    }

    @Override
    public List<BomCatalogItemVO> listBomCatalog() {
        return warehouseBomService.listCatalogSummaries();
    }

    @Override
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

        return list(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    static byte[] exportRecords(List<MaterialLedger> records) throws IOException {
        List<MaterialLedgerExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (MaterialLedger record : records) {
            MaterialLedgerExportRow row = new MaterialLedgerExportRow();
            row.setIndex(rowIndex++);
            row.setCategory(record.getCategory());
            row.setGenericName(record.getGenericName());
            row.setBrand(record.getBrand());
            row.setName(record.getName());
            row.setBinLocation(record.getBinLocation());
            row.setStockQuantity(record.getStockQuantity());
            row.setUnitPrice(formatUnitPrice(record.getUnitPrice()));
            row.setRemark(record.getRemark());
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("物料台账", MaterialLedgerExportRow.class, rows);
    }

    private static String formatUnitPrice(BigDecimal unitPrice) {
        return unitPrice == null ? "" : unitPrice.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
