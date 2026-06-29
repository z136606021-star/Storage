package com.storage.controller;

import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialIoBatchSaveDTO;
import com.storage.dto.MaterialIoQueryDTO;
import com.storage.dto.MaterialIoRecordVO;
import com.storage.dto.MaterialIoSafetyHintVO;
import com.storage.dto.MaterialIoUpdateDTO;
import com.storage.dto.PageResult;
import com.storage.service.MaterialIoExportService;
import com.storage.service.MaterialIoImportService;
import com.storage.service.MaterialIoService;
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
import java.util.List;

@RestController
@RequestMapping("/api/material-io")
@RequiredArgsConstructor
public class MaterialIoController {

    private final MaterialIoService materialIoService;
    private final MaterialIoExportService materialIoExportService;
    private final MaterialIoImportService materialIoImportService;

    @GetMapping
    @RequiresPermissions("warehouse:material-io:read")
    public PageResult<MaterialIoRecordVO> page(MaterialIoQueryDTO query) {
        return materialIoService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("warehouse:material-io:read")
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialIoService.filterOptions(query);
    }

    @GetMapping("/safety-hints")
    @RequiresPermissions("warehouse:material-io:read")
    public List<MaterialIoSafetyHintVO> safetyHints(@RequestParam List<Long> materialLedgerIds) {
        return materialIoService.safetyHints(materialLedgerIds);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("warehouse:material-io:read")
    public MaterialIoRecordVO getById(@PathVariable Long id) {
        return materialIoService.getById(id);
    }

    @PostMapping("/batch")
    @RequiresPermissions("warehouse:material-io:write")
    public List<MaterialIoRecordVO> batchCreate(@Valid @RequestBody MaterialIoBatchSaveDTO dto) {
        return materialIoService.batchCreate(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("warehouse:material-io:write")
    public MaterialIoRecordVO update(@PathVariable Long id, @Valid @RequestBody MaterialIoUpdateDTO dto) {
        return materialIoService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("warehouse:material-io:write")
    public void delete(@PathVariable Long id) {
        materialIoService.delete(id);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("warehouse:material-io:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        materialIoService.batchDelete(dto);
    }

    @GetMapping("/export")
    @RequiresPermissions("warehouse:material-io:read")
    public ResponseEntity<byte[]> export(MaterialIoQueryDTO query) throws IOException {
        byte[] content = materialIoExportService.export(materialIoService.listByQuery(query));
        String filename = "物料出入库-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/import-template")
    @RequiresPermissions("warehouse:material-io:read")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = materialIoExportService.exportImportTemplate();
        String filename = "物料出入库导入模板.xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @PostMapping("/import")
    @RequiresPermissions("warehouse:material-io:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return materialIoImportService.importExcel(file);
    }
}
