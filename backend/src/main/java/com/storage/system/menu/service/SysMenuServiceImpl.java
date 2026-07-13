package com.storage.system.menu.service;

import com.storage.common.exception.BusinessException;
import com.storage.system.auth.service.AuthService;
import com.storage.system.menu.MenuTreeUtils;
import com.storage.system.menu.MenuTypes;
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
        Set<Long> expandedMenuIds = MenuTreeUtils.expandWithAncestors(
                assignedMenus.stream().map(SysMenu::getId).toList(),
                allMenusById
        );

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
        String menuType = menu.getMenuType() == null ? MenuTypes.SUB : menu.getMenuType();
        List<SysMenu> children = childrenMap.getOrDefault(menu.getId(), List.of());
        List<NavMenuNodeVO> childNodes = new ArrayList<>();
        for (SysMenu child : children) {
            NavMenuNodeVO childNode = buildNavNode(child, childrenMap, permissions);
            if (childNode != null) {
                childNodes.add(childNode);
            }
        }

        if (MenuTypes.isTop(menuType)) {
            if (!childNodes.isEmpty()) {
                return buildGroupNavNode(menu, childNodes);
            }
            if (StringUtils.hasText(menu.getPermission())
                    && permissions.contains(menu.getPermission())
                    && StringUtils.hasText(menu.getPath())) {
                return NavMenuNodeVO.builder()
                        .key(String.valueOf(menu.getId()))
                        .label(menu.getName())
                        .path(menu.getPath())
                        .permission(menu.getPermission())
                        .componentKey(menu.getComponentKey())
                        .icon(menu.getIcon())
                        .visible(menu.getVisible())
                        .build();
            }
            return null;
        }

        if (!StringUtils.hasText(menu.getPermission())) {
            if (childNodes.isEmpty()) {
                return null;
            }
            return buildGroupNavNode(menu, childNodes);
        }
        if (!permissions.contains(menu.getPermission())) {
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

    private NavMenuNodeVO buildGroupNavNode(SysMenu menu, List<NavMenuNodeVO> childNodes) {
        return NavMenuNodeVO.builder()
                .key(String.valueOf(menu.getId()))
                .label(menu.getName())
                .icon(menu.getIcon())
                .visible(menu.getVisible())
                .children(childNodes)
                .build();
    }

    private boolean includeInNavTree(SysMenu menu) {
        if (MenuTypes.isButton(menu.getMenuType())) {
            return false;
        }
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
        if (!List.of(MenuTypes.TOP, MenuTypes.SUB, MenuTypes.BUTTON).contains(dto.getMenuType())) {
            throw new BusinessException("菜单类型无效，仅支持 TOP、SUB、BUTTON");
        }

        SysMenu parent = null;
        if (dto.getParentId() != null) {
            parent = sysMenuMapper.selectById(dto.getParentId());
            if (parent == null) {
                throw new BusinessException("父菜单不存在");
            }
        }

        if (MenuTypes.TOP.equals(dto.getMenuType())) {
            if (dto.getParentId() != null) {
                throw new BusinessException("一级菜单不能有父级");
            }
        } else if (dto.getParentId() == null) {
            throw new BusinessException("子菜单和按钮权限必须选择父级");
        }

        if (MenuTypes.BUTTON.equals(dto.getMenuType())) {
            if (!StringUtils.hasText(dto.getPermission())) {
                throw new BusinessException("按钮权限必须填写权限标识");
            }
            if (StringUtils.hasText(dto.getPath()) || StringUtils.hasText(dto.getComponentKey())) {
                throw new BusinessException("按钮权限不能填写路由或组件路径");
            }
            if (parent != null && MenuTypes.isButton(parent.getMenuType())) {
                throw new BusinessException("按钮权限只能挂在页面子菜单下");
            }
        }

        if (MenuTypes.SUB.equals(dto.getMenuType())) {
            if (!StringUtils.hasText(dto.getPermission())) {
                if (StringUtils.hasText(dto.getPath()) || StringUtils.hasText(dto.getComponentKey())) {
                    throw new BusinessException("分组子菜单不能填写路由或组件路径");
                }
            } else if (Integer.valueOf(1).equals(dto.getVisible())
                    && StringUtils.hasText(dto.getPath())
                    && !StringUtils.hasText(dto.getComponentKey())) {
                throw new BusinessException("可见路由菜单必须填写组件路径");
            }
        }

        if (StringUtils.hasText(dto.getPermission())) {
            long count = excludeId == null
                    ? sysMenuMapper.countByPermission(dto.getPermission())
                    : sysMenuMapper.countByPermissionExcludeId(dto.getPermission(), excludeId);
            if (count > 0) {
                throw new BusinessException("权限标识已存在");
            }
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
