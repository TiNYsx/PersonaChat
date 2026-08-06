package com.tinysx.personachat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerHeadDisplayCommand implements CommandExecutor {

    private static final Map<UUID, ItemDisplay> spawnedDisplays = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            ItemDisplay existing = spawnedDisplays.remove(player.getUniqueId());
            if (existing != null && !existing.isDead()) {
                existing.remove();
                player.sendMessage("§aRemoved your spawned player head display.");
            } else {
                player.sendMessage("§cNo active player head display found.");
            }
            return true;
        }

        String targetName = args.length > 0 ? args[0] : player.getName();
        String mode = "3d";
        if (args.length > 1) {
            mode = args[1].toLowerCase();
        }

        ItemDisplay existing = spawnedDisplays.remove(player.getUniqueId());
        if (existing != null && !existing.isDead()) {
            existing.remove();
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
            head.setItemMeta(meta);
        }

        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2)).add(0, 1.2, 0);

        boolean is2D = mode.equals("2d");

        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, entity -> {
            entity.setItemStack(head);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setItemDisplayTransform(is2D ? ItemDisplay.ItemDisplayTransform.FIXED : ItemDisplay.ItemDisplayTransform.HEAD);

            Transformation transformation = entity.getTransformation();
            if (is2D) {
                transformation.getScale().set(1.0f, 1.0f, 0.001987f);
                transformation.getRightRotation().set(new AxisAngle4f(new Quaternionf().rotateY((float) Math.PI)));
            } else {
                transformation.getScale().set(1.0f, 1.0f, 1.0f);
            }
            entity.setTransformation(transformation);
        });

        spawnedDisplays.put(player.getUniqueId(), display);
        player.sendMessage("§aSpawned " + (is2D ? "§b[2D Half-Body]" : "§e[3D Head]") + " §adisplay for §e" + targetName + "§a!");
        return true;
    }

    public static void cleanupAll() {
        for (ItemDisplay display : spawnedDisplays.values()) {
            if (display != null && !display.isDead()) {
                display.remove();
            }
        }
        spawnedDisplays.clear();
    }
}
