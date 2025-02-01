package ru.xczdev.ticketsManager.utils;

import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class DatabaseUtil {
    private static ConfigurationSection settings;
    private static Connection connection;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    public DatabaseUtil() {
        settings = getInstance().getSettings();
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/TicketsManager/database.db");
            createTicketsTable();
            createModeratorRatingsTable();
            startScheduledSynchronization();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTicketsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "publishDate TEXT NOT NULL," +
                "text TEXT NOT NULL," +
                "owner TEXT NOT NULL," +
                "moderator TEXT NOT NULL," +
                "expireDate TEXT NOT NULL" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createModeratorRatingsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS moderator_ratings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ticket_id INTEGER NOT NULL," +
                "moderator TEXT NOT NULL," +
                "rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5)," +
                "date TEXT NOT NULL," +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id)" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Double getAverageModeratorRating(String moderator, String period) {
        String sql = "";
        String dateCondition = "";
        switch (period.toLowerCase()) {
            case "day":
                sql = "SELECT AVG(rating) FROM moderator_ratings WHERE moderator = ? AND date = ?";
                break;
            case "week":
                sql = "SELECT AVG(rating) FROM moderator_ratings WHERE moderator = ? AND date >= ?";
                // Вычисляем дату неделю назад
                dateCondition = LocalDate.parse(getCurrentDate()).minusDays(7).toString();
                break;
            case "month":
                sql = "SELECT AVG(rating) FROM moderator_ratings WHERE moderator = ? AND date >= ?";
                // Вычисляем дату месяц назад
                dateCondition = LocalDate.parse(getCurrentDate()).minusMonths(1).toString();
                break;
            default:
                return null;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, moderator);
            if (period.equalsIgnoreCase("day")) {
                pstmt.setString(2, getCurrentDate());
            } else {
                pstmt.setString(2, dateCondition);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Double average = rs.getDouble(1);
                if (average != null) {
                    if (average == Math.floor(average)) {
                        return average;
                    } else {
                        return Math.round(average * 100.0) / 100.0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Boolean addModeratorRating(int ticketId, String moderator, int rating) {
        String sql = "INSERT INTO moderator_ratings (ticket_id, moderator, rating, date) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String currentDate = getCurrentDate();
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, moderator);
            pstmt.setInt(3, rating);
            pstmt.setString(4, currentDate);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Double getAverageModeratorRatingOverall(String moderator) {
        String sql = "SELECT AVG(rating) FROM moderator_ratings WHERE moderator = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, moderator);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Double average = rs.getDouble(1);
                if (average != null) {
                    if (average == Math.floor(average)) {
                        return average;
                    } else {
                        return Math.round(average * 100.0) / 100.0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Boolean createTicket(String owner, String text) {
        String sql = "INSERT INTO tickets (publishDate, text, owner, moderator, expireDate) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String currentDate = getCurrentDate(); // Метод для получения текущей даты в формате YYYY-MM-DD
            String expireDate = getExpireDate(currentDate); // Метод для получения даты истечения срока действия

            pstmt.setString(1, currentDate);
            pstmt.setString(2, text);
            pstmt.setString(3, owner);
            pstmt.setString(4, ""); // Модератор пока не назначен
            pstmt.setString(5, expireDate);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean deleteTicket(int id) {
        String sql = "DELETE FROM tickets WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Тикет с id " + id + " не найден.");
            } else {
                System.out.println("Тикет с id " + id + " успешно удален.");
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean acceptTicket(String moderator, int id) {
        String sql = "UPDATE tickets SET moderator = ?, expireDate = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String currentDate = getCurrentDate(); // Метод для получения текущей даты в формате YYYY-MM-DD
            String newExpireDate = getExpireDate(currentDate); // Метод для получения новой даты истечения срока действия

            pstmt.setString(1, moderator);
            pstmt.setString(2, newExpireDate);
            pstmt.setInt(3, id);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.out.println("Тикет с id " + id + " не найден.");
            } else {
                System.out.println("Тикет с id " + id + " успешно принят модератором " + moderator + ".");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static List<List> getAllTickets() {
        List<List> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                List ticket = new ArrayList();
                ticket.add(rs.getInt("id"));
                ticket.add(rs.getString("text"));
                ticket.add(rs.getString("owner"));
                ticket.add(rs.getString("moderator"));
                ticket.add(rs.getString("expireDate"));
                ticket.add(rs.getString("publishDate"));
                tickets.add(ticket);
            }
            return tickets;
        } catch (SQLException e) {
            e.printStackTrace();
            return tickets;
        }
    }

    public static List getTicketByModerator(String moderator) {
        String sql = "SELECT * FROM tickets WHERE moderator = ?";
        List ticket = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, moderator);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                ticket.add(rs.getInt("id"));
                ticket.add(rs.getString("publishDate"));
                ticket.add(rs.getString("text"));
                ticket.add(rs.getString("owner"));
                ticket.add(rs.getString("moderator"));
                ticket.add(rs.getString("expireDate"));
                return ticket;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static List getTicketByPlayer(String owner) {
        String sql = "SELECT * FROM tickets WHERE owner = ?";
        List ticket = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, owner);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                ticket.add(rs.getInt("id"));
                ticket.add(rs.getString("publishDate"));
                ticket.add(rs.getString("text"));
                ticket.add(rs.getString("owner"));
                ticket.add(rs.getString("moderator"));
                ticket.add(rs.getString("expireDate"));
                return ticket;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private void startScheduledSynchronization() {
        Runnable syncTask = this::synchronizeDatabase;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        long initialDelay = ChronoUnit.SECONDS.between(now, nextRun);
        long period = TimeUnit.DAYS.toSeconds(1);
        scheduler.scheduleAtFixedRate(syncTask, initialDelay, period, TimeUnit.SECONDS);
    }

    public void synchronizeDatabase() {
        String currentDate = getCurrentDate();
        String sql = "DELETE FROM tickets WHERE expireDate = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, currentDate);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                Logger.getLogger("Minecraft").info("Удалено " + affectedRows + " тикетов с истекшим сроком действия.");
            } else {
                Logger.getLogger("Minecraft").info("Нет тикетов с истекшим сроком действия для удаления.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Integer getTicketCountByModerator(String moderator, String period) {
        String sql = "";
        String dateCondition = "";
        LocalDate currentDate = LocalDate.now();

        switch (period.toLowerCase()) {
            case "day":
                sql = "SELECT COUNT(*) FROM moderator_ratings WHERE moderator = ? AND date = ?";
                dateCondition = currentDate.toString();
                break;
            case "week":
                sql = "SELECT COUNT(*) FROM moderator_ratings WHERE moderator = ? AND date >= ?";
                dateCondition = currentDate.minusDays(7).toString();
                break;
            case "month":
                sql = "SELECT COUNT(*) FROM moderator_ratings WHERE moderator = ? AND date >= ?";
                dateCondition = currentDate.minusMonths(1).toString();
                break;
            case "all":
                sql = "SELECT COUNT(*) FROM moderator_ratings WHERE moderator = ?";
                break;
            default:
                return null;
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (!period.equalsIgnoreCase("all")) {
                pstmt.setString(1, moderator);
                pstmt.setString(2, dateCondition);
            } else {
                pstmt.setString(1, moderator);
            }

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCurrentDate() {
        LocalDate currentDate = LocalDate.now();
        return currentDate.toString();
    }

    public static String getExpireDate(String currentDate) {
        LocalDate expireDate = LocalDate.parse(currentDate).plusDays(settings.getInt("ticketExpire"));
        return expireDate.toString();
    }


}
