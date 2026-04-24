package com.lifesteal.utils;

import com.lifesteal.LifeSteal;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemUtils {

    private static final int HEART_MODEL_DATA = 1001;
    private static final int REVIVE_MODEL_DATA = 1002;

    // 하트 아이템 생성
    public static ItemStack createHeartItem(int amount) {
        LifeSteal plugin = LifeSteal.getInstance();
        String matName = plugin.getConfig().getString("recipes.heart.result.material", "NETHER_STAR");
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        String name = plugin.getConfig().getString("recipes.heart.result.name", "§c§l하트");
        List<String> lore = plugin.getConfig().getStringList("recipes.heart.result.lore");
        int customModel = plugin.getConfig().getInt("recipes.heart.result.custom-model-data", HEART_MODEL_DATA);

        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.setCustomModelData(customModel);
        item.setItemMeta(meta);
        return item;
    }

    // 생환서 아이템 생성
    public static ItemStack createReviveBook(int amount) {
        LifeSteal plugin = LifeSteal.getInstance();
        String matName = plugin.getConfig().getString("recipes.revive-book.result.material", "BOOK");
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.BOOK;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        String name = plugin.getConfig().getString("recipes.revive-book.result.name", "§d§l생환서");
        List<String> lore = plugin.getConfig().getStringList("recipes.revive-book.result.lore");
        int customModel = plugin.getConfig().getInt("recipes.revive-book.result.custom-model-data", REVIVE_MODEL_DATA);

        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.setCustomModelData(customModel);
        item.setItemMeta(meta);
        return item;
    }

    // 하트 아이템 여부 확인
    public static boolean isHeartItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData()) return false;
        return meta.getCustomModelData() == HEART_MODEL_DATA;
    }

    // 생환서 여부 확인
    public static boolean isReviveBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData()) return false;
        return meta.getCustomModelData() == REVIVE_MODEL_DATA;
    }
}
