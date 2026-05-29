package com.Hihelloy.chatmoderator.listeners;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.Hihelloy.chatmoderator.config.ConfigManager;
import com.Hihelloy.chatmoderator.data.MuteDatabase;
import com.Hihelloy.chatmoderator.services.ModerationService;
import com.Hihelloy.chatmoderator.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ModerationService moderationService;
    private final MuteDatabase muteDatabase;

    public ChatListener(ChatModeratorPlugin plugin, MuteDatabase muteDatabase) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.moderationService = plugin.getModerationService();
        this.muteDatabase = muteDatabase;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (configManager.isDebugEnabled() && configManager.shouldLogAllMessages()) {
            plugin.getLogger().info("[DEBUG] Chat from " + player.getName() + ": " + message);
        }

        if (!configManager.isModerationEnabled()) return;

        if (isMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are muted and cannot chat.");
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Muted player " + player.getName() + " blocked.");
            }
            return;
        }

        if (player.hasPermission("chatmoderator.bypass")) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] " + player.getName() + " bypassed moderation.");
            }
            return;
        }

        if (configManager.isWordFilterEnabled()) {
            Set<String> blockedSet = configManager.getBlockedWords().stream()
                    .map(String::toLowerCase)
                    .filter(w -> !w.isEmpty())
                    .collect(Collectors.toSet());

            String[] tokens = message.toLowerCase().split("\\W+");
            for (String token : tokens) {
                if (blockedSet.contains(token)) {
                    event.setCancelled(true);
                    applyPunishment(player, message, "Word filter: blocked word \"" + token + "\"");
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] Word filter blocked message from " + player.getName());
                    }
                    return;
                }
            }
        }

        if (configManager.isAIModerationEnabled()) {
            event.setCancelled(true);

            final Set<Player> recipients = event.getRecipients().stream()
                    .filter(Player::isOnline)
                    .collect(Collectors.toSet());
            final String format = event.getFormat();

            moderationService.checkAIModerationAsync(message).thenAccept(result -> {
                if (result.isBlocked()) {
                    SchedulerUtil.runGlobal(() -> applyPunishment(player, message, result.getReason()));
                } else {
                    SchedulerUtil.runGlobal(() -> {
                        String formatted = String.format(format, player.getDisplayName(), message);
                        for (Player recipient : recipients) {
                            if (recipient.isOnline()) {
                                recipient.sendMessage(formatted);
                            }
                        }
                    });
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isMuted(player)) return;

        String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/msg ") || cmd.startsWith("/tell ")
                || cmd.startsWith("/w ") || cmd.startsWith("/whisper ")
                || cmd.startsWith("/pm ")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are muted and cannot send private messages.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isMuted(player)) {
            Long unmuteAt = muteDatabase.getUnmuteTime(player.getUniqueId());
            if (unmuteAt != null && unmuteAt != -1L) {
                long secsLeft = (unmuteAt - System.currentTimeMillis()) / 1000;
                player.sendMessage(ChatColor.RED + "You are still muted. Time remaining: " + secsLeft + "s.");
            } else {
                player.sendMessage(ChatColor.RED + "You are permanently muted.");
            }
        }
    }

    private void applyPunishment(Player player, String message, String reason) {
        int muteDurationSeconds = configManager.getMuteDurationSeconds();
        long unmuteAt = System.currentTimeMillis() + muteDurationSeconds * 1000L;
        muteDatabase.mute(player.getUniqueId(), unmuteAt);

        if (configManager.shouldWarnPlayer()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getViolationWarning()));
        }

        if (configManager.shouldNotifyAdmins()) {
            String notification = ChatColor.translateAlternateColorCodes('&',
                    configManager.getAdminNotification()
                            .replace("{player}", player.getName())
                            .replace("{message}", message)
                            .replace("{reason}", reason));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("chatmoderator.admin")) {
                    p.sendMessage(notification);
                }
            }
        }

        if (configManager.shouldLogViolations()) {
            plugin.getLogger().warning("[ChatMod] Violation by " + player.getName()
                    + " | Reason: " + reason + " | Message: " + message);
        }
    }

    public boolean isMuted(Player player) {
        return muteDatabase.isMuted(player.getUniqueId());
    }

    public void mutePlayer(Player player, int durationSeconds) {
        long unmuteAt = durationSeconds < 0
                ? -1L
                : System.currentTimeMillis() + durationSeconds * 1000L;
        muteDatabase.mute(player.getUniqueId(), unmuteAt);
    }

    public void unmutePlayer(UUID uuid) {
        muteDatabase.unmute(uuid);
    }

    public void unmutePlayer(Player player) {
        muteDatabase.unmute(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You have been unmuted.");
    }

    public Map<UUID, Long> getMutedPlayers() {
        return muteDatabase.getAll();
    }
}