package com.storage.warehouse.bin.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.warehouse.bin.dto.WarehouseBinQueryDTO;
import com.storage.warehouse.bin.dto.WarehouseBinSaveDTO;
import com.storage.warehouse.bin.entity.WarehouseBin;
import com.storage.warehouse.bin.service.WarehouseBinImportService;
import com.storage.warehouse.bin.service.WarehouseBinService;
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
import java.util.List;

@RestController
@RequestMapping("/api/warehouse-bins")
@RequiredArgsConstructor
public class WarehouseBinController {

    private final WarehouseBinService warehouseBinService;
    private final WarehouseBinImportService warehouseBinImportService;

    @GetMapping
    @RequiresPermissions("warehouse:bin:read")
    public PageResult<WarehouseBin> page(WarehouseBinQueryDTO query) {
        return warehouseBinService.page(query);
    }

    @GetMapping("/codes")
    @RequiresPermissions("warehouse:bin:read")
    public List<String> listCodes() {
        return warehouseBinService.listAllCodes();
    }

    @GetMapping("/export")
    @RequiresPermissions("warehouse:bin:read")
    public ResponseEntity<byte[]> export(WarehouseBinQueryDTO query) throws IOException {
        byte[] content = warehouseBinService.export(query);
        String filename = "Bin位-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/import-template")
    @RequiresPermissions("warehouse:bin:read")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = warehouseBinService.exportTemplate();
        String filename = "Bin位导入模板.xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @PostMapping("/import")
    @RequiresPermissions("warehouse:bin:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return warehouseBinImportService.importExcel(file);
    }

    @PostMapping
    @RequiresPermissions("warehouse:bin:write")
    public WarehouseBin create(@Valid @RequestBody WarehouseBinSaveDTO dto) {
        return warehouseBinService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("warehouse:bin:write")
    public WarehouseBin update(@PathVariable Long id, @Valid @RequestBody WarehouseBinSaveDTO dto) {
        return warehouseBinService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("warehouse:bin:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        warehouseBinService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("warehouse:bin:write")
    public void delete(@PathVariable Long id) {
        warehouseBinService.delete(id);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("warehouse:bin:read")
    public WarehouseBin getById(@PathVariable Long id) {
        return warehouseBinService.getById(id);
    }
}
