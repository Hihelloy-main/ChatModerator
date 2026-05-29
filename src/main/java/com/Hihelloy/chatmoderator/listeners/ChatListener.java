package com.Hihelloy.chatmoderator.listeners;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.Hihelloy.chatmoderator.config.ConfigManager;
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
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatListener implements Listener {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ModerationService moderationService;

    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();

    public ChatListener(ChatModeratorPlugin plugin) {
        this.plugin            = plugin;
        this.configManager     = plugin.getConfigManager();
        this.moderationService = plugin.getModerationService();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player  = event.getPlayer();
        String message = event.getMessage();

        if (configManager.isDebugEnabled() && configManager.shouldLogAllMessages()) {
            plugin.getLogger().info("[DEBUG] Chat from " + player.getName() + ": " + message);
        }

        if (!configManager.isModerationEnabled()) return;

        if (isMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are temporarily muted and cannot chat.");
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
                || cmd.startsWith("/w ")   || cmd.startsWith("/whisper ")
                || cmd.startsWith("/pm ")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are muted and cannot send private messages.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        mutedPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void applyPunishment(Player player, String message, String reason) {

        int muteDurationSeconds = configManager.getMuteDurationSeconds();
        mutedPlayers.put(player.getUniqueId(),
                System.currentTimeMillis() + muteDurationSeconds * 1000L);

        if (configManager.shouldWarnPlayer()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getViolationWarning()));
        }

        if (configManager.shouldNotifyAdmins()) {
            String notification = ChatColor.translateAlternateColorCodes('&',
                    configManager.getAdminNotification()
                            .replace("{player}",  player.getName())
                            .replace("{message}", message)
                            .replace("{reason}",  reason));
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
        Long unmuteTime = mutedPlayers.get(player.getUniqueId());
        if (unmuteTime == null) return false;
        if (unmuteTime != -1L && System.currentTimeMillis() >= unmuteTime) {
            mutedPlayers.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void mutePlayer(Player player, int durationSeconds) {
        long until = durationSeconds < 0
                ? -1L
                : System.currentTimeMillis() + durationSeconds * 1000L;
        mutedPlayers.put(player.getUniqueId(), until);
    }

    public void unmutePlayer(Player player) {
        if (mutedPlayers.remove(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.GREEN + "You have been unmuted.");
        } else {
            player.sendMessage(ChatColor.RED + "You were not muted.");
        }
    }

    public Map<UUID, Long> getMutedPlayers() {
        return Map.copyOf(mutedPlayers);
    }
}

