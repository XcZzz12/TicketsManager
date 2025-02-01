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

public class TicketClose implements CommandExecutor {
    private static ConfigurationSection lang;
    private static ConfigurationSection settings;

    public TicketClose() {
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

                player.sendMessage(lang.getString("ticketWasClosedModeratorForModerator").replace("&", "§")
                        .replace("%moderator%", moderator)
                        .replace("%id%", id)
                        .replace("%publishDate%", publishDate)
                        .replace("%text%", text)
                        .replace("%player%", owner)
                        .replace("%expireDate%", expireDate));
                Bukkit.getPlayer(owner).sendMessage(lang.getString("ticketWasClosedModeratorForPlayer").replace("&", "§")
                        .replace("%moderator%", moderator)
                        .replace("%id%", id)
                        .replace("%publishDate%", publishDate)
                        .replace("%text%", text)
                        .replace("%player%", owner)
                        .replace("%expireDate%", expireDate));
                DatabaseUtil.deleteTicket(Integer.parseInt(id));
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
                    player.sendMessage(lang.getString("ticketWasClosedPlayerForPlayer").replace("&", "§")
                            .replace("%id%", id)
                            .replace("%publishDate%", publishDate)
                            .replace("%text%", text)
                            .replace("%player%", owner)
                            .replace("%expireDate%", expireDate));
                    DatabaseUtil.deleteTicket(Integer.parseInt(id));
                    return true;
                }

                if (args.length > 0) {
                    try {
                        int ticketRate = Integer.parseInt(args[0]);
                        if (ticketRate <= 0 || ticketRate > 5) {
                            player.sendMessage(lang.getString("ticketRateError").replace("&", "§"));
                            return true;
                        }
                        player.sendMessage(lang.getString("ticketWasClosedPlayerForPlayerWithRate").replace("&", "§")
                                .replace("%id%", id)
                                .replace("%publishDate%", publishDate)
                                .replace("%text%", text)
                                .replace("%player%", owner)
                                .replace("%expireDate%", expireDate)
                                .replace("%rate%", String.valueOf(ticketRate)));
                        Bukkit.getPlayer(moderator).sendMessage(lang.getString("ticketWasClosedPlayerForModeratorWithRate").replace("&", "§")
                                .replace("%moderator%", moderator)
                                .replace("%id%", id)
                                .replace("%publishDate%", publishDate)
                                .replace("%text%", text)
                                .replace("%player%", owner)
                                .replace("%expireDate%", expireDate)
                                .replace("%rate%", String.valueOf(ticketRate)));
                        DatabaseUtil.deleteTicket(Integer.parseInt(id));
                        DatabaseUtil.addModeratorRating(Integer.parseInt(id), moderator, ticketRate);
                        return true;
                    } catch (NumberFormatException e) {
                        player.sendMessage(lang.getString("ticketRateError").replace("&", "§"));
                        return true;
                    }
                } else {
                    player.sendMessage(lang.getString("ticketWasClosedPlayerForPlayer").replace("&", "§")
                            .replace("%id%", id)
                            .replace("%publishDate%", publishDate)
                            .replace("%text%", text)
                            .replace("%player%", owner)
                            .replace("%expireDate%", expireDate));
                    Bukkit.getPlayer(moderator).sendMessage(lang.getString("ticketWasClosedPlayerForModerator").replace("&", "§")
                            .replace("%moderator%", moderator)
                            .replace("%id%", id)
                            .replace("%publishDate%", publishDate)
                            .replace("%text%", text)
                            .replace("%player%", owner)
                            .replace("%expireDate%", expireDate));
                    DatabaseUtil.deleteTicket(Integer.parseInt(id));
                    return true;
                }
            } else {
                player.sendMessage(lang.getString("ticketNotCreated").replace("&", "§"));
            }
        }
        return true;
    }
}
