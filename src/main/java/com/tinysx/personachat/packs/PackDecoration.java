package com.tinysx.personachat.packs;

import com.tinysx.personachat.cosmetics.CosmeticItem;
import com.tinysx.personachat.cosmetics.CosmeticType;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a decoration loaded from a pack's Part 2 Decorations section.
 */
public class PackDecoration {

    private final String id;
    private final String packId;
    private final CosmeticType type;
    private final String displayName;
    private final List<String> description;
    private final Material iconMaterial;
    private final int customModelData;
    private final String permission;
    private final String format;
    private final String hexColor;
    private final double frameScale;
    private final double frameOffsetY;
    private final String assetRef;
    private final ChatBackgroundSettings backgroundSettings;

    public PackDecoration(String id, String packId, CosmeticType type, String displayName,
                          List<String> description, Material iconMaterial, int customModelData,
                          String permission, String format, String hexColor,
                          double frameScale, double frameOffsetY, String assetRef,
                          ChatBackgroundSettings backgroundSettings) {
        this.id = id;
        this.packId = packId;
        this.type = type;
        this.displayName = displayName != null ? displayName : id;
        this.description = description != null ? description : new ArrayList<>();
        this.iconMaterial = iconMaterial != null ? iconMaterial : Material.PAPER;
        this.customModelData = customModelData;
        this.permission = permission != null ? permission : "";
        this.format = format != null ? format : "";
        this.hexColor = hexColor != null ? hexColor : "";
        this.frameScale = frameScale > 0 ? frameScale : 1.0;
        this.frameOffsetY = frameOffsetY;
        this.assetRef = assetRef != null ? assetRef : "";
        this.backgroundSettings = backgroundSettings;
    }

    public String getId() {
        return id;
    }

    public String getPackId() {
        return packId;
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

    public double getFrameScale() {
        return frameScale;
    }

    public double getFrameOffsetY() {
        return frameOffsetY;
    }

    public String getAssetRef() {
        return assetRef;
    }

    public ChatBackgroundSettings getBackgroundSettings() {
        return backgroundSettings;
    }

    public CosmeticItem toCosmeticItem() {
        int opacity = backgroundSettings != null ? backgroundSettings.getBackgroundOpacity() : -1;
        return new CosmeticItem(
                id, type, displayName, description, iconMaterial,
                customModelData, permission, format, hexColor,
                opacity, frameScale, frameOffsetY
        );
    }
}
