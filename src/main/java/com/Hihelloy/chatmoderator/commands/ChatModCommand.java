package com.Hihelloy.chatmoderator.commands;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.Hihelloy.chatmoderator.config.ConfigManager;
import com.Hihelloy.chatmoderator.listeners.ChatListener;
import com.Hihelloy.chatmoderator.services.ModerationService;
import com.Hihelloy.chatmoderator.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatModCommand implements CommandExecutor, TabCompleter {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ChatListener chatListener;
    private final ModerationService moderationService;

    public ChatModCommand(ChatModeratorPlugin plugin, ChatListener chatListener) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatListener = chatListener;
        this.moderationService = plugin.getModerationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "status":
            case "toggle":
            case "add-word":
            case "remove-word":
            case "mutedplayers":
            case "aitest":
                if (!sender.hasPermission("chatmoderator.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                break;
            case "unmute":
                if (!sender.hasPermission("chatmoderator.admin")
                        && !sender.hasPermission("chatmoderator.command.unmute")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                break;
            default:
                sendHelpMessage(sender);
                return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "toggle":
                handleToggle(sender);
                break;
            case "add-word":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod add-word <word>");
                    return true;
                }
                handleAddWord(sender, args[1]);
                break;
            case "remove-word":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod remove-word <word>");
                    return true;
                }
                handleRemoveWord(sender, args[1]);
                break;
            case "unmute":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod unmute <player>");
                    return true;
                }
                handleUnmute(sender, args[1]);
                break;
            case "aitest":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod aitest <message>");
                    return true;
                }
                handleAITest(sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                break;
            case "mutedplayers":
                handleMutedPlayers(sender);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Chat Moderator Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod reload" + ChatColor.WHITE + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod status" + ChatColor.WHITE + " - Show plugin status");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod toggle" + ChatColor.WHITE + " - Toggle moderation on/off");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod add-word <word>" + ChatColor.WHITE + " - Add a blocked word");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod remove-word <word>" + ChatColor.WHITE + " - Remove a blocked word");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod unmute <player>" + ChatColor.WHITE + " - Unmute a player");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod aitest <message>" + ChatColor.WHITE + " - Test AI moderation");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod mutedplayers" + ChatColor.WHITE + " - List currently muted players");
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.reloadPluginConfig();
            moderationService.initClients();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getPluginReloaded()));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading config: " + e.getMessage());
        }
    }

    private void handleStatus(CommandSender sender) {
        String provider = configManager.getPreferredAIProvider();
        boolean openaiOk = !configManager.getOpenAIApiKey().equals("your-openai-api-key-here")
                && !configManager.getOpenAIApiKey().isEmpty();
        boolean geminiOk = !configManager.getGeminiApiKey().equals("your-gemini-api-key-here")
                && !configManager.getGeminiApiKey().isEmpty();

        sender.sendMessage(ChatColor.GOLD + "=== Chat Moderator Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Moderation Enabled: " + bool(configManager.isModerationEnabled()));
        sender.sendMessage(ChatColor.YELLOW + "AI Moderation: " + bool(configManager.isAIModerationEnabled()));
        sender.sendMessage(ChatColor.YELLOW + "Word Filter: " + bool(configManager.isWordFilterEnabled()));
        sender.sendMessage(ChatColor.YELLOW + "Blocked Words: " + ChatColor.WHITE
                + configManager.getBlockedWords().stream().filter(w -> !w.isEmpty()).count());
        sender.sendMessage(ChatColor.YELLOW + "Preferred AI Provider: "
                + ChatColor.WHITE + (provider != null ? provider : "none"));
        sender.sendMessage(ChatColor.YELLOW + "OpenAI API Key: "
                + (openaiOk ? ChatColor.GREEN + "Configured" : ChatColor.RED + "Not Configured"));
        sender.sendMessage(ChatColor.YELLOW + "Gemini API Key: "
                + (geminiOk ? ChatColor.GREEN + "Configured" : ChatColor.RED + "Not Configured"));
        sender.sendMessage(ChatColor.YELLOW + "Currently Muted: "
                + ChatColor.WHITE + chatListener.getMutedPlayers().size() + " player(s)");
    }

    private void handleToggle(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        boolean newState = !configManager.isModerationEnabled();
        config.set("moderation.enabled", newState);
        plugin.saveConfig();
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                newState ? configManager.getPluginEnabled() : configManager.getPluginDisabled()));
    }

    private void handleAddWord(CommandSender sender, String word) {
        FileConfiguration config = plugin.getConfig();
        List<String> blocked = new ArrayList<>(config.getStringList("moderation.blocked-words"));
        String lc = word.toLowerCase();

        if (blocked.contains(lc)) {
            sender.sendMessage(ChatColor.RED + "'" + word + "' is already in the blocked list.");
            return;
        }
        blocked.add(lc);
        config.set("moderation.blocked-words", blocked);
        plugin.saveConfig();
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Added '" + word + "' to the blocked words list.");
    }

    private void handleRemoveWord(CommandSender sender, String word) {
        FileConfiguration config = plugin.getConfig();
        List<String> blocked = new ArrayList<>(config.getStringList("moderation.blocked-words"));
        String lc = word.toLowerCase();

        if (!blocked.remove(lc)) {
            sender.sendMessage(ChatColor.RED + "'" + word + "' is not in the blocked list.");
            return;
        }
        config.set("moderation.blocked-words", blocked);
        plugin.saveConfig();
        configManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Removed '" + word + "' from the blocked words list.");
    }

    private void handleUnmute(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' is not online.");
            return;
        }
        if (chatListener.isMuted(target)) {
            chatListener.unmutePlayer(target);
            sender.sendMessage(ChatColor.GREEN + target.getName() + " has been unmuted.");
        } else {
            sender.sendMessage(ChatColor.RED + target.getName() + " is not currently muted.");
        }
    }

    private void handleAITest(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GOLD + "Running AI moderation test on: " + ChatColor.WHITE + message);

        moderationService.checkAIModerationAsync(message).thenAccept(result ->
                SchedulerUtil.runGlobal(() -> {
                    sender.sendMessage(ChatColor.GOLD + "=== AI Moderation Test Result ===");
                    sender.sendMessage(ChatColor.YELLOW + "Verdict:  "
                            + (result.isBlocked() ? ChatColor.RED + "BLOCKED" : ChatColor.GREEN + "SAFE"));
                    sender.sendMessage(ChatColor.YELLOW + "Reason:   " + ChatColor.WHITE + result.getReason());
                    sender.sendMessage(ChatColor.YELLOW + "Category: " + ChatColor.WHITE + result.getViolationType());
                })
        );
    }

    private void handleMutedPlayers(CommandSender sender) {
        Map<UUID, Long> muted = chatListener.getMutedPlayers();
        sender.sendMessage(ChatColor.GOLD + "=== Muted Players (" + muted.size() + ") ===");
        if (muted.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No players are currently muted.");
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : muted.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : entry.getKey().toString();
            String timeStr;
            if (entry.getValue() == -1L) {
                timeStr = "permanent";
            } else {
                long secsLeft = (entry.getValue() - now) / 1000;
                timeStr = secsLeft + "s remaining";
            }
            sender.sendMessage(ChatColor.YELLOW + name + ChatColor.GRAY + " — " + timeStr);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : new String[]{
                    "reload", "status", "toggle", "add-word", "remove-word",
                    "unmute", "aitest", "mutedplayers"}) {
                if (sub.startsWith(prefix)) completions.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove-word")) {
                String prefix = args[1].toLowerCase();
                for (String w : configManager.getBlockedWords()) {
                    if (!w.isEmpty() && w.startsWith(prefix)) completions.add(w);
                }
            } else if (args[0].equalsIgnoreCase("unmute")) {
                String prefix = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (chatListener.isMuted(p) && p.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }

    private String bool(boolean value) {
        return value ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No";
    }
}