package dev.yourserver.smpdirector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DirectorCommand implements CommandExecutor {

    private final SMPDirectorPlugin plugin;
    private final TensionManager tension;
    private final EventRegistry registry;

    public DirectorCommand(SMPDirectorPlugin plugin, TensionManager tension, EventRegistry registry) {
        this.plugin = plugin;
        this.tension = tension;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " <start|stop|pause|status|debug|trigger|list|reload>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start":
                plugin.startDirectorTick();
                sender.sendMessage(ChatColor.GREEN + "SMPDirector started.");
                return true;
            case "stop":
            case "pause":
                plugin.stopDirectorTick();
                sender.sendMessage(ChatColor.RED + "SMPDirector paused/stopped.");
                return true;
            case "status":
                sender.sendMessage(ChatColor.AQUA + "SMPDirector is " + (plugin.isDirectorEnabled() ? "ENABLED" : "DISABLED"));
                return true;
            case "debug": {
                if (sender instanceof Player p) {
                    double v = tension.get(p.getUniqueId());
                    sender.sendMessage(ChatColor.GRAY + "Your tension: " + String.format("%.1f", v));
                } else {
                    sender.sendMessage(ChatColor.GRAY + "Online players:");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        sender.sendMessage(" - " + p.getName() + ": " + String.format("%.1f", tension.get(p.getUniqueId())));
                    }
                }
                return true;
            }
            case "list": {
                sender.sendMessage(ChatColor.AQUA + "Available events:");
                for (DirectorEvent ev : registry.getEvents()) {
                    sender.sendMessage(ChatColor.GRAY + " - " + ev.id());
                }
                return true;
            }
            case "reload": {
                plugin.reloadConfig();
                registry.reloadSettings();
                sender.sendMessage(ChatColor.GREEN + "SMPDirector config reloaded.");
                return true;
            }
            case "trigger": {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " trigger <ambush|relief_drop|weather_cell>");
                    return true;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(ChatColor.RED + "Only players can trigger events for themselves.");
                    return true;
                }
                String id = args[1].toLowerCase();
                for (DirectorEvent ev : registry.getEvents()) {
                    if (ev.id().equalsIgnoreCase(id)) {
                        ev.runFor(p);
                        registry.markRun(ev, p);
                        sender.sendMessage(ChatColor.GREEN + "Triggered: " + ev.id());
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.RED + "Unknown event: " + id);
                return true;
            }
            default:
                sender.sendMessage(ChatColor.YELLOW + "Unknown subcommand.");
                return true;
        }
    }
}