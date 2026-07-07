package com.storage.system.menu.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.service.AuthService;
import com.storage.system.menu.dto.NavMenuNodeVO;
import com.storage.system.menu.dto.SysMenuSaveDTO;
import com.storage.system.menu.dto.SysMenuVO;
import com.storage.system.menu.entity.SysMenu;
import com.storage.system.menu.mapper.SysMenuMapper;
import com.storage.system.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final AuthService authService;

    @Override
    public List<SysMenuVO> listTree() {
        List<SysMenu> menus = sysMenuMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .toList();
        return buildMenuTree(menus, null);
    }

    @Override
    public List<NavMenuNodeVO> navTreeForCurrentUser() {
        SysUser user = authService.currentUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        List<SysMenu> assignedMenus = sysMenuMapper.selectMenusByUserId(user.getId());
        Set<String> permissions = new HashSet<>(assignedMenus.stream()
                .map(SysMenu::getPermission)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));

        Map<Long, SysMenu> allMenusById = sysMenuMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysMenu::getId, menu -> menu));
        Set<Long> expandedMenuIds = expandMenuIdsWithAncestors(assignedMenus, allMenusById);

        List<SysMenu> routeMenus = allMenusById.values().stream()
                .filter(menu -> expandedMenuIds.contains(menu.getId()))
                .filter(this::includeInNavTree)
                .sorted(Comparator.comparing(SysMenu::getSortOrder).thenComparing(SysMenu::getId))
                .toList();

        Map<Long, List<SysMenu>> childrenMap = routeMenus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        List<SysMenu> roots = routeMenus.stream()
                .filter(menu -> menu.getParentId() == null)
                .toList();

        List<NavMenuNodeVO> result = new ArrayList<>();
        for (SysMenu root : roots) {
            NavMenuNodeVO node = buildNavNode(root, childrenMap, permissions);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    private Set<Long> expandMenuIdsWithAncestors(List<SysMenu> assignedMenus, Map<Long, SysMenu> allMenusById) {
        Set<Long> expandedIds = new HashSet<>();
        for (SysMenu menu : assignedMenus) {
            Long currentId = menu.getId();
            while (currentId != null && expandedIds.add(currentId)) {
                SysMenu current = allMenusById.get(currentId);
                currentId = current != null ? current.getParentId() : null;
            }
        }
        return expandedIds;
    }

    @Override
    @Transactional
    public SysMenuVO create(SysMenuSaveDTO dto) {
        validateMenu(dto, null);
        SysMenu menu = toEntity(new SysMenu(), dto);
        sysMenuMapper.insert(menu);
        return toVo(menu, List.of());
    }

    @Override
    @Transactional
    public SysMenuVO update(Long id, SysMenuSaveDTO dto) {
        SysMenu menu = requireMenu(id);
        validateMenu(dto, id);
        if (id.equals(dto.getParentId())) {
            throw new BusinessException("父菜单不能选择自己");
        }
        toEntity(menu, dto);
        sysMenuMapper.updateById(menu);
        return toVo(menu, List.of());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireMenu(id);
        if (sysMenuMapper.countByParentId(id) > 0) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        sysMenuMapper.deleteById(id);
    }

    private NavMenuNodeVO buildNavNode(SysMenu menu, Map<Long, List<SysMenu>> childrenMap, Set<String> permissions) {
        String menuType = menu.getMenuType() == null ? "MENU" : menu.getMenuType();
        List<SysMenu> children = childrenMap.getOrDefault(menu.getId(), List.of());
        List<NavMenuNodeVO> childNodes = new ArrayList<>();
        for (SysMenu child : children) {
            NavMenuNodeVO childNode = buildNavNode(child, childrenMap, permissions);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }

        if ("CATALOG".equals(menuType)) {
            if (childNodes.isEmpty()) {
                return null;
            }
            return NavMenuNodeVO.builder()
                    .key(String.valueOf(menu.getId()))
                    .label(menu.getName())
                    .icon(menu.getIcon())
                    .permission(menu.getPermission())
                    .componentKey(menu.getComponentKey())
                    .visible(menu.getVisible())
                    .children(childNodes)
                    .build();
        }

        if (!StringUtils.hasText(menu.getPermission()) || !permissions.contains(menu.getPermission())) {
            return null;
        }
        return NavMenuNodeVO.builder()
                .key(String.valueOf(menu.getId()))
                .label(menu.getName())
                .path(menu.getPath())
                .permission(menu.getPermission())
                .componentKey(menu.getComponentKey())
                .icon(menu.getIcon())
                .visible(menu.getVisible())
                .children(childNodes.isEmpty() ? null : childNodes)
                .build();
    }

    private boolean includeInNavTree(SysMenu menu) {
        if (menu.getVisible() != null && menu.getVisible() == 1) {
            return true;
        }
        return menu.getParentId() != null
                && StringUtils.hasText(menu.getPath())
                && StringUtils.hasText(menu.getComponentKey());
    }

    private List<SysMenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> {
                    if (parentId == null) {
                        return menu.getParentId() == null;
                    }
                    return parentId.equals(menu.getParentId());
                })
                .map(menu -> toVo(menu, buildMenuTree(menus, menu.getId())))
                .toList();
    }

    private void validateMenu(SysMenuSaveDTO dto, Long excludeId) {
        if (!List.of("CATALOG", "MENU").contains(dto.getMenuType())) {
            throw new BusinessException("菜单类型无效");
        }
        if ("MENU".equals(dto.getMenuType()) && !StringUtils.hasText(dto.getPermission())) {
            throw new BusinessException("菜单类型为 MENU 时必须填写权限标识");
        }
        if ("MENU".equals(dto.getMenuType())
                && Integer.valueOf(1).equals(dto.getVisible())
                && StringUtils.hasText(dto.getPath())
                && !StringUtils.hasText(dto.getComponentKey())) {
            throw new BusinessException("可见路由菜单必须填写组件 Key");
        }
        if (StringUtils.hasText(dto.getPermission())) {
            long count = excludeId == null
                    ? sysMenuMapper.countByPermission(dto.getPermission())
                    : sysMenuMapper.countByPermissionExcludeId(dto.getPermission(), excludeId);
            if (count > 0) {
                throw new BusinessException("权限标识已存在");
            }
        }
        if (dto.getParentId() != null && sysMenuMapper.selectById(dto.getParentId()) == null) {
            throw new BusinessException("父菜单不存在");
        }
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }
        return menu;
    }

    private SysMenu toEntity(SysMenu menu, SysMenuSaveDTO dto) {
        menu.setParentId(dto.getParentId());
        menu.setMenuType(dto.getMenuType());
        menu.setName(dto.getName());
        menu.setPermission(StringUtils.hasText(dto.getPermission()) ? dto.getPermission() : null);
        menu.setPath(StringUtils.hasText(dto.getPath()) ? dto.getPath() : null);
        menu.setComponentKey(StringUtils.hasText(dto.getComponentKey()) ? dto.getComponentKey() : null);
        menu.setIcon(StringUtils.hasText(dto.getIcon()) ? dto.getIcon() : null);
        menu.setVisible(dto.getVisible());
        menu.setSortOrder(dto.getSortOrder());
        return menu;
    }

    private SysMenuVO toVo(SysMenu menu, List<SysMenuVO> children) {
        return SysMenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .menuType(menu.getMenuType())
                .name(menu.getName())
                .permission(menu.getPermission())
                .path(menu.getPath())
                .componentKey(menu.getComponentKey())
                .icon(menu.getIcon())
                .visible(menu.getVisible())
                .sortOrder(menu.getSortOrder())
                .children(children)
                .build();
    }
}
