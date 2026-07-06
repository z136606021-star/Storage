package com.storage.system.customer.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.ExcelCellUtils;
import com.storage.common.exception.ImportFormatException;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.excel.SysCustomerExcelColumn;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysCustomerImportService {

    private final SysCustomerService sysCustomerService;

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
                    SysCustomerSaveDTO dto = parseRow(row);
                    sysCustomerService.create(dto);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception ex) {
                    result.setFailCount(result.getFailCount() + 1);
                    errors.add(new ImportResultVO.ImportErrorVO(excelRow, ex.getMessage()));
                }
            }
        }

        result.setErrors(errors);
        return result;
    }

    private SysCustomerSaveDTO parseRow(Row row) {
        SysCustomerSaveDTO dto = new SysCustomerSaveDTO();
        String customerCode = ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.CUSTOMER_CODE.getIndex());
        String name = ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.NAME.getIndex());
        if (!StringUtils.hasText(customerCode)) {
            throw new IllegalArgumentException("客户编号不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("客户名称不能为空");
        }
        dto.setCustomerCode(customerCode.trim());
        dto.setName(name.trim());
        dto.setContactName(ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.CONTACT_NAME.getIndex()));
        dto.setPhone(ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.PHONE.getIndex()));
        dto.setEmail(ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.EMAIL.getIndex()));
        dto.setAddress(ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.ADDRESS.getIndex()));
        dto.setStatus(SysCustomerService.parseStatus(
                ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.STATUS.getIndex())
        ));
        dto.setRemark(ExcelCellUtils.getCellString(row, SysCustomerExcelColumn.REMARK.getIndex()));
        return dto;
    }

    private boolean isEmptyRow(Row row) {
        for (SysCustomerExcelColumn column : SysCustomerExcelColumn.values()) {
            if (column == SysCustomerExcelColumn.INDEX) {
                continue;
            }
            if (StringUtils.hasText(ExcelCellUtils.getCellString(row, column.getIndex()))) {
                return false;
            }
        }
        return true;
    }
}
