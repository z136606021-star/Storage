package com.storage.system.customer.service;

import com.storage.common.excel.ExcelCellUtils;
import com.storage.system.customer.entity.SysCustomer;
import com.storage.system.customer.excel.SysCustomerExcelColumn;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SysCustomerExportServiceTest {

    private final SysCustomerExportService exportService = new SysCustomerExportServiceImpl();

    @Test
    void export_writesExpectedSheetHeadersAndValues() throws IOException {
        SysCustomer customer = new SysCustomer();
        customer.setCustomerCode("C001");
        customer.setName("测试客户");
        customer.setContactName("张三");
        customer.setPhone("13800000000");
        customer.setEmail("test@example.com");
        customer.setAddress("上海市");
        customer.setStatus(1);
        customer.setRemark("重点客户");

        byte[] bytes = exportService.export(List.of(customer));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("客户");

            Row headerRow = sheet.getRow(0);
            String[] headers = SysCustomerExcelColumn.headers();
            for (int i = 0; i < headers.length; i++) {
                assertThat(ExcelCellUtils.getCellString(headerRow, i)).isEqualTo(headers[i]);
            }

            Row dataRow = sheet.getRow(1);
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.INDEX.getIndex()))
                    .isEqualTo("1");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.CUSTOMER_CODE.getIndex()))
                    .isEqualTo("C001");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.NAME.getIndex()))
                    .isEqualTo("测试客户");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.CONTACT_NAME.getIndex()))
                    .isEqualTo("张三");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.PHONE.getIndex()))
                    .isEqualTo("13800000000");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.EMAIL.getIndex()))
                    .isEqualTo("test@example.com");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.ADDRESS.getIndex()))
                    .isEqualTo("上海市");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.STATUS.getIndex()))
                    .isEqualTo("启用");
            assertThat(ExcelCellUtils.getCellString(dataRow, SysCustomerExcelColumn.REMARK.getIndex()))
                    .isEqualTo("重点客户");
        }
    }

    @Test
    void exportTemplate_writesHeaderOnly() throws IOException {
        byte[] bytes = exportService.exportTemplate();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("客户");
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
