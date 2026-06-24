package com.storage.controller;

import com.storage.dto.NavMenuNodeVO;
import com.storage.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuNavController {

    private final SysMenuService sysMenuService;

    @GetMapping("/nav-tree")
    public List<NavMenuNodeVO> navTree() {
        return sysMenuService.navTreeForCurrentUser();
    }
}
