package com.tinysx.personachat.scene;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Scene node managing a TextDisplay entity (Nameplate or Message Text).
 */
public class TextNode extends SceneNode {

    private TextDisplay textDisplay;
    private float textBaseScale = 0.4f;
    private int backgroundOpacity = 80;
    private int targetTeleportDuration = 1;
    private boolean initialized = false;

    public TextNode(TextDisplay textDisplay, float textBaseScale, int backgroundOpacity, int targetTeleportDuration) {
        this.textDisplay = textDisplay;
        this.textBaseScale = 1.0f;
        this.backgroundOpacity = backgroundOpacity;
        this.targetTeleportDuration = targetTeleportDuration;
    }

    @Override
    protected void onApplyTransform(Location worldLoc, Quaternionf worldRot, Vector3f worldScale,
                                    Vector forward, Vector right, Vector up) {
        if (textDisplay != null && !textDisplay.isDead()) {
            textDisplay.teleport(worldLoc);
            Transformation trans = textDisplay.getTransformation();
            trans.getRightRotation().set(new AxisAngle4f(worldRot));

            float s = textBaseScale * worldScale.x;
            trans.getScale().set(s, s, s);
            textDisplay.setTransformation(trans);

            if (!initialized) {
                initialized = true;
                textDisplay.setTeleportDuration(targetTeleportDuration);
                textDisplay.setInterpolationDuration(targetTeleportDuration);
            }

            if (backgroundOpacity >= 0) {
                textDisplay.setBackgroundColor(Color.fromARGB(backgroundOpacity, 0, 0, 0));
            }
        }
    }


    @Override
    protected void onSetVisible(boolean visible, Player viewer, JavaPlugin plugin) {
        if (textDisplay != null && !textDisplay.isDead()) {
            if (visible) viewer.showEntity(plugin, textDisplay);
            else viewer.hideEntity(plugin, textDisplay);
        }
    }

    @Override
    protected void onDestroy() {
        if (textDisplay != null && !textDisplay.isDead()) {
            textDisplay.remove();
            textDisplay = null;
        }
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public void setBackgroundOpacity(int backgroundOpacity) {
        this.backgroundOpacity = backgroundOpacity;
    }
}
