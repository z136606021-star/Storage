package com.storage.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.exception.BusinessException;
import com.storage.common.query.PageSupport;
import com.storage.infrastructure.file.service.FileStorageService;
import com.storage.warehouse.converter.WarehouseBomConverter;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.exception.WarehouseBomNotFoundException;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.query.WarehouseBomQueryBuilder;
import com.storage.warehouse.dto.BomCatalogItemVO;
import com.storage.warehouse.dto.BomFilterOptionsVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
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
public class WarehouseBomServiceImpl extends ServiceImpl<WarehouseBomMapper, WarehouseBom>
        implements WarehouseBomService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseBomConverter warehouseBomConverter;
    private final WarehouseBomExportService warehouseBomExportService;
    private final FileStorageService fileStorageService;

    @Override
    public PageResult<WarehouseBom> page(WarehouseBomQueryDTO query) {
        var result = page(
                PageSupport.page(query.getPage(), query.getPageSize()),
                WarehouseBomQueryBuilder.build(query)
        );
        enrichImageUrl(result.getRecords());
        return PageSupport.result(result);
    }

    @Override
    public WarehouseBom getById(Long id) {
        WarehouseBom bom = super.getById(id);
        if (bom == null) {
            throw new WarehouseBomNotFoundException(id);
        }
        enrichImageUrl(bom);
        return bom;
    }

    @Override
    public boolean existsByCatalogKey(String category, String genericName, String brand, String name, String model) {
        if (!StringUtils.hasText(category)
                || !StringUtils.hasText(genericName)
                || !StringUtils.hasText(name)
                || !StringUtils.hasText(model)) {
            return false;
        }
        return count(buildCatalogKeyWrapper(category, genericName, brand, name, model, null)) > 0;
    }

    @Override
    public void assertCatalogExists(String category, String genericName, String brand, String name, String model) {
        if (!existsByCatalogKey(category, genericName, brand, name, model)) {
            throw new BusinessException("\u7269\u6599\u6e05\u5355\u4e2d\u4e0d\u5b58\u5728: " + formatCatalogLabel(category, genericName, brand, name, model));
        }
    }

    @Override
    public List<BomCatalogItemVO> listCatalogSummaries() {
        return list(Wrappers.<WarehouseBom>lambdaQuery()
                        .orderByAsc(WarehouseBom::getCategory, WarehouseBom::getGenericName,
                                WarehouseBom::getBrand, WarehouseBom::getName, WarehouseBom::getModel))
                .stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());
    }

    @Override
    public WarehouseBom create(WarehouseBomSaveDTO dto) {
        assertNotDuplicate(dto, null);
        WarehouseBom entity = warehouseBomConverter.toNewEntity(dto);
        save(entity);
        enrichImageUrl(entity);
        return entity;
    }

    @Override
    public WarehouseBom update(Long id, WarehouseBomSaveDTO dto) {
        WarehouseBom existing = getById(id);
        assertNotDuplicate(dto, id);
        warehouseBomConverter.applySaveDto(existing, dto);
        updateById(existing);
        enrichImageUrl(existing);
        return existing;
    }

    @Override
    public void delete(Long id) {
        WarehouseBom existing = getById(id);
        assertNotInUse(existing);
        removeById(id);
    }

    @Override
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<WarehouseBom> listByQuery(WarehouseBomQueryDTO query) {
        return list(WarehouseBomQueryBuilder.build(query));
    }

    @Override
    public byte[] export(WarehouseBomQueryDTO query) throws IOException {
        return warehouseBomExportService.export(listByQuery(query));
    }

    @Override
    public byte[] exportTemplate() throws IOException {
        return warehouseBomExportService.exportTemplate();
    }

    @Override
    public BomFilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return new BomFilterOptionsVO(
                distinctValues(WarehouseBomQueryBuilder.withCategory(query), WarehouseBom::getCategory),
                distinctValues(WarehouseBomQueryBuilder.withCategoryAndGeneric(query), WarehouseBom::getGenericName),
                distinctValues(WarehouseBomQueryBuilder.withCategoryGenericAndBrand(query), WarehouseBom::getBrand)
        );
    }

    private BomCatalogItemVO toCatalogItem(WarehouseBom bom) {
        return new BomCatalogItemVO(
                bom.getId(),
                bom.getCategory(),
                bom.getGenericName(),
                bom.getBrand(),
                bom.getName(),
                bom.getModel(),
                formatCatalogLabel(bom.getCategory(), bom.getGenericName(), bom.getBrand(), bom.getName(), bom.getModel())
        );
    }

    private String formatCatalogLabel(String category, String genericName, String brand, String name) {
        return formatCatalogLabel(category, genericName, brand, name, null);
    }

    private String formatCatalogLabel(String category, String genericName, String brand, String name, String model) {
        String brandPart = StringUtils.hasText(brand) ? brand.trim() : "\u2014";
        String modelPart = StringUtils.hasText(model) ? model.trim() : "\u2014";
        return category.trim() + " / " + genericName.trim() + " / " + brandPart + " / " + name.trim() + " / " + modelPart;
    }

    private LambdaQueryWrapper<WarehouseBom> buildCatalogKeyWrapper(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            Long excludeId
    ) {
        LambdaQueryWrapper<WarehouseBom> wrapper = Wrappers.<WarehouseBom>lambdaQuery()
                .eq(WarehouseBom::getCategory, category.trim())
                .eq(WarehouseBom::getGenericName, genericName.trim())
                .eq(WarehouseBom::getName, name.trim())
                .eq(WarehouseBom::getModel, model.trim());

        if (StringUtils.hasText(brand)) {
            wrapper.eq(WarehouseBom::getBrand, brand.trim());
        } else {
            wrapper.and(w -> w.isNull(WarehouseBom::getBrand).or().eq(WarehouseBom::getBrand, ""));
        }

        if (excludeId != null) {
            wrapper.ne(WarehouseBom::getId, excludeId);
        }
        return wrapper;
    }

    private long countLedgerUsage(WarehouseBom bom) {
        LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.<MaterialLedger>lambdaQuery()
                .eq(MaterialLedger::getCategory, bom.getCategory())
                .eq(MaterialLedger::getGenericName, bom.getGenericName())
                .eq(MaterialLedger::getName, bom.getName())
                .eq(MaterialLedger::getModel, bom.getModel());

        if (StringUtils.hasText(bom.getBrand())) {
            wrapper.eq(MaterialLedger::getBrand, bom.getBrand());
        } else {
            wrapper.and(w -> w.isNull(MaterialLedger::getBrand).or().eq(MaterialLedger::getBrand, ""));
        }

        return materialLedgerMapper.selectCount(wrapper);
    }

    private void assertNotInUse(WarehouseBom bom) {
        long count = countLedgerUsage(bom);
        if (count > 0) {
            throw new BusinessException("\u8be5\u7269\u6599\u6e05\u5355\u9879\u5df2\u88ab " + count + " \u6761\u7269\u6599\u53f0\u8d26\u5f15\u7528\uff0c\u65e0\u6cd5\u5220\u9664");
        }
    }

    private List<String> distinctValues(
            LambdaQueryWrapper<WarehouseBom> wrapper,
            Function<WarehouseBom, String> extractor
    ) {
        wrapper.select(WarehouseBom::getCategory, WarehouseBom::getGenericName, WarehouseBom::getBrand);

        return list(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void assertNotDuplicate(WarehouseBomSaveDTO dto, Long excludeId) {
        if (count(
                buildCatalogKeyWrapper(dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName(), dto.getModel(), excludeId)
        ) > 0) {
            throw new BusinessException("相同品类/统称/品牌/名称/型号的物料清单项已存在");
        }
    }

    private void enrichImageUrl(WarehouseBom bom) {
        if (bom == null) {
            return;
        }
        bom.setImageUrl(fileStorageService.resolveAccessUrl(bom.getImageObjectKey()));
    }

    private void enrichImageUrl(List<WarehouseBom> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(this::enrichImageUrl);
    }
}
