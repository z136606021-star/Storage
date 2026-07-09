package com.storage.design.controller;

import com.storage.common.dto.BatchDeleteDTO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.design.dto.DesignProductTypeQueryDTO;
import com.storage.design.dto.DesignProductTypeSaveDTO;
import com.storage.design.entity.DesignProductType;
import com.storage.design.service.DesignProductTypeService;
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
@RequestMapping("/api/design/product-types")
@RequiredArgsConstructor
public class DesignProductTypeController {

    private final DesignProductTypeService designProductTypeService;

    @GetMapping
    @RequiresPermissions("platform:design:read")
    public PageResult<DesignProductType> page(DesignProductTypeQueryDTO query) {
        return designProductTypeService.page(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("platform:design:read")
    public ResponseEntity<byte[]> export(DesignProductTypeQueryDTO query) throws IOException {
        byte[] content = designProductTypeService.export(query);
        return ExcelResponseBuilder.build(content, "产品类型配置-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/{id}")
    @RequiresPermissions("platform:design:read")
    public DesignProductType getById(@PathVariable Long id) {
        return designProductTypeService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("platform:design:write")
    public DesignProductType create(@Valid @RequestBody DesignProductTypeSaveDTO dto) {
        return designProductTypeService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public DesignProductType update(@PathVariable Long id, @Valid @RequestBody DesignProductTypeSaveDTO dto) {
        return designProductTypeService.update(id, dto);
    }

    @DeleteMapping("/batch")
    @RequiresPermissions("platform:design:write")
    public void batchDelete(@Valid @RequestBody BatchDeleteDTO dto) {
        designProductTypeService.batchDelete(dto);
    }

    @DeleteMapping("/{id}")
    @RequiresPermissions("platform:design:write")
    public void delete(@PathVariable Long id) {
        designProductTypeService.delete(id);
    }
}
