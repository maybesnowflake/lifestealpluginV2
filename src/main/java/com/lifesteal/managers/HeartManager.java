package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartManager {

    private final LifeSteal plugin;
    private final Map<UUID, Integer> heartMap = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public HeartManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadData();
    }

    // 플레이어 하트 수 조회 (없으면 기본값)
    public int getHearts(Player player) {
        return heartMap.getOrDefault(player.getUniqueId(), getStartHearts());
    }

    // 하트 설정
    public void setHearts(Player player, int hearts) {
        int max = getMaxHearts();
        int min = plugin.getConfig().getInt("hearts.min", 1);
        hearts = Math.max(min, Math.min(max, hearts));
        heartMap.put(player.getUniqueId(), hearts);
        applyHearts(player, hearts);
    }

    // 하트 추가
    public void addHeart(Player player) {
        int current = getHearts(player);
        if (current < getMaxHearts()) {
            setHearts(player, current + 1);
            player.sendMessage("§c❤ §a최대 체력이 §c+1§a 증가했습니다! §7(" + getHearts(player) + "/" + getMaxHearts() + ")");
        } else {
            player.sendMessage("§c이미 최대 체력입니다! §7(" + getMaxHearts() + "/" + getMaxHearts() + ")");
        }
    }

    // 하트 제거
    public boolean removeHeart(Player player) {
        int current = getHearts(player);
        if (current <= 1) {
            setHearts(player, 0);
            return false; // 체력 0 → 밴 처리 필요
        }
        setHearts(player, current - 1);
        player.sendMessage("§c❤ 최대 체력이 §c-1§c 감소했습니다. §7(" + getHearts(player) + "/" + getMaxHearts() + ")");
        return true;
    }

    // 실제 MaxHealth 적용
    public void applyHearts(Player player, int hearts) {
        double maxHp = hearts * 2.0;
        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHp);
        if (player.getHealth() > maxHp) player.setHealth(maxHp);
    }

    // 처음 접속 시 하트 초기화
    public void initPlayer(Player player) {
        if (!heartMap.containsKey(player.getUniqueId())) {
            heartMap.put(player.getUniqueId(), getStartHearts());
        }
        applyHearts(player, getHearts(player));
    }

    public int getStartHearts() { return plugin.getConfig().getInt("hearts.start", 10); }
    public int getMaxHearts() { return plugin.getConfig().getInt("hearts.max", 20); }
    public boolean loseOnNaturalDeath() { return plugin.getConfig().getBoolean("hearts.lose-on-natural-death", true); }

    // 데이터 저장/불러오기
    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "hearts.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : dataConfig.getKeys(false)) {
            try {
                heartMap.put(UUID.fromString(key), dataConfig.getInt(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Integer> entry : heartMap.entrySet()) {
            dataConfig.set(entry.getKey().toString(), entry.getValue());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
