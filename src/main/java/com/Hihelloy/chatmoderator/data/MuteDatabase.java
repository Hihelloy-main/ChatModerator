package com.Hihelloy.chatmoderator.data;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuteDatabase {

    private final ChatModeratorPlugin plugin;
    private final File file;
    private final Gson gson;

    private final Map<UUID, Long> muteMap = new ConcurrentHashMap<>();

    public MuteDatabase(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mutes.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load() {
        muteMap.clear();

        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            long now = System.currentTimeMillis();

            for (String key : json.keySet()) {
                long unmuteAt = json.get(key).getAsLong();
                if (unmuteAt == -1L || unmuteAt > now) {
                    muteMap.put(UUID.fromString(key), unmuteAt);
                }
            }

            plugin.getLogger().info("[ChatMod] Loaded " + muteMap.size() + " active mute(s) from mutes.json.");
        } catch (IOException e) {
            plugin.getLogger().severe("[ChatMod] Failed to load mutes.json: " + e.getMessage());
        }
    }

    public void save() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        JsonObject json = new JsonObject();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : muteMap.entrySet()) {
            long unmuteAt = entry.getValue();
            if (unmuteAt == -1L || unmuteAt > now) {
                json.addProperty(entry.getKey().toString(), unmuteAt);
            }
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("[ChatMod] Failed to save mutes.json: " + e.getMessage());
        }
    }

    public void mute(UUID uuid, long unmuteAt) {
        muteMap.put(uuid, unmuteAt);
        save();
    }

    public void unmute(UUID uuid) {
        muteMap.remove(uuid);
        save();
    }

    public boolean isMuted(UUID uuid) {
        Long unmuteAt = muteMap.get(uuid);
        if (unmuteAt == null) return false;
        if (unmuteAt != -1L && System.currentTimeMillis() >= unmuteAt) {
            muteMap.remove(uuid);
            save();
            return false;
        }
        return true;
    }

    public Long getUnmuteTime(UUID uuid) {
        return muteMap.get(uuid);
    }

    public Map<UUID, Long> getAll() {
        return Map.copyOf(muteMap);
    }
}