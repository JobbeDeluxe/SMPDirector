package dev.yourserver.smpdirector.events;

import dev.yourserver.smpdirector.DirectorEvent;
import dev.yourserver.smpdirector.SMPDirectorPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AmbushEvent implements DirectorEvent {

    private final SMPDirectorPlugin plugin;
    private final Random random = new Random();
    private final NamespacedKey tagKey;

    public AmbushEvent(SMPDirectorPlugin plugin) {
        this.plugin = plugin;
        this.tagKey = new NamespacedKey(plugin, plugin.getConfig().getString("global.taggedMobKey", "director"));
    }

    @Override public String id() { return "ambush"; }

    @Override
    public boolean canRunFor(Player p) {
        // Basic: only in overworld-like conditions and not in spectator
        return p.getGameMode() == GameMode.SURVIVAL;
    }

    @Override
    public void runFor(Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events.ambush");
        if (sec == null) return;
        int count = sec.getInt("count", 4);
        int radius = sec.getInt("radius", 8);
        List<String> mobNames = sec.getStringList("mobSet");
        if (mobNames == null || mobNames.isEmpty()) mobNames = List.of("ZOMBIE","SKELETON");

        World w = p.getWorld();
        Location base = p.getLocation();

        int spawned = 0;
        int tries = 0;
        while (spawned < count && tries < count * 5) {
            tries++;
            double angle = random.nextDouble() * Math.PI * 2;
            int dx = (int)Math.round(Math.cos(angle) * (2 + random.nextInt(Math.max(2, radius-1))));
            int dz = (int)Math.round(Math.sin(angle) * (2 + random.nextInt(Math.max(2, radius-1))));
            Location loc = base.clone().add(dx + 0.5, 0, dz + 0.5);
            loc = highestSolidBelow(w, loc, 5);
            if (loc == null) continue;

            String pick = mobNames.get(random.nextInt(mobNames.size()));
            EntityType type;
            try {
                type = EntityType.valueOf(pick.toUpperCase());
            } catch (IllegalArgumentException ex) {
                type = EntityType.ZOMBIE;
            }

            LivingEntity e = (LivingEntity) w.spawnEntity(loc, type);
            e.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte)1);
            e.setRemoveWhenFarAway(true);
            if (e instanceof Mob mob) { mob.setTarget(p); }

            w.playSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f + random.nextFloat()*0.4f);
            w.spawnParticle(Particle.CLOUD, loc, 8, 0.3,0.2,0.3, 0.01);

            spawned++;
        }

        p.sendActionBar(ChatColor.RED + "Ambush!");
    }

    private Location highestSolidBelow(World w, Location loc, int updown) {
        Location test = loc.clone();
        // adjust Y: try a few blocks up/down to land on ground
        for (int dy = updown; dy >= -updown; dy--) {
            Location l = test.clone().add(0, dy, 0);
            Material m = l.getBlock().getType();
            if (!m.isAir() && m.isSolid()) {
                return l.add(0, 1, 0);
            }
        }
        return null;
    }

    @Override
    public int cooldownSeconds() {
        return plugin.getConfig().getInt("events.ambush.cooldownSeconds", 240);
    }
}