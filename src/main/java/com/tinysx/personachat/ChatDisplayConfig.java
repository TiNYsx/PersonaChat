package com.tinysx.personachat;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Reads and holds all configurable values for the chat display system.
 */
public class ChatDisplayConfig {

    // General
    private boolean enabledByDefault = true;
    private int maxMessages = 10;
    private int lineLength = 40;
    private int messageLifetimeSeconds = 10;

    private String displayMode = "ALWAYS";
    private int stillTimeTicks = 5;

    // Positioning (relative to player view)
    private MathEvaluator.Expression forwardDistance;
    private MathEvaluator.Expression leftOffset;
    private MathEvaluator.Expression verticalOffset;
    private MathEvaluator.Expression lineSpacing;
    private MathEvaluator.Expression messageGap;

    // Mathematical Layout Offsets
    private MathEvaluator.Expression mathOffsetsRight;
    private MathEvaluator.Expression mathOffsetsUp;
    private MathEvaluator.Expression mathOffsetsForward;

    // Rotation / Tilt (Panel)
    private MathEvaluator.Expression panelYaw;
    private MathEvaluator.Expression panelPitch;
    private MathEvaluator.Expression panelRoll;
    
    // Rotation / Tilt (Head specific)
    private MathEvaluator.Expression headYaw;
    private MathEvaluator.Expression headPitch;
    private MathEvaluator.Expression headRoll;

    // Appearance
    private MathEvaluator.Expression textScale;
    private MathEvaluator.Expression headScale;
    private MathEvaluator.Expression headLeftOffset;
    private MathEvaluator.Expression headVerticalOffset;
    private MathEvaluator.Expression backgroundOpacity;
    
    private String nameFormat = "{player}";
    private String messageFormat = "{message}";
    private String nameAlignment = "LEFT";
    private String messageAlignment = "LEFT";
    private String nameColor = "#FFFF55";
    private String messageColor = "#FFFFFF";

    // Sounds
    private String soundEffect = "";
    private float soundVolume = 1.0f;
    private MathEvaluator.Expression soundPitch;

    // Head Model Toggle
    private String headModelType = "2D_HALF_BODY";

    // Performance
    private int updateTicks = 1;

    public void load(FileConfiguration config) {
        enabledByDefault = config.getBoolean("chat-display.enabled-by-default", true);
        maxMessages = config.getInt("chat-display.max-messages", 10);
        lineLength = config.getInt("chat-display.line-length", 40);
        messageLifetimeSeconds = config.getInt("chat-display.message-lifetime-seconds", 10);
        
        displayMode = config.getString("chat-display.display-mode", "ALWAYS");
        stillTimeTicks = config.getInt("chat-display.still-time-ticks", 5);

        forwardDistance = loadExpr(config, "chat-display.forward-distance", "5.0");
        leftOffset = loadExpr(config, "chat-display.left-offset", "-2.5");
        verticalOffset = loadExpr(config, "chat-display.vertical-offset", "-0.2");
        lineSpacing = loadExpr(config, "chat-display.line-spacing", "0.12");
        messageGap = loadExpr(config, "chat-display.message-gap", "0.01");

        mathOffsetsRight = loadExpr(config, "chat-display.math-offset-right", "0");
        mathOffsetsUp = loadExpr(config, "chat-display.math-offset-up", "0");
        mathOffsetsForward = loadExpr(config, "chat-display.math-offset-forward", "0");

        panelYaw = loadExpr(config, "chat-display.panel-yaw", "0.0");
        panelPitch = loadExpr(config, "chat-display.panel-pitch", "0.0");
        panelRoll = loadExpr(config, "chat-display.panel-roll", "0.0");
        
        headYaw = loadExpr(config, "chat-display.head-yaw", "0.0");
        headPitch = loadExpr(config, "chat-display.head-pitch", "0.0");
        headRoll = loadExpr(config, "chat-display.head-roll", "0.0");

        textScale = loadExpr(config, "chat-display.text-scale", "0.4");
        headScale = loadExpr(config, "chat-display.head-scale", "0.35");
        headLeftOffset = loadExpr(config, "chat-display.head-left-offset", "0.28");
        headVerticalOffset = loadExpr(config, "chat-display.head-vertical-offset", "0.06");
        backgroundOpacity = loadExpr(config, "chat-display.background-opacity", "80");
        
        nameFormat = config.getString("chat-display.name-format", "{player}");
        messageFormat = config.getString("chat-display.message-format", "{message}");
        nameAlignment = config.getString("chat-display.name-alignment", "LEFT").toUpperCase();
        messageAlignment = config.getString("chat-display.message-alignment", "LEFT").toUpperCase();
        nameColor = config.getString("chat-display.name-color", "#FFFF55");
        messageColor = config.getString("chat-display.message-color", "#FFFFFF");

        soundEffect = config.getString("chat-display.sound-effect", "");
        soundVolume = (float) config.getDouble("chat-display.sound-volume", 1.0);
        soundPitch = loadExpr(config, "chat-display.sound-pitch", "1.0");

        headModelType = config.getString("chat-display.head-model-type", "2D_HALF_BODY").toUpperCase();

        updateTicks = config.getInt("chat-display.update-ticks", 1);
    }

    private MathEvaluator.Expression loadExpr(FileConfiguration config, String path, String def) {
        return MathEvaluator.compile(config.getString(path, def));
    }

    // --- Getters ---

    public boolean isEnabledByDefault() { return enabledByDefault; }
    public int getMaxMessages() { return maxMessages; }
    public int getLineLength() { return lineLength; }
    public int getMessageLifetimeSeconds() { return messageLifetimeSeconds; }
    public String getDisplayMode() { return displayMode; }
    public int getStillTimeTicks() { return stillTimeTicks; }
    
    public double getForwardDistance(double i, double t, double l, double r) { return forwardDistance.evaluate(i, t, l, r); }
    public double getLeftOffset(double i, double t, double l, double r) { return leftOffset.evaluate(i, t, l, r); }
    public double getVerticalOffset(double i, double t, double l, double r) { return verticalOffset.evaluate(i, t, l, r); }
    public double getLineSpacing(double i, double t, double l, double r) { return lineSpacing.evaluate(i, t, l, r); }
    public double getMessageGap(double i, double t, double l, double r) { return messageGap.evaluate(i, t, l, r); }

    public double getMathOffsetRight(double i, double t, double l, double r) { return mathOffsetsRight.evaluate(i, t, l, r); }
    public double getMathOffsetUp(double i, double t, double l, double r) { return mathOffsetsUp.evaluate(i, t, l, r); }
    public double getMathOffsetForward(double i, double t, double l, double r) { return mathOffsetsForward.evaluate(i, t, l, r); }

    public float getPanelYaw(double i, double t, double l, double r) { return (float) panelYaw.evaluate(i, t, l, r); }
    public float getPanelPitch(double i, double t, double l, double r) { return (float) panelPitch.evaluate(i, t, l, r); }
    public float getPanelRoll(double i, double t, double l, double r) { return (float) panelRoll.evaluate(i, t, l, r); }
    
    public float getHeadYaw(double i, double t, double l, double r) { return (float) headYaw.evaluate(i, t, l, r); }
    public float getHeadPitch(double i, double t, double l, double r) { return (float) headPitch.evaluate(i, t, l, r); }
    public float getHeadRoll(double i, double t, double l, double r) { return (float) headRoll.evaluate(i, t, l, r); }
    
    public float getTextScale(double i, double t, double l, double r) { return (float) textScale.evaluate(i, t, l, r); }
    public float getHeadScale(double i, double t, double l, double r) { return (float) headScale.evaluate(i, t, l, r); }
    public double getHeadLeftOffset(double i, double t, double l, double r) { return headLeftOffset.evaluate(i, t, l, r); }
    public double getHeadVerticalOffset(double i, double t, double l, double r) { return headVerticalOffset.evaluate(i, t, l, r); }
    public int getBackgroundOpacity(double i, double t, double l, double r) { return (int) backgroundOpacity.evaluate(i, t, l, r); }

    public String getNameFormat() { return nameFormat; }
    public String getMessageFormat() { return messageFormat; }
    public String getNameAlignment() { return nameAlignment; }
    public String getMessageAlignment() { return messageAlignment; }
    public String getNameColor() { return nameColor; }
    public String getMessageColor() { return messageColor; }

    public String getSoundEffect() { return soundEffect; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch(double i, double t, double l, double r) { return (float) soundPitch.evaluate(i, t, l, r); }

    public String getHeadModelType() { return headModelType; }

    public int getUpdateTicks() { return updateTicks; }
}
