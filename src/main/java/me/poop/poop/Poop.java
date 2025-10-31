package me.poop.poop;

import org.bukkit.plugin.java.JavaPlugin;

public class Poop extends JavaPlugin {
    private PoopManager poopManager;
    private CommandManager commandManager;
    private EventManager eventManager;
    private DataManager dataManager;
    private ConfigManager configManager;
    private BlockManager blockManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.blockManager = new BlockManager(this, configManager);
        this.poopManager = new PoopManager(this, configManager, dataManager, blockManager);
        this.eventManager = new EventManager(this, poopManager, configManager, blockManager);
        this.commandManager = new CommandManager(this, poopManager, configManager, dataManager, blockManager);

        configManager.loadConfig();
        dataManager.loadPoopData();

        eventManager.registerEvents();
        commandManager.registerCommands();

        new UpdateChecker(this).checkForUpdates();

        getLogger().info("Poop plugin enabled. You can now poop after shifting!");
    }

    @Override
    public void onDisable() {
        dataManager.savePoopData();
    }
}