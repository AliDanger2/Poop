package me.poop.poop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class DataManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Integer> poopCounts = new HashMap<>();
    private static final int LEADERBOARD_ENTRIES_PER_PAGE = 10;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPoopData() {
        File dataFile = new File(plugin.getDataFolder(), "poop_data.json");
        if (!dataFile.exists()) {
            return;
        }

        try (Reader reader = new FileReader(dataFile)) {
            Map<String, Integer> loadedData = new Gson().fromJson(reader, new TypeToken<Map<String, Integer>>() {}.getType());
            for (Map.Entry<String, Integer> entry : loadedData.entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey());
                poopCounts.put(uuid, entry.getValue());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load poop data: " + e.getMessage());
        }
    }

    public void savePoopData() {
        File dataFile = new File(plugin.getDataFolder(), "poop_data.json");
        dataFile.getParentFile().mkdirs();

        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, Integer> dataToSave = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : poopCounts.entrySet()) {
                dataToSave.put(entry.getKey().toString(), entry.getValue());
            }

            new Gson().toJson(dataToSave, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save poop data: " + e.getMessage());
        }
    }

    public void incrementPoopCount(UUID playerUUID) {
        incrementPoopCount(playerUUID, 1);
    }

    public void incrementPoopCount(UUID playerUUID, int amount) {
        poopCounts.put(playerUUID, poopCounts.getOrDefault(playerUUID, 0) + amount);
    }

    public void showLeaderboard(Player player, int page) {
        List<Map.Entry<UUID, Integer>> sortedEntries = poopCounts.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        int totalEntries = sortedEntries.size();
        int totalPages = (int) Math.ceil((double) totalEntries / LEADERBOARD_ENTRIES_PER_PAGE);

        if (page > totalPages) {
            player.sendMessage(ChatColor.RED + "There are only " + totalPages + " pages.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "=== Top Poopers - Page " + page + "/" + totalPages + " ===");

        int start = (page - 1) * LEADERBOARD_ENTRIES_PER_PAGE;
        int end = Math.min(start + LEADERBOARD_ENTRIES_PER_PAGE, totalEntries);

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int poopCount = entry.getValue();

            player.sendMessage(ChatColor.GOLD + String.valueOf(i + 1) + ". " + ChatColor.YELLOW + playerName + ChatColor.GRAY + " - " + ChatColor.GREEN + poopCount + " poops");
        }

        player.sendMessage(ChatColor.GRAY + "Use /toppoop <page> to view other pages.");
    }

    public Map<UUID, Integer> getPoopCounts() {
        return poopCounts;
    }
}