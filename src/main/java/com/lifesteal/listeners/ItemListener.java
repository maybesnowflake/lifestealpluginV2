package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.managers.CombatManager;
import com.lifesteal.managers.HeartManager;
import com.lifesteal.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemListener implements Listener {

    private final LifeSteal plugin;
    private final HeartManager heartManager;
    private final CombatManager combatManager;

    // XRay 감지: UUID → 최근 광석 채굴 타임스탬프 목록
    private final Map<UUID, List<Long>> oreMineLog = new HashMap<>();

    // 감지 대상 광석
    private static final Set<Material> ORES = EnumSet.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.ANCIENT_DEBRIS,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE
    );

    public ItemListener(LifeSteal plugin) {
        this.plugin = plugin;
        this.heartManager = plugin.getHeartManager();
        this.combatManager = plugin.getCombatManager();
    }

    // 하트 아이템 우클릭 사용
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // 하트 아이템 확인
        if (ItemUtils.isHeartItem(item)) {
            event.setCancelled(true);

            if (!combatManager.canUseHeart(player)) {
                if (combatManager.isTagged(player)) {
                    player.sendMessage("§c전투 중에는 하트를 사용할 수 없습니다! §7(" + combatManager.getRemainingSeconds(player) + "초 남음)");
                } else {
                    player.sendMessage("§c아직 하트를 사용할 수 없습니다! §7(" + combatManager.getHeartCooldownRemaining(player) + "초 후 가능)");
                }
                return;
            }

            heartManager.addHeart(player);
            // 아이템 1개 소비
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        // 생환서 아이템 확인 → GUI 열기
        if (ItemUtils.isReviveBook(item)) {
            event.setCancelled(true);
            plugin.getReviveGUI().openReviveGUI(player);
        }
    }

    // XRay 감지
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("xray.enabled", true)) return;
        Player player = event.getPlayer();
        if (player.hasPermission("lifesteal.bypass.xray")) return;
        if (!ORES.contains(event.getBlock().getType())) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        int checkInterval = plugin.getConfig().getInt("xray.check-interval", 200) * 50L > 0
                ? plugin.getConfig().getInt("xray.check-interval", 200) * 50
                : 10000;

        oreMineLog.computeIfAbsent(uuid, k -> new ArrayList<>());
        List<Long> log = oreMineLog.get(uuid);
        // 오래된 기록 제거
        log.removeIf(t -> now - t > checkInterval);
        log.add(now);

        int threshold = plugin.getConfig().getInt("xray.ore-threshold", 10);
        if (log.size() >= threshold) {
            log.clear();
            String msg = "§c[XRay 경고] §f" + player.getName() + " §c님이 단시간에 광석을 " + threshold + "개 이상 채굴했습니다! (위치: " +
                    event.getBlock().getLocation().getBlockX() + ", " +
                    event.getBlock().getLocation().getBlockY() + ", " +
                    event.getBlock().getLocation().getBlockZ() + ")";

            if (plugin.getConfig().getBoolean("xray.alert-admins", true)) {
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("lifesteal.admin"))
                        .forEach(p -> p.sendMessage(msg));
            }
            if (plugin.getConfig().getBoolean("xray.log-to-file", true)) {
                plugin.getLogger().warning("[XRAY] " + player.getName() + " at " +
                        event.getBlock().getLocation().toString());
            }
        }
    }
}
