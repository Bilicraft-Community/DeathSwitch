package com.bilicraft.deathswitch;

import org.bukkit.plugin.Plugin;

public class Configuration {

    private static Configuration instance;

    private final Plugin plugin;

    private final int minPlayer;
    private final int maxPlayer;
    private final long switchTimeMinTick;
    private final long switchTimeMaxTick;
    private final long switchReduceTimeTick;

    public int getMinPlayer() {
        return minPlayer;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public long getSwitchTimeMinTick() {
        return switchTimeMinTick;
    }

    public long getSwitchTimeMaxTick() {
        return switchTimeMaxTick;
    }

    public long getSwitchReduceTimeTick() {
        return switchReduceTimeTick;
    }

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration(DeathSwitch.getPlugin(DeathSwitch.class));
        }
        return instance;
    }

    private Configuration(Plugin plugin) {
        this.plugin = plugin;

        minPlayer = plugin.getConfig().getInt("MinPlayer");
        maxPlayer = plugin.getConfig().getInt("MaxPlayer");
        switchTimeMinTick = plugin.getConfig().getLong("SwitchTimeMinTick");
        switchTimeMaxTick = plugin.getConfig().getLong("SwitchTimeMaxTick");
        switchReduceTimeTick = plugin.getConfig().getLong("SwitchReduceTimeTick");
    }
}
