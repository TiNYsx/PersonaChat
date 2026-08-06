package com.tinysx.personachat.gui;

import com.tinysx.personachat.cosmetics.CosmeticItem;
import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.cosmetics.CosmeticType;
import com.tinysx.personachat.cosmetics.PlayerCosmeticProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Interactive Chest GUI for viewing, unlocking, and equipping Profile Cosmetics.
 */
public class CosmeticsMenu implements Listener {

    private final CosmeticManager cosmeticManager;
    private static final String MAIN_TITLE = "§8§l» §3§lPersonaChat Cosmetics";
    private static final String CATEGORY_TITLE_PREFIX = "§8§l» §3§lCosmetics: §e";

    public CosmeticsMenu(CosmeticManager cosmeticManager) {
        this.cosmeticManager = cosmeticManager;
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, Component.text(MAIN_TITLE));
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, glass);
        }

        PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());

        // Player Head Overview Slot 4
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sMeta = (SkullMeta) skull.getItemMeta();
        if (sMeta != null) {
            sMeta.setOwningPlayer(player);
            sMeta.displayName(Component.text("§e§l" + player.getName() + "'s Profile").color(NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7Current equipped cosmetics:"));
            lore.add(Component.text("§8• §7Frame: §e" + getEquippedName(CosmeticType.FRAME, profile.getEquippedFrame())));
            lore.add(Component.text("§8• §7Badge: §e" + getEquippedName(CosmeticType.BADGE, profile.getEquippedBadge())));
            lore.add(Component.text("§8• §7Bubble: §e" + getEquippedName(CosmeticType.BUBBLE, profile.getEquippedBubble())));
            lore.add(Component.text("§8• §7Color: §e" + getEquippedName(CosmeticType.COLOR, profile.getEquippedColor())));
            lore.add(Component.text(""));
            lore.add(Component.text("§aClick categories below to customize!"));
            sMeta.lore(lore);
            skull.setItemMeta(sMeta);
        }
        inv.setItem(4, skull);

        // Categories
        inv.setItem(19, createCategoryIcon(Material.GOLDEN_HELMET, "§6§lAvatar Frames",
                "§7Decorate your head/half-body with", "§7borders, halos, and custom 3D models.",
                "§eEquipped: §f" + getEquippedName(CosmeticType.FRAME, profile.getEquippedFrame())));

        inv.setItem(21, createCategoryIcon(Material.NETHER_STAR, "§b§lTitles & Badges",
                "§7Wear custom ranks, font glyphs, and", "§7exclusive status symbols before your name.",
                "§eEquipped: §f" + getEquippedName(CosmeticType.BADGE, profile.getEquippedBadge())));

        inv.setItem(23, createCategoryIcon(Material.END_CRYSTAL, "§d§lMessage Bubbles",
                "§7Choose speech bubble shapes, brackets,", "§7and custom translucent background tints.",
                "§eEquipped: §f" + getEquippedName(CosmeticType.BUBBLE, profile.getEquippedBubble())));

        inv.setItem(25, createCategoryIcon(Material.PRISMARINE_CRYSTALS, "§a§lColor Palettes",
                "§7Choose vibrant hex color gradients", "§7for your name and floating messages.",
                "§eEquipped: §f" + getEquippedName(CosmeticType.COLOR, profile.getEquippedColor())));

        // Reset All Button at Slot 31
        inv.setItem(31, createItem(Material.BARRIER, "§c§lUnequip All Cosmetics",
                "§7Click to clear all equipped", "§7cosmetic decorations."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    public void openCategoryMenu(Player player, CosmeticType type) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text(CATEGORY_TITLE_PREFIX + type.getDisplayName()));
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, "§7");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
        String currentEquipped = profile.getEquipped(type);

        // Unequip Slot at 40
        inv.setItem(40, createItem(Material.BARRIER, "§c§lUnequip " + type.getDisplayName(), "§7Click to remove current " + type.getDisplayName().toLowerCase() + "."));
        // Back Button at 36
        inv.setItem(36, createItem(Material.ARROW, "§e§l« Back to Main Menu", "§7Return to category selection."));

        Map<String, CosmeticItem> items = cosmeticManager.getCosmetics(type);
        int slot = 10;

        for (Map.Entry<String, CosmeticItem> entry : items.entrySet()) {
            String id = entry.getKey();
            CosmeticItem item = entry.getValue();
            boolean isEquipped = id.equalsIgnoreCase(currentEquipped);
            boolean hasPerm = cosmeticManager.hasPermission(player, item);

            ItemStack icon = new ItemStack(item.getIconMaterial());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(colorize(item.getDisplayName())));
                List<Component> lore = new ArrayList<>();
                for (String line : item.getDescription()) {
                    lore.add(Component.text(colorize(line)));
                }
                lore.add(Component.text(""));

                if (isEquipped) {
                    lore.add(Component.text("§a§l★ CURRENTLY EQUIPPED"));
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else if (hasPerm) {
                    lore.add(Component.text("§e§l✔ UNLOCKED §7- Click to Equip"));
                } else {
                    lore.add(Component.text("§c§l✖ LOCKED"));
                    if (!item.getPermission().isEmpty()) {
                        lore.add(Component.text("§8Requires: " + item.getPermission()));
                    }
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            inv.setItem(slot, icon);
            slot++;
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot >= 35) break;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        String title = e.getView().getTitle();

        if (title.equals(MAIN_TITLE)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == 19) openCategoryMenu(player, CosmeticType.FRAME);
            else if (slot == 21) openCategoryMenu(player, CosmeticType.BADGE);
            else if (slot == 23) openCategoryMenu(player, CosmeticType.BUBBLE);
            else if (slot == 25) openCategoryMenu(player, CosmeticType.COLOR);
            else if (slot == 31) {
                PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
                profile.setEquippedFrame("none");
                profile.setEquippedBadge("none");
                profile.setEquippedBubble("none");
                profile.setEquippedColor("none");
                cosmeticManager.saveProfile(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
                openMainMenu(player);
            }
        } else if (title.startsWith(CATEGORY_TITLE_PREFIX)) {
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot == 36) {
                openMainMenu(player);
                return;
            }

            String catName = title.replace(CATEGORY_TITLE_PREFIX, "").trim();
            CosmeticType type = null;
            for (CosmeticType t : CosmeticType.values()) {
                if (t.getDisplayName().equalsIgnoreCase(catName)) {
                    type = t;
                    break;
                }
            }
            if (type == null) return;

            if (slot == 40) {
                PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
                profile.setEquipped(type, "none");
                cosmeticManager.saveProfile(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.2f);
                openCategoryMenu(player, type);
                return;
            }

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.AIR) return;

            Map<String, CosmeticItem> items = cosmeticManager.getCosmetics(type);
            int currentSlot = 10;
            for (Map.Entry<String, CosmeticItem> entry : items.entrySet()) {
                if (currentSlot == slot) {
                    CosmeticItem item = entry.getValue();
                    if (!cosmeticManager.hasPermission(player, item)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
                        player.sendMessage("§cYou do not have permission to equip this cosmetic!");
                        return;
                    }

                    PlayerCosmeticProfile profile = cosmeticManager.getProfile(player.getUniqueId());
                    profile.setEquipped(type, entry.getKey());
                    cosmeticManager.saveProfile(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
                    openCategoryMenu(player, type);
                    return;
                }
                currentSlot++;
                if (currentSlot == 17) currentSlot = 19;
                if (currentSlot == 26) currentSlot = 28;
            }
        }
    }

    private String getEquippedName(CosmeticType type, String id) {
        if (id == null || id.equalsIgnoreCase("none")) return "None";
        CosmeticItem item = cosmeticManager.getCosmetic(type, id);
        return item != null ? colorize(item.getDisplayName()) : id;
    }

    private ItemStack createCategoryIcon(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                lore.add(Component.text(l));
            }
            lore.add(Component.text(""));
            lore.add(Component.text("§e▶ Click to browse & customize!"));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) {
                    lore.add(Component.text(l));
                }
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String colorize(String s) {
        return s.replace('&', '§');
    }
}
