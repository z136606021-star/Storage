package com.storage.system.user.controller;

import com.storage.common.dto.ImportResultVO;
import com.storage.common.dto.PageResult;
import com.storage.common.web.ExcelResponseBuilder;
import com.storage.system.user.dto.ResetPasswordDTO;
import com.storage.system.user.dto.SysUserQueryDTO;
import com.storage.system.user.dto.SysUserSaveDTO;
import com.storage.system.user.dto.SysUserVO;
import com.storage.system.user.dto.UserPermissionsVO;
import com.storage.system.user.dto.UserStatusDTO;
import com.storage.system.user.service.SysUserImportService;
import com.storage.system.user.service.SysUserService;
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

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysUserImportService sysUserImportService;

    @GetMapping
    @RequiresPermissions("system:user:read")
    public PageResult<SysUserVO> page(SysUserQueryDTO query) {
        return sysUserService.page(query);
    }

    @GetMapping("/export")
    @RequiresPermissions("system:user:read")
    public ResponseEntity<byte[]> export(SysUserQueryDTO query) throws IOException {
        byte[] content = sysUserService.export(query);
        return ExcelResponseBuilder.build(content, "用户列表_" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/import-template")
    @RequiresPermissions("system:user:write")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] content = sysUserService.exportTemplate();
        return ExcelResponseBuilder.build(content, "用户导入模板.xlsx");
    }

    @PostMapping("/import")
    @RequiresPermissions("system:user:write")
    public ImportResultVO importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        return sysUserImportService.importExcel(file);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("system:user:read")
    public SysUserVO getById(@PathVariable Long id) {
        return sysUserService.getById(id);
    }

    @GetMapping("/{id}/permissions")
    @RequiresPermissions("system:user:read")
    public UserPermissionsVO getPermissions(@PathVariable Long id) {
        return sysUserService.getPermissions(id);
    }

    @PostMapping
    @RequiresPermissions("system:user:write")
    public SysUserVO create(@Valid @RequestBody SysUserSaveDTO dto) {
        return sysUserService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("system:user:write")
    public SysUserVO update(@PathVariable Long id, @Valid @RequestBody SysUserSaveDTO dto) {
        return sysUserService.update(id, dto);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermissions("system:user:write")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        sysUserService.resetPassword(id, dto);
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermissions("system:user:write")
    public void updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        sysUserService.updateStatus(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermissions("system:user:write")
    public void delete(@PathVariable Long id) {
        sysUserService.delete(id);
    }
}
