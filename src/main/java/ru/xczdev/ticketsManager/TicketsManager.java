package ru.xczdev.ticketsManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.xczdev.ticketsManager.commands.TicketClose;
import ru.xczdev.ticketsManager.commands.TicketCommand;
import ru.xczdev.ticketsManager.commands.TicketReply;
import ru.xczdev.ticketsManager.utils.DatabaseUtil;
import ru.xczdev.ticketsManager.utils.TicketCommandTabCompleter;

public final class TicketsManager extends JavaPlugin {

    private TicketsManagerPlaceholder placeholderExpansion;
    private static TicketsManager instance;
    private static FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new TicketsManagerPlaceholder(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI расширение зарегистрировано.");
        } else {
            getLogger().info("PlaceholderAPI не найден. Заполнители не будут работать.");
        }
        initDatabase();
        registerCommands();
        registerListeners();
        getLogger().info("TicketsManager has been enabled!");
    }

    public static TicketsManager getInstance() {
        return instance;
    }

    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
    }

    public void reloadPlugin() {
        reloadConfig();
        config = getConfig();
        initDatabase();
        registerCommands();
        registerListeners();
        getLogger().info("TicketsManager configuration reloaded!");
    }

    private void initDatabase() {
        new DatabaseUtil();
    }

    private void registerCommands() {
        getCommand("ticket").setExecutor(new TicketCommand());
        getCommand("ticket").setTabCompleter(new TicketCommandTabCompleter());
        getCommand("ticketreply").setExecutor(new TicketReply());
        getCommand("ticketclose").setExecutor(new TicketClose());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MenuManager(), this);
    }

    public ConfigurationSection getLang() {
        return config.getConfigurationSection("lang");
    }

    public ConfigurationSection getSettings() {
        return config.getConfigurationSection("settings");
    }

    public TicketsManagerPlaceholder getPlaceholderExpansion() {
        return placeholderExpansion;
    }
}
