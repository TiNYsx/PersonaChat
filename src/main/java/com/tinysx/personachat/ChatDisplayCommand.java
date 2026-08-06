package com.tinysx.personachat;

import com.tinysx.personachat.cosmetics.CosmeticItem;
import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.cosmetics.CosmeticType;
import com.tinysx.personachat.cosmetics.PlayerCosmeticProfile;
import com.tinysx.personachat.gui.CosmeticsMenu;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatDisplayCommand implements CommandExecutor, TabCompleter {

    private final ChatDisplayManager displayManager;
    private final CosmeticManager cosmeticManager;
    private final CosmeticsMenu cosmeticsMenu;

    public ChatDisplayCommand(ChatDisplayManager displayManager, CosmeticManager cosmeticManager, CosmeticsMenu cosmeticsMenu) {
        this.displayManager = displayManager;
        this.cosmeticManager = cosmeticManager;
        this.cosmeticsMenu = cosmeticsMenu;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && cosmeticsMenu != null) {
                cosmeticsMenu.openMainMenu(player);
                return true;
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "menu", "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                if (cosmeticsMenu != null) {
                    cosmeticsMenu.openMainMenu(player);
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("personachat.reload") && !sender.isOp()) {
                    sender.sendMessage("§cYou do not have permission to reload PersonaChat.");
                    return true;
                }
                displayManager.reload();
                sender.sendMessage("§a[PersonaChat] Configuration, cosmetics, and modular packs reloaded successfully!");
                return true;
            }
            case "resourcepack", "pack" -> {
                if (!sender.hasPermission("personachat.admin") && !sender.isOp()) {
                    sender.sendMessage("§cYou do not have permission to export the resourcepack.");
                    return true;
                }
                sender.sendMessage("§e[PersonaChat] Generating Resource Pack & Core Shaders in background...");
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(PersonaChat.getInstance(), () -> {
                    com.tinysx.personachat.packs.ResourcePackGenerator.generate(PersonaChat.getInstance());
                    sender.sendMessage("§a[PersonaChat] Resource pack & core shaders exported to: §eplugins/PersonaChat/PersonaChat_ResourcePack.zip");
                });
                return true;
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                boolean state = displayManager.toggleDisplay(player.getUniqueId());
                player.sendMessage("§a[PersonaChat] Floating chat display is now: " + (state ? "§2ENABLED" : "§cDISABLED"));
                return true;
            }
            case "on" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                PlayerChatDisplay display = displayManager.getDisplay(player.getUniqueId());
                if (display != null) display.setEnabled(true);
                player.sendMessage("§a[PersonaChat] Floating chat display §2ENABLED");
                return true;
            }
            case "off" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                PlayerChatDisplay display = displayManager.getDisplay(player.getUniqueId());
                if (display != null) display.setEnabled(false);
                player.sendMessage("§a[PersonaChat] Floating chat display §cDISABLED");
                return true;
            }
            case "equip" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /pc equip <frame|badge|bubble|color> <id>");
                    return true;
                }
                CosmeticType type = parseType(args[1]);
                if (type == null) {
                    player.sendMessage("§cInvalid category. Choose from: frame, badge, bubble, color");
                    return true;
                }
                String id = args[2].toLowerCase();
                CosmeticItem item = cosmeticManager.getCosmetic(type, id);
                if (item == null) {
                    player.sendMessage("§cUnknown cosmetic ID: §e" + id);
                    return true;
                }
                if (!cosmeticManager.hasPermission(player, item)) {
                    player.sendMessage("§cYou do not have permission to equip: " + item.getDisplayName());
                    return true;
                }
                PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
                profile.setEquipped(type, id);
                cosmeticManager.saveProfile(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
                player.sendMessage("§a[PersonaChat] Equipped §e" + item.getDisplayName() + " §ain slot §b" + type.getDisplayName() + "§a!");
                return true;
            }
            case "unequip" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /pc unequip <frame|badge|bubble|color|all>");
                    return true;
                }
                PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
                if (args[1].equalsIgnoreCase("all")) {
                    profile.setEquippedFrame("none");
                    profile.setEquippedBadge("none");
                    profile.setEquippedBubble("none");
                    profile.setEquippedColor("none");
                    cosmeticManager.saveProfile(player.getUniqueId());
                    player.sendMessage("§a[PersonaChat] Unequipped all cosmetics.");
                    return true;
                }
                CosmeticType type = parseType(args[1]);
                if (type == null) {
                    player.sendMessage("§cInvalid category. Choose from: frame, badge, bubble, color, all");
                    return true;
                }
                profile.setEquipped(type, "none");
                cosmeticManager.saveProfile(player.getUniqueId());
                player.sendMessage("§a[PersonaChat] Unequipped cosmetic for slot §b" + type.getDisplayName() + "§a.");
                return true;
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cThis command can only be run by a player.");
                    return true;
                }
                PlayerChatDisplay display = displayManager.getDisplay(player.getUniqueId());
                if (display == null) {
                    sender.sendMessage("§cNo active chat display session found for your player.");
                    return true;
                }

                if (args.length > 1) {
                    String subDebug = args[1].toLowerCase();
                    if (subDebug.equals("info") || subDebug.equals("status") || subDebug.equals("report")) {
                        display.sendDebugReport(player);
                        return true;
                    } else if (subDebug.equals("on") || subDebug.equals("true") || subDebug.equals("enable")) {
                        display.setDebugMode(true);
                        player.sendMessage("§a[PersonaChat] Debug mode & Visual Particles: §2ENABLED");
                        display.sendDebugReport(player);
                        return true;
                    } else if (subDebug.equals("off") || subDebug.equals("false") || subDebug.equals("disable")) {
                        display.setDebugMode(false);
                        player.sendMessage("§a[PersonaChat] Debug mode & Visual Particles: §cDISABLED");
                        return true;
                    }
                }

                boolean state = display.toggleDebugMode();
                player.sendMessage("§a[PersonaChat] Debug HUD & Particles: " + (state ? "§2ENABLED §7(Run §e/pc debug info §7for text report)" : "§cDISABLED"));
                if (state) {
                    display.sendDebugReport(player);
                }
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private CosmeticType parseType(String str) {
        return switch (str.toLowerCase()) {
            case "frame", "frames" -> CosmeticType.FRAME;
            case "badge", "badges", "title", "nameplate" -> CosmeticType.BADGE;
            case "bubble", "bubbles" -> CosmeticType.BUBBLE;
            case "color", "colors" -> CosmeticType.COLOR;
            default -> null;
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§3§l===== §b§lPersonaChat by TiNYsx §3§l=====");
        sender.sendMessage("§e/pc menu §7- Open the Profile Cosmetics GUI");
        sender.sendMessage("§e/pc toggle §7- Toggle floating chat display on/off");
        sender.sendMessage("§e/pc on §7/ §e/pc off §7- Enable or disable floating chat");
        sender.sendMessage("§e/pc equip <category> <id> §7- Equip a specific cosmetic");
        sender.sendMessage("§e/pc unequip <category|all> §7- Unequip a cosmetic");
        sender.sendMessage("§e/pc debug [info|on|off] §7- Toggle visual particle debug HUD & logs");
        sender.sendMessage("§e/pc resourcepack §7- Auto-generate/export the Shader & Font ResourcePack");
        sender.sendMessage("§e/pc reload §7- Reload configuration, cosmetics, and packs");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("menu", "toggle", "on", "off", "equip", "unequip", "debug", "resourcepack", "reload");
            return filter(subs, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("debug")) {
                return filter(Arrays.asList("info", "on", "off"), args[1]);
            }
            if (args[0].equalsIgnoreCase("equip") || args[0].equalsIgnoreCase("unequip")) {
                List<String> types = new ArrayList<>(Arrays.asList("frame", "badge", "bubble", "color"));
                if (args[0].equalsIgnoreCase("unequip")) types.add("all");
                return filter(types, args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("equip")) {
            CosmeticType type = parseType(args[1]);
            if (type != null && cosmeticManager != null) {
                return filter(new ArrayList<>(cosmeticManager.getCosmetics(type).keySet()), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String prefix) {
        List<String> res = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) {
                res.add(s);
            }
        }
        return res;
    }
}
