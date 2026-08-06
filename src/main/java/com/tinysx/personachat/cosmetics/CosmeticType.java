package com.tinysx.personachat.cosmetics;

public enum CosmeticType {
    FRAME("Avatar Frame", "Decorates around your avatar head/half-body"),
    BADGE("Title & Badge", "Decorates before or after your name with icons and titles"),
    BUBBLE("Message Bubble", "Custom speech bubble wrapper and background styling"),
    COLOR("Color Palette", "Custom hex color theme for name and messages");

    private final String displayName;
    private final String description;

    CosmeticType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
