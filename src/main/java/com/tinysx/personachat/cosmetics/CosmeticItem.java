package com.tinysx.personachat.cosmetics;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a single cosmetic definition (Frame, Badge, Bubble, or Color).
 */
public class CosmeticItem {

    private final String id;
    private final CosmeticType type;
    private final String displayName;
    private final List<String> description;
    private final Material iconMaterial;
    private final int customModelData;
    private final String permission;
    
    // Type-specific properties
    private final String format;            // for BADGE and BUBBLE (e.g. "🗡 %player% |" or "「 %message% 」")
    private final String hexColor;          // for COLOR (e.g. "#00FFCC")
    private final int backgroundOpacity;    // for BUBBLE (-1 if default)
    private final double frameScale;        // for FRAME (default 1.0)
    private final double frameOffsetY;      // for FRAME

    public CosmeticItem(String id, CosmeticType type, String displayName, List<String> description,
                        Material iconMaterial, int customModelData, String permission,
                        String format, String hexColor, int backgroundOpacity,
                        double frameScale, double frameOffsetY) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.iconMaterial = iconMaterial != null ? iconMaterial : Material.PAPER;
        this.customModelData = customModelData;
        this.permission = permission != null ? permission : "";
        this.format = format != null ? format : "";
        this.hexColor = hexColor != null ? hexColor : "";
        this.backgroundOpacity = backgroundOpacity;
        this.frameScale = frameScale > 0 ? frameScale : 1.0;
        this.frameOffsetY = frameOffsetY;
    }

    public String getId() {
        return id;
    }

    public CosmeticType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getDescription() {
        return description;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getPermission() {
        return permission;
    }

    public String getFormat() {
        return format;
    }

    public String getHexColor() {
        return hexColor;
    }

    public int getBackgroundOpacity() {
        return backgroundOpacity;
    }

    public double getFrameScale() {
        return frameScale;
    }

    public double getFrameOffsetY() {
        return frameOffsetY;
    }
}
