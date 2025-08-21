package dev.yourserver.smpdirector;

import dev.yourserver.smpdirector.events.AmbushEvent;
import dev.yourserver.smpdirector.events.ReliefDropEvent;
import dev.yourserver.smpdirector.events.WeatherCellEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class EventRegistry {

    private final SMPDirectorPlugin plugin;
    private final List<DirectorEvent> events = new ArrayList<>();
    public List<DirectorEvent> getEvents(){ return events; }
    private final Map<String, Map<UUID, Long>> lastRun = new HashMap<>();
    private final Random random = new Random();

    // Timing & chances
    private int evalEverySeconds = 5;
    private int minSecondsBetween = 90;
    private double chanceCalm = 0.12, chanceNormal = 0.28, chanceHigh = 0.55;
    private final Map<UUID, Long> lastAny = new HashMap<>();

    public EventRegistry(SMPDirectorPlugin plugin) {
        this.plugin = plugin;
        events.add(new AmbushEvent(plugin));
        events.add(new ReliefDropEvent(plugin));
        events.add(new WeatherCellEvent(plugin));
        reloadSettings();
    }

    public void reloadSettings() {
        FileConfiguration cfg = plugin.getConfig();
        this.evalEverySeconds = Math.max(1, cfg.getInt("global.evaluateEverySeconds", 5));
        this.minSecondsBetween = Math.max(0, cfg.getInt("global.minSecondsBetweenEvents", 90));
        this.chanceCalm = clamp01(cfg.getDouble("global.baseTriggerChance.calm", 0.12));
        this.chanceNormal = clamp01(cfg.getDouble("global.baseTriggerChance.normal", 0.28));
        this.chanceHigh = clamp01(cfg.getDouble("global.baseTriggerChance.high", 0.55));
    }

    private double clamp01(double v){ return Math.max(0.0, Math.min(1.0, v)); }

    public boolean shouldEvaluateTick(int tickSeconds){
        return (tickSeconds % Math.max(1, evalEverySeconds)) == 0;
    }

    public boolean canRunAny(Player p){
        long last = lastAny.getOrDefault(p.getUniqueId(), 0L);
        return (System.currentTimeMillis() - last) >= (minSecondsBetween * 1000L);
    }
    public void markAny(Player p){
        lastAny.put(p.getUniqueId(), System.currentTimeMillis());
    }

    public double chanceFor(double tensionVal){
        String band = bandOf(tensionVal);
        switch (band) {
            case "calm": return chanceCalm;
            case "high": return chanceHigh;
            default: return chanceNormal;
        }
    }

    public DirectorEvent pickFor(Player p) {
        double val = plugin.getTensionManager().get(p.getUniqueId());
        return weightedPick(p, val);
    }

    private DirectorEvent weightedPick(Player p, double tensionVal) {
        String band = bandOf(tensionVal);
        Map<DirectorEvent, Integer> weights = new HashMap<>();
        int total = 0;

        for (DirectorEvent ev : events) {
            int w = getWeight(ev.id(), band);
            if (w > 0 && ev.canRunFor(p) && !onCooldown(ev, p)) {
                weights.put(ev, w);
                total += w;
            }
        }
        if (total <= 0) return null;
        int r = random.nextInt(total);
        int acc = 0;
        for (Map.Entry<DirectorEvent, Integer> e : weights.entrySet()) {
            acc += e.getValue();
            if (r < acc) return e.getKey();
        }
        return null;
    }

    private boolean onCooldown(DirectorEvent ev, Player p) {
        Map<UUID, Long> m = lastRun.computeIfAbsent(ev.id(), k -> new HashMap<>());
        long now = System.currentTimeMillis();
        long last = m.getOrDefault(p.getUniqueId(), 0L);
        return (now - last) < (ev.cooldownSeconds() * 1000L);
    }

    public void markRun(DirectorEvent ev, Player p) {
        lastRun.computeIfAbsent(ev.id(), k -> new HashMap<>()).put(p.getUniqueId(), System.currentTimeMillis());
        markAny(p);
    }

    private int getWeight(String id, String band) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events." + id + ".weight");
        if (sec == null) return 0;
        return sec.getInt(band, 0);
    }

    private String bandOf(double val) {
        double calm = plugin.getConfig().getDouble("thresholds.calm", 20);
        double high = plugin.getConfig().getDouble("thresholds.high", 80);
        if (val < calm) return "calm";
        if (val >= high) return "high";
        return "normal";
    }
}