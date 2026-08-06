package com.tinysx.personachat;

import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.packs.PackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all players' chat displays, cosmetics, and packs.
 */
public class ChatDisplayManager {

    private final JavaPlugin plugin;
    private final ChatDisplayConfig config;
    private final CosmeticManager cosmeticManager;
    private final PackManager packManager;
    private final Map<UUID, PlayerChatDisplay> displays = new ConcurrentHashMap<>();
    private BukkitTask updaterTask;

    public ChatDisplayManager(JavaPlugin plugin, ChatDisplayConfig config,
                              CosmeticManager cosmeticManager, PackManager packManager) {
        this.plugin = plugin;
        this.config = config;
        this.cosmeticManager = cosmeticManager;
        this.packManager = packManager;
    }

    public void startUpdater() {
        if (updaterTask != null) updaterTask.cancel();

        updaterTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerChatDisplay display = displays.get(player.getUniqueId());
                if (display != null && display.isEnabled()) {
                    display.updatePositions(player);
                    display.removeExpiredMessages();
                }
            }
        }, 0L, config.getUpdateTicks());
    }

    public void stopUpdater() {
        if (updaterTask != null) {
            updaterTask.cancel();
            updaterTask = null;
        }
    }

    public void broadcastMessage(ChatMessage msg) {
        Bukkit.getConsoleSender().sendMessage(
                Component.text("[PersonaChat] " + msg.getSenderName() + ": " + msg.getRawMessage())
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerChatDisplay display = displays.get(player.getUniqueId());

            if (display != null && display.isEnabled()) {
                display.addMessage(msg);
            } else {
                Component fallback = Component.text(msg.getSenderName() + ": ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(msg.getRawMessage()).color(NamedTextColor.WHITE));
                player.sendActionBar(fallback);
            }
        }
    }

    public void onPlayerJoin(Player player) {
        PlayerChatDisplay display = new PlayerChatDisplay(
                player.getUniqueId(), config, plugin, cosmeticManager, packManager
        );
        displays.put(player.getUniqueId(), display);
    }

    public void onPlayerQuit(Player player) {
        PlayerChatDisplay display = displays.remove(player.getUniqueId());
        if (display != null) {
            display.destroy();
        }
        if (cosmeticManager != null) {
            cosmeticManager.unloadProfile(player.getUniqueId());
        }
    }

    public PlayerChatDisplay getDisplay(UUID playerUUID) {
        return displays.get(playerUUID);
    }

    public boolean toggleDisplay(UUID playerUUID) {
        PlayerChatDisplay display = displays.get(playerUUID);
        if (display == null) return false;

        boolean newState = !display.isEnabled();
        display.setEnabled(newState);
        return newState;
    }

    public void reload() {
        plugin.reloadConfig();
        config.load(plugin.getConfig());
        if (packManager != null) {
            packManager.loadAll();
        }
        stopUpdater();
        startUpdater();
    }

    public void shutdown() {
        stopUpdater();
        for (PlayerChatDisplay display : displays.values()) {
            display.destroy();
        }
        displays.clear();
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public PackManager getPackManager() {
        return packManager;
    }

    public ChatDisplayConfig getConfig() {
        return config;
    }
}
