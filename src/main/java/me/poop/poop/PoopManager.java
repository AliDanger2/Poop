package me.poop.poop;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class PoopManager {
    private final Poop plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final BlockManager blockManager;

    private final Map<String, String> cachedExpressions = new HashMap<>();
    private final Map<Player, Boolean> diarrheaSafeFall = new HashMap<>();
    private final Map<Player, Boolean> poopingEnabled = new HashMap<>();
    private final Map<Player, Boolean> diarrheaMode = new HashMap<>();
    private final Map<Player, Boolean> plungeMode = new HashMap<>();
    private final Map<UUID, Long> diarrheaCooldown = new HashMap<>();
    private final Map<UUID, Long> plungeCooldown = new HashMap<>();
    private final Map<UUID, Boolean> messageCooldown = new HashMap<>();

    private final Map<UUID, Set<Item>> activeItems = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activePlungeTasks = new HashMap<>();

    public PoopManager(Poop plugin, ConfigManager configManager, DataManager dataManager, BlockManager blockManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.blockManager = blockManager;
    }

    public void togglePooping(Player player) {
        if (!player.hasPermission("poop.poop")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        boolean isEnabled = poopingEnabled.getOrDefault(player, false);
        poopingEnabled.put(player, !isEnabled);

        if (diarrheaMode.getOrDefault(player, false)) {
            diarrheaMode.put(player, false);
        }

        if (plungeMode.getOrDefault(player, false)) {
            plungeMode.put(player, false);
        }

        player.sendMessage("Pooping has been " + (isEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
    }

    public void toggleDiarrhea(Player player) {
        if (!player.hasPermission("poop.diarrhea")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        boolean isDiarrheaEnabled = diarrheaMode.getOrDefault(player, false);
        diarrheaMode.put(player, !isDiarrheaEnabled);

        if (poopingEnabled.getOrDefault(player, false)) {
            poopingEnabled.put(player, false);
        }

        if (plungeMode.getOrDefault(player, false)) {
            plungeMode.put(player, false);
        }

        player.sendMessage("Diarrhea mode has been " + (isDiarrheaEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
    }

    public void togglePlunge(Player player) {
        if (!player.hasPermission("poop.plunge")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        boolean isPlungeEnabled = plungeMode.getOrDefault(player, false);
        plungeMode.put(player, !isPlungeEnabled);

        if (poopingEnabled.getOrDefault(player, false)) {
            poopingEnabled.put(player, false);
        }

        if (diarrheaMode.getOrDefault(player, false)) {
            diarrheaMode.put(player, false);
        }

        player.sendMessage("Plunge mode has been " + (isPlungeEnabled ? ChatColor.RED + "disabled." : ChatColor.GREEN + "enabled."));
    }

    public void executePoop(Player player) {
        if (!player.hasPermission("poop.poop")) {
            return;
        }

        if (!configManager.isPoopEnabled()) return;

        double scale = 1.0D;
        if (player.getAttribute(Attribute.GENERIC_SCALE) != null) {
            scale = player.getAttribute(Attribute.GENERIC_SCALE).getValue();
        }

        // Correct scale-dependent Y offset
        double yOffset = (scale >= 3.0) ? 0.5 * scale : 0.25 * scale;
        Location poopLocation = player.getLocation().add(0.0D, yOffset, 0.0D);

        Location direction = player.getLocation();
        Vector playerDirection = direction.getDirection();

        if (Math.abs(direction.getPitch()) == 90) {
            playerDirection.setX(Math.random() * 0.2 - 0.1);
            playerDirection.setZ(Math.random() * 0.2 - 0.1);
            playerDirection.setY(player.getLocation().getPitch() > 0 ? -0.2 : 0.2);
        } else {
            playerDirection.setY(-0.2);
        }

        ItemStack poop = new ItemStack(configManager.getPoopItem(), 1);
        ItemMeta meta = poop.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RESET + configManager.getPoopName());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "poop_tag"), PersistentDataType.BYTE, (byte) 1);
            poop.setItemMeta(meta);
        }

        Item droppedPoop = player.getWorld().dropItem(poopLocation, poop);
        droppedPoop.setPickupDelay(Integer.MAX_VALUE);
        droppedPoop.setVelocity(playerDirection.normalize().multiply(-0.3));

        activeItems.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(droppedPoop);

        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 10) {
                nearbyPlayer.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            droppedPoop.remove();
            removeActiveItem(player.getUniqueId(), droppedPoop);
        }, 40L);
        dataManager.incrementPoopCount(player.getUniqueId());
    }

    public void executeDiarrhea(Player player) {
        if (!player.hasPermission("poop.diarrhea")) {
            return;
        }

        if (!configManager.isDiarrheaEnabled()) return;

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastDiarrheaTime = diarrheaCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastDiarrheaTime < configManager.getMaxDiarrheaCooldown() * 1000L) {
            long remainingTime = (configManager.getMaxDiarrheaCooldown() * 1000L - (currentTime - lastDiarrheaTime)) / 1000L;

            if (!messageCooldown.getOrDefault(playerUUID, false)) {
                player.sendMessage(ChatColor.RED + "Diarrhea is currently on cooldown. Please wait " + ChatColor.GOLD + remainingTime + ChatColor.RED + " seconds.");
                messageCooldown.put(playerUUID, true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    messageCooldown.put(playerUUID, false);
                }, 20L);
            }
            return;
        }

        diarrheaCooldown.put(playerUUID, currentTime);

        boolean hasNearbyDiarrhea = false;

        List<List<BlockManager.BlockSnapshot>> allSnapshots = blockManager.getActiveDiarrheaEvents().get(playerUUID);
        if (allSnapshots != null) {
            for (List<BlockManager.BlockSnapshot> snapshotList : allSnapshots) {
                blockManager.revertBlocks(snapshotList);
            }
            hasNearbyDiarrhea = true;
        }

        for (Map.Entry<UUID, List<List<BlockManager.BlockSnapshot>>> entry : blockManager.getActiveDiarrheaEvents().entrySet()) {
            UUID otherPlayerUUID = entry.getKey();
            if (!otherPlayerUUID.equals(playerUUID)) {
                List<List<BlockManager.BlockSnapshot>> snapshotsList = entry.getValue();
                for (List<BlockManager.BlockSnapshot> snapshots : snapshotsList) {
                    for (BlockManager.BlockSnapshot snapshot : snapshots) {
                        if (snapshot.getLocation().getWorld().equals(player.getWorld()) &&
                                snapshot.getLocation().distance(player.getLocation()) <= 10) {
                            blockManager.revertBlocks(snapshots);
                            hasNearbyDiarrhea = true;
                            break;
                        }
                    }
                    if (hasNearbyDiarrhea) break;
                }
            }
            if (hasNearbyDiarrhea) break;
        }

        if (diarrheaSafeFall.getOrDefault(player, false)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, false, false));
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<BlockManager.BlockSnapshot> currentDiarrheaSnapshots = blockManager.changeGroundBlocks(player);
            executeDiarrheaExplosion(player, currentDiarrheaSnapshots);
        }, 1L);
    }

    private void executeDiarrheaExplosion(Player player, List<BlockManager.BlockSnapshot> currentDiarrheaSnapshots) {
        if (diarrheaMode.getOrDefault(player, false)) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 20, 1.0, 1.0, 1.0, 0.1);
            player.setVelocity(player.getVelocity().setY(20.0));
            spawnBrownDyeCircle(player);
            launchNearbyEntities(player);
        }

        blockManager.getActiveDiarrheaEvents().computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(currentDiarrheaSnapshots);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            blockManager.revertBlocks(currentDiarrheaSnapshots);
            List<List<BlockManager.BlockSnapshot>> events = blockManager.getActiveDiarrheaEvents().get(player.getUniqueId());
            if (events != null) {
                events.remove(currentDiarrheaSnapshots);
                if (events.isEmpty()) {
                    blockManager.getActiveDiarrheaEvents().remove(player.getUniqueId());
                }
            }
        }, 140L);
    }

    public void executePlunge(Player player) {
        if (!player.hasPermission("poop.plunge")) {
            return;
        }

        if (!configManager.isPlungeEnabled()) return;

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastPlungeTime = plungeCooldown.getOrDefault(playerUUID, 0L);

        if (currentTime - lastPlungeTime < configManager.getMaxPlungeCooldown() * 1000L) {
            long remainingTime = (configManager.getMaxPlungeCooldown() * 1000L - (currentTime - lastPlungeTime)) / 1000L;

            if (!messageCooldown.getOrDefault(playerUUID, false)) {
                player.sendMessage(ChatColor.RED + "Plunge is currently on cooldown. Please wait " + ChatColor.GOLD + remainingTime + ChatColor.RED + " seconds.");
                messageCooldown.put(playerUUID, true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    messageCooldown.put(playerUUID, false);
                }, 20L);
            }
            return;
        }

        plungeCooldown.put(playerUUID, currentTime);

        Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(configManager.getPlungeStrength()));

        spawnPlungePoopTrail(player);
    }

    private void spawnPlungePoopTrail(Player player) {
        UUID playerUUID = player.getUniqueId();
        int lungePoopCount = calculatePlungePoopCount();

        BukkitRunnable task = new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= lungePoopCount || !player.isOnline() || player.isDead()) {
                    cancel();
                    activePlungeTasks.remove(playerUUID);
                    return;
                }

                double scale = 1.0D;
                if (player.getAttribute(Attribute.GENERIC_SCALE) != null) {
                    scale = player.getAttribute(Attribute.GENERIC_SCALE).getValue();
                }

                // Correct scale-dependent Y offset
                double yOffset = (scale >= 3.0) ? 0.5 * scale : 0.25 * scale;
                Location poopLocation = player.getLocation().add(0.0D, yOffset, 0.0D);

                ItemStack poop = new ItemStack(configManager.getPlungeItem(), 1);
                ItemMeta meta = poop.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.RESET + configManager.getPoopName());
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "poop_tag"), PersistentDataType.BYTE, (byte) 1);
                    poop.setItemMeta(meta);
                }

                Item droppedPoop = player.getWorld().dropItem(poopLocation, poop);
                droppedPoop.setPickupDelay(Integer.MAX_VALUE);
                Vector poopVelocity = player.getLocation().getDirection().multiply(-0.3);
                droppedPoop.setVelocity(poopVelocity);

                activeItems.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(droppedPoop);

                for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                    if (nearbyPlayer.getLocation().distanceSquared(player.getLocation()) <= 100) {
                        nearbyPlayer.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1.0f, 1.0f);
                    }
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    droppedPoop.remove();
                    removeActiveItem(playerUUID, droppedPoop);
                }, 40L);
                dataManager.incrementPoopCount(playerUUID);
                count++;
            }
        };
        task.runTaskTimer(plugin, 0L, 1L);
        activePlungeTasks.put(playerUUID, task);
    }

    private int calculatePlungePoopCount() {
        String expression = configManager.getPlungePoopTrailExpression();
        double strength = configManager.getPlungeStrength();
        expression = expression.replace("strength", String.valueOf(strength));

        try {
            double result = evaluateExpression(expression);
            return (int) result;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to evaluate plunge poop trail expression: " + e.getMessage());
            return (int) (5 * strength); // Default fallback
        }
    }

    private double evaluateExpression(String expression) {
        if (expression == null) throw new IllegalArgumentException("Expression is null");
        expression = expression.replaceAll("\\s+", ""); // remove whitespace

        // Resolve parentheses by repeatedly computing the innermost (...) and replacing it
        while (expression.contains("(")) {
            int open = expression.lastIndexOf('('); // innermost '('
            if (open == -1) break; // safety
            int close = expression.indexOf(')', open);
            if (close == -1) throw new IllegalArgumentException("Unbalanced parentheses");
            String inner = expression.substring(open + 1, close);
            double innerVal = evaluateExpression(inner); // recursive
            // replace the parentheses (including them) with the computed numeric value
            expression = expression.substring(0, open) + doubleToString(innerVal) + expression.substring(close + 1);
        }

        // Now expression has no parentheses. Evaluate * / first, then + -
        return evaluateNoParentheses(expression);
    }

    // Convert double to string without scientific notation; keep decimal when needed.
    private String doubleToString(double v) {
        if (v == (long) v) {
            return Long.toString((long) v);
        } else {
            return Double.toString(v);
        }
    }

    // Evaluate an expression string that contains NO parentheses.
    // Steps: tokenize into numbers/operators, handle unary minus, process */ left-to-right, then +- left-to-right.
    private double evaluateNoParentheses(String expr) {
        if (expr == null || expr.isEmpty()) throw new IllegalArgumentException("Empty expression");

        List<Double> numbers = new ArrayList<>();
        List<Character> ops = new ArrayList<>();

        int i = 0;
        boolean expectingNumber = true; // true at start or after an operator

        while (i < expr.length()) {
            char c = expr.charAt(i);

            if (c == '+' || c == '-' || c == '*' || c == '/') {
                // handle unary minus (or unary plus) as part of number
                if (expectingNumber && (c == '+' || c == '-')) {
                    // parse sign with the following number
                    int j = i + 1;
                    StringBuilder numSb = new StringBuilder();
                    numSb.append(c); // sign
                    // allow digits and decimal point
                    boolean foundDigit = false;
                    while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) {
                        numSb.append(expr.charAt(j));
                        foundDigit = true;
                        j++;
                    }
                    if (!foundDigit) {
                        throw new IllegalArgumentException("Invalid unary operator usage at position " + i + " in " + expr);
                    }
                    double val = Double.parseDouble(numSb.toString());
                    numbers.add(val);
                    i = j;
                    expectingNumber = false;
                    continue;
                } else {
                    // binary operator
                    ops.add(c);
                    i++;
                    expectingNumber = true;
                    continue;
                }
            } else if (Character.isDigit(c) || c == '.') {
                int j = i;
                StringBuilder numSb = new StringBuilder();
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) {
                    numSb.append(expr.charAt(j));
                    j++;
                }
                double val = Double.parseDouble(numSb.toString());
                numbers.add(val);
                i = j;
                expectingNumber = false;
                continue;
            } else {
                throw new IllegalArgumentException("Invalid character in expression: " + c);
            }
        }

        if (numbers.size() == 0) throw new IllegalArgumentException("No numbers in expression");

        // First pass: handle * and / left-to-right, by collapsing numbers and ops lists.
        List<Double> numbersPass = new ArrayList<>();
        List<Character> opsPass = new ArrayList<>();

        numbersPass.add(numbers.get(0));
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k);
            double nextNum = numbers.get(k + 1);
            if (op == '*' || op == '/') {
                double prev = numbersPass.remove(numbersPass.size() - 1);
                double res = (op == '*') ? (prev * nextNum) : safeDivide(prev, nextNum);
                numbersPass.add(res);
            } else {
                // + or - : defer to next pass
                opsPass.add(op);
                numbersPass.add(nextNum);
            }
        }

        // Second pass: handle + and - left-to-right
        double result = numbersPass.get(0);
        for (int k = 0; k < opsPass.size(); k++) {
            char op = opsPass.get(k);
            double num = numbersPass.get(k + 1);
            if (op == '+') result = result + num;
            else if (op == '-') result = result - num;
            else throw new IllegalArgumentException("Unexpected operator in second pass: " + op);
        }

        return result;
    }

    private double safeDivide(double a, double b) {
        if (b == 0.0) throw new ArithmeticException("Division by zero");
        return a / b;
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

            ItemStack brownDye = new ItemStack(configManager.getDiarrheaItem(), 1);
            Item item = world.dropItem(spawnLocation, brownDye);
            item.setPickupDelay(Integer.MAX_VALUE);
            spawnedItems.add(item);
            activeItems.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(item);
        }

        dataManager.incrementPoopCount(player.getUniqueId(), 25);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Item item : spawnedItems) {
                if (item != null && !item.isDead()) {
                    item.remove();
                }
                removeActiveItem(player.getUniqueId(), item);
            }
        }, 7 * 20L);
    }

    private void launchNearbyEntities(Player player) {
        List<Entity> nearbyEntities = player.getNearbyEntities(10, 10, 10);

        for (Entity entity : nearbyEntities) {
            if (entity.equals(player)) continue;

            Vector launchDirection = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            if (launchDirection.lengthSquared() == 0 || !Double.isFinite(launchDirection.getX()) ||
                    !Double.isFinite(launchDirection.getY()) || !Double.isFinite(launchDirection.getZ())) {
                continue;
            }
            launchDirection.normalize().setY(1.0);
            entity.setVelocity(launchDirection.multiply(1.5));
        }
    }

    public void removeSlowFalling(Player player) {
        if (player.isOnGround() && player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        }
    }

    public boolean isPoopingEnabled(Player player) {
        return poopingEnabled.getOrDefault(player, false);
    }

    public boolean isDiarrheaMode(Player player) {
        return diarrheaMode.getOrDefault(player, false);
    }

    public boolean isPlungeMode(Player player) {
        return plungeMode.getOrDefault(player, false);
    }

    public void toggleDiarrheaSafeFall(Player player) {
        boolean isEnabled = diarrheaSafeFall.getOrDefault(player, false);
        diarrheaSafeFall.put(player, !isEnabled);
    }

    public boolean hasDiarrheaSafeFall(Player player) {
        return diarrheaSafeFall.getOrDefault(player, false);
    }

    private void removeActiveItem(UUID uuid, Item item) {
        Set<Item> items = activeItems.get(uuid);
        if (items != null) {
            items.remove(item);
            if (items.isEmpty()) {
                activeItems.remove(uuid);
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        Set<Item> items = activeItems.remove(uuid);
        if (items != null) {
            for (Item item : items) {
                if (item != null && !item.isDead()) {
                    item.remove();
                }
            }
        }

        BukkitRunnable task = activePlungeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        List<List<BlockManager.BlockSnapshot>> events = blockManager.getActiveDiarrheaEvents().remove(uuid);
        if (events != null) {
            for (List<BlockManager.BlockSnapshot> snapshots : events) {
                blockManager.revertBlocks(snapshots);
            }
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            poopingEnabled.remove(player);
            diarrheaMode.remove(player);
            plungeMode.remove(player);
            diarrheaSafeFall.remove(player);
            if (player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING);
            }
        }
        diarrheaCooldown.remove(uuid);
        plungeCooldown.remove(uuid);
        messageCooldown.remove(uuid);
    }
}