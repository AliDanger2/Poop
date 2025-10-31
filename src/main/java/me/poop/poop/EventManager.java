package me.poop.poop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EventManager implements Listener {
    private final JavaPlugin plugin;
    private final PoopManager poopManager;
    private final ConfigManager configManager;
    private final BlockManager blockManager;

    public EventManager(JavaPlugin plugin, PoopManager poopManager, ConfigManager configManager, BlockManager blockManager) {
        this.plugin = plugin;
        this.poopManager = poopManager;
        this.configManager = configManager;
        this.blockManager = blockManager;
    }

    public void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(blockManager, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (!configManager.isHopperPickupable()) {
            if (event.getItem().getItemStack().getType() == configManager.getPoopItem()) {
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
}