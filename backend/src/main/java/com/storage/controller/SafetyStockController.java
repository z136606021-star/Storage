package com.storage.controller;

import com.storage.dto.FilterLinkageQueryDTO;
import com.storage.dto.FilterOptionsVO;
import com.storage.dto.PageResult;
import com.storage.dto.SafetyStockQueryDTO;
import com.storage.dto.SafetyStockRecordVO;
import com.storage.dto.SafetyStockUpdateDTO;
import com.storage.service.SafetyStockService;
import com.storage.web.ExcelResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/safety-stock")
@RequiredArgsConstructor
public class SafetyStockController {

    private final SafetyStockService safetyStockService;

    @GetMapping
    @RequiresPermissions("warehouse:safety-stock:read")
    public PageResult<SafetyStockRecordVO> page(SafetyStockQueryDTO query) {
        return safetyStockService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("warehouse:safety-stock:read")
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return safetyStockService.filterOptions(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("warehouse:safety-stock:read")
    public ResponseEntity<byte[]> export(SafetyStockQueryDTO query) throws IOException {
        byte[] content = safetyStockService.export(query);
        String filename = "安全库存-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/{materialLedgerId}")
    @RequiresPermissions("warehouse:safety-stock:read")
    public SafetyStockRecordVO getByMaterialLedgerId(@PathVariable Long materialLedgerId) {
        return safetyStockService.getByMaterialLedgerId(materialLedgerId);
    }

    @PutMapping("/{materialLedgerId}")
    @RequiresPermissions("warehouse:safety-stock:write")
    public SafetyStockRecordVO upsert(
            @PathVariable Long materialLedgerId,
            @Valid @RequestBody SafetyStockUpdateDTO dto
    ) {
        return safetyStockService.upsert(materialLedgerId, dto);
    }
}
