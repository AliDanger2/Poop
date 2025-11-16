package me.poop.poop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.UUID;

public class EventManager implements Listener {
    private final JavaPlugin plugin;
    private final PoopManager poopManager;
    private final ConfigManager configManager;
    private final BlockManager blockManager;
    private final CommandManager commandManager;

    public EventManager(JavaPlugin plugin, PoopManager poopManager, ConfigManager configManager, BlockManager blockManager, CommandManager commandManager) {
        this.plugin = plugin;
        this.poopManager = poopManager;
        this.configManager = configManager;
        this.blockManager = blockManager;
        this.commandManager = commandManager;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(blockManager, plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        poopManager.cleanupPlayer(player.getUniqueId());
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!configManager.isHopperPickupable()) {
            Material type = event.getItem().getItemStack().getType();
            if (type == configManager.getPoopItem() || type == configManager.getDiarrheaItem() || type == configManager.getPlungeItem()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            if (poopManager.isPoopingEnabled(player)) {
                poopManager.executePoop(player);
            } else if (poopManager.isDiarrheaMode(player)) {
                poopManager.executeDiarrhea(player);
            } else if (poopManager.isPlungeMode(player)) {
                poopManager.executePlunge(player);
            }
        }
    }

    @EventHandler
    public void onPlayerLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        poopManager.removeSlowFalling(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                    poopManager.hasDiarrheaSafeFall(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getView().getTitle().equals(ChatColor.YELLOW + "Poop Admin GUI")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) return;
            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            Inventory inv = event.getInventory();
            int slot = event.getSlot();
            String yellow = ChatColor.YELLOW.toString();

            if (displayName.equals(yellow + "Toggle Poop")) {
                if (!player.hasPermission("poop.admin.toggle_poop")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                boolean newPoopState = !configManager.isPoopEnabled();
                configManager.setPoopEnabled(newPoopState);
                player.sendMessage(ChatColor.YELLOW + "Pooping has been " + (newPoopState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                Material poopMaterial = configManager.getPoopItem();
                if (poopMaterial == Material.AIR) poopMaterial = Material.BARRIER;
                ItemStack updatedItem = new ItemStack(poopMaterial);
                ItemMeta meta = updatedItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Toggle Poop");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (newPoopState ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
                updatedItem.setItemMeta(meta);
                inv.setItem(slot, updatedItem);
            } else if (displayName.equals(yellow + "Toggle Hopper Pickupable")) {
                if (!player.hasPermission("poop.admin.hopper_pickupable")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                boolean newHopperState = !configManager.isHopperPickupable();
                configManager.setHopperPickupable(newHopperState);
                player.sendMessage(ChatColor.YELLOW + "Hopper pickup has been " + (newHopperState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                ItemStack updatedItem = new ItemStack(Material.HOPPER);
                ItemMeta meta = updatedItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Toggle Hopper Pickupable");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (newHopperState ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
                updatedItem.setItemMeta(meta);
                inv.setItem(slot, updatedItem);
            } else if (displayName.equals(yellow + "Toggle Diarrhea")) {
                if (!player.hasPermission("poop.admin.toggle_diarrhea")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                boolean newDiarrheaState = !configManager.isDiarrheaEnabled();
                configManager.setDiarrheaEnabled(newDiarrheaState);
                player.sendMessage(ChatColor.YELLOW + "Diarrhea has been " + (newDiarrheaState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                Material diarrheaMaterial = configManager.getDiarrheaItem();
                if (diarrheaMaterial == Material.AIR) diarrheaMaterial = Material.BARRIER;
                ItemStack updatedItem = new ItemStack(diarrheaMaterial);
                ItemMeta meta = updatedItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Toggle Diarrhea");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (newDiarrheaState ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
                updatedItem.setItemMeta(meta);
                inv.setItem(slot, updatedItem);
            } else if (displayName.equals(yellow + "Toggle Diarrhea Safe Fall")) {
                if (!player.hasPermission("poop.admin.diarrhea_safe_fall")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                poopManager.toggleDiarrheaSafeFall(player);
                boolean newSafeFallState = poopManager.hasDiarrheaSafeFall(player);
                player.sendMessage(ChatColor.YELLOW + "Diarrhea safe fall has been " + (newSafeFallState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                ItemStack updatedItem = new ItemStack(Material.FEATHER);
                ItemMeta meta = updatedItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Toggle Diarrhea Safe Fall");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (newSafeFallState ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
                updatedItem.setItemMeta(meta);
                inv.setItem(slot, updatedItem);
            } else if (displayName.equals(yellow + "Toggle Plunge")) {
                if (!player.hasPermission("poop.admin.toggle_plunge")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                boolean newPlungeState = !configManager.isPlungeEnabled();
                configManager.setPlungeEnabled(newPlungeState);
                player.sendMessage(ChatColor.YELLOW + "Plunge has been " + (newPlungeState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                Material plungeMaterial = configManager.getPlungeItem();
                if (plungeMaterial == Material.AIR) plungeMaterial = Material.BARRIER;
                ItemStack updatedItem = new ItemStack(plungeMaterial);
                ItemMeta meta = updatedItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Toggle Plunge");
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (newPlungeState ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
                updatedItem.setItemMeta(meta);
                inv.setItem(slot, updatedItem);
            } else if (displayName.startsWith(yellow + "Set ")) {
                String inputType = displayName.substring(yellow.length() + 4).toLowerCase().replace(" ", "_");
                String permission = "poop.admin." + inputType;
                if (!player.hasPermission(permission)) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this.");
                    return;
                }
                player.closeInventory();
                commandManager.pendingInputs.put(player.getUniqueId(), inputType);
                player.sendMessage(ChatColor.YELLOW + "Enter the new value for '" + inputType.replace("_", " ") + "' in chat (or 'cancel' to abort):");
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String input = event.getMessage().trim();
        if (commandManager.pendingInputs.containsKey(uuid)) {
            String type = commandManager.pendingInputs.remove(uuid);
            event.setCancelled(true);
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Input cancelled.");
                return;
            }
            try {
                switch (type) {
                    case "poop_name":
                        String newName = ChatColor.translateAlternateColorCodes('&', input);
                        configManager.setPoopName(newName);
                        player.sendMessage(ChatColor.GREEN + "Poop item name has been set to: " + newName);
                        break;
                    case "poop_item":
                        Material newPoopItem = Material.valueOf(input.toUpperCase());
                        configManager.setPoopItem(newPoopItem);
                        player.sendMessage(ChatColor.GREEN + "Poop item set to " + newPoopItem.name());
                        break;
                    case "diarrhea_cooldown":
                        int newCooldown = Integer.parseInt(input);
                        if (newCooldown < 7) {
                            player.sendMessage(ChatColor.RED + "WARNING! " + ChatColor.YELLOW +
                                    "If the cooldown is less than 7 seconds many issues will happen such as blocks not reverting back normally and more so be sure of what you're doing! " +
                                    "Type 'confirm' in chat to proceed.");
                            commandManager.pendingCooldowns.put(uuid, newCooldown);
                            commandManager.pendingInputs.put(uuid, "confirm_diarrhea_cooldown");
                        } else {
                            configManager.setMaxDiarrheaCooldown(newCooldown);
                            player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + newCooldown + " seconds.");
                        }
                        break;
                    case "plunge_cooldown":
                        int newPlungeCooldown = Integer.parseInt(input);
                        if (newPlungeCooldown < 7) {
                            player.sendMessage(ChatColor.RED + "WARNING! " + ChatColor.YELLOW +
                                    "If the cooldown is less than 7 seconds many issues will happen such as lag and many other things so be sure of what you're doing! " +
                                    "Type 'confirm' in chat to proceed.");
                            commandManager.pendingPlungeCooldowns.put(uuid, newPlungeCooldown);
                            commandManager.pendingInputs.put(uuid, "confirm_plunge_cooldown");
                        } else {
                            configManager.setMaxPlungeCooldown(newPlungeCooldown);
                            player.sendMessage(ChatColor.GREEN + "Plunge cooldown has been set to " + newPlungeCooldown + " seconds.");
                        }
                        break;
                    case "plunge_strength":
                        double newStrength = Double.parseDouble(input);
                        configManager.setPlungeStrength(newStrength);
                        player.sendMessage(ChatColor.GREEN + "Plunge strength has been set to " + newStrength);
                        break;
                    case "plunge_poop_trail":
                        configManager.setPlungePoopTrailExpression(input);
                        player.sendMessage(ChatColor.GREEN + "Plunge poop trail expression has been set to " + input);
                        break;
                    case "diarrhea_block_1":
                        configManager.setDiarrheaBlock("diarrhea-block1", input);
                        player.sendMessage(ChatColor.GREEN + "Block for diarrhea-block1 has been set to " + input.toUpperCase());
                        break;
                    case "diarrhea_block_2":
                        configManager.setDiarrheaBlock("diarrhea-block2", input);
                        player.sendMessage(ChatColor.GREEN + "Block for diarrhea-block2 has been set to " + input.toUpperCase());
                        break;
                    case "diarrhea_item":
                        Material newDiarrheaItem = Material.valueOf(input.toUpperCase());
                        configManager.setDiarrheaItem(newDiarrheaItem);
                        player.sendMessage(ChatColor.GREEN + "Diarrhea item has been set to " + newDiarrheaItem.name());
                        break;
                    case "plunge_item":
                        Material newPlungeItem = Material.valueOf(input.toUpperCase());
                        configManager.setPlungeItem(newPlungeItem);
                        player.sendMessage(ChatColor.GREEN + "Plunge item has been set to " + newPlungeItem.name());
                        break;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number.");
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid material name.");
            }
        } else if ((commandManager.pendingCooldowns.containsKey(uuid) || commandManager.pendingPlungeCooldowns.containsKey(uuid))
                && commandManager.pendingInputs.getOrDefault(uuid, "").startsWith("confirm_")) {
            String confirmType = commandManager.pendingInputs.remove(uuid);
            event.setCancelled(true);
            if (input.equalsIgnoreCase("confirm")) {
                if (confirmType.equals("confirm_diarrhea_cooldown")) {
                    int cooldown = commandManager.pendingCooldowns.remove(uuid);
                    configManager.setMaxDiarrheaCooldown(cooldown);
                    player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + cooldown + " seconds.");
                } else if (confirmType.equals("confirm_plunge_cooldown")) {
                    int cooldown = commandManager.pendingPlungeCooldowns.remove(uuid);
                    configManager.setMaxPlungeCooldown(cooldown);
                    player.sendMessage(ChatColor.GREEN + "Plunge cooldown has been set to " + cooldown + " seconds.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Change cancelled.");
                commandManager.pendingCooldowns.remove(uuid);
                commandManager.pendingPlungeCooldowns.remove(uuid);
            }
        }
    }
}