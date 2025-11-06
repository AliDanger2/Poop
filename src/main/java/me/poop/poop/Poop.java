package me.poop.poop;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.entity.Player;
import java.util.Collection;

public class Poop extends JavaPlugin implements Listener {
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
        this.commandManager = new CommandManager(this, poopManager, configManager, dataManager, blockManager);
        this.eventManager = new EventManager(this, poopManager, configManager, blockManager, commandManager);

        configManager.loadConfig();
        dataManager.loadPoopData();

        eventManager.registerEvents();
        commandManager.registerCommands();

        new UpdateChecker(this).checkForUpdates();

        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Poop plugin enabled. You can now poop after shifting!");
    }

    @Override
    public void onDisable() {
        dataManager.savePoopData();
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        Collection<String> commands = event.getCommands();

        if (!player.hasPermission("poop.poop")) {
            commands.remove("poop");
        }
        if (!player.hasPermission("poop.diarrhea")) {
            commands.remove("diarrhea");
        }
        if (!player.hasPermission("poop.plunge")) {
            commands.remove("plunge");
        }
        if (!player.hasPermission("poop.toppoop")) {
            commands.remove("toppoop");
        }
        if (!player.hasPermission("poop.admin")) {
            commands.remove("pa");
        }
    }
}