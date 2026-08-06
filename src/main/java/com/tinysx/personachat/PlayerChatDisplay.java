package com.tinysx.personachat;

import com.tinysx.personachat.cosmetics.CosmeticItem;
import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.cosmetics.CosmeticType;
import com.tinysx.personachat.cosmetics.PlayerCosmeticProfile;
import com.tinysx.personachat.packs.ChatBackgroundSettings;
import com.tinysx.personachat.packs.PackDecoration;
import com.tinysx.personachat.packs.PackManager;
import com.tinysx.personachat.scene.AvatarNode;
import com.tinysx.personachat.scene.BackgroundSliceNode;
import com.tinysx.personachat.scene.ChatBoxScene;
import com.tinysx.personachat.scene.TextNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the floating chat display for a single player using an OOP Scene Graph hierarchy.
 */
public class PlayerChatDisplay {

    private final UUID ownerUUID;
    private final ChatDisplayConfig config;
    private final JavaPlugin plugin;
    private final CosmeticManager cosmeticManager;
    private final PackManager packManager;
    private boolean enabled;

    private Location lastLocation;
    private int stillTicks = 0;
    private boolean hiddenDueToMovement = false;

    // Full message history buffer (newest first, up to 50)
    private final LinkedList<ChatMessage> messageHistory = new LinkedList<>();
    private int scrollOffset = 0;

    // Active visible scene objects (newest in window first)
    private final LinkedList<ChatBoxScene> activeScenes = new LinkedList<>();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public PlayerChatDisplay(UUID ownerUUID, ChatDisplayConfig config, JavaPlugin plugin,
                             CosmeticManager cosmeticManager, PackManager packManager) {
        this.ownerUUID = ownerUUID;
        this.config = config;
        this.plugin = plugin;
        this.cosmeticManager = cosmeticManager;
        this.packManager = packManager;
        this.enabled = config.isEnabledByDefault();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hideAll();
        } else {
            refreshVisibleMessages();
        }
    }

    public boolean isStandingStill() {
        return stillTicks >= config.getStillTimeTicks();
    }

    public void scroll(int delta) {
        if (!enabled || messageHistory.isEmpty()) return;

        int maxMessages = config.getMaxMessages();
        int maxOffset = Math.max(0, messageHistory.size() - maxMessages);
        if (maxOffset <= 0) return;

        int newOffset = Math.max(0, Math.min(maxOffset, scrollOffset + delta));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            refreshVisibleMessages();

            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null && owner.isOnline()) {
                owner.playSound(owner.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
                int currentView = scrollOffset + 1;
                int totalViews = maxOffset + 1;
                owner.sendActionBar(Component.text("§6[§e▲ Chat Scroll: " + currentView + "/" + totalViews + " §e▼§6] §7(Walk to reset)"));
            }
        }
    }

    public void addMessage(ChatMessage msg) {
        if (!enabled) return;

        messageHistory.addFirst(msg);
        while (messageHistory.size() > 50) {
            messageHistory.removeLast();
        }

        if (scrollOffset > 0) {
            scrollOffset = Math.min(scrollOffset + 1, Math.max(0, messageHistory.size() - config.getMaxMessages()));
        }

        refreshVisibleMessages();

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            String soundStr = config.getSoundEffect();
            if (soundStr != null && !soundStr.isEmpty()) {
                try {
                    Sound sound = Sound.valueOf(soundStr.toUpperCase());
                    double t = System.currentTimeMillis() / 1000.0;
                    float pitch = config.getSoundPitch(0, t, 0, Math.random());
                    owner.playSound(owner.getLocation(), sound, config.getSoundVolume(), pitch);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    private void refreshVisibleMessages() {
        for (ChatBoxScene scene : activeScenes) {
            scene.destroy();
        }
        activeScenes.clear();

        if (!enabled || messageHistory.isEmpty()) return;

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner == null || !owner.isOnline()) return;

        int maxMessages = config.getMaxMessages();
        int start = Math.min(scrollOffset, messageHistory.size());
        int end = Math.min(start + maxMessages, messageHistory.size());

        List<ChatMessage> slice = messageHistory.subList(start, end);

        for (ChatMessage msg : slice) {
            ChatBoxScene scene = createChatBoxScene(owner, msg);
            activeScenes.add(scene);
        }
    }

    private ChatBoxScene createChatBoxScene(Player owner, ChatMessage msg) {
        ChatBoxScene scene = new ChatBoxScene(msg);
        int lineWidthPx = config.getLineLength() * 6;
        UUID senderUUID = msg.getSenderUUID();
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(senderUUID);
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        PlayerCosmeticProfile profile = cosmeticManager != null ? cosmeticManager.getProfile(senderUUID) : null;
        CosmeticItem badgeItem = (cosmeticManager != null && profile != null) ? cosmeticManager.getCosmetic(CosmeticType.BADGE, profile.getEquippedBadge()) : null;
        CosmeticItem bubbleItem = (cosmeticManager != null && profile != null) ? cosmeticManager.getCosmetic(CosmeticType.BUBBLE, profile.getEquippedBubble()) : null;
        CosmeticItem colorItem = (cosmeticManager != null && profile != null) ? cosmeticManager.getCosmetic(CosmeticType.COLOR, profile.getEquippedColor()) : null;
        CosmeticItem frameItem = (cosmeticManager != null && profile != null) ? cosmeticManager.getCosmetic(CosmeticType.FRAME, profile.getEquippedFrame()) : null;

        PackDecoration packBubble = (packManager != null && profile != null) ? packManager.getDecoration(profile.getEquippedBubble()) : null;
        ChatBackgroundSettings bgSettings = packBubble != null ? packBubble.getBackgroundSettings() : null;

        // 1. Format Name
        String badgePrefix = badgeItem != null ? badgeItem.getFormat() + " " : "";
        String rawNameTemplate = config.getNameFormat();
        if (rawNameTemplate.contains("{badge}")) {
            rawNameTemplate = rawNameTemplate.replace("{badge}", badgePrefix);
        } else {
            rawNameTemplate = badgePrefix + rawNameTemplate;
        }

        String rawNameStr = rawNameTemplate.replace("{player}", msg.getSenderName());
        if (hasPapi) {
            rawNameStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, rawNameStr);
        }
        String formattedNameStr = colorize(rawNameStr);

        // 2. Format Message & Calculate Lines
        String rawMsg = msg.getRawMessage();
        String bubbleFormatted = bubbleItem != null ? bubbleItem.getFormat().replace("{message}", rawMsg) : config.getMessageFormat().replace("{message}", rawMsg);
        if (hasPapi) {
            bubbleFormatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, bubbleFormatted);
        }

        TextWidthCalculator.RenderInfo nameInfo = TextWidthCalculator.calculate(formattedNameStr, "", lineWidthPx);
        scene.setNamePixelWidth(nameInfo.width);
        scene.setNameEstimatedLines(nameInfo.lines);

        TextWidthCalculator.RenderInfo msgInfo = TextWidthCalculator.calculate("", colorize(bubbleFormatted), lineWidthPx);
        scene.setMsgPixelWidth(msgInfo.width);
        scene.setMsgEstimatedLines(msgInfo.lines);

        // 3. Inject 3-Slice Font-Image Background
        if (bgSettings != null && bgSettings.isImageMode()) {
            BackgroundSliceNode bgNode = new BackgroundSliceNode(bgSettings, packManager);
            String bgGlyphs = bgNode.buildBackgroundGlyphs(msgInfo.lines);
            if (!bgGlyphs.isEmpty()) {
                if (hasPapi) {
                    bgGlyphs = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, bgGlyphs);
                }
                bubbleFormatted = bgGlyphs + bubbleFormatted;
            }
        }
        String formattedMsgStr = colorize(bubbleFormatted);

        Location spawnLoc = owner.getEyeLocation();
        double t = System.currentTimeMillis() / 1000.0;
        double l = 0;
        double r = scene.getRandomValue();

        // Spawn Avatar & Frame
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(offlinePlayer);
            headItem.setItemMeta(skullMeta);
        }

        ItemDisplay headEntity = owner.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(headItem);
            entity.setBillboard(Display.Billboard.CENTER);
            ItemDisplay.ItemDisplayTransform transform = config.getHeadModelType().equals("3D_BLOCK")
                    ? ItemDisplay.ItemDisplayTransform.HEAD
                    : ItemDisplay.ItemDisplayTransform.FIXED;
            entity.setItemDisplayTransform(transform);
            entity.setVisibleByDefault(false);
            entity.setViewRange(0.5f);
            entity.setShadowRadius(0f);
            entity.setShadowStrength(0f);
        });
        owner.showEntity(plugin, headEntity);
        if (hiddenDueToMovement) owner.hideEntity(plugin, headEntity);

        ItemDisplay frameEntity = null;
        if (frameItem != null && frameItem.getIconMaterial() != Material.AIR) {
            ItemStack frameStack = new ItemStack(frameItem.getIconMaterial());
            if (frameItem.getCustomModelData() > 0) {
                var fMeta = frameStack.getItemMeta();
                if (fMeta != null) {
                    fMeta.setCustomModelData(frameItem.getCustomModelData());
                    frameStack.setItemMeta(fMeta);
                }
            }

            frameEntity = owner.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
                entity.setItemStack(frameStack);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                entity.setVisibleByDefault(false);
                entity.setViewRange(0.5f);
                entity.setShadowRadius(0f);
                entity.setShadowStrength(0f);
            });
            owner.showEntity(plugin, frameEntity);
            if (hiddenDueToMovement) owner.hideEntity(plugin, frameEntity);
        }

        boolean isHalfBody = config.getHeadModelType().equalsIgnoreCase("2D_HALF_BODY");
        float headBaseScale = config.getHeadScale(0, t, l, r);
        float frameScaleMult = frameItem != null ? (float) frameItem.getFrameScale() : 1.0f;
        double frameOffsetY = frameItem != null ? frameItem.getFrameOffsetY() : 0.0;

        AvatarNode avatarNode = new AvatarNode(headEntity, frameEntity, isHalfBody, headBaseScale, frameScaleMult, frameOffsetY);
        scene.setAvatarNode(avatarNode);

        // Spawn Text Nodes
        String activeNameColorHex = (colorItem != null && !colorItem.getHexColor().isEmpty()) ? colorItem.getHexColor() : config.getNameColor();
        String activeMsgColorHex = (colorItem != null && !colorItem.getHexColor().isEmpty()) ? colorItem.getHexColor() : config.getMessageColor();
        int customBgOpacity = (bgSettings != null && bgSettings.getBackgroundOpacity() >= 0) ? bgSettings.getBackgroundOpacity() : (bubbleItem != null && bubbleItem.getBackgroundOpacity() >= 0 ? bubbleItem.getBackgroundOpacity() : config.getBackgroundOpacity(0, t, l, r));

        if (!formattedNameStr.isEmpty()) {
            TextColor nameColor = parseTextColor(activeNameColorHex, TextColor.color(255, 255, 85));
            Component nameComp = Component.text(formattedNameStr).color(nameColor);

            TextDisplay nameEntity = owner.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                entity.text(nameComp);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setLineWidth(lineWidthPx);
                entity.setBackgroundColor(Color.fromARGB(customBgOpacity, 0, 0, 0));
                entity.setSeeThrough(false);
                entity.setAlignment(TextDisplay.TextAlignment.valueOf(config.getNameAlignment()));
                entity.setVisibleByDefault(false);
                entity.setViewRange(0.5f);
                entity.setShadowRadius(0f);
                entity.setShadowStrength(0f);
            });
            owner.showEntity(plugin, nameEntity);
            if (hiddenDueToMovement) owner.hideEntity(plugin, nameEntity);

            TextNode nameNode = new TextNode(nameEntity, config.getTextScale(0, t, l, r), customBgOpacity);
            scene.setNameNode(nameNode);
        }

        if (!formattedMsgStr.isEmpty()) {
            TextColor msgColor = parseTextColor(activeMsgColorHex, TextColor.color(255, 255, 255));
            Component msgComp = Component.text(formattedMsgStr).color(msgColor);

            TextDisplay msgEntity = owner.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                entity.text(msgComp);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setLineWidth(lineWidthPx);
                entity.setBackgroundColor(Color.fromARGB(customBgOpacity, 0, 0, 0));
                entity.setSeeThrough(false);
                entity.setAlignment(TextDisplay.TextAlignment.valueOf(config.getMessageAlignment()));
                entity.setVisibleByDefault(false);
                entity.setViewRange(0.5f);
                entity.setShadowRadius(0f);
                entity.setShadowStrength(0f);
            });
            owner.showEntity(plugin, msgEntity);
            if (hiddenDueToMovement) owner.hideEntity(plugin, msgEntity);

            TextNode msgNode = new TextNode(msgEntity, config.getTextScale(0, t, l, r), customBgOpacity);
            scene.setMsgNode(msgNode);
        }

        return scene;
    }

    public void updatePositions(Player player) {
        if (activeScenes.isEmpty()) return;

        Location currentLoc = player.getLocation();
        boolean moved = false;
        if (lastLocation != null) {
            if (Math.abs(lastLocation.getX() - currentLoc.getX()) > 0.01 ||
                Math.abs(lastLocation.getY() - currentLoc.getY()) > 0.01 ||
                Math.abs(lastLocation.getZ() - currentLoc.getZ()) > 0.01) {
                moved = true;
            }
        }
        lastLocation = currentLoc;

        if (moved) {
            stillTicks = 0;
            if (scrollOffset != 0) {
                scrollOffset = 0;
                refreshVisibleMessages();
            }
        } else {
            stillTicks++;
        }

        if (config.getDisplayMode().equalsIgnoreCase("STANDING_STILL")) {
            if (moved) {
                if (!hiddenDueToMovement) {
                    hiddenDueToMovement = true;
                    setScenesVisible(false, player);
                }
                return;
            } else {
                if (hiddenDueToMovement) {
                    if (stillTicks >= config.getStillTimeTicks()) {
                        hiddenDueToMovement = false;
                        setScenesVisible(true, player);
                    } else {
                        return;
                    }
                }
            }
        } else {
            if (hiddenDueToMovement) {
                hiddenDueToMovement = false;
                setScenesVisible(true, player);
            }
        }

        Location eye = player.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector globalUp = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(globalUp).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vector(1, 0, 0);
        }
        Vector up = right.clone().crossProduct(forward).normalize();

        double yOffset = 0;
        double t = System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < activeScenes.size(); i++) {
            ChatBoxScene scene = activeScenes.get(i);
            double l = (System.currentTimeMillis() - scene.getSpawnTime()) / 1000.0;
            double r = scene.getRandomValue();

            Location base = eye.clone()
                    .add(forward.clone().multiply(config.getForwardDistance(i, t, l, r)))
                    .add(right.clone().multiply(config.getLeftOffset(i, t, l, r)))
                    .add(up.clone().multiply(config.getVerticalOffset(i, t, l, r)));

            float pYaw = (float) Math.toRadians(config.getPanelYaw(i, t, l, r));
            float pPitch = (float) Math.toRadians(config.getPanelPitch(i, t, l, r));
            float pRoll = (float) Math.toRadians(config.getPanelRoll(i, t, l, r));

            Vector localForward = forward.clone();
            Vector localRight = right.clone();
            Vector localUp = up.clone();

            Quaternionf panelRotQuat = new Quaternionf().rotateY(-pYaw).rotateX(-pPitch).rotateZ(-pRoll);
            if (pYaw != 0 || pPitch != 0 || pRoll != 0) {
                Vector3f f3 = panelRotQuat.transform(new Vector3f((float) forward.getX(), (float) forward.getY(), (float) forward.getZ()));
                Vector3f r3 = panelRotQuat.transform(new Vector3f((float) right.getX(), (float) right.getY(), (float) right.getZ()));
                Vector3f u3 = panelRotQuat.transform(new Vector3f((float) up.getX(), (float) up.getY(), (float) up.getZ()));
                localForward = new Vector(f3.x, f3.y, f3.z);
                localRight = new Vector(r3.x, r3.y, r3.z);
                localUp = new Vector(u3.x, u3.y, u3.z);
            }

            double mathX = config.getMathOffsetRight(i, t, l, r);
            double mathY = config.getMathOffsetUp(i, t, l, r);
            double mathZ = config.getMathOffsetForward(i, t, l, r);

            Vector totalOffset = localUp.clone().multiply(yOffset + mathY)
                    .add(localRight.clone().multiply(mathX))
                    .add(localForward.clone().multiply(mathZ));

            Location msgLoc = base.clone().add(totalOffset);

            float tScale = config.getTextScale(i, t, l, r);
            float hScale = config.getHeadScale(i, t, l, r);

            if (scene.getNameNode() != null) {
                scene.getNameNode().setLocalPosition(new Vector(0, 0, 0));
                scene.getNameNode().setLocalScale(new Vector3f(tScale, tScale, tScale));
            }

            if (scene.getMsgNode() != null) {
                double nameShiftY = scene.getNameNode() != null ? (scene.getNameEstimatedLines() * 0.25 * tScale) + config.getLineSpacing(i, t, l, r) : 0;
                scene.getMsgNode().setLocalPosition(new Vector(0, -nameShiftY, 0));
                scene.getMsgNode().setLocalScale(new Vector3f(tScale, tScale, tScale));
            }

            if (scene.getAvatarNode() != null) {
                double headLeftOffset = config.getHeadLeftOffset(i, t, l, r);
                double headVertOffset = config.getHeadVerticalOffset(i, t, l, r);
                scene.getAvatarNode().setLocalPosition(new Vector(-headLeftOffset, headVertOffset, 0));

                float hYaw = (float) Math.toRadians(config.getHeadYaw(i, t, l, r));
                float hPitch = (float) Math.toRadians(config.getHeadPitch(i, t, l, r));
                float hRoll = (float) Math.toRadians(config.getHeadRoll(i, t, l, r));
                Quaternionf headRotQuat = new Quaternionf().rotateY(-hYaw).rotateX(-hPitch).rotateZ(-hRoll);
                scene.getAvatarNode().setLocalRotation(headRotQuat);
                scene.getAvatarNode().setLocalScale(new Vector3f(hScale, hScale, hScale));
            }

            scene.updateWorldTransform(msgLoc, panelRotQuat, new Vector3f(1, 1, 1), localForward, localRight, localUp);

            yOffset += scene.calculateTotalHeight(config.getLineSpacing(i, t, l, r), tScale) + config.getMessageGap(i, t, l, r);
        }
    }

    public void removeExpiredMessages() {
        long lifetimeMs = config.getMessageLifetimeSeconds() * 1000L;
        if (lifetimeMs <= 0) return;

        boolean removed = messageHistory.removeIf(msg -> msg.isExpired(lifetimeMs));
        if (removed) {
            refreshVisibleMessages();
        }
    }

    private void setScenesVisible(boolean visible, Player owner) {
        for (ChatBoxScene scene : activeScenes) {
            scene.setVisible(visible, owner, plugin);
        }
    }

    public void hideAll() {
        for (ChatBoxScene scene : activeScenes) {
            scene.destroy();
        }
        activeScenes.clear();
    }

    public void destroy() {
        hideAll();
        messageHistory.clear();
    }

    private static String colorize(String text) {
        if (text == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace('&', '§');
    }

    private static TextColor parseTextColor(String hexStr, TextColor fallback) {
        if (hexStr == null || hexStr.isEmpty()) return fallback;
        try {
            TextColor color = TextColor.fromHexString(hexStr);
            return color != null ? color : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
