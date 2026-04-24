package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CombatManager {

    private final LifeSteal plugin;
    // Combat Tag 중인 플레이어 UUID → 종료 시각(ms)
    private final Map<UUID, Long> combatMap = new HashMap<>();
    // 전투 종료 후 하트 사용 쿨다운 UUID → 사용 가능 시각(ms)
    private final Map<UUID, Long> heartCooldown = new HashMap<>();

    public CombatManager(LifeSteal plugin) {
        this.plugin = plugin;
        startCombatCheckTask();
    }

    public int getCombatDuration() {
        return plugin.getConfig().getInt("combat.tag-duration", 20);
    }

    public int getHeartCooldown() {
        return plugin.getConfig().getInt("combat.heart-use-cooldown", 30);
    }

    // Combat Tag 적용
    public void tagPlayer(Player player) {
        long endTime = System.currentTimeMillis() + (getCombatDuration() * 1000L);
        boolean wasTagged = isTagged(player);
        combatMap.put(player.getUniqueId(), endTime);
        if (!wasTagged) {
            player.sendMessage("§c⚔ 전투가 시작되었습니다! §7(" + getCombatDuration() + "초)");
        }
    }

    // Combat Tag 여부
    public boolean isTagged(Player player) {
        Long endTime = combatMap.get(player.getUniqueId());
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            combatMap.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    // 남은 Combat Tag 시간 (초)
    public int getRemainingSeconds(Player player) {
        Long endTime = combatMap.get(player.getUniqueId());
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    // Combat Tag 해제
    public void untagPlayer(Player player) {
        if (combatMap.remove(player.getUniqueId()) != null) {
            player.sendMessage("§a전투가 종료되었습니다.");
            // 하트 사용 쿨다운 시작
            long cooldownEnd = System.currentTimeMillis() + (getHeartCooldown() * 1000L);
            heartCooldown.put(player.getUniqueId(), cooldownEnd);
        }
    }

    // 하트 사용 가능 여부
    public boolean canUseHeart(Player player) {
        if (isTagged(player)) return false;
        Long cooldownEnd = heartCooldown.get(player.getUniqueId());
        if (cooldownEnd == null) return true;
        return System.currentTimeMillis() >= cooldownEnd;
    }

    // 남은 하트 쿨다운 (초)
    public int getHeartCooldownRemaining(Player player) {
        Long cooldownEnd = heartCooldown.get(player.getUniqueId());
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    // 주기적으로 Combat Tag 만료 확인
    private void startCombatCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                combatMap.entrySet().removeIf(entry -> {
                    if (now >= entry.getValue()) {
                        Player p = plugin.getServer().getPlayer(entry.getKey());
                        if (p != null) {
                            p.sendMessage("§a전투가 종료되었습니다.");
                            long cooldownEnd = now + (getHeartCooldown() * 1000L);
                            heartCooldown.put(entry.getKey(), cooldownEnd);
                        }
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void saveAll() {
        // 서버 종료 시 전투 중인 플레이어는 태그 해제 (메모리 기반)
        combatMap.clear();
    }
}
