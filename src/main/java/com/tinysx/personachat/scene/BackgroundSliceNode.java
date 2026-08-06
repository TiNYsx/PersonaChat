package com.tinysx.personachat.scene;

import com.tinysx.personachat.packs.ChatBackgroundSettings;
import com.tinysx.personachat.packs.PackAsset;
import com.tinysx.personachat.packs.PackManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and renders dynamic 3-slice font-image backgrounds for multi-line messages,
 * producing 1 background slice per line for a dedicated background TextDisplay layer.
 */
public class BackgroundSliceNode {

    private final ChatBackgroundSettings settings;
    private final PackManager packManager;

    public BackgroundSliceNode(ChatBackgroundSettings settings, PackManager packManager) {
        this.settings = settings;
        this.packManager = packManager;
    }

    /**
     * Generates a multi-line string containing exactly 1 background slice per line
     * for a total of `totalLines` lines (covering all padding-Y and content lines).
     */
    public String buildBackgroundLines(int totalLines, OfflinePlayer player) {
        if (settings == null || !settings.isImageMode() || packManager == null || totalLines <= 0) {
            return "";
        }

        boolean hasPapi = player != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        List<String> outputLines = new ArrayList<>();

        for (int i = 0; i < totalLines; i++) {
            String sliceAssetId;
            if (totalLines == 1) {
                sliceAssetId = !settings.getSingleLineAsset().isEmpty() ? settings.getSingleLineAsset() : settings.getTopSliceAsset();
            } else {
                if (i == 0) {
                    sliceAssetId = !settings.getTopSliceAsset().isEmpty() ? settings.getTopSliceAsset() : settings.getSingleLineAsset();
                } else if (i == totalLines - 1) {
                    sliceAssetId = !settings.getBottomSliceAsset().isEmpty() ? settings.getBottomSliceAsset() : settings.getMiddleSliceAsset();
                } else {
                    sliceAssetId = !settings.getMiddleSliceAsset().isEmpty() ? settings.getMiddleSliceAsset() : settings.getTopSliceAsset();
                }
            }

            PackAsset asset = packManager.resolveAsset(sliceAssetId);
            String sliceFormatted = "";
            if (asset != null) {
                String prefixShift = settings.getPrefixShift();
                String suffixShift = settings.getSuffixShift();
                sliceFormatted = (prefixShift.isEmpty() ? "" : prefixShift) + asset.getFormatted() + (suffixShift.isEmpty() ? "" : suffixShift);
                if (hasPapi) {
                    sliceFormatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, sliceFormatted);
                }
            }

            outputLines.add(sliceFormatted);
        }

        return String.join("\n", outputLines);
    }

    public ChatBackgroundSettings getSettings() {
        return settings;
    }
}
