package com.tinysx.personachat.cosmetics;

import java.util.UUID;

/**
 * Stores the currently equipped cosmetic IDs for an individual player.
 */
public class PlayerCosmeticProfile {

    private final UUID playerUUID;
    private String equippedFrame = "none";
    private String equippedBadge = "none";
    private String equippedBubble = "none";
    private String equippedColor = "none";

    public PlayerCosmeticProfile(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getEquipped(CosmeticType type) {
        return switch (type) {
            case FRAME -> equippedFrame;
            case BADGE -> equippedBadge;
            case BUBBLE -> equippedBubble;
            case COLOR -> equippedColor;
        };
    }

    public void setEquipped(CosmeticType type, String id) {
        String cleanId = (id == null || id.trim().isEmpty()) ? "none" : id.trim().toLowerCase();
        switch (type) {
            case FRAME -> this.equippedFrame = cleanId;
            case BADGE -> this.equippedBadge = cleanId;
            case BUBBLE -> this.equippedBubble = cleanId;
            case COLOR -> this.equippedColor = cleanId;
        }
    }

    public String getEquippedFrame() {
        return equippedFrame;
    }

    public void setEquippedFrame(String equippedFrame) {
        this.equippedFrame = equippedFrame;
    }

    public String getEquippedBadge() {
        return equippedBadge;
    }

    public void setEquippedBadge(String equippedBadge) {
        this.equippedBadge = equippedBadge;
    }

    public String getEquippedBubble() {
        return equippedBubble;
    }

    public void setEquippedBubble(String equippedBubble) {
        this.equippedBubble = equippedBubble;
    }

    public String getEquippedColor() {
        return equippedColor;
    }

    public void setEquippedColor(String equippedColor) {
        this.equippedColor = equippedColor;
    }
}
