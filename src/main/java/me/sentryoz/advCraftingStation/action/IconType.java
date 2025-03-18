package me.sentryoz.advCraftingStation.action;

public enum IconType {
    NONE,
    PREV,
    NEXT,
    CLOSE,
    CRAFT,
    INGREDIENT,
    PREVIEW_ITEM,
    PREVIEW_RESULT,
    PREVIEW_MATERIAL;

    public static IconType fromString(String actionString) {
        if (actionString != null) {
            try {
                return IconType.valueOf(actionString.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
        return NONE;
    }
}