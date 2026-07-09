package com.storage.design.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.design.dto.DesignGuideFilterOptionsVO;
import com.storage.design.dto.DesignGuideQueryDTO;
import com.storage.design.dto.DesignGuideSaveDTO;
import com.storage.design.entity.DesignGuide;
import com.storage.design.service.DesignGuideService;
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
@RequestMapping("/api/design-guides")
@RequiredArgsConstructor
public class DesignGuideController {

    private final DesignGuideService designGuideService;

    @GetMapping
    @RequiresPermissions("platform:design:read")
    public PageResult<DesignGuide> page(DesignGuideQueryDTO query) {
        return designGuideService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("platform:design:read")
    public DesignGuideFilterOptionsVO filterOptions() {
        return designGuideService.filterOptions();
    }

    @GetMapping("/export")
    @RequiresPermissions("platform:design:read")
    public ResponseEntity<byte[]> export(DesignGuideQueryDTO query) throws IOException {
        byte[] content = designGuideService.export(query);
        return ExcelResponseBuilder.build(content, "设计指引-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequiresPermissions("platform:design:read")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = designGuideService.exportTemplate();
        return ExcelResponseBuilder.build(content, "设计指引导入模板.xlsx");
    }

    @PostMapping("/import")
    @RequiresPermissions("platform:design:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return designGuideService.importExcel(file);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("platform:design:read")
    public DesignGuide getById(@PathVariable Long id) {
        return designGuideService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("platform:design:write")
    public DesignGuide create(@Valid @RequestBody DesignGuideSaveDTO dto) {
        return designGuideService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public DesignGuide update(@PathVariable Long id, @Valid @RequestBody DesignGuideSaveDTO dto) {
        return designGuideService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("platform:design:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        designGuideService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public void delete(@PathVariable Long id) {
        designGuideService.delete(id);
    }
}
