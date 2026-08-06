package com.tinysx.personachat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatEventListener implements Listener {

    private final JavaPlugin plugin;
    private final ChatDisplayManager displayManager;

    public ChatEventListener(JavaPlugin plugin, ChatDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String rawMsg = PlainTextComponentSerializer.plainText().serialize(event.message());

        ChatMessage chatMsg = new ChatMessage(player.getName(), player.getUniqueId(), rawMsg);

        // Clear viewers so vanilla chat doesn't show up in the chatbox
        event.viewers().clear();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            displayManager.broadcastMessage(chatMsg);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerChatDisplay display = displayManager.getDisplay(player.getUniqueId());
        if (display == null || !display.isEnabled()) return;

        if (display.isStandingStill()) {
            int prev = event.getPreviousSlot();
            int next = event.getNewSlot();

            int delta = 0;
            if (prev == 8 && next == 0) delta = 1;
            else if (prev == 0 && next == 8) delta = -1;
            else if (next > prev) delta = 1;
            else if (next < prev) delta = -1;

            if (delta != 0) {
                display.scroll(delta);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        PlayerChatDisplay display = displayManager.getDisplay(event.getPlayer().getUniqueId());
        if (display != null && display.isEnabled()) {
            display.hideAll();
            display.updatePositions(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        displayManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        displayManager.onPlayerQuit(event.getPlayer());
    }
}

