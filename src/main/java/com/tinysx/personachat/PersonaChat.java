package com.tinysx.personachat;

import com.tinysx.personachat.cosmetics.CosmeticManager;
import com.tinysx.personachat.gui.CosmeticsMenu;
import com.tinysx.personachat.packs.PackManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PersonaChat extends JavaPlugin {

    private static PersonaChat instance;
    private ChatDisplayManager chatDisplayManager;
    private ChatDisplayConfig chatDisplayConfig;
    private CosmeticManager cosmeticManager;
    private PackManager packManager;
    private CosmeticsMenu cosmeticsMenu;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        // Load config
        chatDisplayConfig = new ChatDisplayConfig();
        chatDisplayConfig.load(getConfig());

        // Initialize cosmetics & packs systems
        cosmeticManager = new CosmeticManager(this);
        cosmeticManager.load();

        packManager = new PackManager(this, cosmeticManager);
        packManager.loadAll();

        cosmeticsMenu = new CosmeticsMenu(cosmeticManager);
        getServer().getPluginManager().registerEvents(cosmeticsMenu, this);

        // Initialize chat display manager
        chatDisplayManager = new ChatDisplayManager(this, chatDisplayConfig, cosmeticManager, packManager);
        chatDisplayManager.startUpdater();

        // Register event listeners
        getServer().getPluginManager().registerEvents(
                new ChatEventListener(this, chatDisplayManager), this
        );

        // Register commands
        ChatDisplayCommand cmdExecutor = new ChatDisplayCommand(chatDisplayManager, cosmeticManager, cosmeticsMenu);
        if (getCommand("personachat") != null) {
            getCommand("personachat").setExecutor(cmdExecutor);
            getCommand("personachat").setTabCompleter(cmdExecutor);
        }
        if (getCommand("headd") != null) {
            getCommand("headd").setExecutor(new PlayerHeadDisplayCommand());
        }

        // Create displays for any already-online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            chatDisplayManager.onPlayerJoin(player);
        }

        getLogger().info("==================================================");
        getLogger().info("  PersonaChat by TiNYsx - Enabled successfully!   ");
        getLogger().info("  Modular Packs: " + packManager.getPacks().size() + " | Font Assets: " + packManager.getGlobalAssets().size());
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        if (chatDisplayManager != null) {
            chatDisplayManager.shutdown();
        }
        if (cosmeticManager != null) {
            cosmeticManager.saveAll();
        }
        PlayerHeadDisplayCommand.cleanupAll();
        getLogger().info("PersonaChat disabled.");
    }

    public static PersonaChat getInstance() {
        return instance;
    }

    public ChatDisplayManager getChatDisplayManager() {
        return chatDisplayManager;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public PackManager getPackManager() {
        return packManager;
    }

    public CosmeticsMenu getCosmeticsMenu() {
        return cosmeticsMenu;
    }
}
