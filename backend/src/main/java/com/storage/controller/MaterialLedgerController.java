package com.storage.controller;

import com.storage.dto.FilterOptionsVO;
import com.storage.dto.MaterialQueryDTO;
import com.storage.dto.PageResult;
import com.storage.entity.MaterialLedger;
import com.storage.service.MaterialLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public FilterOptionsVO filterOptions() {
        return materialLedgerService.filterOptions();
    }
}
