package ru.xczdev.ticketsManager.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.xczdev.ticketsManager.utils.DatabaseUtil;

import java.util.List;
import java.util.Objects;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class TicketReply implements CommandExecutor {
    private static ConfigurationSection lang;
    private static ConfigurationSection settings;

    public TicketReply() {
        lang = getInstance().getLang();
        settings = getInstance().getSettings();
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        if (player.hasPermission(settings.getString("moderatorPermission"))) {
            List ticket = DatabaseUtil.getTicketByModerator(player.getName());
            if (ticket != null) {
                String id = ticket.get(0).toString();
                String publishDate = ticket.get(1).toString();
                String text = ticket.get(2).toString();
                String owner = ticket.get(3).toString();
                String moderator = ticket.get(4).toString();
                String expireDate = ticket.get(5).toString();

                player.sendMessage(lang.getString("moderatorMessage").replace("&", "§")
                        .replace("%moderator%", player.getName())
                        .replace("%message%", String.join(" ", args).replace("&", "§")));
                if (Bukkit.getPlayer(owner) != null) {
                    Bukkit.getPlayer(owner).sendMessage(lang.getString("moderatorMessage").replace("&", "§")
                            .replace("%moderator%", player.getName())
                            .replace("%message%", String.join(" ", args).replace("&", "§")));
                } else {
                    player.sendMessage(lang.getString("userNotOnline").replace("&", "§"));
                }
            } else {
                player.sendMessage(lang.getString("ticketNotAccepted").replace("&", "§"));
            }
        } else {
            List ticket = DatabaseUtil.getTicketByPlayer(player.getName());
            if (ticket != null) {
                String id = ticket.get(0).toString();
                String publishDate = ticket.get(1).toString();
                String text = ticket.get(2).toString();
                String owner = ticket.get(3).toString();
                String moderator = ticket.get(4).toString();
                String expireDate = ticket.get(5).toString();

                if (Objects.equals(moderator, "")) {
                    player.sendMessage(lang.getString("NoOneModeratorsAccepted").replace("&", "§"));
                    return true;
                }

                player.sendMessage(lang.getString("playerMessage").replace("&", "§")
                        .replace("%player%", player.getName())
                        .replace("%message%", String.join(" ", args).replace("&", "§")));
                if (Bukkit.getPlayer(owner) != null) {
                    Bukkit.getPlayer(moderator).sendMessage(lang.getString("playerMessage").replace("&", "§")
                            .replace("%player%", player.getName())
                            .replace("%message%", String.join(" ", args).replace("&", "§")));
                } else {
                    player.sendMessage(lang.getString("userNotOnline").replace("&", "§"));
                }
            } else {
                player.sendMessage(lang.getString("ticketNotCreated").replace("&", "§"));
            }
        }
        return true;
    }
}
