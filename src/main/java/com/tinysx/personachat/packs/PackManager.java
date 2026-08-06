package com.tinysx.personachat.packs;

import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.cosmetics.CosmeticType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers, loads, and manages all packs inside plugins/PersonaChat/packs/*.yml.
 */
public class PackManager {

    private final JavaPlugin plugin;
    private final CosmeticManager cosmeticManager;
    private final File packsFolder;
    private final Map<String, PackConfig> packs = new ConcurrentHashMap<>();
    private final Map<String, PackAsset> globalAssets = new ConcurrentHashMap<>();
    private final Map<String, PackDecoration> globalDecorations = new ConcurrentHashMap<>();

    public PackManager(JavaPlugin plugin, CosmeticManager cosmeticManager) {
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
        this.packsFolder = new File(plugin.getDataFolder(), "packs");
    }

    public void loadAll() {
        packs.clear();
        globalAssets.clear();
        globalDecorations.clear();

        if (!packsFolder.exists()) {
            packsFolder.mkdirs();
            generateDefaultPacks();
        }

        File[] files = packsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null || files.length == 0) {
            generateDefaultPacks();
            files = packsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        }

        if (files != null) {
            for (File file : files) {
                loadPackFile(file);
            }
        }

        plugin.getLogger().info("Loaded " + packs.size() + " packs with " + globalDecorations.size() + " total decorations & " + globalAssets.size() + " assets.");
    }

    private void loadPackFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String packId = yaml.getString("pack-id", file.getName().replace(".yml", "")).toLowerCase();
        String displayName = yaml.getString("display-name", packId);
        String author = yaml.getString("author", "TiNYsx");
        String version = yaml.getString("version", "1.0.0");

        PackConfig pack = new PackConfig(packId, displayName, author, version);

        // ----------------------------------------------------
        // Part 1: Asset Registry
        // ----------------------------------------------------
        ConfigurationSection assetsSec = yaml.getConfigurationSection("assets");
        if (assetsSec != null) {
            for (String assetKey : assetsSec.getKeys(false)) {
                ConfigurationSection sec = assetsSec.getConfigurationSection(assetKey);
                String charOrPlaceholder;
                String shiftPrefix = "";
                String shiftSuffix = "";

                if (sec != null) {
                    charOrPlaceholder = sec.getString("char", sec.getString("placeholder", ""));
                    shiftPrefix = sec.getString("prefix-shift", sec.getString("shift-prefix", ""));
                    shiftSuffix = sec.getString("suffix-shift", sec.getString("shift-suffix", ""));
                } else {
                    charOrPlaceholder = assetsSec.getString(assetKey, "");
                }

                PackAsset asset = new PackAsset(assetKey, charOrPlaceholder, shiftPrefix, shiftSuffix);
                pack.addAsset(asset);
                globalAssets.put(assetKey.toLowerCase(), asset);
            }
        }

        // ----------------------------------------------------
        // Part 2: Decorations
        // ----------------------------------------------------
        ConfigurationSection decoSec = yaml.getConfigurationSection("decorations");
        if (decoSec != null) {
            for (String decoKey : decoSec.getKeys(false)) {
                ConfigurationSection sec = decoSec.getConfigurationSection(decoKey);
                if (sec == null) continue;

                String typeStr = sec.getString("type", "NAMEPLATE").toUpperCase();
                CosmeticType type = parseCosmeticType(typeStr);
                String name = sec.getString("name", decoKey);
                String permission = sec.getString("permission", "");
                List<String> description = sec.getStringList("description");

                String matName = sec.getString("icon", "PAPER");
                Material icon = Material.matchMaterial(matName);
                if (icon == null) icon = Material.PAPER;
                int cmd = sec.getInt("custom-model-data", 0);

                ConfigurationSection settings = sec.getConfigurationSection("settings");
                String format = "";
                String hexColor = "";
                double frameScale = 1.0;
                double frameOffsetY = 0.0;
                String assetRef = "";
                ChatBackgroundSettings bgSettings = null;

                if (settings != null) {
                    format = settings.getString("format", "");
                    hexColor = settings.getString("hex-color", "");
                    frameScale = settings.getDouble("scale", 1.0);
                    frameOffsetY = settings.getDouble("offset-y", 0.0);
                    assetRef = settings.getString("asset", "");

                    if (type == CosmeticType.BUBBLE) {
                        String mode = settings.getString("mode", "IMAGE");
                        String singleAsset = settings.getString("single-line-asset", "");
                        String topAsset = settings.getString("top-slice-asset", "");
                        String middleAsset = settings.getString("middle-slice-asset", "");
                        String bottomAsset = settings.getString("bottom-slice-asset", "");
                        String prefixShift = settings.getString("prefix-shift", "");
                        String suffixShift = settings.getString("suffix-shift", "");
                        int opacity = settings.getInt("background-opacity", -1);

                        bgSettings = new ChatBackgroundSettings(
                                mode, singleAsset, topAsset, middleAsset, bottomAsset,
                                prefixShift, suffixShift, opacity
                        );

                        if (format.isEmpty()) {
                            format = settings.getString("format", "{message}");
                        }
                    }
                }

                PackDecoration deco = new PackDecoration(
                        decoKey, packId, type, name, description, icon, cmd,
                        permission, format, hexColor, frameScale, frameOffsetY,
                        assetRef, bgSettings
                );

                pack.addDecoration(deco);
                globalDecorations.put(decoKey.toLowerCase(), deco);

                if (cosmeticManager != null) {
                    cosmeticManager.registerPackCosmetic(deco.toCosmeticItem());
                }
            }
        }

        packs.put(packId, pack);
    }

    private CosmeticType parseCosmeticType(String typeStr) {
        return switch (typeStr.toUpperCase()) {
            case "PROFILE_FRAME", "FRAME", "AVATAR_FRAME" -> CosmeticType.FRAME;
            case "NAMEPLATE", "BADGE", "TITLE", "LABEL" -> CosmeticType.BADGE;
            case "CHAT_BACKGROUND", "BUBBLE", "MESSAGE_BUBBLE" -> CosmeticType.BUBBLE;
            case "TEXT_COLOR", "COLOR", "PALETTE" -> CosmeticType.COLOR;
            default -> CosmeticType.BADGE;
        };
    }

    public PackAsset resolveAsset(String assetId) {
        if (assetId == null) return null;
        return globalAssets.get(assetId.toLowerCase());
    }

    public PackDecoration getDecoration(String decoId) {
        if (decoId == null) return null;
        return globalDecorations.get(decoId.toLowerCase());
    }

    public Map<String, PackConfig> getPacks() {
        return packs;
    }

    public Map<String, PackAsset> getGlobalAssets() {
        return globalAssets;
    }

    public Map<String, PackDecoration> getGlobalDecorations() {
        return globalDecorations;
    }

    private void generateDefaultPacks() {
        File cyberFile = new File(packsFolder, "cyberpunk.yml");
        if (!cyberFile.exists()) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("pack-id", "cyberpunk");
            yaml.set("display-name", "&#00FFFF⚡ Cyberpunk Edition");
            yaml.set("author", "TiNYsx");
            yaml.set("version", "1.0.0");

            yaml.set("assets.cyber_visor.char", "\uE001");
            yaml.set("assets.cyber_visor.prefix-shift", "%img_offset_-4%");
            yaml.set("assets.cyber_visor.suffix-shift", "%img_offset_4%");

            yaml.set("assets.cyber_bg_single.char", "\uE010");
            yaml.set("assets.cyber_bg_single.prefix-shift", "%img_offset_-16%");

            yaml.set("assets.cyber_bg_top.char", "\uE011");
            yaml.set("assets.cyber_bg_top.prefix-shift", "%img_offset_-16%");

            yaml.set("assets.cyber_bg_mid.char", "\uE012");
            yaml.set("assets.cyber_bg_mid.prefix-shift", "%img_offset_-16%");

            yaml.set("assets.cyber_bg_bot.char", "\uE013");
            yaml.set("assets.cyber_bg_bot.prefix-shift", "%img_offset_-16%");

            yaml.set("decorations.cyber_visor.type", "PROFILE_FRAME");
            yaml.set("decorations.cyber_visor.name", "&#00FFFF⚡ Cyber Hologram Visor");
            yaml.set("decorations.cyber_visor.permission", "personachat.pack.cyberpunk.visor");
            yaml.set("decorations.cyber_visor.description", Arrays.asList("&7A futuristic neon visor hologram.", "&7Anchored to your avatar."));
            yaml.set("decorations.cyber_visor.icon", "BEACON");
            yaml.set("decorations.cyber_visor.settings.asset", "cyber_visor");
            yaml.set("decorations.cyber_visor.settings.scale", 1.15);

            yaml.set("decorations.cyber_matrix_bubble.type", "CHAT_BACKGROUND");
            yaml.set("decorations.cyber_matrix_bubble.name", "&#00FFFF「 Cyber Matrix Bubble 」");
            yaml.set("decorations.cyber_matrix_bubble.permission", "personachat.pack.cyberpunk.bubble");
            yaml.set("decorations.cyber_matrix_bubble.description", Arrays.asList("&7Dynamic multi-line cyber box", "&7with 3-slice font images."));
            yaml.set("decorations.cyber_matrix_bubble.icon", "END_CRYSTAL");
            yaml.set("decorations.cyber_matrix_bubble.settings.mode", "IMAGE");
            yaml.set("decorations.cyber_matrix_bubble.settings.single-line-asset", "cyber_bg_single");
            yaml.set("decorations.cyber_matrix_bubble.settings.top-slice-asset", "cyber_bg_top");
            yaml.set("decorations.cyber_matrix_bubble.settings.middle-slice-asset", "cyber_bg_mid");
            yaml.set("decorations.cyber_matrix_bubble.settings.bottom-slice-asset", "cyber_bg_bot");
            yaml.set("decorations.cyber_matrix_bubble.settings.prefix-shift", "%img_offset_-16%");
            yaml.set("decorations.cyber_matrix_bubble.settings.format", "&#00FFFF「 &f{message} &#00FFFF」");
            yaml.set("decorations.cyber_matrix_bubble.settings.background-opacity", 120);

            yaml.set("decorations.netrunner_badge.type", "NAMEPLATE");
            yaml.set("decorations.netrunner_badge.name", "&#00FFFF[NetRunner] ✦");
            yaml.set("decorations.netrunner_badge.permission", "personachat.pack.cyberpunk.netrunner");
            yaml.set("decorations.netrunner_badge.description", Arrays.asList("&7Exclusive NetRunner title."));
            yaml.set("decorations.netrunner_badge.icon", "NETHER_STAR");
            yaml.set("decorations.netrunner_badge.settings.format", "&#00FFFF[NetRunner] &b✦ &7|");

            try {
                yaml.save(cyberFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
