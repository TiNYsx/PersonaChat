package com.tinysx.personachat.packs;

/**
 * Encapsulates settings for a 3-slice dynamic multi-line font-image or color background.
 */
public class ChatBackgroundSettings {

    private final String mode;
    private final String singleLineAsset;
    private final String topSliceAsset;
    private final String middleSliceAsset;
    private final String bottomSliceAsset;
    private final String prefixShift;
    private final String suffixShift;
    private final int backgroundOpacity;

    public ChatBackgroundSettings(String mode, String singleLineAsset, String topSliceAsset,
                                  String middleSliceAsset, String bottomSliceAsset,
                                  String prefixShift, String suffixShift, int backgroundOpacity) {
        this.mode = (mode != null && !mode.isEmpty()) ? mode.toUpperCase() : "COLOR";
        this.singleLineAsset = singleLineAsset != null ? singleLineAsset : "";
        this.topSliceAsset = topSliceAsset != null ? topSliceAsset : "";
        this.middleSliceAsset = middleSliceAsset != null ? middleSliceAsset : "";
        this.bottomSliceAsset = bottomSliceAsset != null ? bottomSliceAsset : "";
        this.prefixShift = prefixShift != null ? prefixShift : "";
        this.suffixShift = suffixShift != null ? suffixShift : "";
        this.backgroundOpacity = backgroundOpacity;
    }

    public String getMode() {
        return mode;
    }

    public boolean isImageMode() {
        return mode.equalsIgnoreCase("IMAGE") || mode.equalsIgnoreCase("HYBRID");
    }

    public boolean isColorMode() {
        return mode.equalsIgnoreCase("COLOR") || mode.equalsIgnoreCase("HYBRID");
    }

    public String getSingleLineAsset() {
        return singleLineAsset;
    }

    public String getTopSliceAsset() {
        return topSliceAsset;
    }

    public String getMiddleSliceAsset() {
        return middleSliceAsset;
    }

    public String getBottomSliceAsset() {
        return bottomSliceAsset;
    }

    public String getPrefixShift() {
        return prefixShift;
    }

    public String getSuffixShift() {
        return suffixShift;
    }

    public int getBackgroundOpacity() {
        return backgroundOpacity;
    }
}
