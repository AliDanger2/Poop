package me.poop.poop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import javax.management.Attribute;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {
    private final JavaPlugin plugin;
    private final PoopManager poopManager;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final BlockManager blockManager;

    final Map<UUID, Integer> pendingCooldowns = new HashMap<>();
    final Map<UUID, Integer> pendingPlungeCooldowns = new HashMap<>();
    final Map<UUID, String> pendingInputs = new HashMap<>();

    public CommandManager(JavaPlugin plugin, PoopManager poopManager, ConfigManager configManager, DataManager dataManager, BlockManager blockManager) {
        this.plugin = plugin;
        this.poopManager = poopManager;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.blockManager = blockManager;
    }

    public void registerCommands() {
        Objects.requireNonNull(plugin.getCommand("pa")).setExecutor(new AdminCommand());
        Objects.requireNonNull(plugin.getCommand("pa")).setTabCompleter(new AdminTabCompleter());
        Objects.requireNonNull(plugin.getCommand("poop")).setExecutor(new PoopCommand());
        Objects.requireNonNull(plugin.getCommand("diarrhea")).setExecutor(new DiarrheaCommand());
        Objects.requireNonNull(plugin.getCommand("plunge")).setExecutor(new PlungeCommand());
        Objects.requireNonNull(plugin.getCommand("toppoop")).setExecutor(new TopPoopCommand());
    }

    public void openAdminGUI(Player player) {
        if (!player.hasPermission("poop.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 45, ChatColor.YELLOW + "Poop Admin GUI");

        // Toggle Poop
        Material poopMaterial = configManager.getPoopItem();
        if (poopMaterial == Material.AIR) {
            poopMaterial = Material.BARRIER;
        }
        ItemStack togglePoop = new ItemStack(poopMaterial);
        ItemMeta meta = togglePoop.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Toggle Poop");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (configManager.isPoopEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
        togglePoop.setItemMeta(meta);
        inv.setItem(1, togglePoop);

        // Set Poop Name
        ItemStack setPoopName = new ItemStack(Material.NAME_TAG);
        meta = setPoopName.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Poop Name");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getPoopName(), ChatColor.GRAY + "Click to enter in chat"));
        setPoopName.setItemMeta(meta);
        inv.setItem(3, setPoopName);

        // Set Poop Item
        poopMaterial = configManager.getPoopItem();
        if (poopMaterial == Material.AIR) {
            poopMaterial = Material.BARRIER;
        }
        ItemStack setPoopItem = new ItemStack(poopMaterial);
        meta = setPoopItem.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Poop Item");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getPoopItem().name(), ChatColor.GRAY + "Click to enter material in chat"));
        setPoopItem.setItemMeta(meta);
        inv.setItem(4, setPoopItem);

        // Toggle Hopper Pickupable
        ItemStack toggleHopper = new ItemStack(Material.HOPPER);
        meta = toggleHopper.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Toggle Hopper Pickupable");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (configManager.isHopperPickupable() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
        toggleHopper.setItemMeta(meta);
        inv.setItem(5, toggleHopper);

        // Toggle Diarrhea
        ItemStack toggleDiarrhea = new ItemStack(Material.BROWN_DYE);
        meta = toggleDiarrhea.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Toggle Diarrhea");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (configManager.isDiarrheaEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
        toggleDiarrhea.setItemMeta(meta);
        inv.setItem(19, toggleDiarrhea);

        // Set Diarrhea Cooldown
        ItemStack setDiarrheaCooldown = new ItemStack(Material.CLOCK);
        meta = setDiarrheaCooldown.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Diarrhea Cooldown");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getMaxDiarrheaCooldown() + " seconds", ChatColor.GRAY + "Click to enter in chat"));
        setDiarrheaCooldown.setItemMeta(meta);
        inv.setItem(21, setDiarrheaCooldown);

        // Toggle Diarrhea Safe Fall
        ItemStack toggleSafeFall = new ItemStack(Material.FEATHER);
        meta = toggleSafeFall.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Toggle Diarrhea Safe Fall");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (poopManager.hasDiarrheaSafeFall(player) ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
        toggleSafeFall.setItemMeta(meta);
        inv.setItem(22, toggleSafeFall);

        // Set Diarrhea Block 1
        Material block1Material = configManager.getDiarrheaBlock1();
        if (block1Material == Material.AIR) {
            block1Material = Material.BARRIER;
        }
        ItemStack setBlock1 = new ItemStack(block1Material);
        meta = setBlock1.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Diarrhea Block 1");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getDiarrheaBlock1().name(), ChatColor.GRAY + "Click to enter material in chat"));
        setBlock1.setItemMeta(meta);
        inv.setItem(23, setBlock1);

        // Set Diarrhea Block 2
        Material block2Material = configManager.getDiarrheaBlock2();
        if (block2Material == Material.AIR) {
            block2Material = Material.BARRIER;
        }
        ItemStack setBlock2 = new ItemStack(block2Material);
        meta = setBlock2.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Diarrhea Block 2");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getDiarrheaBlock2().name(), ChatColor.GRAY + "Click to enter material in chat"));
        setBlock2.setItemMeta(meta);
        inv.setItem(24, setBlock2);

        // Toggle Plunge
        ItemStack togglePlunge = new ItemStack(Material.TRIDENT);
        meta = togglePlunge.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Toggle Plunge");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + (configManager.isPlungeEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"), ChatColor.GRAY + "Click to toggle"));
        togglePlunge.setItemMeta(meta);
        inv.setItem(37, togglePlunge);

        // Set Plunge Cooldown
        ItemStack setPlungeCooldown = new ItemStack(Material.CLOCK);
        meta = setPlungeCooldown.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Plunge Cooldown");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getMaxPlungeCooldown() + " seconds", ChatColor.GRAY + "Click to enter in chat"));
        setPlungeCooldown.setItemMeta(meta);
        inv.setItem(39, setPlungeCooldown);

        // Set Plunge Strength
        ItemStack setPlungeStrength = new ItemStack(Material.REDSTONE);
        meta = setPlungeStrength.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Plunge Strength");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getPlungeStrength(), ChatColor.GRAY + "Click to enter in chat"));
        setPlungeStrength.setItemMeta(meta);
        inv.setItem(40, setPlungeStrength);

        // Set Plunge Poop Trail
        ItemStack setPlungePoopTrail = new ItemStack(Material.SLIME_BALL);
        meta = setPlungePoopTrail.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Set Plunge Poop Trail");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Current: " + configManager.getPlungePoopTrailExpression(), ChatColor.GRAY + "Click to enter expression in chat"));
        setPlungePoopTrail.setItemMeta(meta);
        inv.setItem(41, setPlungePoopTrail);

        // Fill remaining slots with lime stained-glass panes
        ItemStack pane = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        paneMeta.setDisplayName(" ");
        paneMeta.setHideTooltip(true);
        pane.setItemMeta(paneMeta);

        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, pane);
            }
        }
        player.openInventory(inv);
    }

    private class AdminCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) {
                return true;
            }

            if (!player.hasPermission("poop.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                openAdminGUI(player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "toggle_poop":
                    boolean newPoopState = !configManager.isPoopEnabled();
                    configManager.setPoopEnabled(newPoopState);
                    player.sendMessage(ChatColor.YELLOW + "Pooping has been " + (newPoopState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                    break;

                case "poop_name":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa poop_name <name>");
                        return true;
                    }

                    String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    configManager.setPoopName(ChatColor.translateAlternateColorCodes('&', newName));
                    player.sendMessage(ChatColor.GREEN + "Poop item name has been set to: " + configManager.getPoopName());
                    break;

                case "poop_item":
                    if (args.length >= 2) {
                        String itemName = args[1].toUpperCase();
                        try {
                            Material newPoopItem = Material.valueOf(itemName);
                            configManager.setPoopItem(newPoopItem);
                            player.sendMessage(ChatColor.GREEN + "Poop item set to " + newPoopItem.name());
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(ChatColor.RED + "Invalid item name: " + itemName);
                        }
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa poop_item <item>");
                    }
                    break;

                case "hopper_pickupable":
                    boolean newHopperState = !configManager.isHopperPickupable();
                    configManager.setHopperPickupable(newHopperState);
                    player.sendMessage(ChatColor.YELLOW + "Hopper pickup has been " +
                            (newHopperState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                    break;

                case "toggle_diarrhea":
                    boolean newDiarrheaState = !configManager.isDiarrheaEnabled();
                    configManager.setDiarrheaEnabled(newDiarrheaState);
                    player.sendMessage(ChatColor.YELLOW + "Diarrhea has been " + (newDiarrheaState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
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
                            configManager.setMaxDiarrheaCooldown(newCooldown);
                            player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + newCooldown + " seconds.");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                    }
                    break;

                case "diarrhea_safe_fall":
                    poopManager.toggleDiarrheaSafeFall(player);
                    boolean isEnabled = poopManager.hasDiarrheaSafeFall(player);
                    player.sendMessage(ChatColor.YELLOW + "Diarrhea safe fall has been " +
                            (isEnabled ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                    break;

                case "toggle_plunge":
                    boolean newPlungeState = !configManager.isPlungeEnabled();
                    configManager.setPlungeEnabled(newPlungeState);
                    player.sendMessage(ChatColor.YELLOW + "Plunge has been " + (newPlungeState ? ChatColor.GREEN + "enabled." : ChatColor.RED + "disabled."));
                    break;

                case "plunge_cooldown":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa plunge_cooldown <seconds>");
                        return true;
                    }
                    try {
                        int newCooldown = Integer.parseInt(args[1]);
                        if (newCooldown < 7) {
                            player.sendMessage(ChatColor.RED + "WARNING! " + ChatColor.YELLOW +
                                    "If the cooldown is less than 7 seconds many issues will happen such as lag and many other things so be sure of what you're doing! " +
                                    "If you want to proceed write /pa confirm in the chat.");
                            pendingPlungeCooldowns.put(player.getUniqueId(), newCooldown);
                        } else {
                            configManager.setMaxPlungeCooldown(newCooldown);
                            player.sendMessage(ChatColor.GREEN + "Plunge cooldown has been set to " + newCooldown + " seconds.");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                    }
                    break;

                case "plunge_strength":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa plunge_strength <value>");
                        return true;
                    }
                    try {
                        double newStrength = Double.parseDouble(args[1]);
                        configManager.setPlungeStrength(newStrength);
                        player.sendMessage(ChatColor.GREEN + "Plunge strength has been set to " + newStrength);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Please enter a valid number.");
                    }
                    break;

                case "plunge_poop_trail":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa plunge_poop_trail <expression>");
                        return true;
                    }
                    String expression = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    configManager.setPlungePoopTrailExpression(expression);
                    player.sendMessage(ChatColor.GREEN + "Plunge poop trail expression has been set to " + expression);
                    break;

                case "diarrhea_block1":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa diarrhea_block1 <block>");
                        return true;
                    }
                    configManager.setDiarrheaBlock("diarrhea-block1", args[1]);
                    player.sendMessage(ChatColor.GREEN + "Block for diarrhea-block1 has been set to " + args[1].toUpperCase());
                    break;

                case "diarrhea_block2":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.YELLOW + "Usage: /pa diarrhea_block2 <block>");
                        return true;
                    }
                    configManager.setDiarrheaBlock("diarrhea-block2", args[1]);
                    player.sendMessage(ChatColor.GREEN + "Block for diarrhea-block2 has been set to " + args[1].toUpperCase());
                    break;

                case "confirm":
                    if (pendingCooldowns.containsKey(player.getUniqueId())) {
                        int cooldown = pendingCooldowns.get(player.getUniqueId());
                        configManager.setMaxDiarrheaCooldown(cooldown);
                        player.sendMessage(ChatColor.GREEN + "Diarrhea cooldown has been set to " + cooldown + " seconds.");
                        pendingCooldowns.remove(player.getUniqueId());
                    } else if (pendingPlungeCooldowns.containsKey(player.getUniqueId())) {
                        int cooldown = pendingPlungeCooldowns.get(player.getUniqueId());
                        configManager.setMaxPlungeCooldown(cooldown);
                        player.sendMessage(ChatColor.GREEN + "Plunge cooldown has been set to " + cooldown + " seconds.");
                        pendingPlungeCooldowns.remove(player.getUniqueId());
                    } else {
                        player.sendMessage(ChatColor.RED + "There is no pending cooldown change to confirm.");
                    }
                    return true;

                default:
                    player.sendMessage(ChatColor.YELLOW + "Usage: /pa <toggle_poop | poop_item | poop_name | hopper_pickupable | toggle_diarrhea | diarrhea_cooldown | diarrhea_safe_fall | toggle_plunge | plunge_cooldown | plunge_strength | diarrhea_block1 | diarrhea_block2>");
                    break;
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], Arrays.asList(
                        "toggle_poop", "poop_item", "poop_name", "hopper_pickupable",
                        "toggle_diarrhea", "diarrhea_cooldown", "diarrhea_safe_fall",
                        "toggle_plunge", "plunge_cooldown", "plunge_strength", "plunge_poop_trail",
                        "diarrhea_block1", "diarrhea_block2", "confirm"
                ), new ArrayList<>());
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("diarrhea_block1") ||
                    args[0].equalsIgnoreCase("diarrhea_block2") || args[0].equalsIgnoreCase("poop_item"))) {
                return StringUtil.copyPartialMatches(args[1], getMaterialNames(), new ArrayList<>());
            }
            return new ArrayList<>();
        }

        private List<String> getMaterialNames() {
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private class AdminTabCompleter implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            return false;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return new AdminCommand().onTabComplete(sender, command, alias, args);
        }
    }

    private class PoopCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player player) {
                poopManager.togglePooping(player);
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return new ArrayList<>();
        }
    }

    private class DiarrheaCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player player) {
                poopManager.toggleDiarrhea(player);
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return new ArrayList<>();
        }
    }

    private class PlungeCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player player) {
                poopManager.togglePlunge(player);
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return new ArrayList<>();
        }
    }

    private class TopPoopCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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

                dataManager.showLeaderboard(player, page);
                return true;
            }
            return false;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return new ArrayList<>();
        }
    }
}