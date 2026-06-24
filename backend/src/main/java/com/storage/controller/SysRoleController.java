package com.storage.controller;

import com.storage.dto.ImportResultVO;
import com.storage.dto.SysRoleSaveDTO;
import com.storage.dto.SysRoleVO;
import com.storage.service.SysRoleImportService;
import com.storage.service.SysRoleService;
import com.storage.web.ExcelResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final SysRoleImportService sysRoleImportService;

    @GetMapping
    @RequiresPermissions("system:role:read")
    public List<SysRoleVO> list() {
        return sysRoleService.listAll();
    }

    @GetMapping("/export")
    @RequiresPermissions("system:role:read")
    public ResponseEntity<byte[]> export() throws IOException {
        byte[] content = sysRoleService.export();
        return ExcelResponseBuilder.build(content, "角色列表_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequiresPermissions("system:role:write")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = sysRoleService.exportTemplate();
        return ExcelResponseBuilder.build(content, "角色导入模板.xlsx");
    }

    @PostMapping("/import")
    @RequiresPermissions("system:role:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return sysRoleImportService.importExcel(file);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:role:read")
    public SysRoleVO getById(@PathVariable Long id) {
        return sysRoleService.getById(id);
    }

    @PostMapping
    @RequiresPermissions("system:role:write")
    public SysRoleVO create(@Valid @RequestBody SysRoleSaveDTO dto) {
        return sysRoleService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("system:role:write")
    public SysRoleVO update(@PathVariable Long id, @Valid @RequestBody SysRoleSaveDTO dto) {
        return sysRoleService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermissions("system:role:write")
    public void delete(@PathVariable Long id) {
        sysRoleService.delete(id);
    }
}
