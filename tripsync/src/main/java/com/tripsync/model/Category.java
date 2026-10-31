package com.tripsync.model;

import java.util.Arrays;

public enum Category {

    SIGHT("관광지", "📍", "cat-sight"),
    FOOD("맛집", "🍴", "cat-food"),
    CAFE("카페", "☕", "cat-cafe"),
    STAY("숙소", "🛏", "cat-stay"),
    MOVE("교통", "✈", "cat-move"),
    SHOP("쇼핑", "🛍", "cat-shop"),
    ETC("기타", "◆", "cat-etc");

    private final String label;
    private final String icon;
    private final String styleClass;

    Category(String label, String icon, String styleClass) {
        this.label = label;
        this.icon = icon;
        this.styleClass = styleClass;
    }

    public String label() {
        return label;
    }

    public String icon() {
        return icon;
    }

    public String styleClass() {
        return styleClass;
    }

    public static Category fromLabel(String label) {
        return Arrays.stream(values())
                .filter(category -> category.label.equals(label))
                .findFirst()
                .orElse(ETC);
    }
}
