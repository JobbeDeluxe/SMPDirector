package dev.yourserver.smpdirector;

import org.bukkit.event.EventHandler;
import dev.yourserver.smpdirector.EventRegistry;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStateListener implements Listener {
    private final EventRegistry registry;
    private final TensionManager tension;
    public PlayerStateListener(TensionManager tension, EventRegistry registry){ this.tension = tension; this.registry = registry; }

    @EventHandler public void onJoin(PlayerJoinEvent e){ registry.noteJoin(e.getPlayer().getUniqueId()); }
    @EventHandler public void onQuit(PlayerQuitEvent e){
        tension.hideBossbar(e.getPlayer().getUniqueId());
    }
}