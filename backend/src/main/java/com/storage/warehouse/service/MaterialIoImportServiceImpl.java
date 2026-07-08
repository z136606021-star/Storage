package com.storage.warehouse.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.service.WarehouseBinService;
import com.storage.warehouse.service.WarehouseBomService;
import com.storage.warehouse.dto.MaterialIoSaveDTO;
import com.storage.warehouse.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.query.MaterialIoQueryBuilder;
import com.storage.warehouse.dto.MaterialSaveDTO;
import com.storage.warehouse.entity.MaterialLedger;
import com.storage.warehouse.service.MaterialLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MaterialIoImportServiceImpl implements MaterialIoImportService {

    private static final String DUPLICATE_MATERIAL_MESSAGE = "同一批次不能包含重复物料";
    private static final DateTimeFormatter OPERATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MaterialIoService materialIoService;
    private final MaterialLedgerService materialLedgerService;
    private final MaterialStockMutationService materialStockMutationService;
    private final WarehouseBinService warehouseBinService;
    private final WarehouseBomService warehouseBomService;

    @Transactional
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        ImportResultVO result = new ImportResultVO();
        AutoPoiExcelTemplate.ParsedRows<ValidImportRow> parsedRows = AutoPoiExcelTemplate.parseRows(
                file,
                MaterialIoImportTemplateRow.class,
                this::isEmptyRow,
                this::parseValidRow
        );
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>(parsedRows.errors());
        List<ValidImportRow> validRows = parsedRows.rows().stream()
                .map(AutoPoiExcelTemplate.ParsedRow::value)
                .toList();

        if (!errors.isEmpty()) {
            return buildFailureResult(errors);
        }

        if (validRows.isEmpty()) {
            throw new ImportFormatException("Excel 中没有有效数据行");
        }

        errors.addAll(collectDuplicateMaterialErrors(validRows));
        if (!errors.isEmpty()) {
            return buildFailureResult(errors);
        }

        errors.addAll(collectUnresolvableMaterialErrors(validRows));
        if (!errors.isEmpty()) {
            return buildFailureResult(errors);
        }

        resolveLedgerIds(validRows);

        List<MaterialIoSaveDTO> validDtos = validRows.stream().map(ValidImportRow::dto).toList();
        List<Long> ledgerIds = validDtos.stream()
                .map(MaterialIoSaveDTO::getMaterialLedgerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MaterialLedger> ledgersById = new HashMap<>();
        for (Long ledgerId : ledgerIds) {
            MaterialLedger ledger = materialLedgerService.getById(ledgerId);
            if (ledger != null) {
                ledgersById.put(ledgerId, ledger);
            }
        }

        List<MaterialStockMutationService.ImportStockSimulationRow> simulationRows = validRows.stream()
                .map(row -> new MaterialStockMutationService.ImportStockSimulationRow(row.excelRow(), row.dto()))
                .toList();
        errors.addAll(materialStockMutationService.collectImportStockErrors(simulationRows, ledgersById));
        if (!errors.isEmpty()) {
            return buildFailureResult(errors);
        }

        int imported = materialIoService.importBatch(validDtos);
        result.setSuccessCount(imported);
        result.setErrors(errors);
        return result;
    }

    private ValidImportRow parseValidRow(int excelRow, MaterialIoImportTemplateRow row) {
        MaterialIoSaveDTO dto = parseRow(row);
        validateDto(dto);
        return new ValidImportRow(excelRow, dto);
    }

    private List<ImportResultVO.ImportErrorVO> collectDuplicateMaterialErrors(List<ValidImportRow> validRows) {
        Map<MaterialKey, List<Integer>> rowsByMaterial = new HashMap<>();
        for (ValidImportRow row : validRows) {
            rowsByMaterial.computeIfAbsent(MaterialKey.from(row.dto()), ignored -> new ArrayList<>())
                    .add(row.excelRow());
        }

        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (List<Integer> rows : rowsByMaterial.values()) {
            if (rows.size() <= 1) {
                continue;
            }
            for (Integer excelRow : rows) {
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, DUPLICATE_MATERIAL_MESSAGE));
            }
        }
        return errors;
    }

    private List<ImportResultVO.ImportErrorVO> collectUnresolvableMaterialErrors(List<ValidImportRow> validRows) {
        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (ValidImportRow row : validRows) {
            MaterialIoSaveDTO dto = row.dto();
            try {
                if (MaterialIoQueryBuilder.isInbound(dto.getIoType())) {
                    assertInboundConfigExists(dto);
                } else if (findLedger(dto) == null) {
                    errors.add(new ImportResultVO.ImportErrorVO(row.excelRow(), "未找到匹配的物料台账记录"));
                }
            } catch (RuntimeException ex) {
                errors.add(new ImportResultVO.ImportErrorVO(row.excelRow(), ex.getMessage()));
            }
        }
        return errors;
    }

    private void resolveLedgerIds(List<ValidImportRow> validRows) {
        for (ValidImportRow row : validRows) {
            MaterialIoSaveDTO dto = row.dto();
            MaterialLedger ledger = findLedger(dto);
            if (ledger == null && MaterialIoQueryBuilder.isInbound(dto.getIoType())) {
                ledger = materialLedgerService.create(toMaterialSaveDto(dto));
            }
            dto.setMaterialLedgerId(ledger.getId());
        }
    }

    private void assertInboundConfigExists(MaterialIoSaveDTO dto) {
        warehouseBinService.assertBinExists(dto.getBinLocation());
        warehouseBomService.assertCatalogExists(
                dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName(), dto.getModel());
    }

    private MaterialLedger findLedger(MaterialIoSaveDTO dto) {
        return materialLedgerService.findByMaterialKey(
                dto.getCategory(),
                dto.getGenericName(),
                dto.getBrand(),
                dto.getName(),
                dto.getModel(),
                dto.getBinLocation()
        );
    }

    private MaterialSaveDTO toMaterialSaveDto(MaterialIoSaveDTO dto) {
        MaterialSaveDTO material = new MaterialSaveDTO();
        material.setCategory(dto.getCategory());
        material.setGenericName(dto.getGenericName());
        material.setBrand(dto.getBrand());
        material.setName(dto.getName());
        material.setModel(dto.getModel());
        material.setBinLocation(dto.getBinLocation());
        return material;
    }

    private ImportResultVO buildFailureResult(List<ImportResultVO.ImportErrorVO> errors) {
        ImportResultVO result = new ImportResultVO();
        result.setFailCount(errors.size());
        result.setSuccessCount(0);
        result.setErrors(errors);
        return result;
    }

    private MaterialIoSaveDTO parseRow(MaterialIoImportTemplateRow row) {
        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();
        dto.setCategory(row.getCategory());
        dto.setGenericName(row.getGenericName());
        dto.setBrand(row.getBrand());
        dto.setName(row.getName());
        dto.setModel(row.getModel());
        dto.setBinLocation(row.getBinLocation());
        dto.setQuantity(parseQuantity(row.getQuantity()));
        String purposeRaw = row.getPurpose();
        dto.setPurpose(MaterialIoPurpose.normalizePurpose(purposeRaw));
        dto.setRemark(row.getRemark());
        dto.setIoType(MaterialIoQueryBuilder.normalizeIoType(row.getIoType()));
        dto.setOperatedAt(parseOperatedAt(row.getOperatedAt()));
        return dto;
    }

    private void validateDto(MaterialIoSaveDTO dto) {
        if (!StringUtils.hasText(dto.getCategory())) {
            throw new IllegalArgumentException("品类不能为空");
        }
        if (!StringUtils.hasText(dto.getGenericName())) {
            throw new IllegalArgumentException("统称不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (!StringUtils.hasText(dto.getModel())) {
            throw new IllegalArgumentException("型号不能为空");
        }
        if (!StringUtils.hasText(dto.getBinLocation())) {
            throw new IllegalArgumentException("Bin位不能为空");
        }
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        MaterialIoQueryBuilder.assertValidIoType(dto.getIoType());
        MaterialIoPurpose.validatePurposeForIoType(dto.getIoType(), dto.getPurpose());
    }

    private LocalDateTime parseOperatedAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), OPERATED_AT_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("操作时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private Integer parseQuantity(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            int quantity = Integer.parseInt(value.trim());
            if (quantity < 1) {
                throw new IllegalArgumentException("数量必须大于 0");
            }
            return quantity;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("数量格式不正确");
        }
    }

    private boolean isEmptyRow(MaterialIoImportTemplateRow row) {
        return !StringUtils.hasText(row.getCategory())
                && !StringUtils.hasText(row.getGenericName())
                && !StringUtils.hasText(row.getBrand())
                && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getModel())
                && !StringUtils.hasText(row.getBinLocation())
                && !StringUtils.hasText(row.getQuantity())
                && !StringUtils.hasText(row.getPurpose())
                && !StringUtils.hasText(row.getRemark())
                && !StringUtils.hasText(row.getIoType());
    }

    private record ValidImportRow(int excelRow, MaterialIoSaveDTO dto) {
    }

    private record MaterialKey(
            String category,
            String genericName,
            String brand,
            String name,
            String model,
            String binLocation
    ) {
        static MaterialKey from(MaterialIoSaveDTO dto) {
            return new MaterialKey(
                    normalize(dto.getCategory()),
                    normalize(dto.getGenericName()),
                    normalize(dto.getBrand()),
                    normalize(dto.getName()),
                    normalize(dto.getModel()),
                    normalize(dto.getBinLocation())
            );
        }

        private static String normalize(String value) {
            return StringUtils.hasText(value) ? value.trim() : "";
        }
    }
}
