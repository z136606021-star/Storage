package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.storage.converter.WarehouseBomConverter;
import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.BomCatalogItemVO;
import com.storage.dto.BomFilterOptionsVO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.PageResult;
import com.storage.dto.WarehouseBomQueryDTO;
import com.storage.dto.WarehouseBomSaveDTO;
import com.storage.entity.MaterialLedger;
import com.storage.entity.WarehouseBom;
import com.storage.exception.BusinessException;
import com.storage.exception.WarehouseBomNotFoundException;
import com.storage.mapper.MaterialLedgerMapper;
import com.storage.mapper.WarehouseBomMapper;
import com.storage.query.WarehouseBomQueryBuilder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
public class WarehouseBomService {

    private final WarehouseBomMapper warehouseBomMapper;
    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseBomConverter warehouseBomConverter;
    private final WarehouseBomExportService warehouseBomExportService;
    private final FileStorageService fileStorageService;

    public PageResult<WarehouseBom> page(WarehouseBomQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<WarehouseBom> result = warehouseBomMapper.selectPage(
                new Page<>(page, pageSize),
                WarehouseBomQueryBuilder.build(query)
        );
        enrichImageUrl(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    public WarehouseBom getById(Long id) {
        WarehouseBom bom = warehouseBomMapper.selectById(id);
        if (bom == null) {
            throw new WarehouseBomNotFoundException(id);
        }
        enrichImageUrl(bom);
        return bom;
    }

    public boolean existsByCatalogKey(String category, String genericName, String brand, String name) {
        if (!StringUtils.hasText(category) || !StringUtils.hasText(genericName) || !StringUtils.hasText(name)) {
            return false;
        }
        return warehouseBomMapper.selectCount(buildCatalogKeyWrapper(category, genericName, brand, name, null)) > 0;
    }

    public void assertCatalogExists(String category, String genericName, String brand, String name) {
        if (!existsByCatalogKey(category, genericName, brand, name)) {
            throw new BusinessException("物料清单中不存在: " + formatCatalogLabel(category, genericName, brand, name));
        }
    }

    public List<BomCatalogItemVO> listCatalogSummaries() {
        return warehouseBomMapper.selectList(
                        Wrappers.<WarehouseBom>lambdaQuery()
                                .orderByAsc(WarehouseBom::getCategory, WarehouseBom::getGenericName,
                                        WarehouseBom::getBrand, WarehouseBom::getName)
                ).stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());
    }

    public WarehouseBom create(WarehouseBomSaveDTO dto) {
        assertNotDuplicate(dto, null);
        WarehouseBom entity = warehouseBomConverter.toNewEntity(dto);
        warehouseBomMapper.insert(entity);
        enrichImageUrl(entity);
        return entity;
    }

    public WarehouseBom update(Long id, WarehouseBomSaveDTO dto) {
        WarehouseBom existing = getById(id);
        assertNotDuplicate(dto, id);
        warehouseBomConverter.applySaveDto(existing, dto);
        warehouseBomMapper.updateById(existing);
        enrichImageUrl(existing);
        return existing;
    }

    public void delete(Long id) {
        WarehouseBom existing = getById(id);
        assertNotInUse(existing);
        warehouseBomMapper.deleteById(id);
    }

    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    public List<WarehouseBom> listByQuery(WarehouseBomQueryDTO query) {
        return warehouseBomMapper.selectList(WarehouseBomQueryBuilder.build(query));
    }

    public byte[] export(WarehouseBomQueryDTO query) throws IOException {
        return warehouseBomExportService.export(listByQuery(query));
    }

    public byte[] exportTemplate() throws IOException {
        return warehouseBomExportService.exportTemplate();
    }

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
                formatCatalogLabel(bom.getCategory(), bom.getGenericName(), bom.getBrand(), bom.getName())
        );
    }

    private String formatCatalogLabel(String category, String genericName, String brand, String name) {
        String brandPart = StringUtils.hasText(brand) ? brand.trim() : "—";
        return category.trim() + " / " + genericName.trim() + " / " + brandPart + " / " + name.trim();
    }

    private LambdaQueryWrapper<WarehouseBom> buildCatalogKeyWrapper(
            String category,
            String genericName,
            String brand,
            String name,
            Long excludeId
    ) {
        LambdaQueryWrapper<WarehouseBom> wrapper = Wrappers.<WarehouseBom>lambdaQuery()
                .eq(WarehouseBom::getCategory, category.trim())
                .eq(WarehouseBom::getGenericName, genericName.trim())
                .eq(WarehouseBom::getName, name.trim());

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
                .eq(MaterialLedger::getName, bom.getName());

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
            throw new BusinessException("该物料清单项已被 " + count + " 条物料台账引用，无法删除");
        }
    }

    private List<String> distinctValues(
            LambdaQueryWrapper<WarehouseBom> wrapper,
            Function<WarehouseBom, String> extractor
    ) {
        wrapper.select(WarehouseBom::getCategory, WarehouseBom::getGenericName, WarehouseBom::getBrand);

        return warehouseBomMapper.selectList(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void assertNotDuplicate(WarehouseBomSaveDTO dto, Long excludeId) {
        if (warehouseBomMapper.selectCount(
                buildCatalogKeyWrapper(dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName(), excludeId)
        ) > 0) {
            throw new BusinessException("相同品类/统称/品牌/名称的物料清单项已存在");
        }
    }

    private void enrichImageUrl(WarehouseBom bom) {
        if (bom == null) {
            return;
        }
        bom.setImageUrl(fileStorageService.resolvePresignedUrl(bom.getImageObjectKey()));
    }

    private void enrichImageUrl(List<WarehouseBom> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(this::enrichImageUrl);
    }
}
