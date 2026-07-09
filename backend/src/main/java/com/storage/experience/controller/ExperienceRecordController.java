package com.storage.experience.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.experience.dto.ExperienceFilterOptionsVO;
import com.storage.experience.dto.ExperienceRecordDetailVO;
import com.storage.experience.dto.ExperienceRecordQueryDTO;
import com.storage.experience.dto.ExperienceRecordSaveDTO;
import com.storage.experience.dto.ExperienceRecordVO;
import com.storage.experience.service.ExperienceRecordImportService;
import com.storage.experience.service.ExperienceRecordService;
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
@RequestMapping("/api/experience/records")
@RequiredArgsConstructor
public class ExperienceRecordController {

    private final ExperienceRecordService experienceRecordService;
    private final ExperienceRecordImportService experienceRecordImportService;

    @GetMapping
    @RequiresPermissions("platform:experience:read")
    public PageResult<ExperienceRecordVO> page(ExperienceRecordQueryDTO query) {
        return experienceRecordService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("platform:experience:read")
    public ExperienceFilterOptionsVO filterOptions() {
        return experienceRecordService.filterOptions();
    }

    @GetMapping("/export")
    @RequiresPermissions("platform:experience:read")
    public ResponseEntity<byte[]> export(ExperienceRecordQueryDTO query) throws IOException {
        byte[] content = experienceRecordService.export(query);
        return ExcelResponseBuilder.build(content, "经验库_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequiresPermissions("platform:experience:write")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = experienceRecordService.exportTemplate();
        return ExcelResponseBuilder.build(content, "经验库导入模板.xlsx");
    }

    @PostMapping("/import")
    @RequiresPermissions("platform:experience:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return experienceRecordImportService.importExcel(file);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("platform:experience:read")
    public ExperienceRecordDetailVO getById(@PathVariable Long id) {
        return experienceRecordService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("platform:experience:write")
    public ExperienceRecordDetailVO create(@Valid @RequestBody ExperienceRecordSaveDTO dto) {
        return experienceRecordService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("platform:experience:write")
    public ExperienceRecordDetailVO update(@PathVariable Long id, @Valid @RequestBody ExperienceRecordSaveDTO dto) {
        return experienceRecordService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("platform:experience:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        experienceRecordService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("platform:experience:write")
    public void delete(@PathVariable Long id) {
        experienceRecordService.delete(id);
    }
}
