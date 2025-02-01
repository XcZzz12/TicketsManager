package ru.xczdev.ticketsManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ru.xczdev.ticketsManager.utils.DatabaseUtil;

import java.util.List;

public class TicketsManagerPlaceholder extends PlaceholderExpansion {

    private TicketsManager plugin;

    /**
     * Конструктор для инициализации расширения.
     *
     * @param plugin Экземпляр вашего плагина.
     */
    public TicketsManagerPlaceholder(TicketsManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "TicketsManager";
    }

    @Override
    public String getAuthor() {
        return "XcZzz";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean register() {
        return super.register();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        
        String[] params = identifier.split("_");

        if (params[0].equals("rating")) {
            if (params.length == 2) {
                if (!params[1].equals("day") || !params[1].equals("month") || !params[1].equals("week")) {
                    return "Неверное использование (day, week, month)";
                } else {
                    return DatabaseUtil.getAverageModeratorRating(player.getName(), params[1]).toString();
                }
            }
            return DatabaseUtil.getAverageModeratorRatingOverall(player.getName()).toString();
        } else if (params[0].equals("count")) {
            List tickets = DatabaseUtil.getAllTickets();
            return String.valueOf(tickets.size());
        } else if (params[0].equals("moderatorRating")) {
            if (params.length < 2) {
                return "Неверное использование (Укажите ник модератора)";
            }

            if (params.length == 3) {
                if (!params[2].equals("day") || !params[2].equals("month") || !params[2].equals("week")) {
                    return "Неверное использование (day, week, month)";
                } else {
                    return DatabaseUtil.getAverageModeratorRating(params[1], params[2]).toString();
                }
            } else {
                return DatabaseUtil.getAverageModeratorRatingOverall(params[1]).toString();
            }
        }

        return null;
    }
}
