package com.storage.system.customer.service;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.excel.AutoPoiExcelTemplate;
import com.storage.system.customer.dto.SysCustomerSaveDTO;
import com.storage.system.customer.excel.SysCustomerExportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SysCustomerImportService {

    private final SysCustomerService sysCustomerService;

    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        return AutoPoiExcelTemplate.importRows(file, SysCustomerExportRow.class, this::isEmptyRow, (excelRow, row) ->
                sysCustomerService.create(parseRow(row)));
    }

    private SysCustomerSaveDTO parseRow(SysCustomerExportRow row) {
        SysCustomerSaveDTO dto = new SysCustomerSaveDTO();
        String customerCode = row.getCustomerCode();
        String name = row.getName();
        if (!StringUtils.hasText(customerCode)) {
            throw new IllegalArgumentException("客户编号不能为空");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("客户名称不能为空");
        }
        dto.setCustomerCode(customerCode.trim());
        dto.setName(name.trim());
        dto.setContactName(row.getContactName());
        dto.setPhone(row.getPhone());
        dto.setEmail(row.getEmail());
        dto.setAddress(row.getAddress());
        dto.setStatus(SysCustomerService.parseStatus(row.getStatus()));
        dto.setRemark(row.getRemark());
        return dto;
    }

    private boolean isEmptyRow(SysCustomerExportRow row) {
        return !StringUtils.hasText(row.getCustomerCode())
                && !StringUtils.hasText(row.getName())
                && !StringUtils.hasText(row.getContactName())
                && !StringUtils.hasText(row.getPhone())
                && !StringUtils.hasText(row.getEmail())
                && !StringUtils.hasText(row.getAddress())
                && !StringUtils.hasText(row.getStatus())
                && !StringUtils.hasText(row.getRemark());
    }
}
