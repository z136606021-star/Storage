package com.storage.warehouse.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.warehouse.dto.BomFilterOptionsVO;
import com.storage.warehouse.dto.FilterLinkageQueryDTO;
import com.storage.warehouse.dto.WarehouseBomQueryDTO;
import com.storage.warehouse.dto.WarehouseBomSaveDTO;
import com.storage.warehouse.entity.WarehouseBom;
import com.storage.warehouse.service.WarehouseBomImportService;
import com.storage.warehouse.service.WarehouseBomService;
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
@RequestMapping("/api/warehouse-boms")
@RequiredArgsConstructor
public class WarehouseBomController {

    private final WarehouseBomService warehouseBomService;
    private final WarehouseBomImportService warehouseBomImportService;

    @GetMapping
    @RequiresPermissions("warehouse:bom:read")
    public PageResult<WarehouseBom> page(WarehouseBomQueryDTO query) {
        return warehouseBomService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("warehouse:bom:read")
    public BomFilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return warehouseBomService.filterOptions(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("warehouse:bom:read")
    public ResponseEntity<byte[]> export(WarehouseBomQueryDTO query) throws IOException {
        byte[] content = warehouseBomService.export(query);
        String filename = "物料清单-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/import-template")
    @RequiresPermissions("warehouse:bom:read")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = warehouseBomService.exportTemplate();
        String filename = "物料清单导入模板.xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @PostMapping("/import")
    @RequiresPermissions("warehouse:bom:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return warehouseBomImportService.importExcel(file);
    }

    @PostMapping
    @RequiresPermissions("warehouse:bom:write")
    public WarehouseBom create(@Valid @RequestBody WarehouseBomSaveDTO dto) {
        return warehouseBomService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("warehouse:bom:write")
    public WarehouseBom update(@PathVariable Long id, @Valid @RequestBody WarehouseBomSaveDTO dto) {
        return warehouseBomService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("warehouse:bom:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        warehouseBomService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("warehouse:bom:write")
    public void delete(@PathVariable Long id) {
        warehouseBomService.delete(id);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("warehouse:bom:read")
    public WarehouseBom getById(@PathVariable Long id) {
        return warehouseBomService.getById(id);
    }
}
