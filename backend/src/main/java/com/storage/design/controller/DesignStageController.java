package com.storage.design.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.design.dto.DesignStageQueryDTO;
import com.storage.design.dto.DesignStageSaveDTO;
import com.storage.design.entity.DesignStage;
import com.storage.design.service.DesignStageService;
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
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/design/stages")
@RequiredArgsConstructor
public class DesignStageController {

    private final DesignStageService designStageService;

    @GetMapping
    @RequiresPermissions("platform:design:read")
    public PageResult<DesignStage> page(DesignStageQueryDTO query) {
        return designStageService.page(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("platform:design:read")
    public ResponseEntity<byte[]> export(DesignStageQueryDTO query) throws IOException {
        byte[] content = designStageService.export(query);
        return ExcelResponseBuilder.build(content, "阶段配置-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/{id}")
    @RequiresPermissions("platform:design:read")
    public DesignStage getById(@PathVariable Long id) {
        return designStageService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("platform:design:write")
    public DesignStage create(@Valid @RequestBody DesignStageSaveDTO dto) {
        return designStageService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public DesignStage update(@PathVariable Long id, @Valid @RequestBody DesignStageSaveDTO dto) {
        return designStageService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("platform:design:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        designStageService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public void delete(@PathVariable Long id) {
        designStageService.delete(id);
    }
}
