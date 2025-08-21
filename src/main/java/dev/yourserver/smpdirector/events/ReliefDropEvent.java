package dev.yourserver.smpdirector.events;

import dev.yourserver.smpdirector.DirectorEvent;
import dev.yourserver.smpdirector.SMPDirectorPlugin;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReliefDropEvent implements DirectorEvent {

    private final SMPDirectorPlugin plugin;
    private final Random random = new Random();

    public ReliefDropEvent(SMPDirectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "relief_drop"; }

    @Override
    public boolean canRunFor(Player p) {
        return p.getGameMode() == GameMode.SURVIVAL;
    }

    @Override
    public void runFor(Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events.relief_drop");
        if (sec == null) return;

        int radius = sec.getInt("radius", 6);
        int removeAfter = sec.getInt("removeAfterSeconds", 180);
        List<String> pool = sec.getStringList("lootPool");
        if (pool == null || pool.isEmpty()) pool = List.of("BREAD:6-12","TORCH:16-32");

        Location drop = findDrop(p.getLocation(), radius);
        if (drop == null) drop = p.getLocation();

        Block b = drop.getBlock();
        b.setType(Material.BARREL);
        Barrel barrel = (Barrel) b.getState();
        Inventory inv = barrel.getInventory();
        // Fill with randomized loot (supports optional chance via "@p" or "@p%")
// Examples: "DIAMOND:1-3@0.15", "GOLD_INGOT:2-8@25%", "BREAD:6-12"
for (String spec : pool) {
    try {
        String itemPart = spec;
        double chance = 1.0;
        if (spec.contains("@")) {
            String[] pc = spec.split("@");
            itemPart = pc[0].trim();
            String probStr = pc[1].trim().replace("%","");
            double prob = Double.parseDouble(probStr);
            if (pc[1].contains("%")) prob = prob / 100.0;
            chance = Math.max(0.0, Math.min(1.0, prob));
        }
        if (random.nextDouble() > chance) continue;

        String[] parts = itemPart.split(":");
        Material mat = Material.valueOf(parts[0].toUpperCase());
        int min = 1, max = 1;
        if (parts.length >= 2) {
            String[] range = parts[1].split("-");
            if (range.length == 2) {
                min = Integer.parseInt(range[0]);
                max = Integer.parseInt(range[1]);
            } else {
                max = Integer.parseInt(parts[1]);
            }
        }
        if (max < min) { int t = min; min = max; max = t; }
        int amount = min + random.nextInt(Math.max(1, (max - min + 1)));
        inv.addItem(new ItemStack(mat, Math.min(amount, mat.getMaxStackSize())));
    } catch (Exception ignored) {}
}
barrel.update(true, false);

        World w = p.getWorld();
        w.playSound(drop, Sound.ENTITY_PARROT_FLY, 1f, 1.2f);
        w.spawnParticle(Particle.CLOUD, drop.clone().add(0.5, 1.2, 0.5), 20, 0.4, 0.3, 0.4, 0.01);
        p.sendActionBar(ChatColor.AQUA + "Relief drop nearby!");

        // Schedule removal
        if (removeAfter > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (b.getType() == Material.BARREL) {
                    Inventory inv2 = ((Barrel) b.getState()).getInventory();
                    if (inv2.isEmpty()) {
                        b.setType(Material.AIR);
                        w.playSound(drop, Sound.BLOCK_BARREL_CLOSE, 0.8f, 0.8f);
                        w.spawnParticle(Particle.CLOUD, drop.clone().add(0.5, 0.8, 0.5), 10, 0.3,0.2,0.3, 0.01);
                    }
                }
            }, removeAfter * 20L);
        }
    }

    private Location findDrop(Location base, int radius) {
        World w = base.getWorld();
        for (int i = 0; i < 30; i++) {
            int dx = -radius + random.nextInt(radius * 2 + 1);
            int dz = -radius + random.nextInt(radius * 2 + 1);
            Location l = base.clone().add(dx, 0, dz);
            l = highestSolidBelow(w, l, 6);
            if (l != null && w.getBlockAt(l).getType().isAir()) return l;
        }
        return null;
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
    public int cooldownSeconds() { return plugin.getConfig().getInt("events.relief_drop.cooldownSeconds", 600); }
}