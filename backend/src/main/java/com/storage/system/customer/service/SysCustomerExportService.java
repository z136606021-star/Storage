package com.storage.system.customer.service;

import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.system.customer.entity.SysCustomer;
import com.storage.system.customer.excel.SysCustomerExportRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysCustomerExportService {

    public byte[] export(List<SysCustomer> records) throws IOException {
        return exportWorkbook(records);
    }

    public byte[] exportTemplate() throws IOException {
        return exportWorkbook(List.of());
    }

    private byte[] exportWorkbook(List<SysCustomer> records) throws IOException {
        List<SysCustomerExportRow> rows = new ArrayList<>();
        int rowIndex = 1;
        for (SysCustomer record : records) {
            SysCustomerExportRow row = new SysCustomerExportRow();
            row.setIndex(rowIndex++);
            row.setCustomerCode(record.getCustomerCode());
            row.setName(record.getName());
            row.setContactName(record.getContactName());
            row.setPhone(record.getPhone());
            row.setEmail(record.getEmail());
            row.setAddress(record.getAddress());
            row.setStatus(SysCustomerService.formatStatusLabel(record.getStatus()));
            row.setRemark(record.getRemark());
            rows.add(row);
        }
        return AutoPoiExcelTemplate.exportBytes("客户", SysCustomerExportRow.class, rows);
    }
}
