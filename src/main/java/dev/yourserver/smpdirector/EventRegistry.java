package dev.yourserver.smpdirector;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class EventRegistry {

    private final SMPDirectorPlugin plugin;
    private final List<DirectorEvent> events = new ArrayList<>();
    public List<DirectorEvent> getEvents(){ return events; }
    private final Map<String, Map<UUID, Long>> lastRun = new HashMap<>();
    private final Random random = new Random();

    public EventRegistry(SMPDirectorPlugin plugin) {
        this.plugin = plugin;
        // Register built-in events
        events.add(new events.AmbushEvent(plugin));
        events.add(new events.ReliefDropEvent(plugin));
        events.add(new events.WeatherCellEvent(plugin));
    }

    public DirectorEvent pickFor(Player p) {
        double val = plugin.getTensionManager().get(p.getUniqueId()); // helper method needed
        // We cannot access getTensionManager() - add a helper or compute category from config via tension only.
        // Workaround: Use TensionManager via reflection is ugly; better: pass val in. For now, we'll fetch via public accessor added.
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
    }

    private int getWeight(String id, String band) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events." + id + ".weight");
        if (sec == null) return 0;
        return sec.getInt(band, 0);
    }

    private String bandOf(double val) {
        double calm = plugin.getConfig().getDouble("thresholds.calm", 20);
        double high = plugin.getConfig().getDouble("thresholds.high", 80);
        double normal = plugin.getConfig().getDouble("thresholds.normal", 50); // fallback if defined
        if (val < calm) return "calm";
        if (val >= high) return "high";
        return "normal";
    }
}