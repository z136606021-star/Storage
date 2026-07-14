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
import com.storage.warehouse.dto.BomCatalogItemVO;
import com.storage.warehouse.dto.BomFilterOptionsVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.entity.WarehouseBomImage;
import com.storage.warehouse.exception.WarehouseBomNotFoundException;
import com.storage.warehouse.mapper.MaterialLedgerMapper;
import com.storage.warehouse.mapper.WarehouseBomImageMapper;
import com.storage.warehouse.mapper.WarehouseBomMapper;
import com.storage.warehouse.query.WarehouseBomQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseBomServiceImpl extends ServiceImpl<WarehouseBomMapper, WarehouseBom>
        implements WarehouseBomService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final WarehouseBomImageMapper warehouseBomImageMapper;
    private final WarehouseBomConverter warehouseBomConverter;
    private final WarehouseBomExportService warehouseBomExportService;
    private final FileStorageService fileStorageService;

    @Override
    public PageResult<WarehouseBom> page(WarehouseBomQueryDTO query) {
        var result = page(
                PageSupport.page(query.getPage(), query.getPageSize()),
                WarehouseBomQueryBuilder.build(query)
        );
        enrichImages(result.getRecords());
        return PageSupport.result(result);
    }

    @Override
    public WarehouseBom getById(Long id) {
        WarehouseBom bom = super.getById(id);
        if (bom == null) {
            throw new WarehouseBomNotFoundException(id);
        }
        enrichImages(bom);
        return bom;
    }

    @Override
    public boolean existsByCatalogKey(String category, String genericName, String brand, String name, String model) {
        if (!StringUtils.hasText(category)
                || !StringUtils.hasText(genericName)
                || !StringUtils.hasText(name)) {
            return false;
        }
        WarehouseBom bom = findByNaturalKey(category, genericName, brand, name);
        if (bom == null) {
            return false;
        }
        if (!StringUtils.hasText(model)) {
            return true;
        }
        return Objects.equals(normalizeModel(bom.getModel()), normalizeModel(model));
    }

    @Override
    public void assertCatalogExists(String category, String genericName, String brand, String name, String model) {
        WarehouseBom bom = findByNaturalKey(category, genericName, brand, name);
        if (bom == null) {
            throw new BusinessException("物料清单中不存在: " + formatCatalogLabel(category, genericName, brand, name, model));
        }
        if (StringUtils.hasText(model) && !Objects.equals(normalizeModel(bom.getModel()), normalizeModel(model))) {
            throw new BusinessException("物料清单规格不匹配: " + formatCatalogLabel(category, genericName, brand, name, model));
        }
    }

    @Override
    public List<BomCatalogItemVO> listCatalogSummaries() {
        return list(Wrappers.<WarehouseBom>lambdaQuery()
                        .orderByDesc(WarehouseBom::getUpdatedAt, WarehouseBom::getId))
                .stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarehouseBom create(WarehouseBomSaveDTO dto) {
        assertNotDuplicate(dto, null);
        WarehouseBom entity = warehouseBomConverter.toNewEntity(dto);
        entity.setImageObjectKey(resolvePrimaryImageObjectKey(dto));
        save(entity);
        replaceImages(entity.getId(), resolveImageObjectKeys(dto));
        enrichImages(entity);
        return entity;
    }

    @Override
    @Transactional
    public WarehouseBom update(Long id, WarehouseBomSaveDTO dto) {
        WarehouseBom existing = super.getById(id);
        if (existing == null) {
            throw new WarehouseBomNotFoundException(id);
        }
        assertNotDuplicate(dto, id);
        warehouseBomConverter.applySaveDto(existing, dto);
        existing.setImageObjectKey(resolvePrimaryImageObjectKey(dto));
        updateById(existing);
        replaceImages(id, resolveImageObjectKeys(dto));
        enrichImages(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WarehouseBom existing = getById(id);
        assertNotInUse(existing);
        warehouseBomImageMapper.delete(Wrappers.<WarehouseBomImage>lambdaQuery()
                .eq(WarehouseBomImage::getBomId, id));
        removeById(id);
    }

    @Override
    @Transactional
    public void batchDelete(BatchDeleteDTO dto) {
        for (Long id : dto.getIds()) {
            delete(id);
        }
    }

    @Override
    public List<WarehouseBom> listByQuery(WarehouseBomQueryDTO query) {
        List<WarehouseBom> records = list(WarehouseBomQueryBuilder.build(query));
        enrichImages(records);
        return records;
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

    private String formatCatalogLabel(String category, String genericName, String brand, String name, String model) {
        String brandPart = StringUtils.hasText(brand) ? brand.trim() : "—";
        String modelPart = StringUtils.hasText(model) ? model.trim() : "—";
        return category.trim() + " / " + genericName.trim() + " / " + brandPart + " / " + name.trim() + " / " + modelPart;
    }

    private WarehouseBom findByNaturalKey(String category, String genericName, String brand, String name) {
        return getOne(buildNaturalKeyWrapper(category, genericName, brand, name, null));
    }

    private LambdaQueryWrapper<WarehouseBom> buildNaturalKeyWrapper(
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

        if (StringUtils.hasText(bom.getModel())) {
            wrapper.eq(MaterialLedger::getModel, bom.getModel());
        }
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

        return list(wrapper).stream()
                .map(extractor)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private void assertNotDuplicate(WarehouseBomSaveDTO dto, Long excludeId) {
        if (count(buildNaturalKeyWrapper(dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName(), excludeId)) > 0) {
            throw new BusinessException("相同品类/统称/品牌/名称的物料清单项已存在");
        }
    }

    private List<String> resolveImageObjectKeys(WarehouseBomSaveDTO dto) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(dto.getImageObjectKeys())) {
            dto.getImageObjectKeys().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(keys::add);
        }
        if (StringUtils.hasText(dto.getImageObjectKey())) {
            keys.add(dto.getImageObjectKey().trim());
        }
        List<String> objectKeys = new ArrayList<>(keys);
        for (String objectKey : objectKeys) {
            fileStorageService.assertImageFile(objectKey);
        }
        return objectKeys;
    }

    private String resolvePrimaryImageObjectKey(WarehouseBomSaveDTO dto) {
        List<String> keys = resolveImageObjectKeys(dto);
        return keys.isEmpty() ? null : keys.get(0);
    }

    private void replaceImages(Long bomId, List<String> objectKeys) {
        warehouseBomImageMapper.delete(Wrappers.<WarehouseBomImage>lambdaQuery()
                .eq(WarehouseBomImage::getBomId, bomId));
        if (CollectionUtils.isEmpty(objectKeys)) {
            return;
        }
        for (int i = 0; i < objectKeys.size(); i++) {
            WarehouseBomImage image = new WarehouseBomImage();
            image.setBomId(bomId);
            image.setObjectKey(objectKeys.get(i));
            image.setSortOrder(i);
            warehouseBomImageMapper.insert(image);
        }
    }

    private void enrichImages(WarehouseBom bom) {
        if (bom == null) {
            return;
        }
        enrichImages(List.of(bom));
    }

    private void enrichImages(List<WarehouseBom> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> bomIds = records.stream().map(WarehouseBom::getId).toList();
        List<WarehouseBomImage> images = warehouseBomImageMapper.selectList(
                Wrappers.<WarehouseBomImage>lambdaQuery()
                        .in(WarehouseBomImage::getBomId, bomIds)
                        .orderByAsc(WarehouseBomImage::getSortOrder, WarehouseBomImage::getId)
        );
        Map<Long, List<WarehouseBomImage>> imagesByBomId = images.stream()
                .collect(Collectors.groupingBy(WarehouseBomImage::getBomId));

        for (WarehouseBom bom : records) {
            List<WarehouseBomImage> bomImages = imagesByBomId.getOrDefault(bom.getId(), List.of());
            List<String> objectKeys = bomImages.stream()
                    .map(WarehouseBomImage::getObjectKey)
                    .filter(StringUtils::hasText)
                    .toList();
            if (objectKeys.isEmpty() && StringUtils.hasText(bom.getImageObjectKey())) {
                objectKeys = List.of(bom.getImageObjectKey());
            }
            bom.setImageObjectKeys(objectKeys);
            bom.setImageUrls(objectKeys.stream()
                    .map(fileStorageService::resolveAccessUrl)
                    .toList());
            bom.setImageObjectKey(objectKeys.isEmpty() ? null : objectKeys.get(0));
            bom.setImageUrl(objectKeys.isEmpty() ? null : fileStorageService.resolveAccessUrl(objectKeys.get(0)));
        }
    }

    private String normalizeModel(String model) {
        return StringUtils.hasText(model) ? model.trim() : "";
    }
}
