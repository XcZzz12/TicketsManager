package ru.xczdev.ticketsManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.xczdev.ticketsManager.utils.DatabaseUtil;
import ru.xczdev.ticketsManager.utils.PersistentData;

import java.util.*;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class MenuManager implements Listener {
    private static ConfigurationSection lang;
    private static ConfigurationSection settings;
    public static HashMap<Player, Inventory> menus;
    public static HashMap<Player, Integer> currentPages;

    public MenuManager() {
        menus = new HashMap<>();
        currentPages = new HashMap<>();
        lang = getInstance().getLang();
        settings = getInstance().getSettings();
        Bukkit.getPluginManager().registerEvents(this, getInstance());
        Bukkit.getLogger().info("MenuManager listener registered.");
    }

    public static void openMenu(Player player) {
        if (!menus.containsKey(player)) {
            int page = currentPages.getOrDefault(player, 1);
            List<List> tickets = DatabaseUtil.getAllTickets();
            Collections.reverse(tickets); // Новые тикеты отображаются первыми

            int ticketsPerPage = 36;
            int totalPages = (int) Math.ceil((double) tickets.size() / ticketsPerPage);
            if (page > totalPages) page = totalPages;
            if (page < 1) page = 1;

            currentPages.put(player, page);

            Inventory inventory = Bukkit.createInventory(null, 54, lang.getConfigurationSection("menu").getString("menuTitle")
                    .replace("&", "§")
                    .replace("%rating%", DatabaseUtil.getAverageModeratorRatingOverall(player.getName()).toString())
                    .replace("%page%", String.valueOf(page))
                    .replace("%totalPages%", String.valueOf(totalPages)));

            // Создание стекла
            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = glassPane.getItemMeta();
            meta.setDisplayName("");
            glassPane.setItemMeta(meta);

            // Заполнение интерфейса стеклом
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, glassPane);
                inventory.setItem(i + 45, glassPane);
            }

            // Создание кнопок для переключения страниц
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta2 = prevPage.getItemMeta();
            meta2.setDisplayName(ChatColor.GREEN + "Предыдущая страница");
            prevPage.setItemMeta(meta2);

            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta3 = nextPage.getItemMeta();
            meta3.setDisplayName(ChatColor.GREEN + "Следующая страница");
            nextPage.setItemMeta(meta3);

            // Установка кнопок
            inventory.setItem(3 + 45, prevPage);
            inventory.setItem(5 + 45, nextPage);

            // Отображение информации о странице
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta meta4 = pageInfo.getItemMeta();
            meta4.setDisplayName(ChatColor.YELLOW + "Страница " + page + " из " + totalPages);
            pageInfo.setItemMeta(meta4);
            inventory.setItem(4 + 45, pageInfo);

            // Отображение тикетов
            int start = (page - 1) * ticketsPerPage;
            int end = Math.min(start + ticketsPerPage, tickets.size());
            for (int i = start; i < end; i++) {
                String id = tickets.get(i).get(0).toString();
                String text = tickets.get(i).get(1).toString();
                String owner = tickets.get(i).get(2).toString();
                String moderator = tickets.get(i).get(3).toString();
                String expiryDate = tickets.get(i).get(4).toString();
                String publishDate = tickets.get(i).get(5).toString();

                ItemStack ticket = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta ticketMeta = (SkullMeta) ticket.getItemMeta();

                ticketMeta.setOwner(owner);
                ticketMeta.setDisplayName(lang.getConfigurationSection("menu")
                        .getConfigurationSection("ticketExample")
                        .getString("title")
                        .replace("&", "§")
                        .replace("%player%", owner)
                        .replace("%id%", id));

                List<String> lore = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                        .getStringList("lore");
                List<String> loreFormatted = new ArrayList<>();
                String accept;
                if (Objects.equals(moderator, "")) {
                    accept = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                            .getString("acceptYes").replace("&", "§");
                } else {
                    accept = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                            .getString("acceptNo").replace("&", "§")
                            .replace("%moderator%", moderator);
                }

                for (String loreString : lore) {
                    loreFormatted.add(loreString.replace("&", "§")
                            .replace("%player%", owner)
                            .replace("%id%", id)
                            .replace("%question%", text)
                            .replace("%expiryDate%", expiryDate)
                            .replace("%publishDate%", publishDate)
                            .replace("%acceptable%", accept));
                }
                ticketMeta.setLore(loreFormatted);
                ticket.setItemMeta(ticketMeta);
                PersistentData.addDataToItem(ticket, "id", id);
                PersistentData.addDataToItem(ticket, "ticketOwner", owner);
                if (Objects.equals(moderator, "")) {
                    PersistentData.addDataToItem(ticket, "acceptable", "true");
                } else {
                    PersistentData.addDataToItem(ticket, "acceptable", "false");
                }

                inventory.setItem(i - start + 9, ticket);
            }

            menus.put(player, inventory);
            player.openInventory(inventory);
        } else {
            // Если меню уже открыто, обновить его
            int page = currentPages.get(player);
            openMenu(player, page);
        }
    }

    private static void openMenu(Player player, int page) {
        if (menus.containsKey(player)) {
            List<List> tickets = DatabaseUtil.getAllTickets();
            Collections.reverse(tickets); // Новые тикеты отображаются первыми

            int ticketsPerPage = 36;
            int totalPages = (int) Math.ceil((double) tickets.size() / ticketsPerPage);
            if (page > totalPages) page = totalPages;
            if (page < 1) page = 1;

            currentPages.put(player, page);

            Inventory inventory = menus.get(player);
            inventory.clear();

            // Создание стекла
            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = glassPane.getItemMeta();
            meta.setDisplayName("");
            glassPane.setItemMeta(meta);

            // Заполнение интерфейса стеклом
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, glassPane);
                inventory.setItem(i + 45, glassPane);
            }

            // Создание кнопок для переключения страниц
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta2 = prevPage.getItemMeta();
            meta2.setDisplayName(ChatColor.GREEN + "Предыдущая страница");
            prevPage.setItemMeta(meta2);

            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta3 = nextPage.getItemMeta();
            meta3.setDisplayName(ChatColor.GREEN + "Следующая страница");
            nextPage.setItemMeta(meta3);

            // Установка кнопок
            inventory.setItem(3 + 45, prevPage);
            inventory.setItem(5 + 45, nextPage);

            // Отображение информации о странице
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta meta4 = pageInfo.getItemMeta();
            meta4.setDisplayName(ChatColor.YELLOW + "Страница " + page + " из " + totalPages);
            pageInfo.setItemMeta(meta4);
            inventory.setItem(4 + 45, pageInfo);

            // Отображение тикетов
            int start = (page - 1) * ticketsPerPage;
            int end = Math.min(start + ticketsPerPage, tickets.size());
            for (int i = start; i < end; i++) {
                String id = tickets.get(i).get(0).toString();
                String text = tickets.get(i).get(1).toString();
                String owner = tickets.get(i).get(2).toString();
                String moderator = tickets.get(i).get(3).toString();
                String expiryDate = tickets.get(i).get(4).toString();
                String publishDate = tickets.get(i).get(5).toString();

                ItemStack ticket = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta ticketMeta = (SkullMeta) ticket.getItemMeta();

                ticketMeta.setOwner(owner);
                ticketMeta.setDisplayName(lang.getConfigurationSection("menu")
                        .getConfigurationSection("ticketExample")
                        .getString("title")
                        .replace("&", "§")
                        .replace("%player%", owner)
                        .replace("%id%", id));

                List<String> lore = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                        .getStringList("lore");
                List<String> loreFormatted = new ArrayList<>();
                String accept;
                if (Objects.equals(moderator, "")) {
                    accept = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                            .getString("acceptYes").replace("&", "§");
                } else {
                    accept = lang.getConfigurationSection("menu").getConfigurationSection("ticketExample")
                            .getString("acceptNo").replace("&", "§")
                            .replace("%moderator%", moderator);
                }

                for (String loreString : lore) {
                    loreFormatted.add(loreString.replace("&", "§")
                            .replace("%player%", owner)
                            .replace("%id%", id)
                            .replace("%question%", text)
                            .replace("%expiryDate%", expiryDate)
                            .replace("%publishDate%", publishDate)
                            .replace("%acceptable%", accept));
                }
                ticketMeta.setLore(loreFormatted);
                ticket.setItemMeta(ticketMeta);
                PersistentData.addDataToItem(ticket, "id", id);
                PersistentData.addDataToItem(ticket, "ticketOwner", owner);
                if (Objects.equals(moderator, "")) {
                    PersistentData.addDataToItem(ticket, "acceptable", "true");
                } else {
                    PersistentData.addDataToItem(ticket, "acceptable", "false");
                }

                inventory.setItem(i - start + 9, ticket);
            }

            menus.put(player, inventory);
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (menus.containsKey(p) && e.getInventory().equals(menus.get(p))) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.equals(ChatColor.GREEN + "Предыдущая страница")) {
                    previousPage(p);
                } else if (displayName.equals(ChatColor.GREEN + "Следующая страница")) {
                    nextPage(p);
                } else {
                    String acceptable = PersistentData.getDataFromItem(item, "acceptable");
                    String id = PersistentData.getDataFromItem(item, "id");
                    String ticketOwner = PersistentData.getDataFromItem(item, "ticketOwner");
                    if (acceptable.equalsIgnoreCase("true")) {
                        if (DatabaseUtil.getTicketByModerator(p.getName()) != null) {
                            List ticket = DatabaseUtil.getTicketByModerator(p.getName());
                            String ticketId = ticket.get(0).toString();
                            p.sendMessage(lang.getString("ModeratorHasAcceptTicket").replace("&", "§")
                                    .replace("%id%", ticketId));
                            p.closeInventory();
                            return;
                        }
                        if (DatabaseUtil.acceptTicket(p.getName(), Integer.parseInt(id))) {
                            p.closeInventory();
                            p.sendMessage(String.join("\n", lang.getStringList("moderatorHasAcceptTicketForModerator")).replace("&", "§")
                                    .replace("%id%", id)
                                    .replace("%player%", ticketOwner));
                            Bukkit.getPlayer(ticketOwner).sendMessage(lang.getString("moderatorHasAcceptTicket").replace("&", "§")
                                    .replace("%id%", id)
                                    .replace("%moderator%", p.getName()));
                        } else {
                            p.sendMessage(lang.getString("ticketAcceptError").replace("&", "§"));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        closeMenu((Player) e.getPlayer());
    }

    private static void nextPage(Player player) {
        int currentPage = currentPages.getOrDefault(player, 1);
        int page = currentPage + 1;
        currentPages.put(player, page);
        openMenu(player, page);
    }

    private static void previousPage(Player player) {
        int currentPage = currentPages.getOrDefault(player, 1);
        int page = currentPage - 1;
        currentPages.put(player, page);
        openMenu(player, page);
    }

    private static void closeMenu(Player player) {
        menus.remove(player);
        currentPages.remove(player);
    }
}