package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.managers.CombatManager;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class CombatListener implements Listener {

    private final LifeSteal plugin;
    private final CombatManager combatManager;

    // 네더라이트 장비 목록
    private static final Set<Material> NETHERITE_EQUIPMENT = EnumSet.of(
            Material.NETHERITE_SWORD,
            Material.NETHERITE_PICKAXE,
            Material.NETHERITE_AXE,
            Material.NETHERITE_SHOVEL,
            Material.NETHERITE_HOE,
            Material.NETHERITE_HELMET,
            Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS,
            Material.NETHERITE_BOOTS
    );

    public CombatListener(LifeSteal plugin) {
        this.plugin = plugin;
        this.combatManager = plugin.getCombatManager();
    }

    // PvP 감지 → Combat Tag
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player victim = null;
        Player attacker = null;

        if (event.getEntity() instanceof Player) {
            victim = (Player) event.getEntity();
        }
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        // 엔드 크리스탈 PvP 금지
        if (event.getDamager() instanceof EnderCrystal && victim != null) {
            event.setCancelled(true);
            victim.sendMessage("§c엔드 크리스탈로 PvP할 수 없습니다!");
            return;
        }

        if (victim != null && attacker != null && !victim.equals(attacker)) {
            combatManager.tagPlayer(victim);
            combatManager.tagPlayer(attacker);
        }
    }

    // 겉날개: 비행 시작 자체를 차단 (이미 착용 중이어도 막힘)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!combatManager.isTagged(player)) return;
        if (event.isGliding()) {
            event.setCancelled(true);
            player.sendMessage("§c전투 중에는 겉날개를 사용할 수 없습니다! §7(" + combatManager.getRemainingSeconds(player) + "초 남음)");
        }
    }

    // ────────────────────────────────────────────
    // 엔더상자: 인벤토리 열기 이벤트로 차단 (손에 없어도 막힘)
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (!combatManager.isTagged(player)) return;
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            player.sendMessage("§c전투 중에는 엔더상자를 열 수 없습니다!");
        }
    }

    // ────────────────────────────────────────────
    // 토템: EntityResurrectEvent로 부활 자체를 완전 차단 (전투 여부 무관)
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        event.setCancelled(true);
        event.getEntity().sendMessage("§c토템은 이 서버에서 사용할 수 없습니다!");
    }

    // ────────────────────────────────────────────
    // 네더라이트 장비: config에서 비활성화 시 착용/사용 차단 및 드롭
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isNetheriteDisabled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (NETHERITE_EQUIPMENT.contains(item.getType())) {
            event.setCancelled(true);
            player.sendMessage("§c네더라이트 장비는 이 서버에서 사용할 수 없습니다!");
        }
    }

    // 네더라이트 장비 착용 중인 플레이어 주기적 검사 (이미 착용된 것도 제거)
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!isNetheriteDisabled()) return;
        Player player = event.getPlayer();
        // 매 이동마다 검사하면 무거우므로 인벤토리 변경 이벤트에서 처리하는 게 더 좋지만
        // 안전하게 여기서도 체크 (실제 서버에서는 InventoryClickEvent 추가 권장)
        removeNetheriteArmor(player);
    }

    // 네더라이트 장비 제거 유틸
    private void removeNetheriteArmor(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        // 장갑 슬롯 검사
        ItemStack[] armor = inv.getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && NETHERITE_EQUIPMENT.contains(armor[i].getType())) {
                player.getWorld().dropItemNaturally(player.getLocation(), armor[i]);
                armor[i] = null;
                changed = true;
                player.sendMessage("§c네더라이트 장비는 착용할 수 없습니다!");
            }
        }
        if (changed) inv.setArmorContents(armor);

        // 손에 든 것도 검사
        ItemStack hand = inv.getItemInMainHand();
        if (NETHERITE_EQUIPMENT.contains(hand.getType())) {
            player.getWorld().dropItemNaturally(player.getLocation(), hand);
            inv.setItemInMainHand(null);
            player.sendMessage("§c네더라이트 장비는 사용할 수 없습니다!");
        }
    }

    // ────────────────────────────────────────────
    // 드래곤알을 엔더상자에 보관 금지
    // ────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST) return;

        // 클릭한 아이템 또는 커서에 든 아이템이 드래곤알인지 확인
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        boolean isDragonEggCursor = cursor != null && cursor.getType() == Material.DRAGON_EGG;
        boolean isDragonEggClicked = clicked != null && clicked.getType() == Material.DRAGON_EGG;

        if (isDragonEggCursor || isDragonEggClicked) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§c드래곤알은 엔더상자에 보관할 수 없습니다!");
        }
    }

    private boolean isNetheriteDisabled() {
        return !plugin.getConfig().getBoolean("netherite.enabled", true);
    }
}
