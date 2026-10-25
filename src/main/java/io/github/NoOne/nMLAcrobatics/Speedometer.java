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
                    Location location = player.getLocation();
                    Location previousLocation = previousLocations.get(uuid);

                    if (previousLocation != null) {
                        Vector delta = location.toVector().subtract(previousLocation.toVector());

                        if (delta.lengthSquared() > 0.0001) {
                            rollDirections.put(uuid, delta.normalize());
                        }

                        playerSpeeds.put(uuid, previousLocation.distance(location) * 20);
                    }

                    previousLocations.put(uuid, location);
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

    public static double getSpeed(Player player) {
        return playerSpeeds.get(player.getUniqueId());
    }
}
