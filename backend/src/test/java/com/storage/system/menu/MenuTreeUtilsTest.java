package com.storage.system.menu;

import com.storage.system.menu.entity.SysMenu;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MenuTreeUtilsTest {

    @Test
    void expandWithAncestors_includesAllParents() {
        SysMenu top = menu(110L, null);
        SysMenu sub = menu(111L, 110L);
        SysMenu button = menu(2L, 111L);
        Map<Long, SysMenu> allMenus = Map.of(
                top.getId(), top,
                sub.getId(), sub,
                button.getId(), button
        );

        Set<Long> expanded = MenuTreeUtils.expandWithAncestors(List.of(2L), allMenus);

        assertThat(expanded).containsExactlyInAnyOrder(2L, 111L, 110L);
    }

    private SysMenu menu(Long id, Long parentId) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        return menu;
    }
}
