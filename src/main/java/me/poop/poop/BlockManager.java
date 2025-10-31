package me.poop.poop;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class BlockManager implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    private final Set<Block> changingBlocks = new HashSet<>();
    private final Set<Block> revertingBlocks = new HashSet<>();
    private final Map<UUID, List<List<BlockSnapshot>>> activeDiarrheaEvents = new HashMap<>();

    public BlockManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public List<BlockSnapshot> changeGroundBlocks(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int radius = 10;
        List<BlockSnapshot> snapshots = new ArrayList<>();

        Material block1 = configManager.getDiarrheaBlock1();
        Material block2 = configManager.getDiarrheaBlock2();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distanceSquared = (double) (x * x + z * z);
                if (distanceSquared <= (double) (radius * radius)) {
                    for (int y = -radius; y <= radius; y++) {
                        Location blockLocation = center.clone().add((double) x, (double) y, (double) z);
                        Block block = world.getBlockAt(blockLocation);

                        if (block.getType() == Material.AIR || block.getType() == Material.WATER ||
                                block.getType() == Material.RAIL ||
                                block.getType() == Material.POWERED_RAIL ||
                                block.getType() == Material.DETECTOR_RAIL ||
                                block.getType() == Material.ACTIVATOR_RAIL) {
                            continue;
                        }

                        BlockSnapshot snapshot = new BlockSnapshot(block.getState());
                        snapshots.add(snapshot);

                        Material newMaterial = (new Random()).nextBoolean() ? block1 : block2;
                        changingBlocks.add(block);
                        block.setType(newMaterial, false);
                    }
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            changingBlocks.removeAll(snapshots.stream().map(s -> s.location.getBlock()).collect(Collectors.toList()));
        }, 140L);

        return snapshots;
    }

    public void revertBlocks(List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            Block block = snapshot.location.getBlock();
            revertingBlocks.add(block);
            snapshot.restore();
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
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

    public Map<UUID, List<List<BlockSnapshot>>> getActiveDiarrheaEvents() {
        return activeDiarrheaEvents;
    }

    public static class BlockSnapshot {
        private final Location location;
        private final Material material;
        private final BlockData blockData;
        private final BlockState blockState;

        public BlockSnapshot(BlockState state) {
            this.location = state.getLocation();
            this.material = state.getType();
            this.blockData = state.getBlockData();
            this.blockState = state;
        }

        public void restore() {
            Block block = this.location.getBlock();
            block.setType(this.material, false);
            block.setBlockData(this.blockData, false);
            BlockState currentState = block.getState();
            if (currentState.getClass().equals(this.blockState.getClass())) {
                this.blockState.update(true, false);
            }
        }

        public Location getLocation() {
            return location;
        }
    }
}