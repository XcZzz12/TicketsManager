package ru.xczdev.ticketsManager.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class TicketCommandTabCompleter implements TabCompleter {

    private ConfigurationSection settings;

    public TicketCommandTabCompleter() {
        settings = getInstance().getSettings();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && player.hasPermission(settings.getString("adminPermission"))) {
            completions.add("info");
            completions.add("reload");
        } else if (args.length == 2 && player.hasPermission(settings.getString("adminPermission"))) {
            if (args[0].equalsIgnoreCase("info")) {
                completions = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission(settings.getString("moderatorPermission")))
                        .toList().stream().map(Player::getName)
                        .collect(Collectors.toList());
            }
        } else if (args.length == 3 && player.hasPermission(settings.getString("adminPermission"))) {
            completions.add("day");
            completions.add("week");
            completions.add("month");
        }

        String currentInput = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(currentInput)) {
                filtered.add(completion);
            }
        }

        return filtered;
    }
}
