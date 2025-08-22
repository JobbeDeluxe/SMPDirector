package dev.yourserver.smpdirector;

import org.bukkit.entity.Player;

public interface DirectorEvent {
    String id();
    boolean canRunFor(Player p);
    void runFor(Player p);
    int cooldownSeconds();
}
