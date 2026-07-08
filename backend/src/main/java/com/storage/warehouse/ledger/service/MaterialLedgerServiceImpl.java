package com.storage.warehouse.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.query.PageSupport;
import com.storage.warehouse.bom.service.WarehouseBomService;
import com.storage.warehouse.bin.service.WarehouseBinService;
import com.storage.warehouse.ledger.converter.MaterialLedgerConverter;
import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.exception.MaterialLedgerNotFoundException;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import com.storage.warehouse.ledger.query.MaterialLedgerQueryBuilder;
import com.storage.warehouse.shared.dto.BomCatalogItemVO;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialLedgerServiceImpl implements MaterialLedgerService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialLedgerExportService materialLedgerExportService;
    private final MaterialLedgerConverter materialLedgerConverter;
    private final WarehouseBinService warehouseBinService;
    private final WarehouseBomService warehouseBomService;

    @Override
    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        var result = materialLedgerMapper.selectPage(
                PageSupport.page(query.getPage(), query.getPageSize()),
                MaterialLedgerQueryBuilder.build(query)
        );
        return PageSupport.result(result);
    }

    @Override
    public MaterialLedger getById(Long id) {
        MaterialLedger material = materialLedgerMapper.selectById(id);
        if (material == null) {
            throw new MaterialLedgerNotFoundException(id);
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
        return materialLedgerMapper.selectOne(MaterialLedgerQueryBuilder.byNaturalKey(
                category, genericName, brand, name, model, binLocation));
    }

    @Override
    public MaterialLedger create(MaterialSaveDTO dto) {
        warehouseBinService.assertBinExists(dto.getBinLocation());
        warehouseBomService.assertCatalogExists(
                dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName(), dto.getModel());
        MaterialLedger entity = materialLedgerConverter.toNewEntity(dto);
        materialLedgerMapper.insert(entity);
        return entity;
    }

    @Override
    public List<MaterialLedger> listByQuery(MaterialQueryDTO query) {
        return materialLedgerMapper.selectList(MaterialLedgerQueryBuilder.build(query));
    }

    @Override
    public byte[] export(MaterialQueryDTO query) throws IOException {
        return materialLedgerExportService.export(listByQuery(query));
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

        return materialLedgerMapper.selectList(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
