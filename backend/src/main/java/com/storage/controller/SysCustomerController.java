package com.storage.controller;

import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.PageResult;
import com.storage.dto.SysCustomerQueryDTO;
import com.storage.dto.SysCustomerSaveDTO;
import com.storage.entity.SysCustomer;
import com.storage.service.SysCustomerImportService;
import com.storage.service.SysCustomerService;
import com.storage.web.ExcelResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/system/customers")
@RequiredArgsConstructor
public class SysCustomerController {

    private final SysCustomerService sysCustomerService;
    private final SysCustomerImportService sysCustomerImportService;

    @GetMapping
    @RequiresPermissions("system:customer:read")
    public PageResult<SysCustomer> page(SysCustomerQueryDTO query) {
        return sysCustomerService.page(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("system:customer:read")
    public ResponseEntity<byte[]> export(SysCustomerQueryDTO query) throws IOException {
        byte[] content = sysCustomerService.export(query);
        return ExcelResponseBuilder.build(content, "客户列表_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequiresPermissions("system:customer:write")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = sysCustomerService.exportTemplate();
        return ExcelResponseBuilder.build(content, "客户导入模板.xlsx");
    }

    @PostMapping("/import")
    @RequiresPermissions("system:customer:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return sysCustomerImportService.importExcel(file);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:customer:read")
    public SysCustomer getById(@PathVariable Long id) {
        return sysCustomerService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("system:customer:write")
    public SysCustomer create(@Valid @RequestBody SysCustomerSaveDTO dto) {
        return sysCustomerService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("system:customer:write")
    public SysCustomer update(@PathVariable Long id, @Valid @RequestBody SysCustomerSaveDTO dto) {
        return sysCustomerService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("system:customer:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        sysCustomerService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("system:customer:write")
    public void delete(@PathVariable Long id) {
        sysCustomerService.delete(id);
    }
}
