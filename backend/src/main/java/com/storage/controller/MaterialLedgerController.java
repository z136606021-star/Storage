package com.storage.controller;

import com.storage.dto.BatchDeleteDTO;
import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.ImportResultVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.MaterialSaveDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.service.MaterialLedgerService;
import com.storage.web.ExcelResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialLedgerController {

    private final MaterialLedgerService materialLedgerService;

    @GetMapping
    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        return materialLedgerService.page(query);
    }

    @GetMapping("/filter-options")
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialLedgerService.filterOptions(query);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(MaterialQueryDTO query) throws IOException {
        byte[] content = materialLedgerService.export(query);
        String filename = "物料台账-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = materialLedgerService.exportTemplate();
        String filename = "物料台账导入模板.xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @PostMapping("/import")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return materialLedgerService.importExcel(file);
    }

    @PostMapping
    public MaterialLedger create(@Valid @RequestBody MaterialSaveDTO dto) {
        return materialLedgerService.create(dto);
    }

    @PutMapping("/{id}")
    public MaterialLedger update(@PathVariable Long id, @Valid @RequestBody MaterialSaveDTO dto) {
        return materialLedgerService.update(id, dto);
    }

    @DeleteMapping("/batch")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        materialLedgerService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        materialLedgerService.delete(id);
    }

    @GetMapping("/{id}")
    public MaterialLedger getById(@PathVariable Long id) {
        return materialLedgerService.getById(id);
    }
}
