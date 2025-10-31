package me.poop.poop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class UpdateChecker {
    private final Plugin plugin;
    private final int resourceId;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
        this.resourceId = 120984;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(
                        "https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=" + resourceId
                ).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = reader.readLine();
                reader.close();

                if (latestVersion != null && !plugin.getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                    notifyAdmins(latestVersion, (JavaPlugin) plugin);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Could not check for updates: " + e.getMessage());
            }
        });
    }

    private void notifyAdmins(String latestVersionJson, JavaPlugin plugin) {
        try {
            Gson gson = new Gson();
            Map<String, Object> response = gson.fromJson(latestVersionJson, new TypeToken<Map<String, Object>>() {}.getType());

            String title = (String) response.get("title");
            String latestVersion = (String) response.get("current_version");
            String currentVersion = plugin.getDescription().getVersion();
            Map<String, Object> stats = (Map<String, Object>) response.get("stats");
            int downloads = ((Double) stats.get("downloads")).intValue();
            String pluginPageUrl = "https://www.spigotmc.org/resources/poop-plugin.120984/";

            if (isNewerVersion(currentVersion, latestVersion)) {
                String updateMessage = ChatColor.GOLD + "[Poop Update] " + ChatColor.GREEN +
                        "A new version of " + title + " is available!\n" +
                        ChatColor.YELLOW + "Current Version: " + ChatColor.WHITE + currentVersion + "\n" +
                        ChatColor.YELLOW + "Latest Version: " + ChatColor.WHITE + latestVersion + "\n" +
                        ChatColor.YELLOW + "Downloads: " + ChatColor.WHITE + downloads + "\n" +
                        ChatColor.GREEN + "Download the new version here: " +
                        ChatColor.AQUA + ChatColor.UNDERLINE + pluginPageUrl;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(updateMessage);
                }

                Bukkit.getLogger().info(ChatColor.stripColor(updateMessage));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to parse update information: " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        return compareVersions(currentVersion, latestVersion) < 0;
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return 0;
    }
}