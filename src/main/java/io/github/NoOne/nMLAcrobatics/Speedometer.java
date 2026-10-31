package io.github.NoOne.nMLAcrobatics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Speedometer {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static HashMap<UUID, Location> previousLocations = new HashMap<>();
    private static HashMap<UUID, Vector> rollDirections = new HashMap<>();
    private static HashMap<UUID, Double> playerSpeeds = new HashMap<>();

    public static void startSpeedometer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    Location playerLocation = player.getLocation();
                    Location previousLocation = previousLocations.getOrDefault(uuid, playerLocation);
                    Vector delta = playerLocation.toVector().subtract(previousLocation.toVector());

                    rollDirections.put(uuid, delta.normalize());
                    playerSpeeds.put(uuid, previousLocation.distance(playerLocation) * 20);
                    previousLocations.put(uuid, playerLocation);
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static Location getPreviousLocation(Player player) {
        return previousLocations.get(player.getUniqueId());
    }

    public static Vector getRollDirection(Player player) {
        return rollDirections.get(player.getUniqueId());
    }

    public static double getSpeedPerSecond(Player player) {
        return playerSpeeds.get(player.getUniqueId());
    }

    public static double getSpeedPerTick(Player player) {
        return getSpeedPerSecond(player) / 20;
    }
}
