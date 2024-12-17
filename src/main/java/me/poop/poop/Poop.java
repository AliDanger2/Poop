package me.poop.poop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.plugin.Plugin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.command.TabCompleter;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;

public class Poop extends JavaPlugin implements Listener {
    private String poopName;
    private boolean hopperPickupable;
    private Material poopItem;
    private final Map<Player, Boolean> diarrheaSafeFall = new HashMap<>();
    private final Set<Block> changingBlocks = new HashSet<>();
    private final Set<Block> revertingBlocks = new HashSet<>();
    private final Map<UUID, Integer> pendingCooldowns = new HashMap<>(); // Track pending cooldown changes
    private final Map<UUID, Boolean> messageCooldown = new HashMap<>(); // Track if the message was sent recently
    private int maxDiarrheaCooldown = 120;
    boolean poopEnabled = true;
    boolean diarrheaEnabled = true;
    private final Map<UUID, List<BlockSnapshot>> activeDiarrheaEvents = new HashMap<>();
    private final Map<Player, Boolean> poopingEnabled = new HashMap<>();
    private final Map<Player, Boolean> diarrheaMode = new HashMap<>();
    private final Map<UUID, Long> diarrheaCooldown = new HashMap<>();
    private final Map<UUID, Integer> poopCounts = new HashMap<>(); // Poop tracker
    private static final int LEADERBOARD_ENTRIES_PER_PAGE = 10; // Entries per leaderboard page

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeDefaultDiarrheaBlocks();
        loadPoopItem();
        loadPoopName();
        loadDiarrheaCooldown();
        loadHopperPickupable();
        Bukkit.getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("pa")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                if (!player.hasPermission("poop.admin")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                if (args.length == 0) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /pa <toggle_poop | poop_item | poop_name | hopper_pickupable | toggle_diarrhea | diarrhea_cooldown | diarrhea_safe_fall>");
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "toggle_poop":
                        poopEnabled = !poopEnabled; // Toggle the poopEnabled boolean
                        player.sendMessage(ChatColor.YELLOW + "Pooping has been " + (poopEnabled ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                        break;

                    case "poop_name":
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.YELLOW + "Usage: /pa poop_name <name>");
                            return true;
                        }

                        // Combine all arguments after the first into the new name
                        String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                        poopName = ChatColor.translateAlternateColorCodes('&', newName); // Support color codes
                        getConfig().set("poop-name", newName); // Save to config
                        saveConfig(); // Save the updated config.yml
                        player.sendMessage(ChatColor.GREEN + "Poop item name has been set to: " + poopName);
                        break;

                    case "poop_item":
                        if (args.length >= 2) {
                            String itemName = args[1].toUpperCase();
                            try {
                                Material newPoopItem = Material.valueOf(itemName);
                                poopItem = newPoopItem;

                                getConfig().set("poop-item", itemName); // Save to config
                                saveConfig();

                                player.sendMessage(ChatColor.GREEN + "Poop item set to " + newPoopItem.name());
                            } catch (IllegalArgumentException e) {
                                player.sendMessage(ChatColor.RED + "Invalid item name: " + itemName);
                            }
                        } else {
                            player.sendMessage(ChatColor.YELLOW + "Usage: /pa poop_item <item>");
                        }
                        break;

                    case "hopper_pickupable":
                        hopperPickupable = !hopperPickupable; // Toggle the state
                        getConfig().set("hopper-pickupable", hopperPickupable); // Save to config
                        saveConfig(); // Save the updated config.yml
                        player.sendMessage(ChatColor.YELLOW + "Hopper pickup has been " +
                                (hopperPickupable ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                        break;

                    case "toggle_diarrhea":
                        diarrheaEnabled = !diarrheaEnabled; // Toggle the diarrheaEnabled boolean
                        player.sendMessage(ChatColor.YELLOW + "Diarrhea has been " + (diarrheaEnabled ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                        break;

                    case "diarrhea_cooldown":
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.YELLOW + "Usage: /pa diarrhea_cooldown <seconds>");
                            return true;
                        }

                        try {
                            int newCooldown = Integer.parseInt(args[1]);
                            if (newCooldown < 7) {
                                player.sendMessage(ChatColor.RED + "WARNING! " + ChatColor.YELLOW +
                                        "If the cooldown is less than 7 seconds many issues will happen such as blocks not reverting back normally and more so be sure of what you're doing! " +
                                        "If you want to proceed write /pa confirm in the chat.");
                                pendingCooldowns.put(player.getUniqueId(), newCooldown);
                            } else {
                                maxDiarrheaCooldown = newCooldown;
                                getConfig().set("diarrhea-cooldown", newCooldown); // Save to config
                                saveConfig(); // Save the updated config.yml
                                player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + newCooldown + " seconds.");
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                        }
                        break;

                    case "diarrhea_safe_fall":
                        boolean isEnabled = diarrheaSafeFall.getOrDefault(player, false);
                        diarrheaSafeFall.put(player, !isEnabled);
                        player.sendMessage(ChatColor.YELLOW + "Diarrhea safe fall has been " +
                                (isEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
                        break;

                    case "diarrhea_block1":
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.YELLOW + "Usage: /pa diarrhea_block1 <block>");
                            return true;
                        }
                        setDiarrheaBlock(player, "diarrhea-block1", args[1]);
                        break;

                    case "diarrhea_block2":
                        if (args.length < 2) {
                            player.sendMessage(ChatColor.YELLOW + "Usage: /pa diarrhea_block2 <block>");
                            return true;
                        }
                        setDiarrheaBlock(player, "diarrhea-block2", args[1]);
                        break;

                    case "confirm":

                        if (pendingCooldowns.containsKey(player.getUniqueId())) {

                            maxDiarrheaCooldown = pendingCooldowns.get(player.getUniqueId());

                            player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + maxDiarrheaCooldown + " seconds.");

                            pendingCooldowns.remove(player.getUniqueId()); // Remove the pending cooldown

                        } else {

                            player.sendMessage(ChatColor.RED + "There is no pending cooldown change to confirm.");

                        }

                        return true;

                    default:
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa <toggle_poop | poop_item | poop_name | hopper_pickupable | toggle_diarrhea | diarrhea_cooldown | diarrhea_safe_fall>");
                        break;
                }
            }
            return true;
        });
        Objects.requireNonNull(getCommand("pa")).setTabCompleter(new PaTabCompleter());
        getLogger().info("Poop plugin enabled. You can now poop after shifting!");

        // Register commands
        Objects.requireNonNull(getCommand("poop")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                boolean isEnabled = poopingEnabled.getOrDefault(player, false);
                poopingEnabled.put(player, !isEnabled);

                if (diarrheaMode.getOrDefault(player, false)) {
                    diarrheaMode.put(player, false);
                }

                player.sendMessage("Pooping has been " + (isEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
            }
            return true;
        });
        Objects.requireNonNull(getCommand("diarrhea")).setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player player) {
                boolean isDiarrheaEnabled = diarrheaMode.getOrDefault(player, false);
                diarrheaMode.put(player, !isDiarrheaEnabled);

                if (poopingEnabled.getOrDefault(player, false)) {
                    poopingEnabled.put(player, false);
                }

                player.sendMessage("Diarrhea mode has been " + (isDiarrheaEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
            }
            return true;
        });
        Objects.requireNonNull(getCommand("toppoop")).setExecutor(this::onTopPoopCommand);

        // Initialize update checker
        new UpdateChecker(this).checkForUpdates();

        // Load plugin data
        loadPoopData();
    }

    @Override
    public void onDisable() {
        savePoopData();
    }

    public static class UpdateChecker {
        private final Plugin plugin;
        private final int resourceId; // Spigot resource ID

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
                // Parse JSON response
                Gson gson = new Gson();
                Map<String, Object> response = gson.fromJson(latestVersionJson, new TypeToken<Map<String, Object>>() {}.getType());

                String title = (String) response.get("title");
                String latestVersion = (String) response.get("current_version");
                String currentVersion = plugin.getDescription().getVersion(); // Accessing plugin version through plugin instance
                Map<String, Object> stats = (Map<String, Object>) response.get("stats");
                int downloads = ((Double) stats.get("downloads")).intValue();
                String pluginPageUrl = "https://www.spigotmc.org/resources/poop-plugin.120984/"; // Plugin URL

                // Check if an update is actually available
                if (isNewerVersion(currentVersion, latestVersion)) {
                    // Craft a cleaner message
                    String updateMessage = ChatColor.GOLD + "[Poop Update] " + ChatColor.GREEN +
                            "A new version of " + title + " is available!\n" +
                            ChatColor.YELLOW + "Current Version: " + ChatColor.WHITE + currentVersion + "\n" +
                            ChatColor.YELLOW + "Latest Version: " + ChatColor.WHITE + latestVersion + "\n" +
                            ChatColor.YELLOW + "Downloads: " + ChatColor.WHITE + downloads + "\n" +
                            ChatColor.GREEN + "Download the new version here: " +
                            ChatColor.AQUA + ChatColor.UNDERLINE + pluginPageUrl;

                    // Notify all players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(updateMessage); // Send the update message to all players
                    }

                    // Optionally log to console
                    Bukkit.getLogger().info(ChatColor.stripColor(updateMessage));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to parse update information: " + e.getMessage());
            }
        }

        // Helper method to compare versions
        private boolean isNewerVersion(String currentVersion, String latestVersion) {
            return compareVersions(currentVersion, latestVersion) < 0;
        }

        // Version comparison method
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
            return 0; // Versions are equal
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        poopCounts.putIfAbsent(playerUUID, 0); // Initialize poop count for new players
    }

    public static class PaTabCompleter implements TabCompleter {

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                // Suggest subcommands for the first argument
                return StringUtil.copyPartialMatches(args[0], Arrays.asList(
                        "toggle_poop", "poop_item", "poop_name", "hopper_pickupable",
                        "toggle_diarrhea", "diarrhea_cooldown", "diarrhea_safe_fall",
                        "diarrhea_block1", "diarrhea_block2"
                ), new ArrayList<>());
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("diarrhea_block1") || args[0].equalsIgnoreCase("diarrhea_block2") || args[0].equalsIgnoreCase("poop_item"))) {
                // Suggest all Material names for diarrhea_block1 and diarrhea_block2 subcommands
                return StringUtil.copyPartialMatches(args[1], getMaterialNames(), new ArrayList<>());
            }
            return new ArrayList<>();
        }

        private List<String> getMaterialNames() {
            // Get all Material names
            return Arrays.stream(Material.values())
                    .map(Material::name) // Convert Materials to their names
                    .sorted() // Sort alphabetically
                    .collect(Collectors.toList());
        }
    }

    private void initializeDefaultDiarrheaBlocks() {
        if (!getConfig().contains("diarrhea-block1")) {
            getConfig().set("diarrhea-block1", Material.BROWN_CONCRETE.name());
        }
        if (!getConfig().contains("diarrhea-block2")) {
            getConfig().set("diarrhea-block2", Material.BROWN_WOOL.name());
        }
        saveConfig();
    }

    private void setDiarrheaBlock(Player player, String configKey, String blockName) {
        try {
            Material blockMaterial = Material.valueOf(blockName.toUpperCase());
            getConfig().set(configKey, blockMaterial.name());
            saveConfig();
            player.sendMessage(ChatColor.GREEN + "Block for " + configKey + " has been set to " + blockMaterial.name());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid block name: " + blockName);
        }
    }

    private void loadPoopName() {
        poopName = getConfig().getString("poop-name", "Poop"); // Default to "Poop"
    }

    private void loadHopperPickupable() {
        hopperPickupable = getConfig().getBoolean("hopper-pickupable", true); // Default to true
    }

    private void loadDiarrheaCooldown() {
        maxDiarrheaCooldown = getConfig().getInt("diarrhea-cooldown", 120); // Default to 120 seconds
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!hopperPickupable) {
            // Check if the item is the configured poop item
            if (event.getItem().getItemStack().getType() == poopItem) {
                event.setCancelled(true); // Cancel hopper pickup if disabled
            }
        }
    }

    @EventHandler
    public void onPlayerSneakForPoop(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!poopEnabled) {
            return;
        }

        if (event.isSneaking() && poopingEnabled.getOrDefault(player, false)) {
            double scale = 1.0D;

            if (player.getAttribute(Attribute.GENERIC_SCALE) != null) {
                scale = player.getAttribute(Attribute.GENERIC_SCALE).getValue();
            }

            Location poopLocation = player.getLocation().add(0.0D, 0.5D * scale, 0.0D);
            Location direction = player.getLocation();
            org.bukkit.util.Vector playerDirection = direction.getDirection();

            if (Math.abs(direction.getPitch()) == 90) {
                playerDirection.setX(Math.random() * 0.2 - 0.1);
                playerDirection.setZ(Math.random() * 0.2 - 0.1);
                playerDirection.setY(player.getLocation().getPitch() > 0 ? -0.2 : 0.2);
            } else {
                playerDirection.setY(-0.2);
            }

            ItemStack poop = new ItemStack(poopItem, 1);
            ItemMeta meta = poop.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RESET + poopName); // Apply custom name from config
                meta.getPersistentDataContainer().set(new NamespacedKey(this, "poop_tag"), PersistentDataType.BYTE, (byte) 1); // Add unique tag
                poop.setItemMeta(meta);
            }

            Item droppedPoop = player.getWorld().dropItem(poopLocation, poop);
            droppedPoop.setPickupDelay(Integer.MAX_VALUE);
            droppedPoop.setVelocity(playerDirection.normalize().multiply(-0.3));

            for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 10) {
                    nearbyPlayer.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
                }
            }

            Bukkit.getScheduler().runTaskLater(this, droppedPoop::remove, 40L);

            // Increment player's poop count in tracking
            UUID playerUUID = player.getUniqueId();
            poopCounts.put(playerUUID, poopCounts.getOrDefault(playerUUID, 0) + 1);
        }
    }

    private void loadPoopItem() {
        String configItem = getConfig().getString("poop-item", "BROWN_DYE");
        try {
            poopItem = Material.valueOf(configItem);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid poop item in config.yml: " + configItem + ". Defaulting to BROWN_DYE.");
            poopItem = Material.BROWN_DYE;
        }
    }

    @EventHandler
    public void onPlayerSneakForDiarrhea(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (diarrheaMode.getOrDefault(player, false)) {
            if (diarrheaSafeFall.getOrDefault(player, false)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, false, false));
            }
        }

        if (event.isSneaking() && diarrheaMode.getOrDefault(player, false)) {
            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            long lastDiarrheaTime = diarrheaCooldown.getOrDefault(playerUUID, 0L);

            // Check if the cooldown has passed
            if (currentTime - lastDiarrheaTime < maxDiarrheaCooldown * 1000L) {
                long remainingTime = (maxDiarrheaCooldown * 1000L - (currentTime - lastDiarrheaTime)) / 1000L;

                // Check if the message has been sent recently
                if (!messageCooldown.getOrDefault(playerUUID, false)) {
                    player.sendMessage(ChatColor.RED + "Diarrhea is currently on cooldown. Please wait " + ChatColor.GOLD + remainingTime + ChatColor.RED + " seconds.");
                    messageCooldown.put(playerUUID, true); // Set the flag to true
                    // Reset the flag after 1 second
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        messageCooldown.put(playerUUID, false); // Reset the flag
                    }, 20L); // 20 ticks = 1 second
                }
                return;
            }

            // Update the cooldown time
            diarrheaCooldown.put(playerUUID, currentTime);

            // Check for nearby diarrhea events and revert blocks if necessary
            boolean hasNearbyDiarrhea = false;

// Check for the current player's own diarrhea events first
            List<BlockSnapshot> currentPlayerSnapshots = activeDiarrheaEvents.get(playerUUID);
            if (currentPlayerSnapshots != null) {
                // Revert the current player's diarrhea blocks
                revertBlocks(currentPlayerSnapshots);
                hasNearbyDiarrhea = true; // Set to true since we reverted our own blocks
            }

// Now check for other players' diarrhea events
            for (Map.Entry<UUID, List<BlockSnapshot>> entry : activeDiarrheaEvents.entrySet()) {
                UUID otherPlayerUUID = entry.getKey();
                if (!otherPlayerUUID.equals(playerUUID)) {
                    List<BlockSnapshot> snapshots = entry.getValue();
                    for (BlockSnapshot snapshot : snapshots) {
                        if (snapshot.location.getWorld().equals(player.getWorld()) &&
                                snapshot.location.distance(player.getLocation()) <= 10) {
                            revertBlocks(snapshots); // Revert other players' diarrhea blocks
                            hasNearbyDiarrhea = true; // Set to true since we reverted other players' blocks
                            break; // Exit the loop after reverting
                        }
                    }
                }
                if (hasNearbyDiarrhea) break; // Exit the outer loop if we reverted any blocks
            }

// Schedule the diarrhea explosion after 1 tick
            Bukkit.getScheduler().runTaskLater(this, () -> {
                List<BlockSnapshot> currentDiarrheaSnapshots = new ArrayList<>();
                currentDiarrheaSnapshots.addAll(changeGroundBlocks(player)); // Change ground blocks for the new diarrhea event
                executeDiarrheaExplosion(player, currentDiarrheaSnapshots);
            }, 1L); // Delay the explosion by 1 tick
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOnGround() && player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && diarrheaSafeFall.getOrDefault(player, false)) {
                event.setCancelled(true); // Prevent fall damage if safe fall is enabled.
            }
        }
    }

    private void executeDiarrheaExplosion(Player player, List<BlockSnapshot> currentDiarrheaSnapshots) {
        if (diarrheaMode.getOrDefault(player, false)) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 1.0, 1.0, 1.0, 0.1);
            player.setVelocity(player.getVelocity().setY(20.0));
            spawnBrownDyeCircle(player);
            launchNearbyEntities(player);
        }
        // Store the current player's diarrhea blocks
        activeDiarrheaEvents.put(player.getUniqueId(), currentDiarrheaSnapshots);

        // Schedule cleanup of the event after 7 seconds
        Bukkit.getScheduler().runTaskLater(this, () -> {
            activeDiarrheaEvents.remove(player.getUniqueId()); // Remove the player's diarrhea event after 7 seconds
            revertBlocks(currentDiarrheaSnapshots); // Restore the blocks
        }, 140L); // 7 seconds (140 ticks)
    }

    private List<BlockSnapshot> changeGroundBlocks(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int radius = 10;
        List<BlockSnapshot> snapshots = new ArrayList<>();

        // Get block types from config or use default values
        Material block1 = getConfigMaterial("diarrhea-block1", Material.BROWN_CONCRETE);
        Material block2 = getConfigMaterial("diarrhea-block2", Material.BROWN_WOOL);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distanceSquared = (double) (x * x + z * z);
                if (distanceSquared <= (double) (radius * radius)) {
                    for (int y = -radius; y <= radius; y++) {
                        Location blockLocation = center.clone().add((double) x, (double) y, (double) z);
                        Block block = world.getBlockAt(blockLocation);

                        // Skip air, water, and rail types
                        if (block.getType() == Material.AIR || block.getType() == Material.WATER ||
                                block.getType() == Material.RAIL ||
                                block.getType() == Material.POWERED_RAIL ||
                                block.getType() == Material.DETECTOR_RAIL ||
                                block.getType() == Material.ACTIVATOR_RAIL) {
                            continue;
                        }

                        // Save the block state (including metadata)
                        BlockSnapshot snapshot = new BlockSnapshot(block.getState());
                        snapshots.add(snapshot); // Save locally for this diarrhea event

                        // Set new block type using player's chosen blocks
                        Material newMaterial = (new Random()).nextBoolean() ? block1 : block2;
                        changingBlocks.add(block); // Add to the set of changing blocks
                        block.setType(newMaterial, false);
                    }
                }
            }
        }

        // Schedule a task to remove the blocks from the set after a delay
        Bukkit.getScheduler().runTaskLater(this, () -> {
            changingBlocks.removeAll(snapshots.stream().map(s -> s.location.getBlock()).collect(Collectors.toList()));
        }, 140L); // Adjust the delay as needed (140 ticks = 7 seconds)

        return snapshots; // Return the list of snapshots
    }

    private Material getConfigMaterial(String configKey, Material defaultMaterial) {
        String materialName = getConfig().getString(configKey, defaultMaterial.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid material '" + materialName + "' in config for " + configKey + ". Using default: " + defaultMaterial.name());
            return defaultMaterial;
        }
    }

    private void spawnBrownDyeCircle(Player player) {
        Location center = player.getLocation();
        List<Item> spawnedItems = new ArrayList<>();
        World world = center.getWorld();

        for (int i = 0; i < 25; i++) {
            double angle = 3 * Math.PI * i / 25;
            double x = center.getX() + Math.cos(angle);
            double z = center.getZ() + Math.sin(angle);
            Location spawnLocation = new Location(world, x, center.getY() + 0.5, z);

            ItemStack brownDye = new ItemStack(poopItem, 1);
            assert world != null;
            Item item = world.dropItem(spawnLocation, brownDye);
            item.setPickupDelay(Integer.MAX_VALUE);
            spawnedItems.add(item);
        }

        UUID playerUUID = player.getUniqueId();
        poopCounts.put(playerUUID, poopCounts.getOrDefault(playerUUID, 0) + 25);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Item item : spawnedItems) {
                if (item != null && !item.isDead()) {
                    item.remove();
                }
            }
        }, 7 * 20L); // 7 seconds (20 ticks per second)
    }

    private void launchNearbyEntities(Player player) {
        List<Entity> nearbyEntities = player.getNearbyEntities(10, 10, 10);

        for (Entity entity : nearbyEntities) {
            if (entity.equals(player)) continue;

            org.bukkit.util.Vector launchDirection = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            launchDirection.setY(1.0);
            entity.setVelocity(launchDirection.multiply(1.5));
        }
    }

    private void revertBlocks(List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            Block block = snapshot.location.getBlock();
            revertingBlocks.add(block);
            snapshot.restore();
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (BlockSnapshot snapshot : snapshots) {
                Block block = snapshot.location.getBlock();
                revertingBlocks.remove(block);
            }
        }, 140L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (changingBlocks.contains(event.getBlock())) {
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot break this block while it is being changed!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && changingBlocks.contains(event.getClickedBlock())) {
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot interact with this block while it is being changed!");
            event.setCancelled(true);
        }
    }

    private static class BlockSnapshot {
        private final Location location;
        private final Material material;
        private final BlockData blockData;
        private final BlockState blockState;

        public BlockSnapshot(BlockState state) {
            this.location = state.getLocation();
            this.material = state.getType();
            this.blockData = state.getBlockData();
            this.blockState = state; // Store the complete block state
        }

        public void restore() {
            Block block = this.location.getBlock();
            block.setType(this.material, false);
            block.setBlockData(this.blockData, false);
            BlockState currentState = block.getState();
            if (currentState.getClass().equals(this.blockState.getClass())) {
                this.blockState.update(true, false); // Restore the original block state including metadata
            }
        }
    }

    private void loadPoopData() {
        File dataFile = new File(getDataFolder(), "poop_data.json");
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
            getLogger().severe("Failed to load poop data: " + e.getMessage());
        }
    }

    private void savePoopData() {
        File dataFile = new File(getDataFolder(), "poop_data.json");
        dataFile.getParentFile().mkdirs();

        try (Writer writer = new FileWriter(dataFile)) {
            Map<String, Integer> dataToSave = new HashMap<>();
            for (Map.Entry<UUID, Integer> entry : poopCounts.entrySet()) {
                dataToSave.put(entry.getKey().toString(), entry.getValue());
            }

            new Gson().toJson(dataToSave, writer);
        } catch (IOException e) {
            getLogger().severe("Failed to save poop data: " + e.getMessage());
        }
    }

    private boolean onTopPoopCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            int page = 1;

            if (args.length > 0) {
                try {
                    page = Math.max(1, Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid page number!");
                    return true;
                }
            }

            showLeaderboard(player, page);
            return true;
        }
        return false;
    }

    private void showLeaderboard(Player player, int page) {
        List<Map.Entry<UUID, Integer>> sortedEntries = poopCounts.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Sort by poop count (desc)
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
}