package dev.yourserver.smpdirector.events;

import dev.yourserver.smpdirector.DirectorEvent;
import dev.yourserver.smpdirector.SMPDirectorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Random;

public class WeatherCellEvent implements DirectorEvent {

    private final SMPDirectorPlugin plugin;
    private final Random random = new Random();

    public WeatherCellEvent(SMPDirectorPlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "weather_cell"; }

    @Override
    public boolean canRunFor(Player p) { return p.getGameMode() == GameMode.SURVIVAL; }

    @Override
    public void runFor(Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events.weather_cell");
        if (sec == null) return;
        int duration = sec.getInt("durationSeconds", 45);

        p.setPlayerWeather(WeatherType.DOWNFALL);
        p.sendActionBar(ChatColor.BLUE + "A storm gathers above you...");
        World w = p.getWorld();

        for (int i = 0; i < 4; i++) {
            int delay = 20 + random.nextInt(20 * Math.max(1, duration - 5));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location l = p.getLocation().clone().add(-8 + random.nextInt(17), 0, -8 + random.nextInt(17));
                l = highestSolidBelow(w, l, 6);
                if (l != null) {
                    w.strikeLightningEffect(l);
                    w.playSound(l, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
                }
            }, delay);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            p.resetPlayerWeather();
            p.sendActionBar(ChatColor.GRAY + "The storm passes.");
        }, duration * 20L);
    }

    private Location highestSolidBelow(World w, Location loc, int updown) {
        for (int dy = updown; dy >= -updown; dy--) {
            Location l = loc.clone().add(0, dy, 0);
            Material m = l.getBlock().getType();
            if (!m.isAir() && m.isSolid()) return l.add(0,1,0);
        }
        return null;
    }

    @Override
    public int cooldownSeconds() { return plugin.getConfig().getInt("events.weather_cell.cooldownSeconds", 360); }
}