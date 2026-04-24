package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.managers.BanManager;
import com.lifesteal.managers.CombatManager;
import com.lifesteal.managers.HeartManager;
import com.lifesteal.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class PlayerListener implements Listener {

    private final LifeSteal plugin;
    private final HeartManager heartManager;
    private final CombatManager combatManager;
    private final BanManager banManager;
    private final Random random = new Random();

    public PlayerListener(LifeSteal plugin) {
        this.plugin = plugin;
        this.heartManager = plugin.getHeartManager();
        this.combatManager = plugin.getCombatManager();
        this.banManager = plugin.getBanManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        heartManager.initPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        Location deathLoc = victim.getLocation();

        boolean shouldLoseHeart = true;
        if (killer == null && !heartManager.loseOnNaturalDeath()) {
            shouldLoseHeart = false;
        }

        if (shouldLoseHeart) {
            // 킬러가 최대 체력인 경우 → 하트 아이템으로 드롭
            if (killer != null && heartManager.getHearts(killer) >= heartManager.getMaxHearts()) {
                // 하트 감소는 하되 킬러에게 하트 추가 대신 바닥에 드롭
                boolean survived = heartManager.removeHeart(victim);
                dropHeartItem(deathLoc);
                killer.sendMessage("§c이미 최대 체력이라 하트가 바닥에 드롭되었습니다!");
                combatManager.untagPlayer(killer);
                combatManager.untagPlayer(victim);
                if (!survived) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> banManager.banPlayer(victim), 20L);
                }
                // 아이템 퍼짐 반경 적용
                applyDeathDropRadius(event, deathLoc);
                return;
            }

            boolean survived = heartManager.removeHeart(victim);
            if (!survived) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> banManager.banPlayer(victim), 20L);
            }
        }

        // 킬러가 플레이어면 하트 +1
        if (killer != null) {
            heartManager.addHeart(killer);
            combatManager.untagPlayer(killer);
            combatManager.untagPlayer(victim);
        }

        // 아이템 퍼짐 반경 적용
        applyDeathDropRadius(event, deathLoc);
    }

    // 사망 아이템 퍼짐 반경 적용
    private void applyDeathDropRadius(PlayerDeathEvent event, Location center) {
        double radius = plugin.getConfig().getDouble("death.drop-radius", 0);
        if (radius <= 0) return; // 0이면 기본 동작 (제자리 드롭)

        // 기본 드롭 취소 후 직접 퍼뜨리기
        for (ItemStack drop : event.getDrops()) {
            if (drop == null || drop.getType() == Material.AIR) continue;
            double offsetX = (random.nextDouble() * 2 - 1) * radius;
            double offsetZ = (random.nextDouble() * 2 - 1) * radius;
            Location dropLoc = center.clone().add(offsetX, 0, offsetZ);
            center.getWorld().dropItemNaturally(dropLoc, drop);
        }
        event.getDrops().clear();
    }

    // 하트 아이템 바닥에 드롭
    private void dropHeartItem(Location location) {
        ItemStack heart = ItemUtils.createHeartItem(1);
        location.getWorld().dropItemNaturally(location, heart);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                heartManager.applyHearts(event.getPlayer(), heartManager.getHearts(event.getPlayer())), 5L);
    }

    // Combat Tag 중 로그아웃 → 사망 처리
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (combatManager.isTagged(player) && !player.hasPermission("lifesteal.bypass.combat")) {
            plugin.getServer().broadcastMessage("§c[LifeSteal] §f" + player.getName() + " §c님이 전투 중 로그아웃하여 사망 처리되었습니다!");
            player.setHealth(0);
            boolean survived = heartManager.removeHeart(player);
            heartManager.saveAll();
            if (!survived) banManager.banPlayer(player);
        }
        combatManager.untagPlayer(player);
    }

    // ────────────────────────────────────────────
    // 엔더펄 순간이동 기능 제거
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleportByPearl(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c엔더펄 순간이동은 이 서버에서 사용할 수 없습니다!");
        }
    }

    // ────────────────────────────────────────────
    // 급류 삼지창 차단 - 삼지창에 급류 인챈트 있으면 사용 차단
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRiptide(PlayerItemHeldEvent event) {
        // 아이템 스위치 시 급류 삼지창 체크는 interact에서 처리
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRiptideUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.TRIDENT) return;
        if (!item.hasItemMeta()) return;
        // 급류 인챈트 확인
        if (item.getItemMeta().getEnchants().containsKey(org.bukkit.enchantments.Enchantment.RIPTIDE)) {
            event.setCancelled(true);
            player.sendMessage("§c급류 삼지창은 이 서버에서 사용할 수 없습니다!");
        }
    }
}
