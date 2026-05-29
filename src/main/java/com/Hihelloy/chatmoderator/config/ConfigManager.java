package com.Hihelloy.chatmoderator.config;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {

    private final ChatModeratorPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        applyNewConfigOptions();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        applyNewConfigOptions();
    }

    public void applyNewConfigOptions() {

        if (!config.contains("ai.preferred-provider")) {
            config.set("ai.preferred-provider", "openai");
        }
        if (!config.contains("moderation.use-ai-moderation")) {
            config.set("moderation.use-ai-moderation", true);
        }
        if (!config.contains("moderation.use-word-filter")) {
            config.set("moderation.use-word-filter", true);
        }
        if (!config.contains("actions.block-message")) {
            config.set("actions.block-message", true);
        }
        if (!config.contains("actions.warn-player")) {
            config.set("actions.warn-player", true);
        }
        if (!config.contains("actions.notify-admins")) {
            config.set("actions.notify-admins", true);
        }
        if (!config.contains("actions.log-violations")) {
            config.set("actions.log-violations", true);
        }
        if (!config.contains("debug.enabled")) {
            config.set("debug.enabled", false);
        }
        if (!config.contains("debug.log-all-messages")) {
            config.set("debug.log-all-messages", false);
        }
        if (!config.contains("moderation.mute-duration-seconds")) {
            config.set("moderation.mute-duration-seconds", 600);
        }
        if (!config.contains("messages.message-blocked")) {
            config.set("messages.message-blocked", "&cYour message was blocked by the chat filter.");
        }
        if (!config.contains("messages.violation-warning")) {
            config.set("messages.violation-warning", "&eYour message contains inappropriate content. You have been muted, ask an admin for an unmute.");
        }
        if (!config.contains("messages.admin-notification")) {
            config.set("messages.admin-notification", "&6[ChatMod] &c{player} &7tried to send: &f{message}");
        }
        if (!config.contains("messages.plugin-reloaded")) {
            config.set("messages.plugin-reloaded", "&aChat Moderator configuration reloaded!");
        }
        if (!config.contains("messages.plugin-enabled")) {
            config.set("messages.plugin-enabled", "&aChat moderation enabled!");
        }
        if (!config.contains("messages.plugin-disabled")) {
            config.set("messages.plugin-disabled", "&cChat moderation disabled!");
        }

        plugin.saveConfig();
    }

    public String getOpenAIApiKey() {
        return config.getString("openai.api-key", "");
    }

    public String getOpenAIModel() {
        return config.getString("openai.model", "text-moderation-latest");
    }

    public String getGeminiApiKey() {
        return config.getString("gemini.api-key", "");
    }

    public String getGeminiModel() {
        return config.getString("gemini.model", "gemini-1.5-flash");
    }

    public String getPreferredAIProvider() {
        return config.getString("ai.preferred-provider", "openai");
    }

    public boolean isModerationEnabled() {
        return config.getBoolean("moderation.enabled", true);
    }

    public boolean isAIModerationEnabled() {
        return config.getBoolean("moderation.use-ai-moderation", true);
    }

    public boolean isWordFilterEnabled() {
        return config.getBoolean("moderation.use-word-filter", true);
    }

    public List<String> getBlockedWords() {
        return config.getStringList("moderation.blocked-words");
    }

    public Map<String, Double> getModerationThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        if (config.isConfigurationSection("moderation.thresholds")) {
            for (String key : config.getConfigurationSection("moderation.thresholds").getKeys(false)) {
                thresholds.put(key, config.getDouble("moderation.thresholds." + key));
            }
        }
        return thresholds;
    }

    public boolean shouldBlockMessage() {
        return config.getBoolean("actions.block-message", true);
    }

    public boolean shouldWarnPlayer() {
        return config.getBoolean("actions.warn-player", true);
    }

    public boolean shouldNotifyAdmins() {
        return config.getBoolean("actions.notify-admins", true);
    }

    public boolean shouldLogViolations() {
        return config.getBoolean("actions.log-violations", true);
    }

    public String getMessageBlocked() {
        return config.getString("messages.message-blocked", "&cYour message was blocked by the chat filter.");
    }

    public String getViolationWarning() {
        return config.getString("messages.violation-warning", "&eYour message contains inappropriate content. You have been muted, ask an admin for an unmute.");
    }

    public String getAdminNotification() {
        return config.getString("messages.admin-notification", "&6[ChatMod] &c{player} &7tried to send: &f{message}");
    }

    public String getPluginReloaded() {
        return config.getString("messages.plugin-reloaded", "&aChat Moderator configuration reloaded!");
    }

    public String getPluginEnabled() {
        return config.getString("messages.plugin-enabled", "&aChat moderation enabled!");
    }

    public String getPluginDisabled() {
        return config.getString("messages.plugin-disabled", "&cChat moderation disabled!");
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean shouldLogAllMessages() {
        return config.getBoolean("debug.log-all-messages", false);
    }

    public int getMuteDurationSeconds() {
        return config.getInt("moderation.mute-duration-seconds", 600);
    }
}

