package dev.yourserver.smpdirector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SMPDirectorPlugin extends JavaPlugin {

    public TensionManager tensionManager;
    private EventRegistry eventRegistry;
    private BukkitTask tickTask;
    private boolean enabled = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        mergeDefaults();

        this.tensionManager = new TensionManager(this);
        this.eventRegistry = new EventRegistry(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new CombatListener(tensionManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerStateListener(tensionManager), this);

        // Commands
        getCommand("director").setExecutor(new DirectorCommand(this, tensionManager, eventRegistry));

        // Tick loop (1s)
        startDirectorTick();

        getLogger().info("SMPDirector enabled.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) tickTask.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            tensionManager.hideBossbar(p.getUniqueId());
        }
        getLogger().info("SMPDirector disabled.");
    }

    public void startDirectorTick() {
        if (tickTask != null) tickTask.cancel();
        tickTask = Bukkit.getScheduler().runTaskTimer(this, new DirectorTick(this, tensionManager, eventRegistry), 20L, 20L);
        enabled = true;
    }

    public void stopDirectorTick() {
        if (tickTask != null) tickTask.cancel();
        tickTask = null;
        enabled = false;
    }

    public boolean isDirectorEnabled() {
        return enabled;
    }

    public EventRegistry getEventRegistry() { return eventRegistry; }
    public TensionManager getTensionManager(){ return tensionManager; }

    private void mergeDefaults() {
        try {
            java.io.InputStream is = getResource("config.yml");
            if (is != null) {
                java.io.InputStreamReader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
                YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                getConfig().setDefaults(def);
                getConfig().options().copyDefaults(true);
                saveConfig();
            }
        } catch (Exception ignored) {}
    }
}