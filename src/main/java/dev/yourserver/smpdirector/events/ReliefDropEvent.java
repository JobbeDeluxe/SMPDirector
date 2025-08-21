package dev.yourserver.smpdirector.events;

import dev.yourserver.smpdirector.DirectorEvent;
import dev.yourserver.smpdirector.SMPDirectorPlugin;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class ReliefDropEvent implements DirectorEvent {

    private final SMPDirectorPlugin plugin;
    private final Random random = new Random();

    public ReliefDropEvent(SMPDirectorPlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "relief_drop"; }

    @Override
    public boolean canRunFor(Player p) { return p.getGameMode() == GameMode.SURVIVAL; }

    @Override
    public void runFor(Player p) {
    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("events.relief_drop");
    if (sec == null) return;

    int radius = sec.getInt("radius", 8);
    int removeAfter = sec.getInt("removeAfterSeconds", 180);
    int minItems = Math.max(1, sec.getInt("minItems", 2));
    int maxItems = Math.max(minItems, sec.getInt("maxItems", 6));
    java.util.List<String> pool = sec.getStringList("lootPool");
    if (pool == null || pool.isEmpty()) pool = java.util.List.of("BREAD:6-12","TORCH:16-32");

    Location drop = findDrop(p.getLocation(), radius);
    if (drop == null) drop = p.getLocation();

    // Pre-calc items to add
    java.util.List<ItemStack> toAdd = new java.util.ArrayList<>();
    int added = 0;
    java.util.Random rnd = this.random;
    for (String spec : pool) {
        if (added >= maxItems) break;
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
            if (rnd.nextDouble() > chance) continue;

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
            int amount = min + rnd.nextInt(Math.max(1, (max - min + 1)));
            toAdd.add(new ItemStack(mat, Math.min(amount, mat.getMaxStackSize())));
            added++;
        } catch (Exception ignored) {}
    }

    // Place barrel block
    Block b = drop.getBlock();
    b.setType(Material.BARREL);

    final Block fb = b;
    final Location fdrop = drop.clone();
    final World fw = p.getWorld();
    final Player fp = p;
    final java.util.List<ItemStack> fItems = toAdd;

    // Fill inventory one tick later to ensure TE exists
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        org.bukkit.block.BlockState state = fb.getState();
        if (!(state instanceof Barrel)) { fb.setType(Material.BARREL); state = fb.getState(); if (!(state instanceof Barrel)) return; }
        Barrel barrel = (Barrel) state;
        Inventory inv = barrel.getInventory();

        if (fItems.isEmpty()) {
            // fallback content
            inv.addItem(new ItemStack(Material.BREAD, 6));
            inv.addItem(new ItemStack(Material.TORCH, 16));
            inv.addItem(new ItemStack(Material.IRON_INGOT, 4));
        } else {
            for (ItemStack is : fItems) inv.addItem(is);
        }
        barrel.update(true, false);

        fw.playSound(fdrop, Sound.ENTITY_PARROT_FLY, 1f, 1.2f);
        fw.spawnParticle(Particle.CLOUD, fdrop.clone().add(0.5, 1.2, 0.5), 20, 0.4, 0.3, 0.4, 0.01);
        fp.sendActionBar(ChatColor.AQUA + "Relief drop nearby!");
        fp.sendMessage(ChatColor.AQUA + "[SMPDirector] Relief drop at " + fdrop.getBlockX()+" "+fdrop.getBlockY()+" "+fdrop.getBlockZ());
    });

    // Schedule removal if still empty later
    if (removeAfter > 0) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            org.bukkit.block.BlockState state2 = fb.getState();
            if (state2 instanceof Barrel) {
                Inventory inv2 = ((Barrel) state2).getInventory();
                if (inv2.isEmpty()) {
                    fb.setType(Material.AIR);
                    fw.playSound(fdrop, Sound.BLOCK_BARREL_CLOSE, 0.8f, 0.8f);
                    fw.spawnParticle(Particle.CLOUD, fdrop.clone().add(0.5, 0.8, 0.5), 10, 0.3, 0.2, 0.3, 0.01);
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