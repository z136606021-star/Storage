package com.storage.warehouse.io.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.common.exception.ImportFormatException;
import com.storage.warehouse.io.dto.MaterialIoSaveDTO;
import com.storage.warehouse.io.excel.MaterialIoImportTemplateRow;
import com.storage.warehouse.io.query.MaterialIoQueryBuilder;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.service.MaterialLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
public class MaterialIoImportService {

    private static final String DUPLICATE_MATERIAL_MESSAGE = "同一批次不能包含重复物料";
    private static final DateTimeFormatter OPERATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MaterialIoService materialIoService;
    private final MaterialLedgerService materialLedgerService;
    private final MaterialStockMutationService materialStockMutationService;

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

        errors.addAll(collectDuplicateLedgerErrors(validRows));
        if (!errors.isEmpty()) {
            return buildFailureResult(errors);
        }

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
        MaterialLedger ledger = materialLedgerService.findByMaterialKey(
                dto.getCategory(),
                dto.getGenericName(),
                dto.getBrand(),
                dto.getName(),
                dto.getModel(),
                dto.getBinLocation()
        );
        if (ledger == null) {
            throw new IllegalArgumentException("未找到匹配的物料台账记录");
        }
        dto.setMaterialLedgerId(ledger.getId());
        return new ValidImportRow(excelRow, dto);
    }

    private List<ImportResultVO.ImportErrorVO> collectDuplicateLedgerErrors(List<ValidImportRow> validRows) {
        Map<Long, List<Integer>> rowsByLedger = new HashMap<>();
        for (ValidImportRow row : validRows) {
            rowsByLedger.computeIfAbsent(row.dto().getMaterialLedgerId(), ignored -> new ArrayList<>())
                    .add(row.excelRow());
        }

        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();
        for (List<Integer> rows : rowsByLedger.values()) {
            if (rows.size() <= 1) {
                continue;
            }
            for (Integer excelRow : rows) {
                errors.add(new ImportResultVO.ImportErrorVO(excelRow, DUPLICATE_MATERIAL_MESSAGE));
            }
        }
        return errors;
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
}
