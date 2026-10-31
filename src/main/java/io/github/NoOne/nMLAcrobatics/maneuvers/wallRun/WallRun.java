package io.github.NoOne.nMLAcrobatics.maneuvers.wallRun;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.Speedometer;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class WallRun {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static HashMap<UUID, Vector> wallRunDirections = new HashMap<>(); // the cardinal direction that the player is wall running in
    private static HashMap<UUID, Double> wallRunSpeeds = new HashMap<>(); // the speeds that the players started wall running in
    private static Sound pingSound = Sound.ITEM_TRIDENT_RETURN;
    private static String wallRunString = "wall running";

    public static void startWallRunTask() {
        // todo: make wall jumping, then pulling the player up from a wall
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!WallRun.isWallRunning(player)) continue;
                    if (player.isOnGround() || !isTouchingWall(player)) {
                        stopWallRunning(player);
                        continue;
                    }

                    // give the player constant falling velocity while wall running
                    double speed = wallRunSpeeds.get(player.getUniqueId()) * .05;
                    Vector direction = player.getLocation().getDirection();

                    player.setVelocity(player.getVelocity().add(direction.multiply(.05).setY(0)));
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startWallRunning(Player player) {
        UUID uuid = player.getUniqueId();

        player.setMetadata(wallRunString, new FixedMetadataValue(nmlAcrobatics, true));
        wallRunDirections.put(uuid, player.getFacing().getDirection());
        wallRunSpeeds.put(uuid, Speedometer.getSpeedPerTick(player));
        player.playSound(player.getLocation(), pingSound, 1, 1);
        player.setAllowFlight(true); // flight is how we make the player stay on the wall
        player.setFlying(true);
        player.setFlySpeed(.05f); // only really controls the players ascending speed
    }

    public static void stopWallRunning(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata(wallRunString, nmlAcrobatics);
        wallRunDirections.remove(uuid);
        wallRunSpeeds.remove(uuid);
        player.setFlySpeed(.1f);

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
        }
    }

    public static boolean isAboutToHitAWall(Player player) {
        for (Block block : getFacingWall(player, player.getFacing())) {
            if (!block.isSolid() || block.isLiquid()) {
                return false;
            }
        }

        return true;
    }

    public static boolean isWallRunning(Player player) {
        return player.hasMetadata(wallRunString);
    }

    // returns t/f if the player is still touching the wall that they started wall running from
    private static boolean isTouchingWall(Player player) {
        for (Block block : getFacingWall(player, RailGrind.convertVectorToCardinal(wallRunDirections.get(player.getUniqueId())))) {
            if (!block.isSolid()) {
                return false;
            }
        }

        return true;
    }

    private static ArrayList<Block> getFacingWall(Player player, BlockFace facing) {
        return new ArrayList<>(){{
            World world = player.getWorld();

            add(world.getBlockAt(player.getLocation().add(facing.getDirection())));
            add(world.getBlockAt(player.getLocation().add(facing.getDirection()).add(0, 1, 0)));
        }};
    }
}
