package com.storage.warehouse.ledger.controller;

import com.storage.common.dto.FilterOptionsVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.warehouse.ledger.dto.MaterialQueryDTO;
import com.storage.warehouse.ledger.entity.MaterialLedger;
import com.storage.warehouse.ledger.service.MaterialLedgerService;
import com.storage.warehouse.shared.dto.BomCatalogItemVO;
import com.storage.warehouse.shared.dto.FilterLinkageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialLedgerController {

    private final MaterialLedgerService materialLedgerService;

    @GetMapping
    @RequiresPermissions("warehouse:material-ledger:read")
    public PageResult<MaterialLedger> page(MaterialQueryDTO query) {
        return materialLedgerService.page(query);
    }

    @GetMapping("/filter-options")
    @RequiresPermissions("warehouse:material-ledger:read")
    public FilterOptionsVO filterOptions(FilterLinkageQueryDTO query) {
        return materialLedgerService.filterOptions(query);
    }

    @GetMapping("/bin-codes")
    @RequiresPermissions("warehouse:material-ledger:read")
    public List<String> listBinCodes() {
        return materialLedgerService.listBinCodes();
    }

    @GetMapping("/bom-catalog")
    @RequiresPermissions("warehouse:material-ledger:read")
    public List<BomCatalogItemVO> listBomCatalog() {
        return materialLedgerService.listBomCatalog();
    }

    @GetMapping("/export")
    @RequiresPermissions("warehouse:material-ledger:read")
    public ResponseEntity<byte[]> export(MaterialQueryDTO query) throws IOException {
        byte[] content = materialLedgerService.export(query);
        String filename = "物料台账-" + LocalDate.now() + ".xlsx";
        return ExcelResponseBuilder.build(content, filename);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("warehouse:material-ledger:read")
    public MaterialLedger getById(@PathVariable Long id) {
        return materialLedgerService.getById(id);
    }
}
