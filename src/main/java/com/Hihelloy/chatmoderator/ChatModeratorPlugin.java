package com.Hihelloy.chatmoderator;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.Hihelloy.chatmoderator.config.ConfigManager;
import com.Hihelloy.chatmoderator.listeners.ChatListener;
import com.Hihelloy.chatmoderator.services.ModerationService;
import com.Hihelloy.chatmoderator.commands.ChatModCommand;
import com.Hihelloy.chatmoderator.utils.SchedulerUtil;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class ChatModeratorPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ModerationService moderationService;
    private SchedulerUtil schedulerUtil;
    private ChatListener chatListener;

    public static ChatModeratorPlugin plugin;
    public static ServerImplementation scheduler;
    public static Logger log;

    @Override
    public void onEnable() {
        plugin = this;
        log = getLogger();

        scheduler = new FoliaCompatibility(this).getServerImplementation();

        schedulerUtil = new SchedulerUtil(this);
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        moderationService = new ModerationService(this, configManager);

        chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        ChatModCommand cmd = new ChatModCommand(this, chatListener);
        getCommand("chatmod").setExecutor(cmd);
        getCommand("chatmod").setTabCompleter(cmd);

        log.info(color(configManager.getPluginEnabled()));
        checkAPIKeys();
    }

    @Override
    public void onDisable() {
        SchedulerUtil.shutdown();
        log.info(color(configManager.getPluginDisabled()));
    }

    public void reloadPluginConfig() {
        configManager.reloadConfig();
        moderationService.initClients();
        log.info(color(configManager.getPluginReloaded()));
    }

    private void checkAPIKeys() {
        String provider = configManager.getPreferredAIProvider();
        String openaiKey = configManager.getOpenAIApiKey();
        String geminiKey = configManager.getGeminiApiKey();

        if ("openai".equalsIgnoreCase(provider)
                && (openaiKey == null || openaiKey.equals("your-openai-api-key-here") || openaiKey.isEmpty())) {
            log.warning("OpenAI API key not configured — AI moderation disabled until you add it to config.yml.");
        } else if ("gemini".equalsIgnoreCase(provider)
                && (geminiKey == null || geminiKey.equals("your-gemini-api-key-here") || geminiKey.isEmpty())) {
            log.warning("Gemini API key not configured — AI moderation disabled until you add it to config.yml.");
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public SchedulerUtil getSchedulerUtil() {
        return schedulerUtil;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}