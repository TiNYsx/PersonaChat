package com.tinysx.personachat;

import com.tinysx.personachat.cosmetics.CosmeticItem;
import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.cosmetics.CosmeticType;
import com.tinysx.personachat.cosmetics.PlayerCosmeticProfile;
import com.tinysx.personachat.packs.ChatBackgroundSettings;
import com.tinysx.personachat.packs.PackAsset;
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

    private boolean debugMode = false;

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

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean toggleDebugMode() {
        this.debugMode = !this.debugMode;
        return this.debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
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

        double initialYOffset = 0.0;
        double t = System.currentTimeMillis() / 1000.0;

        for (int i = 0; i < slice.size(); i++) {
            ChatMessage msg = slice.get(i);
            ChatBoxScene scene = createChatBoxScene(owner, msg, i, initialYOffset);
            activeScenes.add(scene);

            double r = scene.getRandomValue();
            float tScale = config.getTextScale(i, t, 0, r);
            float hScale = config.getHeadScale(i, t, 0, r);
            double messageGap = config.getMessageGap(i, t, 0, r);
            double lineHeight = config.getLineHeight(i, t, 0, r);
            boolean is3D = config.getHeadModelType().equalsIgnoreCase("3D_BLOCK");
            initialYOffset += scene.calculateTotalHeight(tScale, hScale, lineHeight, is3D) + messageGap;
        }

        // Immediately position all entities in the current server tick so they never spawn at player's face
        updatePositions(owner);
    }

    private ChatBoxScene createChatBoxScene(Player owner, ChatMessage msg, int index, double initialYOffset) {
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
        PackDecoration packFrame = (packManager != null && profile != null) ? packManager.getDecoration(profile.getEquippedFrame()) : null;
        ChatBackgroundSettings bgSettings = packBubble != null ? packBubble.getBackgroundSettings() : null;

        // 1. Format Name (PAPI FIRST)
        String badgePrefix = badgeItem != null ? badgeItem.getFormat() + " " : "";
        String rawNameTemplate = config.getNameFormat();
        if (rawNameTemplate.contains("{badge}")) {
            rawNameTemplate = rawNameTemplate.replace("{badge}", badgePrefix);
        } else {
            rawNameTemplate = badgePrefix + rawNameTemplate;
        }

        if (hasPapi) {
            rawNameTemplate = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, rawNameTemplate);
        }
        String rawNameStr = rawNameTemplate.replace("{player}", msg.getSenderName());
        String formattedNameStr = colorize(rawNameStr);

        // 2. Format Message (PAPI FIRST)
        String rawMsg = msg.getRawMessage();
        String bubbleTemplate = bubbleItem != null ? bubbleItem.getFormat() : config.getMessageFormat();

        if (hasPapi) {
            bubbleTemplate = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, bubbleTemplate);
        }
        String finalMsgStr = bubbleTemplate.replace("{message}", rawMsg);
        String formattedMsgStr = colorize(finalMsgStr);

        // 3. Build unified card text (Name + Message merged into one TextDisplay)
        String activeNameColorHex = (colorItem != null && !colorItem.getHexColor().isEmpty()) ? colorItem.getHexColor() : config.getNameColor();
        String activeMsgColorHex = (colorItem != null && !colorItem.getHexColor().isEmpty()) ? colorItem.getHexColor() : config.getMessageColor();

        String nameColorCode = hexToSectionColor(activeNameColorHex);
        String msgColorCode = hexToSectionColor(activeMsgColorHex);

        String coloredName = nameColorCode + formattedNameStr;
        String coloredMsg = msgColorCode + formattedMsgStr;

        // Use formatUnifiedCard to merge name + message with padding
        TextWidthCalculator.FormattedTextResult cardResult = TextWidthCalculator.formatUnifiedCard(
                coloredName,
                coloredMsg,
                lineWidthPx,
                config.isFillLineWidth(),
                config.getLayoutAlignment(),
                config.getPaddingX(),
                config.getPaddingY()
        );
        scene.setTotalLines(cardResult.lines);
        scene.setPixelWidth(cardResult.pixelWidth);
        String cardText = cardResult.text;



        // Precompute accurate world spawn location to prevent teleporting from player's face
        Location eye = owner.getEyeLocation();
        Vector forward = eye.getDirection().normalize();
        Vector globalUp = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(globalUp).normalize();
        if (right.lengthSquared() < 0.001) right = new Vector(1, 0, 0);
        Vector up = right.clone().crossProduct(forward).normalize();

        double t = System.currentTimeMillis() / 1000.0;
        double l = 0;
        double r = scene.getRandomValue();

        Location base = eye.clone()
                .add(forward.clone().multiply(config.getForwardDistance(index, t, l, r)))
                .add(right.clone().multiply(config.getLeftOffset(index, t, l, r)))
                .add(up.clone().multiply(config.getVerticalOffset(index, t, l, r)));

        double mathX = config.getMathOffsetRight(index, t, l, r);
        double mathY = config.getMathOffsetUp(index, t, l, r);
        double mathZ = config.getMathOffsetForward(index, t, l, r);

        Vector totalOffset = up.clone().multiply(initialYOffset + mathY)
                .add(right.clone().multiply(mathX))
                .add(forward.clone().multiply(mathZ));

        Location spawnLoc = base.clone().add(totalOffset);

        // Spawn Avatar & Frame
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(offlinePlayer);
            headItem.setItemMeta(skullMeta);
        }

        int updateTicks = config.getUpdateTicks();

        ItemDisplay headEntity = owner.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(headItem);
            entity.setBillboard(Display.Billboard.CENTER);
            ItemDisplay.ItemDisplayTransform transform = config.getHeadModelType().equals("3D_BLOCK")
                    ? ItemDisplay.ItemDisplayTransform.HEAD
                    : ItemDisplay.ItemDisplayTransform.FIXED;
            entity.setItemDisplayTransform(transform);
            entity.setVisibleByDefault(false);
            entity.setViewRange(2.0f);
            entity.setPersistent(false);
            entity.setTeleportDuration(0);
            entity.setInterpolationDuration(0);
            entity.setShadowRadius(0f);
            entity.setShadowStrength(0f);
        });
        owner.showEntity(plugin, headEntity);
        if (hiddenDueToMovement) owner.hideEntity(plugin, headEntity);

        Display frameEntity = null;
        if (frameItem != null) {
            String frameFormat = frameItem.getFormat();
            if (frameFormat.isEmpty() && packFrame != null && !packFrame.getAssetRef().isEmpty()) {
                PackAsset asset = packManager.resolveAsset(packFrame.getAssetRef());
                if (asset != null) frameFormat = asset.getFormatted();
            }

            if (!frameFormat.isEmpty()) {
                // Font-Image Frame (TextDisplay)
                String formattedFrame = frameFormat;
                if (hasPapi) {
                    formattedFrame = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(offlinePlayer, formattedFrame);
                }
                formattedFrame = colorize(formattedFrame);
                final String finalFrameStr = formattedFrame;

                TextDisplay textFrame = owner.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                    entity.text(Component.text(finalFrameStr));
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                    entity.setSeeThrough(false);
                    entity.setVisibleByDefault(false);
                    entity.setViewRange(2.0f);
                    entity.setPersistent(false);
                    entity.setTeleportDuration(0);
                    entity.setInterpolationDuration(0);
                    entity.setShadowRadius(0f);
                    entity.setShadowStrength(0f);
                });
                owner.showEntity(plugin, textFrame);
                if (hiddenDueToMovement) owner.hideEntity(plugin, textFrame);
                frameEntity = textFrame;
            } else if (frameItem.getIconMaterial() != Material.AIR && frameItem.getIconMaterial() != Material.PAPER) {
                // 3D Item Frame (ItemDisplay)
                ItemStack frameStack = new ItemStack(frameItem.getIconMaterial());
                if (frameItem.getCustomModelData() > 0) {
                    var fMeta = frameStack.getItemMeta();
                    if (fMeta != null) {
                        fMeta.setCustomModelData(frameItem.getCustomModelData());
                        frameStack.setItemMeta(fMeta);
                    }
                }

                ItemDisplay itemFrame = owner.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
                    entity.setItemStack(frameStack);
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    entity.setVisibleByDefault(false);
                    entity.setViewRange(2.0f);
                    entity.setPersistent(false);
                    entity.setTeleportDuration(0);
                    entity.setInterpolationDuration(0);
                    entity.setShadowRadius(0f);
                    entity.setShadowStrength(0f);
                });
                owner.showEntity(plugin, itemFrame);
                if (hiddenDueToMovement) owner.hideEntity(plugin, itemFrame);
                frameEntity = itemFrame;
            }
        }

        boolean isHalfBody = config.getHeadModelType().equalsIgnoreCase("2D_HALF_BODY");
        float headBaseScale = config.getHeadScale(index, t, l, r);
        float frameScaleMult = frameItem != null ? (float) frameItem.getFrameScale() : 1.0f;
        double frameOffsetY = frameItem != null ? frameItem.getFrameOffsetY() : 0.0;

        AvatarNode avatarNode = new AvatarNode(headEntity, frameEntity, isHalfBody, headBaseScale, frameScaleMult, frameOffsetY, updateTicks);
        scene.setAvatarNode(avatarNode);

        // Spawn unified card TextDisplay (Name + Message in one entity)
        int customBgOpacity = (bgSettings != null && bgSettings.getBackgroundOpacity() >= 0) ? bgSettings.getBackgroundOpacity() : (bubbleItem != null && bubbleItem.getBackgroundOpacity() >= 0 ? bubbleItem.getBackgroundOpacity() : config.getBackgroundOpacity(index, t, l, r));

        TextDisplay.TextAlignment cardAlign;
        try {
            cardAlign = TextDisplay.TextAlignment.valueOf(config.getLayoutAlignment());
        } catch (Exception e) {
            cardAlign = TextDisplay.TextAlignment.LEFT;
        }

        final TextDisplay.TextAlignment finalCardAlign = cardAlign;
        final String finalCardText = cardText;
        final int finalLineWidthPx = lineWidthPx + (config.getPaddingX() * 8);

        // 4. Spawn Background TextDisplay layer (if Image Mode is active)
        if (bgSettings != null && bgSettings.isImageMode()) {
            BackgroundSliceNode bgNode = new BackgroundSliceNode(bgSettings, packManager);
            String bgText = bgNode.buildBackgroundLines(cardResult.lines, offlinePlayer);
            if (!bgText.isEmpty()) {
                TextDisplay bgCardEntity = owner.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
                    entity.text(Component.text(bgText));
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setLineWidth(2000); // Prevent line-wrapping so background slices never shatter
                    entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    entity.setSeeThrough(false);
                    entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                    entity.setVisibleByDefault(false);
                    entity.setViewRange(2.0f);
                    entity.setPersistent(false);
                    entity.setTeleportDuration(0);
                    entity.setInterpolationDuration(0);
                    entity.setShadowRadius(0f);
                    entity.setShadowStrength(0f);
                });
                owner.showEntity(plugin, bgCardEntity);
                if (hiddenDueToMovement) owner.hideEntity(plugin, bgCardEntity);

                TextNode bgCardNode = new TextNode(bgCardEntity, config.getTextScale(index, t, l, r), 0, updateTicks);
                scene.setBgCardNode(bgCardNode);
            }
        }

        // When image background is active, set foreground text background opacity to 0
        int textBgOpacity = (bgSettings != null && bgSettings.isImageMode()) ? 0 : customBgOpacity;

        TextDisplay cardEntity = owner.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
            entity.text(Component.text(finalCardText));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setLineWidth(finalLineWidthPx);
            entity.setBackgroundColor(Color.fromARGB(textBgOpacity, 0, 0, 0));
            entity.setSeeThrough(false);
            entity.setAlignment(finalCardAlign);
            entity.setVisibleByDefault(false);
            entity.setViewRange(2.0f);
            entity.setPersistent(false);
            entity.setTeleportDuration(0);
            entity.setInterpolationDuration(0);
            entity.setShadowRadius(0f);
            entity.setShadowStrength(0f);
        });
        owner.showEntity(plugin, cardEntity);
        if (hiddenDueToMovement) owner.hideEntity(plugin, cardEntity);

        TextNode cardNode = new TextNode(cardEntity, config.getTextScale(index, t, l, r), textBgOpacity, updateTicks);
        scene.setCardNode(cardNode);

        return scene;
    }

    public void updatePositions(Player player) {
        if (activeScenes.isEmpty()) return;

        Location currentLoc = player.getLocation();

        // Cross-world or far-teleport detection: safely purge old world scenes and recreate
        boolean worldChanged = lastLocation != null && lastLocation.getWorld() != null && !lastLocation.getWorld().equals(currentLoc.getWorld());
        boolean farTeleport = lastLocation != null && lastLocation.getWorld() != null && lastLocation.getWorld().equals(currentLoc.getWorld()) && lastLocation.distanceSquared(currentLoc) > 2500;

        if (worldChanged || farTeleport) {
            hideAll();
            lastLocation = currentLoc;
            refreshVisibleMessages();
            return;
        }

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

            float tScale = config.getTextScale(i, t, l, r);
            float hScale = config.getHeadScale(i, t, l, r);
            double messageGap = config.getMessageGap(i, t, l, r);
            double headToNameGap = config.getHeadToNameGap(i, t, l, r);
            double lineHeight = config.getLineHeight(i, t, l, r);

            String layout = config.getLayoutAlignment();
            boolean is3D = config.getHeadModelType().equalsIgnoreCase("3D_BLOCK");
            double avatarFactor = is3D ? 1.0 : 0.5;

            // Calculate card & avatar dimensions in blocks
            double cardWidthBlocks = (scene.getPixelWidth() + 2) * 0.025 * tScale;
            double cardHeight = (scene.getTotalLines() > 0) ? ((scene.getTotalLines() * lineHeight + 0.025) * tScale) : 0;
            double avatarHeight = (scene.getAvatarNode() != null) ? (hScale * avatarFactor) : 0;
            double avatarWidth = avatarHeight;

            double totalSceneHeight = Math.max(cardHeight, avatarHeight);

            double headVerticalOffset = config.getHeadVerticalOffset(i, t, l, r);
            double headHorizontalOffset = config.getHeadHorizontalOffset(i, t, l, r);

            // Local coordinates for components (Bottom-anchored at local y=0)
            double cardX = 0;
            double cardY = 0; // TextDisplay renders upwards from local y=0 to local y=cardHeight

            double headX = headHorizontalOffset;
            // Top Anchor: Align the top of the avatar with the top of the text card (local y=totalSceneHeight)
            double headY = totalSceneHeight - (avatarHeight / 2.0) + headVerticalOffset; // ItemDisplay centers at headY

            if (layout.equalsIgnoreCase("RIGHT")) {
                // Head on the right, card extends to the left
                double cardRightEdge = (scene.getAvatarNode() != null) ? (-(avatarWidth / 2.0) - headToNameGap) : 0;
                cardX = cardRightEdge - (cardWidthBlocks / 2.0);
                headX = headHorizontalOffset;
            } else if (layout.equalsIgnoreCase("CENTER")) {
                // Card centered, head placed to the left of card
                headX = (-(cardWidthBlocks / 2.0) - headToNameGap - (avatarWidth / 2.0)) + headHorizontalOffset;
            } else {
                // LEFT: Head on the left, card extends to the right
                double cardLeftEdge = (scene.getAvatarNode() != null) ? ((avatarWidth / 2.0) + headToNameGap) : 0;
                cardX = cardLeftEdge + (cardWidthBlocks / 2.0);
                headX = headHorizontalOffset;
            }

            // Place scene origin (local y=0, the bottom edge) directly at yOffset
            double currentSceneOriginY = yOffset;

            Vector totalOffset = localUp.clone().multiply(currentSceneOriginY + mathY)
                    .add(localRight.clone().multiply(mathX))
                    .add(localForward.clone().multiply(mathZ));

            Location msgLoc = base.clone().add(totalOffset);

            // Apply card transform (foreground text placed in front towards the viewer)
            if (scene.getCardNode() != null) {
                scene.getCardNode().setLocalPosition(new Vector(cardX, cardY, -0.005));
                scene.getCardNode().setLocalScale(new Vector3f(tScale, tScale, tScale));
            }

            // Apply background slice card transform (background image placed behind the text away from viewer)
            if (scene.getBgCardNode() != null) {
                scene.getBgCardNode().setLocalPosition(new Vector(cardX, cardY, 0.005));
                scene.getBgCardNode().setLocalScale(new Vector3f(tScale, tScale, tScale));
            }

            // Apply avatar transform
            if (scene.getAvatarNode() != null) {
                scene.getAvatarNode().setLocalPosition(new Vector(headX, headY, 0));

                float hYaw = (float) Math.toRadians(config.getHeadYaw(i, t, l, r));
                float hPitch = (float) Math.toRadians(config.getHeadPitch(i, t, l, r));
                float hRoll = (float) Math.toRadians(config.getHeadRoll(i, t, l, r));
                Quaternionf headRotQuat = new Quaternionf().rotateY(-hYaw).rotateX(-hPitch).rotateZ(-hRoll);
                scene.getAvatarNode().setLocalRotation(headRotQuat);
                scene.getAvatarNode().setLocalScale(new Vector3f(hScale, hScale, hScale));
            }

            scene.updateWorldTransform(msgLoc, panelRotQuat, new Vector3f(1, 1, 1), localForward, localRight, localUp);

            // Real-time Visual Particles in Debug Mode
            if (debugMode) {
                player.spawnParticle(org.bukkit.Particle.FLAME, base, 1, 0, 0, 0, 0);
                player.spawnParticle(org.bukkit.Particle.END_ROD, msgLoc, 1, 0, 0, 0, 0);
                if (scene.getAvatarNode() != null && scene.getAvatarNode().getHeadEntity() != null) {
                    player.spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, scene.getAvatarNode().getHeadEntity().getLocation(), 1, 0, 0, 0, 0);
                }
            }

            yOffset += scene.calculateTotalHeight(tScale, hScale, lineHeight, is3D) + messageGap;
        }




        // Real-time Action Bar HUD in Debug Mode
        if (debugMode) {
            double dist = activeScenes.isEmpty() ? 0 : eye.distance(eye.clone().add(forward.clone().multiply(config.getForwardDistance(0, t, 0, 0))));
            String hud = String.format(
                    "§6[PC DEBUG] §fEye: §b(%.1f, %.1f, %.1f) §fY/P: §e%.0f°/%.0f° §7| §fDist: §a%.2fm §7| §fStill: §d%s(%dt) §7| §fScenes: §b%d",
                    eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch(), dist,
                    hiddenDueToMovement ? "HIDDEN" : "SHOW", stillTicks, activeScenes.size()
            );
            player.sendActionBar(Component.text(hud));
        }
    }

    public void sendDebugReport(Player player) {
        Location eye = player.getEyeLocation();
        double t = System.currentTimeMillis() / 1000.0;
        player.sendMessage("§6§m----------------[ §ePersonaChat Diagnostics §6§m]----------------");
        player.sendMessage("§ePlayer: §f" + player.getName() + " §7(UUID: " + ownerUUID + ")");
        player.sendMessage("§eDisplay Enabled: §f" + enabled + " §7| §eDebug HUD: §f" + debugMode);
        player.sendMessage("§eDisplay Mode: §b" + config.getDisplayMode() + " §7| §eStill Ticks: §b" + stillTicks + "/" + config.getStillTimeTicks() + " §7| §eHidden: §c" + hiddenDueToMovement);
        player.sendMessage("§eEye Pos: §fX=§a" + String.format("%.2f", eye.getX()) + " §fY=§a" + String.format("%.2f", eye.getY()) + " §fZ=§a" + String.format("%.2f", eye.getZ()) + " §7(Yaw: §e" + String.format("%.1f", eye.getYaw()) + "°§7, Pitch: §e" + String.format("%.1f", eye.getPitch()) + "°§7)");
        player.sendMessage("§eConfig: §7Forward=§a" + config.getForwardDistance(0, t, 0, 0) + "m§7, Left=§a" + config.getLeftOffset(0, t, 0, 0) + "m§7, Vert=§a" + config.getVerticalOffset(0, t, 0, 0) + "m§7, UpdateTicks=§a" + config.getUpdateTicks());
        player.sendMessage("§eActive Scenes: §6" + activeScenes.size() + " §7| §eHistory Buffer: §6" + messageHistory.size() + " §7(Scroll Offset: §e" + scrollOffset + "§7)");
        
        int idx = 0;
        for (ChatBoxScene sc : activeScenes) {
            idx++;
            player.sendMessage(" §7▶ §eScene #" + idx + " §7[Msg: '§f" + sc.getMessage().getRawMessage() + "§7']:");
            if (sc.getAvatarNode() != null) {
                var h = sc.getAvatarNode().getHeadEntity();
                var f = sc.getAvatarNode().getFrameEntity();
                player.sendMessage("   §7• §bAvatar: §7HeadId=" + (h != null ? h.getEntityId() : "none") + ", HeadDead=" + (h != null && h.isDead()) + ", FrameId=" + (f != null ? f.getEntityId() : "none"));
            }
            if (sc.getCardNode() != null) {
                var c = sc.getCardNode().getTextDisplay();
                player.sendMessage("   §7• §aCardDisplay: §7Id=" + (c != null ? c.getEntityId() : "none") + ", Dead=" + (c != null && c.isDead()) + ", Lines=" + sc.getTotalLines() + ", WidthPx=" + sc.getPixelWidth());
            }
        }
        player.sendMessage("§6§m--------------------------------------------------");
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

    private static String hexToSectionColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) return "";
        String hex = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;
        if (hex.length() != 6) return "";
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
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
