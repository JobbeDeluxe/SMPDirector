package dev.yourserver.smpdirector;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

public class CombatListener implements Listener {
    private final TensionManager tension;
    public CombatListener(TensionManager tension) { this.tension = tension; }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player p) {
            tension.add(p.getUniqueId(), p.getServer().getPluginManager().getPlugin("SMPDirector") != null
                    ? ((SMPDirectorPlugin)p.getServer().getPluginManager().getPlugin("SMPDirector")).getConfig().getDouble("tension.hit", 6)
                    : 6);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        tension.add(p.getUniqueId(), p.getServer().getPluginManager().getPlugin("SMPDirector") != null
                ? ((SMPDirectorPlugin)p.getServer().getPluginManager().getPlugin("SMPDirector")).getConfig().getDouble("tension.death", 35)
                : 35);
    }
}