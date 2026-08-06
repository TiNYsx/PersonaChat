package com.tinysx.personachat.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all registered cosmetics and persistent player profiles.
 */
public class CosmeticManager {

    private final JavaPlugin plugin;
    private final File cosmeticsFolder;
    private final File playerDataFolder;

    // Cache of all loaded cosmetics: Type -> (ID -> Item)
    private final Map<CosmeticType, Map<String, CosmeticItem>> cosmetics = new EnumMap<>(CosmeticType.class);

    // Cache of player profiles: UUID -> Profile
    private final Map<UUID, PlayerCosmeticProfile> profiles = new ConcurrentHashMap<>();

    public CosmeticManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cosmeticsFolder = new File(plugin.getDataFolder(), "cosmetics");
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        for (CosmeticType type : CosmeticType.values()) {
            cosmetics.put(type, new LinkedHashMap<>());
        }
    }

    public void load() {
        for (CosmeticType type : CosmeticType.values()) {
            cosmetics.get(type).clear();
        }

        if (!cosmeticsFolder.exists()) {
            cosmeticsFolder.mkdirs();
            generateDefaultCosmeticFiles();
        }

        loadCategoryFile(CosmeticType.FRAME, "frames.yml");
        loadCategoryFile(CosmeticType.BADGE, "badges.yml");
        loadCategoryFile(CosmeticType.BUBBLE, "bubbles.yml");
        loadCategoryFile(CosmeticType.COLOR, "colors.yml");

        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        int total = cosmetics.values().stream().mapToInt(Map::size).sum();
        plugin.getLogger().info("Loaded " + total + " total cosmetic items across 4 categories.");
    }

    private void loadCategoryFile(CosmeticType type, String fileName) {
        File file = new File(cosmeticsFolder, fileName);
        if (!file.exists()) {
            generateDefaultFile(type, file);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection sec = yaml.getConfigurationSection(id);
            if (sec == null) continue;

            String name = sec.getString("name", id);
            List<String> lore = sec.getStringList("description");
            String matName = sec.getString("icon", "PAPER");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.PAPER;
            int cmd = sec.getInt("custom-model-data", 0);
            String permission = sec.getString("permission", "");
            String format = sec.getString("format", "");
            String hexColor = sec.getString("hex-color", "");
            int opacity = sec.getInt("background-opacity", -1);
            double frameScale = sec.getDouble("frame-scale", 1.0);
            double frameOffsetY = sec.getDouble("frame-offset-y", 0.0);

            CosmeticItem item = new CosmeticItem(
                    id, type, name, lore, mat, cmd, permission,
                    format, hexColor, opacity, frameScale, frameOffsetY
            );
            cosmetics.get(type).put(id.toLowerCase(), item);
        }
    }

    public Map<String, CosmeticItem> getCosmetics(CosmeticType type) {
        return cosmetics.get(type);
    }

    public void registerPackCosmetic(CosmeticItem item) {
        if (item == null) return;
        cosmetics.get(item.getType()).put(item.getId().toLowerCase(), item);
    }

    public CosmeticItem getCosmetic(CosmeticType type, String id) {
        if (id == null || id.equalsIgnoreCase("none")) return null;
        return cosmetics.get(type).get(id.toLowerCase());
    }

    public boolean hasPermission(Player player, CosmeticItem item) {
        if (item == null) return true;
        if (item.getPermission() == null || item.getPermission().isEmpty()) return true;
        return player.hasPermission(item.getPermission()) || player.isOp();
    }

    public PlayerCosmeticProfile getProfile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, this::loadProfileFromDisk);
    }

    private PlayerCosmeticProfile loadProfileFromDisk(UUID uuid) {
        PlayerCosmeticProfile profile = new PlayerCosmeticProfile(uuid);
        File file = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!file.exists()) return profile;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        profile.setEquippedFrame(yaml.getString("equipped.frame", "none"));
        profile.setEquippedBadge(yaml.getString("equipped.badge", "none"));
        profile.setEquippedBubble(yaml.getString("equipped.bubble", "none"));
        profile.setEquippedColor(yaml.getString("equipped.color", "none"));
        return profile;
    }

    public void saveProfile(UUID uuid) {
        PlayerCosmeticProfile profile = profiles.get(uuid);
        if (profile == null) return;

        final String frame = profile.getEquippedFrame();
        final String badge = profile.getEquippedBadge();
        final String bubble = profile.getEquippedBubble();
        final String color = profile.getEquippedColor();

        // Asynchronous disk write to avoid stalling the main server thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(playerDataFolder, uuid.toString() + ".yml");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("equipped.frame", frame);
            yaml.set("equipped.badge", badge);
            yaml.set("equipped.bubble", bubble);
            yaml.set("equipped.color", color);

            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save player cosmetic data for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void saveProfileSync(UUID uuid) {
        PlayerCosmeticProfile profile = profiles.get(uuid);
        if (profile == null) return;

        File file = new File(playerDataFolder, uuid.toString() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("equipped.frame", profile.getEquippedFrame());
        yaml.set("equipped.badge", profile.getEquippedBadge());
        yaml.set("equipped.bubble", profile.getEquippedBubble());
        yaml.set("equipped.color", profile.getEquippedColor());

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player cosmetic data for " + uuid + ": " + e.getMessage());
        }
    }

    public void unloadProfile(UUID uuid) {
        saveProfileSync(uuid);
        profiles.remove(uuid);
    }

    public void saveAll() {
        for (UUID uuid : profiles.keySet()) {
            saveProfileSync(uuid);
        }
    }


    private void generateDefaultCosmeticFiles() {
        generateDefaultFile(CosmeticType.FRAME, new File(cosmeticsFolder, "frames.yml"));
        generateDefaultFile(CosmeticType.BADGE, new File(cosmeticsFolder, "badges.yml"));
        generateDefaultFile(CosmeticType.BUBBLE, new File(cosmeticsFolder, "bubbles.yml"));
        generateDefaultFile(CosmeticType.COLOR, new File(cosmeticsFolder, "colors.yml"));
    }

    private void generateDefaultFile(CosmeticType type, File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        switch (type) {
            case FRAME -> {
                yaml.set("golden_crest.name", "&6⚜ Golden Crest Frame");
                yaml.set("golden_crest.description", Arrays.asList("&7A majestic golden border", "&7anchored to your avatar."));
                yaml.set("golden_crest.icon", "GOLD_INGOT");
                yaml.set("golden_crest.custom-model-data", 1001);
                yaml.set("golden_crest.permission", "personachat.cosmetic.frame.gold");
                yaml.set("golden_crest.frame-scale", 1.1);

                yaml.set("cyber_hologram.name", "&b⚡ Cyber Hologram");
                yaml.set("cyber_hologram.description", Arrays.asList("&7A futuristic glowing hologram frame."));
                yaml.set("cyber_hologram.icon", "BEACON");
                yaml.set("cyber_hologram.custom-model-data", 1002);
                yaml.set("cyber_hologram.permission", "personachat.cosmetic.frame.cyber");
                yaml.set("cyber_hologram.frame-scale", 1.15);
            }
            case BADGE -> {
                yaml.set("dragon_killer.name", "&c🗡 Dragon Killer");
                yaml.set("dragon_killer.description", Arrays.asList("&7Display your dragon slayer pride!", "&7Renders before your name."));
                yaml.set("dragon_killer.icon", "DRAGON_HEAD");
                yaml.set("dragon_killer.permission", "personachat.cosmetic.badge.dragon_killer");
                yaml.set("dragon_killer.format", "&c🗡 Dragon Killer &7|");

                yaml.set("veteran.name", "&e✦ Veteran");
                yaml.set("veteran.description", Arrays.asList("&7Honored veteran star symbol."));
                yaml.set("veteran.icon", "NETHER_STAR");
                yaml.set("veteran.permission", "personachat.cosmetic.badge.veteran");
                yaml.set("veteran.format", "&e[Veteran] ✦");

                yaml.set("vip_badge.name", "&a[VIP] ✦");
                yaml.set("vip_badge.description", Arrays.asList("&7Supporter VIP badge prefix."));
                yaml.set("vip_badge.icon", "EMERALD");
                yaml.set("vip_badge.permission", "personachat.cosmetic.badge.vip");
                yaml.set("vip_badge.format", "&a[VIP] ✦");
            }
            case BUBBLE -> {
                yaml.set("cyber_matrix.name", "&b「 Cyber Matrix 」");
                yaml.set("cyber_matrix.description", Arrays.asList("&7Encloses message in futuristic cyber brackets."));
                yaml.set("cyber_matrix.icon", "END_CRYSTAL");
                yaml.set("cyber_matrix.permission", "personachat.cosmetic.bubble.cyber");
                yaml.set("cyber_matrix.format", "&b「 &f{message} &b」");
                yaml.set("cyber_matrix.background-opacity", 120);

                yaml.set("crimson_flame.name", "&c« Crimson Flame »");
                yaml.set("crimson_flame.description", Arrays.asList("&7Fiery stylish speech brackets."));
                yaml.set("crimson_flame.icon", "FIRE_CHARGE");
                yaml.set("crimson_flame.permission", "personachat.cosmetic.bubble.flame");
                yaml.set("crimson_flame.format", "&c« &f{message} &c»");
                yaml.set("crimson_flame.background-opacity", 100);

                yaml.set("dark_glass.name", "&8Dark Glass Tint");
                yaml.set("dark_glass.description", Arrays.asList("&7Sleek, high-contrast dark translucent glass."));
                yaml.set("dark_glass.icon", "TINTED_GLASS");
                yaml.set("dark_glass.permission", "personachat.cosmetic.bubble.darkglass");
                yaml.set("dark_glass.format", "{message}");
                yaml.set("dark_glass.background-opacity", 220);
            }
            case COLOR -> {
                yaml.set("golden_radiance.name", "&6★ Golden Radiance");
                yaml.set("golden_radiance.description", Arrays.asList("&7Rich amber and gold text colors."));
                yaml.set("golden_radiance.icon", "SUNFLOWER");
                yaml.set("golden_radiance.permission", "personachat.cosmetic.color.gold");
                yaml.set("golden_radiance.hex-color", "#FFB703");

                yaml.set("neon_aqua.name", "&b⚡ Neon Aqua");
                yaml.set("neon_aqua.description", Arrays.asList("&7Vibrant electric cyan tone."));
                yaml.set("neon_aqua.icon", "PRISMARINE_CRYSTALS");
                yaml.set("neon_aqua.permission", "personachat.cosmetic.color.aqua");
                yaml.set("neon_aqua.hex-color", "#00F5D4");

                yaml.set("sakura_bloom.name", "&d🌸 Sakura Bloom");
                yaml.set("sakura_bloom.description", Arrays.asList("&7Soft, elegant cherry blossom pink tone."));
                yaml.set("sakura_bloom.icon", "CHERRY_SAPLING");
                yaml.set("sakura_bloom.permission", "personachat.cosmetic.color.sakura");
                yaml.set("sakura_bloom.hex-color", "#FF70A6");
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create default " + file.getName() + ": " + e.getMessage());
        }
    }
}
