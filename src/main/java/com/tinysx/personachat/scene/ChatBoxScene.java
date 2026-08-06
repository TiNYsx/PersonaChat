package com.tinysx.personachat.scene;

import com.tinysx.personachat.ChatMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Root Composite Display Object for a single chat message group.
 */
public class ChatBoxScene extends SceneNode {

    private final ChatMessage message;
    private final long spawnTime;
    private final double randomValue;

    private AvatarNode avatarNode;
    private TextNode cardNode;
    private TextNode bgCardNode;

    private int totalLines = 1;
    private int pixelWidth = 0;

    public ChatBoxScene(ChatMessage message) {
        this.message = message;
        this.spawnTime = System.currentTimeMillis();
        this.randomValue = Math.random();
    }

    public void setAvatarNode(AvatarNode avatarNode) {
        if (this.avatarNode != null) removeChild(this.avatarNode);
        this.avatarNode = avatarNode;
        if (avatarNode != null) addChild(avatarNode);
    }

    public void setCardNode(TextNode cardNode) {
        if (this.cardNode != null) removeChild(this.cardNode);
        this.cardNode = cardNode;
        if (cardNode != null) addChild(cardNode);
    }

    public void setBgCardNode(TextNode bgCardNode) {
        if (this.bgCardNode != null) removeChild(this.bgCardNode);
        this.bgCardNode = bgCardNode;
        if (bgCardNode != null) addChild(bgCardNode);
    }

    public TextNode getBgCardNode() {
        return bgCardNode;
    }

    // Backwards-compatibility aliases
    public void setNameNode(TextNode nameNode) {
        setCardNode(nameNode);
    }

    public void setMsgNode(TextNode msgNode) {
        setCardNode(msgNode);
    }

    public TextNode getCardNode() {
        return cardNode;
    }

    public TextNode getNameNode() {
        return cardNode;
    }

    public TextNode getMsgNode() {
        return cardNode;
    }

    @Override
    protected void onApplyTransform(Location worldLoc, Quaternionf worldRot, Vector3f worldScale,
                                    Vector forward, Vector right, Vector up) {
    }

    @Override
    protected void onSetVisible(boolean visible, Player viewer, JavaPlugin plugin) {
    }

    @Override
    protected void onDestroy() {
    }

    public double calculateTotalHeight(float textScale, float headScale, double lineHeight, boolean is3DBlock) {
        double textHeight = (cardNode != null && totalLines > 0) ? ((totalLines * lineHeight + 0.025) * textScale) : 0;
        double avatarFactor = is3DBlock ? 1.0 : 0.5;
        double headerHeight = (avatarNode != null) ? (headScale * avatarFactor) : 0;
        return Math.max(headerHeight, textHeight);
    }

    public double calculateTotalHeight(float textScale, float headScale, double lineHeight) {
        return calculateTotalHeight(textScale, headScale, lineHeight, false);
    }

    public double calculateTotalHeight(float textScale, float headScale) {
        return calculateTotalHeight(textScale, headScale, 0.225, false);
    }

    public ChatMessage getMessage() {
        return message;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public double getRandomValue() {
        return randomValue;
    }

    public AvatarNode getAvatarNode() {
        return avatarNode;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public void setPixelWidth(int pixelWidth) {
        this.pixelWidth = pixelWidth;
    }

    // Backwards-compatibility helpers
    public int getNameEstimatedLines() { return totalLines; }
    public void setNameEstimatedLines(int lines) { this.totalLines = lines; }
    public int getNamePixelWidth() { return pixelWidth; }
    public void setNamePixelWidth(int width) { this.pixelWidth = width; }
    public int getMsgEstimatedLines() { return totalLines; }
    public void setMsgEstimatedLines(int lines) { this.totalLines = lines; }
    public int getMsgPixelWidth() { return pixelWidth; }
    public void setMsgPixelWidth(int width) { this.pixelWidth = width; }
}
