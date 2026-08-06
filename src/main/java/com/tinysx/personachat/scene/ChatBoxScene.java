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
    private TextNode nameNode;
    private TextNode msgNode;

    private int nameEstimatedLines = 1;
    private int namePixelWidth = 0;
    private int msgEstimatedLines = 1;
    private int msgPixelWidth = 0;

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

    public void setNameNode(TextNode nameNode) {
        if (this.nameNode != null) removeChild(this.nameNode);
        this.nameNode = nameNode;
        if (nameNode != null) addChild(nameNode);
    }

    public void setMsgNode(TextNode msgNode) {
        if (this.msgNode != null) removeChild(this.msgNode);
        this.msgNode = msgNode;
        if (msgNode != null) addChild(msgNode);
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

    public double calculateTotalHeight(double lineSpacing, float textScale) {
        double nameHeight = (nameNode != null) ? (nameEstimatedLines * 0.25 * textScale) : 0;
        double msgHeight = (msgNode != null) ? (msgEstimatedLines * 0.25 * textScale) : 0;
        double total = nameHeight + msgHeight;
        if (nameNode != null && msgNode != null) {
            total += lineSpacing;
        }
        return total;
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

    public TextNode getNameNode() {
        return nameNode;
    }

    public TextNode getMsgNode() {
        return msgNode;
    }

    public int getNameEstimatedLines() {
        return nameEstimatedLines;
    }

    public void setNameEstimatedLines(int nameEstimatedLines) {
        this.nameEstimatedLines = nameEstimatedLines;
    }

    public int getNamePixelWidth() {
        return namePixelWidth;
    }

    public void setNamePixelWidth(int namePixelWidth) {
        this.namePixelWidth = namePixelWidth;
    }

    public int getMsgEstimatedLines() {
        return msgEstimatedLines;
    }

    public void setMsgEstimatedLines(int msgEstimatedLines) {
        this.msgEstimatedLines = msgEstimatedLines;
    }

    public int getMsgPixelWidth() {
        return msgPixelWidth;
    }

    public void setMsgPixelWidth(int msgPixelWidth) {
        this.msgPixelWidth = msgPixelWidth;
    }
}
