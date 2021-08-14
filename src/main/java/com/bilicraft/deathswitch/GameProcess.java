package com.bilicraft.deathswitch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameProcess implements Listener {

    private final Plugin plugin;

    private Status status = Status.BEFORE_WAITING;

    private final Set<UUID> players = Set.of();

    private final BossBar bossbar;

    private final BukkitTask tickTask;

    private final int waitingTime = 30 * 20;
    private int nowWaitingTime = waitingTime;

    private long switchTime;
    private long nowSwitchTime;

    public GameProcess(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        bossbar = Bukkit.createBossBar("GameBossBar", BarColor.RED, BarStyle.SOLID);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
    }


    private synchronized void tick() {
        switch (status) {
            case BEFORE_WAITING:
                if (players.size() >= Configuration.getInstance().getMinPlayer()) {
                    nowWaitingTime = waitingTime;
                    bossbar.setTitle("距离游戏开始还剩余 " + (nowWaitingTime / 20) + " 秒");
                    bossbar.setProgress((double) nowWaitingTime / waitingTime);
                    bossbar.setVisible(true);
                    status = Status.WAITING;
                } else {
                    bossbar.setTitle("等待玩家中... （" + players.size() + "/" + Configuration.getInstance().getMaxPlayer() + "）");
                    bossbar.setProgress(1.0);
                    bossbar.setVisible(true);
                }
                break;

            case WAITING:
                if (players.size() < Configuration.getInstance().getMinPlayer()) {
                    bossbar.setVisible(false);
                    status = Status.BEFORE_WAITING;
                    break;
                }

                nowWaitingTime--;

                bossbar.setTitle("距离游戏开始还剩余 " + (nowWaitingTime / 20) + " 秒");
                bossbar.setProgress((double) nowWaitingTime / waitingTime);

                if (nowWaitingTime <= 0) {
                    status = Status.BEFORE_RUNNING;
                }
                break;

            case BEFORE_RUNNING:
                bossbar.removeAll();
                bossbar.setVisible(false);
                switchTime = Configuration.getInstance().getSwitchTimeMaxTick();
                nowSwitchTime = switchTime;
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.sendTitle("游戏开始", "小心随时可能到来的死亡交换！", 10, 20, 70);
                });
                status = Status.RUNNING;
                break;

            case RUNNING:
                switch (Math.toIntExact(switchTime)) {
                    case 20 * 60:
                        Bukkit.broadcastMessage("距离死亡交换还有一分钟！");
                        break;
                    case 20 * 30:
                        Bukkit.broadcastMessage("距离死亡交换还有 30 秒！");
                        break;
                    case 20 * 10:
                        Bukkit.broadcastMessage("距离死亡交换还有 10 秒！");
                        break;
                    case 20 * 5:
                        Bukkit.broadcastMessage("距离死亡交换还有 5 秒！");
                        break;
                    case 20 * 3:
                        Bukkit.broadcastMessage("距离死亡交换还有 3 秒！");
                        break;
                    case 20 * 2:
                        Bukkit.broadcastMessage("距离死亡交换还有 2 秒！");
                        break;
                    case 20 * 1:
                        Bukkit.broadcastMessage("距离死亡交换还有 1 秒！");
                        break;
                    case 0:
                        if (switchTime - Configuration.getInstance().getSwitchReduceTimeTick() >= Configuration.getInstance().getSwitchTimeMinTick())
                            switchTime = switchTime - Configuration.getInstance().getSwitchReduceTimeTick();
                        nowSwitchTime = switchTime;

                        doDeathSwitch();

                        break;
                }

                switchTime--;

                if (getAllOnlinePlayers().size() == 1) {
                    status = Status.ENDED;
                }

            case ENDED:
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendTitle("游戏结束", "恭喜 " + getAllOnlinePlayers().get(0).getDisplayName() + " 获得了胜利！", 10, 20, 70);
                });
                DeathSwitch.stopServer(30 * 20);
                status = Status.ENDED_RESET;
                break;
        }
    }

    private List<Player> getAllOnlinePlayers() {
        return players.stream().filter(uuid -> Bukkit.getPlayer(uuid) != null).map(Bukkit::getPlayer).toList();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {

        if (players.contains(e.getPlayer().getUniqueId())) {
            e.getPlayer().sendMessage(ChatColor.GRAY + "已返回游戏");
            return;
        }

        if (players.size() >= Configuration.getInstance().getMaxPlayer()) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            e.getPlayer().sendMessage(ChatColor.RED + "由于游戏人数已满，因此您已成为观察者");
        }

        if (status == Status.BEFORE_WAITING || status == Status.WAITING) {
            bossbar.addPlayer(e.getPlayer());
            addPlayer(e.getPlayer());
            e.getPlayer().sendMessage(ChatColor.AQUA + "您已成功加入游戏");
        } else {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            e.getPlayer().sendMessage(ChatColor.RED + "由于游戏已开始，因此您已成为观察者");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (status == Status.BEFORE_WAITING || status == Status.WAITING) {
            removePlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (status != Status.RUNNING) return;
        removePlayer(e.getEntity());
        e.getEntity().getLocation().getWorld().strikeLightning(e.getEntity().getLocation());
        e.getEntity().spigot().respawn();
        e.getEntity().setGameMode(GameMode.SPECTATOR);
        e.getEntity().sendMessage(ChatColor.RED + "由于您已死亡，因此您已成为观察者");
    }

    private void addPlayer(Player player) {
        players.add(player.getUniqueId());
        Bukkit.broadcastMessage(ChatColor.GOLD + "玩家 " + player.getDisplayName() + " 已加入游戏");
    }

    private void removePlayer(Player player) {
        players.remove(player.getUniqueId());
        Bukkit.broadcastMessage(ChatColor.GOLD + "玩家 " + player.getDisplayName() + " 已退出游戏");
    }


    private void doDeathSwitch() {
        var players = getAllOnlinePlayers();
        Collections.shuffle(players);
        for (int i = 0; i <= players.size() / 2 * 2; i = i + 2) {
            var player1 = players.get(i);
            var player2 = players.get(i + 2);
            var player1Loc = player1.getLocation().clone();
            var player2Loc = player2.getLocation().clone();
            player1.teleport(player2Loc);
            player2.teleport(player1Loc);
            player1.sendTitle("死亡传送！", "你已与 " + player2.getDisplayName() + " 交换了位置", 10, 20, 70);
            player2.sendTitle("死亡传送！", "你已与 " + player1.getDisplayName() + " 交换了位置", 10, 20, 70);
        }
        if (players.size() % 2 == 1) {
            players.get(players.size() - 1).sendTitle("你是幸运者", "你未被传送", 10, 20, 70);
        }
    }


    public void cleanup() {
        bossbar.removeAll();
        tickTask.cancel();
        HandlerList.unregisterAll(this);
    }

    public enum Status {
        BEFORE_WAITING("Internal Status"),
        WAITING("等待中"),
        BEFORE_RUNNING("Internal Status"),
        RUNNING("游戏中"),
        ENDED("游戏结束"),
        ENDED_RESET("Internal Status");

        private String friendlyName;

        Status(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }
}
