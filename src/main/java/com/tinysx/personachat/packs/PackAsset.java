package com.tinysx.personachat.packs;

/**
 * Represents a single asset reference defined in a pack's Part 1 Asset Registry.
 * Holds Unicode glyphs, ItemsAdder/Oraxen/Nexo font placeholders, and horizontal shift offsets.
 */
public class PackAsset {

    private final String id;
    private final String charOrPlaceholder;
    private final String shiftPrefix;
    private final String shiftSuffix;

    public PackAsset(String id, String charOrPlaceholder, String shiftPrefix, String shiftSuffix) {
        this.id = id;
        this.charOrPlaceholder = charOrPlaceholder != null ? charOrPlaceholder : "";
        this.shiftPrefix = shiftPrefix != null ? shiftPrefix : "";
        this.shiftSuffix = shiftSuffix != null ? shiftSuffix : "";
    }

    public String getId() {
        return id;
    }

    public String getCharOrPlaceholder() {
        return charOrPlaceholder;
    }

    public String getShiftPrefix() {
        return shiftPrefix;
    }

    public String getShiftSuffix() {
        return shiftSuffix;
    }

    public String getFormatted() {
        return shiftPrefix + charOrPlaceholder + shiftSuffix;
    }
}
