package dev.yourserver.smpdirector;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerStateListener implements Listener {
    private final TensionManager tension;
    public PlayerStateListener(TensionManager tension){ this.tension = tension; }

    @EventHandler public void onJoin(PlayerJoinEvent e){
        // Bossbar will show on first tick automatically; join grace handled inside EventRegistry.
    }

    @EventHandler public void onQuit(PlayerQuitEvent e){
        tension.hideBossbar(e.getPlayer().getUniqueId());
    }
}