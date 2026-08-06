package com.tinysx.personachat.scene;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Scene node managing the Player Head/Half-Body and Avatar Cosmetic Frame entities.
 * Supports both 3D ItemDisplay frames and 2D font-image TextDisplay frames,
 * rotating in perfect sync with yaw, pitch, and roll.
 */
public class AvatarNode extends SceneNode {

    private ItemDisplay headEntity;
    private Display frameEntity;
    private boolean isHalfBody = false;
    private float headBaseScale = 0.35f;
    private float frameScaleMultiplier = 1.0f;
    private double frameOffsetY = 0.0;
    private int targetTeleportDuration = 1;
    private boolean headInitialized = false;
    private boolean frameInitialized = false;

    public AvatarNode(ItemDisplay headEntity, Display frameEntity, boolean isHalfBody,
                      float headBaseScale, float frameScaleMultiplier, double frameOffsetY, int targetTeleportDuration) {
        this.headEntity = headEntity;
        this.frameEntity = frameEntity;
        this.isHalfBody = isHalfBody;
        this.headBaseScale = 1.0f;
        this.frameScaleMultiplier = frameScaleMultiplier;
        this.frameOffsetY = frameOffsetY;
        this.targetTeleportDuration = targetTeleportDuration;
    }

    @Override
    protected void onApplyTransform(Location worldLoc, Quaternionf worldRot, Vector3f worldScale,
                                    Vector forward, Vector right, Vector up) {
        if (headEntity != null && !headEntity.isDead()) {
            headEntity.teleport(worldLoc);
            Transformation trans = headEntity.getTransformation();
            trans.getLeftRotation().set(worldRot);
            trans.getRightRotation().identity();

            float sx = headBaseScale * worldScale.x;
            float sy = headBaseScale * worldScale.y;
            float sz = isHalfBody ? 0.001987f : (headBaseScale * worldScale.z);
            trans.getScale().set(sx, sy, sz);
            headEntity.setTransformation(trans);

            if (!headInitialized) {
                headInitialized = true;
                headEntity.setTeleportDuration(targetTeleportDuration);
                headEntity.setInterpolationDuration(targetTeleportDuration);
            }
        }

        if (frameEntity != null && !frameEntity.isDead()) {
            // Position frame slightly in front of head towards the viewer (Z = -0.002)
            Location frameLoc = worldLoc.clone()
                    .add(up.clone().multiply(frameOffsetY))
                    .add(forward.clone().multiply(-0.002));
            frameEntity.teleport(frameLoc);
            Transformation trans = frameEntity.getTransformation();
            trans.getLeftRotation().set(worldRot);
            trans.getRightRotation().identity();

            float fx = headBaseScale * frameScaleMultiplier * worldScale.x;
            float fy = headBaseScale * frameScaleMultiplier * worldScale.y;
            float fz = headBaseScale * frameScaleMultiplier * worldScale.z;
            trans.getScale().set(fx, fy, fz);
            frameEntity.setTransformation(trans);

            if (!frameInitialized) {
                frameInitialized = true;
                frameEntity.setTeleportDuration(targetTeleportDuration);
                frameEntity.setInterpolationDuration(targetTeleportDuration);
            }
        }
    }

    @Override
    protected void onSetVisible(boolean visible, Player viewer, JavaPlugin plugin) {
        if (headEntity != null && !headEntity.isDead()) {
            if (visible) viewer.showEntity(plugin, headEntity);
            else viewer.hideEntity(plugin, headEntity);
        }
        if (frameEntity != null && !frameEntity.isDead()) {
            if (visible) viewer.showEntity(plugin, frameEntity);
            else viewer.hideEntity(plugin, frameEntity);
        }
    }

    @Override
    protected void onDestroy() {
        if (headEntity != null && !headEntity.isDead()) {
            headEntity.remove();
            headEntity = null;
        }
        if (frameEntity != null && !frameEntity.isDead()) {
            frameEntity.remove();
            frameEntity = null;
        }
    }

    public ItemDisplay getHeadEntity() {
        return headEntity;
    }

    public Display getFrameEntity() {
        return frameEntity;
    }
}
