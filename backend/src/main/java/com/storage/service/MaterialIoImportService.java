package com.storage.service;



import com.storage.dto.ImportResultVO;

import com.storage.dto.MaterialIoSaveDTO;

import com.storage.entity.MaterialLedger;

import com.storage.excel.ExcelCellUtils;

import com.storage.excel.MaterialIoExcelColumn;

import com.storage.exception.ImportFormatException;

import com.storage.query.MaterialIoQueryBuilder;

import com.storage.service.MaterialIoPurpose;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.springframework.stereotype.Service;

import org.springframework.util.StringUtils;

import org.springframework.web.multipart.MultipartFile;



import java.io.IOException;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.time.format.DateTimeParseException;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.Objects;

import java.util.Set;



@Service

@RequiredArgsConstructor

public class MaterialIoImportService {



    private static final String DUPLICATE_MATERIAL_MESSAGE = "同一批次不能包含重复物料";

    private static final DateTimeFormatter OPERATED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");



    private final MaterialIoService materialIoService;

    private final MaterialLedgerService materialLedgerService;

    private final MaterialStockMutationService materialStockMutationService;



    public ImportResultVO importExcel(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {

            throw new ImportFormatException("请上传 Excel 文件");

        }



        String filename = file.getOriginalFilename();

        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {

            throw new ImportFormatException("仅支持 .xlsx 或 .xls 格式");

        }



        ImportResultVO result = new ImportResultVO();

        List<ImportResultVO.ImportErrorVO> errors = new ArrayList<>();

        List<ValidImportRow> validRows = new ArrayList<>();



        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;

            if (sheet == null) {

                throw new ImportFormatException("Excel 文件中没有工作表");

            }



            int lastRow = sheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {

                Row row = sheet.getRow(i);

                if (row == null || isEmptyRow(row)) {

                    continue;

                }



                int excelRow = i + 1;

                try {

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

                    validRows.add(new ValidImportRow(excelRow, dto));

                } catch (Exception ex) {

                    errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));

                }

            }

        }



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



    private MaterialIoSaveDTO parseRow(Row row) {

        MaterialIoSaveDTO dto = new MaterialIoSaveDTO();

        dto.setCategory(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.CATEGORY.getIndex()));

        dto.setGenericName(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.GENERIC_NAME.getIndex()));

        dto.setBrand(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.BRAND.getIndex()));

        dto.setName(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.NAME.getIndex()));

        dto.setModel(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.MODEL.getIndex()));

        dto.setBinLocation(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.BIN_LOCATION.getIndex()));

        dto.setQuantity(parseQuantity(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.QUANTITY.getIndex())));

        String purposeRaw = ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.PURPOSE.getIndex());
        dto.setPurpose(MaterialIoPurpose.normalizePurpose(purposeRaw));

        dto.setRemark(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.REMARK.getIndex()));

        dto.setIoType(MaterialIoQueryBuilder.normalizeIoType(
                ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.IO_TYPE.getIndex())
        ));

        dto.setOperatedAt(parseOperatedAt(ExcelCellUtils.getCellString(row, MaterialIoExcelColumn.OPERATED_AT.getIndex())));

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



    private boolean isEmptyRow(Row row) {

        for (MaterialIoExcelColumn column : MaterialIoExcelColumn.values()) {

            if (column == MaterialIoExcelColumn.INDEX

                    || column == MaterialIoExcelColumn.OPERATOR

                    || column == MaterialIoExcelColumn.OPERATED_AT) {

                continue;

            }

            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, column.getIndex()))) {

                return false;

            }

        }

        return true;

    }



    private record ValidImportRow(int excelRow, MaterialIoSaveDTO dto) {

    }

}


