package ru.xczdev.ticketsManager.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.xczdev.ticketsManager.MenuManager;
import ru.xczdev.ticketsManager.utils.DatabaseUtil;

import java.util.List;
import java.util.logging.Logger;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class TicketCommand implements CommandExecutor {

    private static ConfigurationSection lang;
    private static ConfigurationSection settings;

    public TicketCommand() {
        lang = getInstance().getLang();
        settings = getInstance().getSettings();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        if (!(player instanceof Player)) {
            Logger.getLogger("Minecraft").warning(lang.getString("consoleError").replace("&", "§"));
            return true;
        }
        if (player.hasPermission(settings.getString("adminPermission"))) {
            if (args.length == 0) {
                MenuManager.openMenu(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                getInstance().reloadPlugin();
                player.sendMessage(lang.getString("reloadSuccess").replace("&", "§"));
                return true;
            } else if (args[0].equalsIgnoreCase("info")) {
                if (args.length < 2) {
                    player.sendMessage(lang.getString("moderatorInfoError").replace("&", "§"));
                    return true;
                } else {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    if (target.getPlayer() == null) {
                        player.sendMessage(lang.getString("moderatorOffline").replace("&", "§"));
                        return true;
                    }
                    if (args.length > 2) {
                        String period = args[2].toLowerCase();
                        Double rating = DatabaseUtil.getAverageModeratorRating(target.getName(), period);
                        String periodFormatted = period.replace("day", lang.getString("periodDay"))
                                        .replace("week", lang.getString("periodWeek"))
                                        .replace("month", lang.getString("periodMonth"));
                        if (rating != null) {
                            int ticketCount = DatabaseUtil.getTicketCountByModerator(target.getName(), period);
                            player.sendMessage(String.join("\n", lang.getStringList("moderatorInfoPeriod")).replace("&", "§")
                                    .replace("%moderator%", target.getName())
                                    .replace("%rating%", String.valueOf(rating))
                                    .replace("%period%", periodFormatted)
                                    .replace("%tickets%", String.valueOf(ticketCount)));
                        } else {
                            player.sendMessage(lang.getString("infoPeriodError").replace("&", "§"));
                        }
                        return true;
                    }
                    if (target.getPlayer().hasPermission(settings.getString("moderatorPermission"))) {
                        Double rating = DatabaseUtil.getAverageModeratorRatingOverall(target.getName());
                        int ticketCount = DatabaseUtil.getTicketCountByModerator(target.getName(), "all");
                        player.sendMessage(String.join("\n", lang.getStringList("moderatorInfo")).replace("&", "§")
                                .replace("%moderator%", target.getName())
                                .replace("%rating%", String.valueOf(rating))
                                .replace("%tickets%", String.valueOf(ticketCount)));
                    } else {
                        player.sendMessage(lang.getString("moderatorInfoErrorNoPerm").replace("&", "§"));
                    }
                }
            }
        } else if (player.hasPermission(settings.getString("moderatorPermission"))) {
            if (args.length == 0) {
                MenuManager.openMenu(player);
                return true;
            }
        } else {
            if (args.length == 0) {
                player.sendMessage(lang.getString("ticketHelp").replace("&", "§"));
                return true;
            }
            if (DatabaseUtil.getTicketByPlayer(player.getName()) != null) {
                player.sendMessage(lang.getString("ticketErrorCreated").replace("&", "§"));
                return true;
            }
            if (DatabaseUtil.createTicket(player.getName(), String.join(" ", args))) {
                player.sendMessage(lang.getString("ticketWasCreated").replace("&", "§"));
                sendMessageToModeration(lang.getString("ticketWasCreateForModerator").replace("&", "§")
                        .replace("%player%", player.getName()));
            } else {
                player.sendMessage(lang.getString("ticketCreateError").replace("&", "§"));
            }
        }

        return true;
    }

    private void sendMessageToModeration(String message) {
        List players = Bukkit.getOnlinePlayers().stream().toList();
        for (Object player : players) {
            Player p = (Player) player;
            if (p.hasPermission(settings.getString("moderatorPermission"))) {
                p.sendMessage(message);
            }
        }
    }
}
