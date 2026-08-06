package com.tinysx.personachat.packs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single loaded pack file from packs/<pack_name>.yml.
 */
public class PackConfig {

    private final String packId;
    private final String displayName;
    private final String author;
    private final String version;
    private final Map<String, PackAsset> assets = new LinkedHashMap<>();
    private final Map<String, PackDecoration> decorations = new LinkedHashMap<>();

    public PackConfig(String packId, String displayName, String author, String version) {
        this.packId = packId;
        this.displayName = displayName != null ? displayName : packId;
        this.author = author != null ? author : "Unknown";
        this.version = version != null ? version : "1.0.0";
    }

    public String getPackId() {
        return packId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public void addAsset(PackAsset asset) {
        assets.put(asset.getId().toLowerCase(), asset);
    }

    public PackAsset getAsset(String assetId) {
        if (assetId == null) return null;
        return assets.get(assetId.toLowerCase());
    }

    public Map<String, PackAsset> getAssets() {
        return assets;
    }

    public void addDecoration(PackDecoration decoration) {
        decorations.put(decoration.getId().toLowerCase(), decoration);
    }

    public PackDecoration getDecoration(String decorationId) {
        if (decorationId == null) return null;
        return decorations.get(decorationId.toLowerCase());
    }

    public Map<String, PackDecoration> getDecorations() {
        return decorations;
    }
}
