package me.poop.poop;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    private String poopName;
    private boolean hopperPickupable;
    private Material poopItem;
    private int maxDiarrheaCooldown;
    private int maxPlungeCooldown;
    private double plungeStrength;
    private Material diarrheaBlock1;
    private Material diarrheaBlock2;
    private boolean poopEnabled;
    private boolean diarrheaEnabled;
    private boolean plungeEnabled;
    private String plungePoopTrailExpression;

    private Material diarrheaItem;
    private Material plungeItem;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        initializeDefaultDiarrheaBlocks();
        initializeDefaultPlungePoopTrail();

        initializeDefaultDiarrheaItem();
        initializeDefaultPlungeItem();

        poopName = ChatColor.translateAlternateColorCodes('&', config.getString("poop-name", "Poop"));
        hopperPickupable = config.getBoolean("hopper-pickupable", true);
        maxDiarrheaCooldown = config.getInt("diarrhea-cooldown", 120);
        maxPlungeCooldown = config.getInt("plunge-cooldown", 60);
        plungeStrength = config.getDouble("plunge-strength", 2.0);
        plungePoopTrailExpression = config.getString("plunge-poop-trail", "5 * strength");
        poopEnabled = true;
        diarrheaEnabled = true;
        plungeEnabled = true;

        String configItem = config.getString("poop-item", "BROWN_DYE");
        try {
            poopItem = Material.valueOf(configItem);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid poop item in config.yml: " + configItem + ". Defaulting to BROWN_DYE.");
            poopItem = Material.BROWN_DYE;
        }

        diarrheaBlock1 = getConfigMaterial("diarrhea-block1", Material.BROWN_CONCRETE);
        diarrheaBlock2 = getConfigMaterial("diarrhea-block2", Material.BROWN_WOOL);

        diarrheaItem = getConfigMaterial("diarrhea-item", Material.BROWN_DYE);
        plungeItem = getConfigMaterial("plunge-item", Material.BROWN_DYE);
    }

    private void initializeDefaultDiarrheaBlocks() {
        if (!config.contains("diarrhea-block1")) {
            config.set("diarrhea-block1", Material.BROWN_CONCRETE.name());
        }
        if (!config.contains("diarrhea-block2")) {
            config.set("diarrhea-block2", Material.BROWN_WOOL.name());
        }
        plugin.saveConfig();
    }

    private void initializeDefaultPlungePoopTrail() {
        if (!config.contains("plunge-poop-trail")) {
            config.set("plunge-poop-trail", "5 * strength");
            plugin.saveConfig();
        }
    }

    private void initializeDefaultDiarrheaItem() {
        if (!config.contains("diarrhea-item")) {
            config.set("diarrhea-item", Material.BROWN_DYE.name());
            plugin.saveConfig();
        }
    }

    private void initializeDefaultPlungeItem() {
        if (!config.contains("plunge-item")) {
            config.set("plunge-item", Material.BROWN_DYE.name());
            plugin.saveConfig();
        }
    }

    private Material getConfigMaterial(String configKey, Material defaultMaterial) {
        String materialName = config.getString(configKey, defaultMaterial.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in config for " + configKey + ". Using default: " + defaultMaterial.name());
            return defaultMaterial;
        }
    }

    public void setDiarrheaBlock(String configKey, String blockName) {
        try {
            Material blockMaterial = Material.valueOf(blockName.toUpperCase());
            config.set(configKey, blockMaterial.name());
            plugin.saveConfig();

            if (configKey.equals("diarrhea-block1")) {
                diarrheaBlock1 = blockMaterial;
            } else {
                diarrheaBlock2 = blockMaterial;
            }
        } catch (IllegalArgumentException e) {
        }
    }

    public String getPoopName() { return poopName; }
    public boolean isHopperPickupable() { return hopperPickupable; }
    public Material getPoopItem() { return poopItem; }
    public int getMaxDiarrheaCooldown() { return maxDiarrheaCooldown; }
    public int getMaxPlungeCooldown() { return maxPlungeCooldown; }
    public double getPlungeStrength() { return plungeStrength; }
    public Material getDiarrheaBlock1() { return diarrheaBlock1; }
    public Material getDiarrheaBlock2() { return diarrheaBlock2; }
    public boolean isPoopEnabled() { return poopEnabled; }
    public boolean isDiarrheaEnabled() { return diarrheaEnabled; }
    public boolean isPlungeEnabled() { return plungeEnabled; }
    public String getPlungePoopTrailExpression() { return plungePoopTrailExpression; }

    public Material getDiarrheaItem() { return diarrheaItem; }
    public Material getPlungeItem() { return plungeItem; }

    public void setPoopName(String poopName) {
        this.poopName = poopName;
        config.set("poop-name", ChatColor.stripColor(poopName));
        plugin.saveConfig();
    }
    public void setHopperPickupable(boolean hopperPickupable) {
        this.hopperPickupable = hopperPickupable;
        config.set("hopper-pickupable", hopperPickupable);
        plugin.saveConfig();
    }
    public void setPoopItem(Material poopItem) {
        this.poopItem = poopItem;
        config.set("poop-item", poopItem.name());
        plugin.saveConfig();
    }
    public void setMaxDiarrheaCooldown(int maxDiarrheaCooldown) {
        this.maxDiarrheaCooldown = maxDiarrheaCooldown;
        config.set("diarrhea-cooldown", maxDiarrheaCooldown);
        plugin.saveConfig();
    }
    public void setMaxPlungeCooldown(int maxPlungeCooldown) {
        this.maxPlungeCooldown = maxPlungeCooldown;
        config.set("plunge-cooldown", maxPlungeCooldown);
        plugin.saveConfig();
    }
    public void setPlungeStrength(double plungeStrength) {
        this.plungeStrength = plungeStrength;
        config.set("plunge-strength", plungeStrength);
        plugin.saveConfig();
    }
    public void setPlungePoopTrailExpression(String expression) {
        this.plungePoopTrailExpression = expression;
        config.set("plunge-poop-trail", expression);
        plugin.saveConfig();
    }
    public void setPoopEnabled(boolean poopEnabled) { this.poopEnabled = poopEnabled; }
    public void setDiarrheaEnabled(boolean diarrheaEnabled) { this.diarrheaEnabled = diarrheaEnabled; }
    public void setPlungeEnabled(boolean plungeEnabled) { this.plungeEnabled = plungeEnabled; }

    public void setDiarrheaItem(Material diarrheaItem) {
        this.diarrheaItem = diarrheaItem;
        config.set("diarrhea-item", diarrheaItem.name());
        plugin.saveConfig();
    }
    public void setPlungeItem(Material plungeItem) {
        this.plungeItem = plungeItem;
        config.set("plunge-item", plungeItem.name());
        plugin.saveConfig();
    }
}