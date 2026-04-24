package com.lifesteal.utils;

import com.lifesteal.LifeSteal;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;

public class RecipeUtil {

    public static void registerRecipes(LifeSteal plugin) {
        if (plugin.getConfig().getBoolean("recipes.heart.enabled", true)) {
            registerHeartRecipe(plugin);
        }
        if (plugin.getConfig().getBoolean("recipes.revive-book.enabled", true)) {
            registerReviveBookRecipe(plugin);
        }
    }

    private static void registerHeartRecipe(LifeSteal plugin) {
        try {
            ItemStack result = ItemUtils.createHeartItem(
                    plugin.getConfig().getInt("recipes.heart.result.amount", 1));
            NamespacedKey key = new NamespacedKey(plugin, "heart");
            ShapedRecipe recipe = new ShapedRecipe(key, result);

            List<String> shape = plugin.getConfig().getStringList("recipes.heart.shape");
            if (shape.size() == 3) {
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));
            } else {
                recipe.shape("AAA", "ABA", "AAA");
            }

            // 재료 등록
            for (String ingKey : plugin.getConfig().getConfigurationSection("recipes.heart.ingredients").getKeys(false)) {
                String matName = plugin.getConfig().getString("recipes.heart.ingredients." + ingKey);
                Material mat = Material.getMaterial(matName);
                if (mat != null) {
                    recipe.setIngredient(ingKey.charAt(0), mat);
                }
            }

            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("하트 레시피가 등록되었습니다.");
        } catch (Exception e) {
            plugin.getLogger().warning("하트 레시피 등록 실패: " + e.getMessage());
        }
    }

    private static void registerReviveBookRecipe(LifeSteal plugin) {
        try {
            ItemStack result = ItemUtils.createReviveBook(
                    plugin.getConfig().getInt("recipes.revive-book.result.amount", 1));
            NamespacedKey key = new NamespacedKey(plugin, "revive_book");
            ShapedRecipe recipe = new ShapedRecipe(key, result);

            List<String> shape = plugin.getConfig().getStringList("recipes.revive-book.shape");
            if (shape.size() == 3) {
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));
            } else {
                recipe.shape("ABA", "BCB", "ABA");
            }

            for (String ingKey : plugin.getConfig().getConfigurationSection("recipes.revive-book.ingredients").getKeys(false)) {
                String matName = plugin.getConfig().getString("recipes.revive-book.ingredients." + ingKey);
                Material mat = Material.getMaterial(matName);
                if (mat != null) {
                    recipe.setIngredient(ingKey.charAt(0), mat);
                }
            }

            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("생환서 레시피가 등록되었습니다.");
        } catch (Exception e) {
            plugin.getLogger().warning("생환서 레시피 등록 실패: " + e.getMessage());
        }
    }
}
