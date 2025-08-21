package dev.yourserver.smpdirector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DirectorTick implements Runnable {

    private final SMPDirectorPlugin plugin;
    private final TensionManager tension;
    private final EventRegistry registry;

    public DirectorTick(SMPDirectorPlugin plugin, TensionManager tension, EventRegistry registry) {
        this.plugin = plugin;
        this.tension = tension;
        this.registry = registry;
    }

    @Override
    public void run() {
        if (!plugin.isDirectorEnabled()) return;

        // Global decay
        tension.decayAll();

        for (Player p : Bukkit.getOnlinePlayers()) {
            tension.tickSensedInputs(p);
            tension.showOrUpdateBossbar(p);
            DirectorEvent ev = registry.pickFor(p);
            if (ev != null && ev.canRunFor(p)) {
                ev.runFor(p);
                registry.markRun(ev, p);
            }
        }
    }
}