package com.storage.system.menu;

public final class MenuTypes {

    public static final String TOP = "TOP";

    public static final String SUB = "SUB";

    public static final String BUTTON = "BUTTON";

    private MenuTypes() {
    }

    public static boolean isTop(String menuType) {
        return TOP.equals(menuType) || "CATALOG".equals(menuType);
    }

    public static boolean isSub(String menuType) {
        return SUB.equals(menuType) || "MENU".equals(menuType);
    }

    public static boolean isButton(String menuType) {
        return BUTTON.equals(menuType);
    }
}
