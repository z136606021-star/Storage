package com.storage.controller;

import com.storage.dto.SysMenuSaveDTO;
import com.storage.dto.SysMenuVO;
import com.storage.service.SysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    @RequiresPermissions("system:menu:read")
    public List<SysMenuVO> tree() {
        return sysMenuService.listTree();
    }

    @PostMapping
    @RequiresPermissions("system:menu:write")
    public SysMenuVO create(@Valid @RequestBody SysMenuSaveDTO dto) {
        return sysMenuService.create(dto);
    }

    @PutMapping("/{id}")
    @RequiresPermissions("system:menu:write")
    public SysMenuVO update(@PathVariable Long id, @Valid @RequestBody SysMenuSaveDTO dto) {
        return sysMenuService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermissions("system:menu:write")
    public void delete(@PathVariable Long id) {
        sysMenuService.delete(id);
    }
}
