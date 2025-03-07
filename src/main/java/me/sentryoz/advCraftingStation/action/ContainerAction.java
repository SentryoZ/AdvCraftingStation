package me.sentryoz.advCraftingStation.action;

public enum ContainerAction {
    NONE,
    PREV,
    NEXT,
    CLOSE,
    CRAFT,
    INGREDIENT,
    PREVIEW_ITEM,
    PREVIEW_RESULT;

    public static ContainerAction fromString(String actionString) {
        if (actionString != null) {
            try {
                return ContainerAction.valueOf(actionString.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
        return NONE;
    }
}