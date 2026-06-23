package com.storage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.exception.MaterialLedgerNotFoundException;
import com.storage.mapper.MaterialLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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

    private static final String ALL = "全部";

    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialLedgerExportService materialLedgerExportService;
    private final MaterialLedgerImportService materialLedgerImportService;

    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();

        Page<MaterialLedger> result = materialLedgerMapper.selectPage(
                new Page<>(page, pageSize),
                buildQueryWrapper(query)
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

    public MaterialLedger create(MaterialSaveDTO dto) {
        MaterialLedger entity = toEntity(dto);
        entity.setStockQuantity(0);
        materialLedgerMapper.insert(entity);
        return entity;
    }

    public MaterialLedger update(Long id, MaterialSaveDTO dto) {
        MaterialLedger existing = getById(id);
        applySaveDto(existing, dto);
        materialLedgerMapper.updateById(existing);
        return existing;
    }

    public void delete(Long id) {
        getById(id);
        materialLedgerMapper.deleteById(id);
    }

    public void batchDelete(BatchDeleteDTO dto) {
        materialLedgerMapper.deleteByIds(dto.getIds());
    }

    public List<MaterialLedger> listByQuery(MaterialQueryDTO query) {
        return materialLedgerMapper.selectList(buildQueryWrapper(query));
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

    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return new FilterOptionsVO(
                distinctValues(Wrappers.lambdaQuery(), MaterialLedger::getCategory),
                distinctValues(withCategory(query), MaterialLedger::getGenericName),
                distinctValues(withCategoryAndGeneric(query), MaterialLedger::getBrand),
                distinctValues(withCategoryGenericAndBrand(query), MaterialLedger::getModel),
                distinctValues(withCategoryGenericAndBrand(query), MaterialLedger::getBinLocation)
        );
    }

    private LambdaQueryWrapper<MaterialLedger> withCategory(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();
        if (isFilterValue(query.getCategory())) {
            wrapper.eq(MaterialLedger::getCategory, query.getCategory());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<MaterialLedger> withCategoryAndGeneric(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<MaterialLedger> wrapper = withCategory(query);
        if (isFilterValue(query.getGenericName())) {
            wrapper.eq(MaterialLedger::getGenericName, query.getGenericName());
        }
        return wrapper;
    }

    private LambdaQueryWrapper<MaterialLedger> withCategoryGenericAndBrand(FilterLinkageQueryDTO query) {
        LambdaQueryWrapper<MaterialLedger> wrapper = withCategoryAndGeneric(query);
        if (isFilterValue(query.getBrand())) {
            wrapper.eq(MaterialLedger::getBrand, query.getBrand());
        }
        return wrapper;
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

    private LambdaQueryWrapper<MaterialLedger> buildQueryWrapper(MaterialQueryDTO query) {
        LambdaQueryWrapper<MaterialLedger> wrapper = Wrappers.lambdaQuery();

        if (!CollectionUtils.isEmpty(query.getIds())) {
            wrapper.in(MaterialLedger::getId, query.getIds());
        }

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
        return wrapper;
    }

    private MaterialLedger toEntity(MaterialSaveDTO dto) {
        MaterialLedger entity = new MaterialLedger();
        applySaveDto(entity, dto);
        return entity;
    }

    private void applySaveDto(MaterialLedger entity, MaterialSaveDTO dto) {
        entity.setCategory(dto.getCategory().trim());
        entity.setGenericName(dto.getGenericName().trim());
        entity.setBrand(trimToNull(dto.getBrand()));
        entity.setName(dto.getName().trim());
        entity.setModel(dto.getModel().trim());
        entity.setBinLocation(dto.getBinLocation().trim());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setRemark(trimToNull(dto.getRemark()));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isFilterValue(String value) {
        return StringUtils.hasText(value) && !ALL.equals(value);
    }
}
