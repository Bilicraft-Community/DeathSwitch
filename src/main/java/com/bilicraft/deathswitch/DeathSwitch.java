package com.bilicraft.deathswitch;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

public final class DeathSwitch extends JavaPlugin {

    private GameProcess process;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        process = new GameProcess(this);
    }

    @Override
    public void onDisable() {
        process.cleanup();
    }

    public static void stopServer(int time) {
        AtomicInteger nowTime = new AtomicInteger(time);
        BossBar stopBossBar = Bukkit.createBossBar("游戏已结束，服务器即将自动重启", BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(stopBossBar::addPlayer);
        stopBossBar.setVisible(true);
        Bukkit.getScheduler().runTaskTimer(DeathSwitch.getPlugin(DeathSwitch.class), () -> {
            stopBossBar.setTitle("游戏即将于 " + (double) nowTime.getAndDecrement() / 20 + " 秒后重启");
            stopBossBar.setProgress((double) nowTime.getAndDecrement() / (30 * 20));
            if (nowTime.get() <= 0) {
                Bukkit.getServer().shutdown();
            }
        }, 0, 1);

    }
}
