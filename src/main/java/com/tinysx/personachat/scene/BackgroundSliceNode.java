package com.tinysx.personachat.scene;

import com.tinysx.personachat.packs.ChatBackgroundSettings;
import com.tinysx.personachat.packs.PackAsset;
import com.tinysx.personachat.packs.PackManager;

/**
 * Builds and renders dynamic 3-slice font-image backgrounds for multi-line messages.
 */
public class BackgroundSliceNode {

    private final ChatBackgroundSettings settings;
    private final PackManager packManager;

    public BackgroundSliceNode(ChatBackgroundSettings settings, PackManager packManager) {
        this.settings = settings;
        this.packManager = packManager;
    }

    public String buildBackgroundGlyphs(int estimatedLines) {
        if (settings == null || !settings.isImageMode() || packManager == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String prefixShift = settings.getPrefixShift();
        String suffixShift = settings.getSuffixShift();

        if (!prefixShift.isEmpty()) {
            sb.append(prefixShift);
        }

        if (estimatedLines <= 1) {
            PackAsset single = packManager.resolveAsset(settings.getSingleLineAsset());
            if (single != null) {
                sb.append(single.getFormatted());
            } else {
                appendTop(sb);
                appendBottom(sb);
            }
        } else {
            appendTop(sb);

            PackAsset mid = packManager.resolveAsset(settings.getMiddleSliceAsset());
            int midCount = Math.max(1, estimatedLines - 1);
            if (mid != null) {
                for (int i = 0; i < midCount; i++) {
                    sb.append(mid.getFormatted());
                }
            }

            appendBottom(sb);
        }

        if (!suffixShift.isEmpty()) {
            sb.append(suffixShift);
        }

        return sb.toString();
    }

    private void appendTop(StringBuilder sb) {
        PackAsset top = packManager.resolveAsset(settings.getTopSliceAsset());
        if (top != null) {
            sb.append(top.getFormatted());
        }
    }

    private void appendBottom(StringBuilder sb) {
        PackAsset bot = packManager.resolveAsset(settings.getBottomSliceAsset());
        if (bot != null) {
            sb.append(bot.getFormatted());
        }
    }

    public ChatBackgroundSettings getSettings() {
        return settings;
    }
}
