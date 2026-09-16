package org.shaku.restrictionPlugin2;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class RestrictionPlugin2 extends JavaPlugin implements Listener {

    private final Set<String> frostWalkerDisabledWorlds = new HashSet<>();
    private final Set<String> noFlyWorlds = new HashSet<>();
    private final Set<String> teleportBlockedWorlds = new HashSet<>();
    private final List<WorldCommandRestriction> commandRestrictions = new ArrayList<>();
    private final Set<String> teleportCommands = new HashSet<>();
    private boolean debugMode = false;

    // 传送状态系统
    private final Map<UUID, TeleportContext> teleportContexts = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfigValues();

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("restrictionreload").setExecutor((sender, command, label, args) -> {
            reloadConfig();
            reloadConfigValues();
            sender.sendMessage("§a配置已重载!");
            return true;
        });

        getLogger().info("§a世界限制插件 RestrictionPlugin2 已启用!");
    }

    @Override
    public void onDisable() {
        teleportContexts.clear();
        getLogger().info("§c世界限制插件 RestrictionPlugin2 已禁用!");
    }

    private void reloadConfigValues() {
        FileConfiguration config = getConfig();

        debugMode = config.getBoolean("debug-mode", false);

        frostWalkerDisabledWorlds.clear();
        frostWalkerDisabledWorlds.addAll(config.getStringList("frost-walker-disabled-worlds"));
        if (debugMode) getLogger().info("冰霜行者禁用世界: " + frostWalkerDisabledWorlds);

        noFlyWorlds.clear();
        noFlyWorlds.addAll(config.getStringList("no-fly-worlds"));
        if (debugMode) getLogger().info("飞行限制世界: " + noFlyWorlds);

        teleportBlockedWorlds.clear();
        teleportBlockedWorlds.addAll(config.getStringList("teleport-blocked-worlds"));
        if (debugMode) getLogger().info("传送限制世界: " + teleportBlockedWorlds);

        teleportCommands.clear();
        teleportCommands.addAll(config.getStringList("teleport-commands"));
        if (debugMode) getLogger().info("传送命令: " + teleportCommands);

        commandRestrictions.clear();
        if (config.getConfigurationSection("command-restrictions") != null) {
            for (String worldName : config.getConfigurationSection("command-restrictions").getKeys(false)) {
                List<String> blockedCommands = config.getStringList("command-restrictions." + worldName);
                // 统一转为小写，防止大小写问题
                List<String> lowerCommands = blockedCommands.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                commandRestrictions.add(new WorldCommandRestriction(worldName, lowerCommands));
                if (debugMode) getLogger().info("命令限制: " + worldName + " -> " + lowerCommands);
            }
        }
    }

    // ========== 冰霜行者限制 ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockForm(EntityBlockFormEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        String worldName = player.getWorld().getName();

        if (frostWalkerDisabledWorlds.contains(worldName)) {
            if (debugMode) getLogger().info("取消冰霜行者在世界 " + worldName + " 的效果");
            event.setCancelled(true);
        }
    }

    // ========== 命令限制（优先级最低，最先拦截） ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        if (message.isEmpty()) return;

        // 调试输出原始命令
        if (debugMode) getLogger().info("原始命令: " + message + " (来自 " + player.getName() + ")");

        // 权限检查
        if (player.hasPermission("worldrestrictions.bypass.command") || player.isOp()) {
            return;
        }

        String worldName = player.getWorld().getName();
        String rawCommand = message.startsWith("/") ? message.substring(1) : message;
        String commandBase = rawCommand.split(" ")[0].toLowerCase();

        // 检查普通命令限制
        for (WorldCommandRestriction restriction : commandRestrictions) {
            if (restriction.worldName.equalsIgnoreCase(worldName)) {
                if (restriction.blockedCommands.contains(commandBase)) {
                    player.sendMessage(getConfig().getString("messages.command-blocked", "§c此命令在该世界被禁用!"));
                    event.setCancelled(true);
                    if (debugMode) getLogger().info("阻止命令: " + rawCommand + " 在世界 " + worldName);
                    return;
                }
            }
        }

        // 检查传送命令限制（记录上下文）
        if (teleportCommands.contains(commandBase)) {
            TeleportContext context = new TeleportContext(
                    player.getLocation().clone(),
                    TeleportType.PLAYER_COMMAND
            );
            teleportContexts.put(player.getUniqueId(), context);
            if (debugMode) getLogger().info("记录玩家指令传送: " + player.getName());
        }
    }

    // ========== 传送事件处理 ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Location toLocation = event.getTo();

        if (toLocation == null || toLocation.getWorld() == null) return;

        String toWorldName = toLocation.getWorld().getName();

        if (teleportBlockedWorlds.contains(toWorldName)) {
            TeleportContext context = teleportContexts.get(playerId);

            if (context == null) {
                if (debugMode) getLogger().info("无上下文传送，允许: " + player.getName());
                return;
            }

            if (context.type == TeleportType.PLAYER_COMMAND) {
                event.setCancelled(true);

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    player.teleport(context.originalLocation);
                    player.sendMessage(getConfig().getString("messages.teleport-blocked", "§c您不能传送到这个世界!"));
                    if (debugMode) {
                        getLogger().info("拦截玩家指令传送: " + player.getName() +
                                " 从 " + context.originalLocation +
                                " 到 " + toLocation);
                    }
                    teleportContexts.remove(playerId);
                }, 1L);
            } else if (context.type == TeleportType.PLUGIN) {
                if (debugMode) getLogger().info("允许插件传送: " + player.getName() + " 到 " + toWorldName);
                teleportContexts.remove(playerId);
            }
        } else {
            teleportContexts.remove(playerId);
        }
    }

    // ========== API方法 ==========
    public void markPluginTeleport(Player player) {
        UUID playerId = player.getUniqueId();
        TeleportContext context = new TeleportContext(
                player.getLocation().clone(),
                TeleportType.PLUGIN
        );
        teleportContexts.put(playerId, context);
        if (debugMode) getLogger().info("标记插件传送: " + player.getName());
    }

    public void clearTeleportContext(Player player) {
        teleportContexts.remove(player.getUniqueId());
    }

    // ========== 飞行限制 ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        checkFlightRestriction(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        checkFlightRestriction(event.getPlayer());
    }

    private void checkFlightRestriction(Player player) {
        if (player.hasPermission("worldrestrictions.bypass.fly") || player.isOp()) {
            return;
        }

        String worldName = player.getWorld().getName();

        if (noFlyWorlds.contains(worldName)) {
            if (player.getAllowFlight() || player.isFlying()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(getConfig().getString("messages.fly-disabled", "§c此世界禁止飞行!"));
                if (debugMode) getLogger().info("禁用飞行: " + player.getName() + " 在世界 " + worldName);
            }
        }
    }

    // ========== 内部类 ==========
    private static class WorldCommandRestriction {
        final String worldName;
        final Set<String> blockedCommands = new HashSet<>();

        WorldCommandRestriction(String worldName, List<String> commands) {
            this.worldName = worldName;
            // 命令在构造时已转为小写
            this.blockedCommands.addAll(commands);
        }
    }

    private enum TeleportType {
        PLAYER_COMMAND,
        PLUGIN
    }

    private static class TeleportContext {
        final Location originalLocation;
        final TeleportType type;

        TeleportContext(Location originalLocation, TeleportType type) {
            this.originalLocation = originalLocation;
            this.type = type;
        }
    }

    // 获取插件实例的API
    public static RestrictionPlugin2 getInstance() {
        return JavaPlugin.getPlugin(RestrictionPlugin2.class);
    }
}