package com.tinysx.personachat.scene;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all nodes in the hierarchical Display Object Model (Scene Graph).
 */
public abstract class SceneNode {

    protected Vector localPosition = new Vector(0, 0, 0);
    protected Quaternionf localRotation = new Quaternionf();
    protected Vector3f localScale = new Vector3f(1, 1, 1);
    protected boolean visible = true;

    protected SceneNode parent;
    protected final List<SceneNode> children = new ArrayList<>();

    public void addChild(SceneNode child) {
        if (child != null) {
            child.parent = this;
            children.add(child);
        }
    }

    public void removeChild(SceneNode child) {
        if (child != null) {
            child.parent = null;
            children.remove(child);
        }
    }

    public Vector getLocalPosition() {
        return localPosition;
    }

    public void setLocalPosition(Vector localPosition) {
        this.localPosition = localPosition != null ? localPosition : new Vector(0, 0, 0);
    }

    public Quaternionf getLocalRotation() {
        return localRotation;
    }

    public void setLocalRotation(Quaternionf localRotation) {
        this.localRotation = localRotation != null ? localRotation : new Quaternionf();
    }

    public Vector3f getLocalScale() {
        return localScale;
    }

    public void setLocalScale(Vector3f localScale) {
        this.localScale = localScale != null ? localScale : new Vector3f(1, 1, 1);
    }

    public boolean isVisible() {
        return visible;
    }

    public void updateWorldTransform(Location worldBase, Quaternionf worldRot, Vector3f worldScale,
                                     Vector forward, Vector right, Vector up) {
        Location currentWorldLoc = worldBase.clone()
                .add(right.clone().multiply(localPosition.getX()))
                .add(up.clone().multiply(localPosition.getY()))
                .add(forward.clone().multiply(localPosition.getZ()));

        Quaternionf currentWorldRot = new Quaternionf(worldRot).mul(localRotation);

        Vector3f currentWorldScale = new Vector3f(
                worldScale.x * localScale.x,
                worldScale.y * localScale.y,
                worldScale.z * localScale.z
        );

        onApplyTransform(currentWorldLoc, currentWorldRot, currentWorldScale, forward, right, up);

        for (SceneNode child : children) {
            child.updateWorldTransform(currentWorldLoc, currentWorldRot, currentWorldScale, forward, right, up);
        }
    }

    protected abstract void onApplyTransform(Location worldLoc, Quaternionf worldRot, Vector3f worldScale,
                                            Vector forward, Vector right, Vector up);

    public void setVisible(boolean visible, Player viewer, JavaPlugin plugin) {
        this.visible = visible;
        onSetVisible(visible, viewer, plugin);
        for (SceneNode child : children) {
            child.setVisible(visible, viewer, plugin);
        }
    }

    protected abstract void onSetVisible(boolean visible, Player viewer, JavaPlugin plugin);

    public void destroy() {
        onDestroy();
        for (SceneNode child : children) {
            child.destroy();
        }
        children.clear();
    }

    protected abstract void onDestroy();
}
