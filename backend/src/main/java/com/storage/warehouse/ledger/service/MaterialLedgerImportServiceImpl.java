package com.storage.warehouse.ledger.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.warehouse.bom.service.WarehouseBomService;
import com.storage.warehouse.bin.service.WarehouseBinService;
import com.storage.warehouse.ledger.converter.MaterialLedgerConverter;
import com.storage.warehouse.ledger.dto.MaterialSaveDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.excel.MaterialLedgerExportRow;
import com.storage.warehouse.ledger.mapper.MaterialLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MaterialLedgerImportServiceImpl implements MaterialLedgerImportService {

    private final MaterialLedgerMapper materialLedgerMapper;
    private final MaterialLedgerConverter materialLedgerConverter;
    private final WarehouseBinService warehouseBinService;
    private final WarehouseBomService warehouseBomService;

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(file, MaterialLedgerExportRow.class, this::isEmptyRow, (excelRow, row) -> {
            MaterialSaveDTO dto = parseRow(row);
            validateDto(dto);
            warehouseBinService.assertBinExists(dto.getBinLocation());
            warehouseBomService.assertCatalogExists(
                    dto.getCategory(), dto.getGenericName(), dto.getBrand(), dto.getName());
            MaterialLedger entity = materialLedgerConverter.toNewEntity(dto);
            materialLedgerMapper.insert(entity);
        });
    }

    private MaterialSaveDTO parseRow(MaterialLedgerExportRow row) {
        MaterialSaveDTO dto = new MaterialSaveDTO();
        dto.setCategory(row.getCategory());
        dto.setGenericName(row.getGenericName());
        dto.setBrand(row.getBrand());
        dto.setName(row.getName());
        dto.setModel(row.getModel());
        dto.setBinLocation(row.getBinLocation());
        dto.setUnitPrice(parseUnitPrice(row.getUnitPrice()));
        dto.setRemark(row.getRemark());
        return dto;
    }

    private void validateDto(MaterialSaveDTO dto) {
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
    }

    private BigDecimal parseUnitPrice(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("单价格式不正确");
        }
    }

    private boolean isEmptyRow(MaterialLedgerExportRow row) {
        return !StringUtils.hasText(row.getCategory())
                && !StringUtils.hasText(row.getGenericName())
                && !StringUtils.hasText(row.getBrand())
                && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getModel())
                && !StringUtils.hasText(row.getBinLocation())
                && row.getStockQuantity() == null
                && !StringUtils.hasText(row.getUnitPrice())
                && !StringUtils.hasText(row.getRemark());
    }
}
