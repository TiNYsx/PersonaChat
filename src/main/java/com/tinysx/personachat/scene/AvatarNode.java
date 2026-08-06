package com.tinysx.personachat.scene;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Scene node managing the Player Head/Half-Body and Avatar Cosmetic Frame entities.
 */
public class AvatarNode extends SceneNode {

    private ItemDisplay headEntity;
    private ItemDisplay frameEntity;
    private boolean isHalfBody = false;
    private float headBaseScale = 0.35f;
    private float frameScaleMultiplier = 1.0f;
    private double frameOffsetY = 0.0;

    public AvatarNode(ItemDisplay headEntity, ItemDisplay frameEntity, boolean isHalfBody,
                      float headBaseScale, float frameScaleMultiplier, double frameOffsetY) {
        this.headEntity = headEntity;
        this.frameEntity = frameEntity;
        this.isHalfBody = isHalfBody;
        this.headBaseScale = headBaseScale;
        this.frameScaleMultiplier = frameScaleMultiplier;
        this.frameOffsetY = frameOffsetY;
    }

    @Override
    protected void onApplyTransform(Location worldLoc, Quaternionf worldRot, Vector3f worldScale,
                                    Vector forward, Vector right, Vector up) {
        AxisAngle4f rotAxis = new AxisAngle4f(worldRot);

        if (headEntity != null && !headEntity.isDead()) {
            headEntity.teleport(worldLoc);
            Transformation trans = headEntity.getTransformation();
            trans.getRightRotation().set(rotAxis);

            float sx = headBaseScale * worldScale.x;
            float sy = headBaseScale * worldScale.y;
            float sz = isHalfBody ? 0.001987f : (headBaseScale * worldScale.z);
            trans.getScale().set(sx, sy, sz);
            headEntity.setTransformation(trans);
        }

        if (frameEntity != null && !frameEntity.isDead()) {
            Location frameLoc = worldLoc.clone().add(up.clone().multiply(frameOffsetY));
            frameEntity.teleport(frameLoc);
            Transformation trans = frameEntity.getTransformation();
            trans.getRightRotation().set(rotAxis);

            float fx = headBaseScale * frameScaleMultiplier * worldScale.x;
            float fy = headBaseScale * frameScaleMultiplier * worldScale.y;
            float fz = headBaseScale * frameScaleMultiplier * worldScale.z;
            trans.getScale().set(fx, fy, fz);
            frameEntity.setTransformation(trans);
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

    public ItemDisplay getFrameEntity() {
        return frameEntity;
    }
}
