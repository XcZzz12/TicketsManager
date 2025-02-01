package ru.xczdev.ticketsManager.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static ru.xczdev.ticketsManager.TicketsManager.getInstance;

public class PersistentData {

    public static void addDataToItem(ItemStack item, String key, String value) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        dataContainer.set(new NamespacedKey(getInstance(), key), PersistentDataType.STRING, value);
        Bukkit.getLogger().info("Added data to item: key=" + key + ", value=" + value);
        item.setItemMeta(meta);
    }

    public static String getDataFromItem(ItemStack item, String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Bukkit.getLogger().info("ItemMeta is null for key: " + key);
            return null;
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        String value = dataContainer.get(new NamespacedKey(getInstance(), key), PersistentDataType.STRING);
        Bukkit.getLogger().info("Retrieved data from item: key=" + key + ", value=" + value);
        return value;
    }
}
