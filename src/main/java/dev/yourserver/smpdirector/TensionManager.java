package dev.yourserver.smpdirector;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class TensionManager {
    private final SMPDirectorPlugin plugin;
    private final Map<UUID, Double> map = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public TensionManager(SMPDirectorPlugin plugin) {
        this.plugin = plugin;
    }

    public double get(UUID id){ return map.getOrDefault(id, 0.0); }
    public void set(UUID id, double v){ map.put(id, clamp(v)); }
    public void add(UUID id, double v){ map.put(id, clamp(get(id) + v)); }

    private double clamp(double v){ return Math.max(0, Math.min(100, v)); }

    public void decayAll() {
        double decay = plugin.getConfig().getDouble("tension.decayPerSecond", 0.8);
        for (UUID id : new ArrayList<>(map.keySet())) {
            set(id, get(id) - decay);
        }
    }

    public void tickSensedInputs(Player p) {
        FileConfiguration cfg = plugin.getConfig();

        if (p.getFoodLevel() <= 7) add(p.getUniqueId(), cfg.getDouble("tension.lowFood", 3));

        int light = p.getLocation().getBlock().getLightLevel();
        if (light <= 7) add(p.getUniqueId(), cfg.getDouble("tension.darkness", 2));

        int radius = cfg.getInt("tension.nearMobRadius", 12);
        int count = 0;
        for (org.bukkit.entity.Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                count++;
            }
        }
        double factor = cfg.getDouble("tension.nearMobFactor", 0.5);
        add(p.getUniqueId(), count * factor);
    }

    public void showOrUpdateBossbar(Player p) {
        if (!plugin.getConfig().getBoolean("global.bossbar.enabled", true)) {
            hideBossbar(p.getUniqueId());
            return;
        }
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), id -> {
            BossBar b = Bukkit.createBossBar("Tension", BarColor.BLUE, BarStyle.SOLID);
            b.addPlayer(p);
            return b;
        });
        double val = get(p.getUniqueId());
        bar.setProgress(Math.max(0.0, Math.min(1.0, val / 100.0)));
        double calm = plugin.getConfig().getDouble("thresholds.calm", 20);
        double high = plugin.getConfig().getDouble("thresholds.high", 80);
        if (val < calm) bar.setColor(BarColor.GREEN);
        else if (val >= high) bar.setColor(BarColor.RED);
        else bar.setColor(BarColor.YELLOW);

        String title = plugin.getConfig().getString("global.bossbar.title", "&bTension: &f%val%");
        title = title.replace("%val%", String.format("%.0f", val)).replace("&", "§");
        bar.setTitle(title);
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
    }

    public void hideBossbar(UUID id){
        BossBar b = bars.remove(id);
        if (b != null) b.removeAll();
    }
}