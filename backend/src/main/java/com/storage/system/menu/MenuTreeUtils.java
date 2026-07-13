package com.storage.system.menu;

import com.storage.system.menu.entity.SysMenu;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuTreeUtils {

    private MenuTreeUtils() {
    }

    public static Set<Long> expandWithAncestors(Collection<Long> menuIds, Map<Long, SysMenu> allMenusById) {
        Set<Long> expandedIds = new HashSet<>();
        for (Long menuId : menuIds) {
            Long currentId = menuId;
            while (currentId != null && expandedIds.add(currentId)) {
                SysMenu current = allMenusById.get(currentId);
                currentId = current != null ? current.getParentId() : null;
            }
        }
        return expandedIds;
    }

    public static Set<Long> expandWithAncestors(Collection<Long> menuIds, List<SysMenu> allMenus) {
        Map<Long, SysMenu> allMenusById = allMenus.stream()
                .collect(java.util.stream.Collectors.toMap(SysMenu::getId, menu -> menu));
        return expandWithAncestors(menuIds, allMenusById);
    }
}
