package com.ghostchu.huskhomesbackfix;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.william278.huskhomes2.HuskHomes;
import me.william278.huskhomes2.data.DataManager;
import me.william278.huskhomes2.teleport.points.TeleportationPoint;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HuskHomesBackFix extends JavaPlugin implements Listener {

    private final Cache<UUID, Long> timeCacheMap = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        this.timeCacheMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !(event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND)) {
            return;
        }
        if (this.timeCacheMap.getIfPresent(event.getPlayer().getUniqueId()) != null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try (Connection connection = HuskHomes.getConnection()) {
                DataManager.setPlayerLastPosition(player, new TeleportationPoint(event.getFrom(), HuskHomes.getSettings().getServerID()), connection);
            } catch (SQLException exception) {
                getLogger().log(Level.WARNING, "Failed to save player last position", exception);
            }
        });

    }
}
