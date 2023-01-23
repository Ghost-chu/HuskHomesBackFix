package com.ghostchu.huskhomesbackfix;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.william278.huskhomes.BukkitHuskHomes;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.player.User;
import net.william278.huskhomes.position.Location;
import net.william278.huskhomes.position.Position;
import net.william278.huskhomes.position.Server;
import net.william278.huskhomes.position.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class HuskHomesBackFix extends JavaPlugin implements Listener {
    private final static Object EMPTY_OBJECT = new Object();
    private final Cache<UUID, Object> timeCacheMap = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .build();

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLogin(PlayerJoinEvent event) {
        this.timeCacheMap.put(event.getPlayer().getUniqueId(), EMPTY_OBJECT);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || !(event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND)) {
            return;
        }
        if (this.timeCacheMap.getIfPresent(event.getPlayer().getUniqueId()) != null) {
            return;
        }
        if (event.getTo() == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            User user = HuskHomesAPI.getInstance().adaptUser(player);
            Server server = HuskHomesAPI.getInstance().getServer();
            org.bukkit.Location bloc = event.getTo();
            if (bloc.getWorld() == null) return;
            World world = new World(bloc.getWorld().getName(), bloc.getWorld().getUID());
            Position position = new Position(new Location(bloc.getX(), bloc.getY(), bloc.getZ(), world), server);
            BukkitHuskHomes.getInstance().getDatabase().setLastPosition(user, position)
                    .whenComplete((result, err) -> {
                        if (err != null) {
                            getLogger().log(Level.WARNING, "Failed to set last position for player " + player.getName(), err);
                        }
                    });
        });

    }
}
