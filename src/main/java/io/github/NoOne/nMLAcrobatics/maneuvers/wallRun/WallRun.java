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
import java.util.List;
import java.util.UUID;

public class WallRun {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static HashMap<UUID, Vector> wallRunDirections = new HashMap<>(); // the cardinal direction that the player is wall running in
    private static HashMap<UUID, Double> wallRunSpeeds = new HashMap<>(); // the speeds that the players started wall running in
    private static HashMap<UUID, Integer> wallRunTimes = new HashMap<>(); // the amount of time that you spend wall running
    private static Sound pingSound = Sound.ITEM_TRIDENT_RETURN;
    private static String wallRunString = "wall running";

    public static void startWallRunTask() {
        // todo: pause energy regen and use energy while wall running
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!WallRun.isWallRunning(player)) continue;
                    if (player.isOnGround()) {
                        stopWallRunning(player);
                        continue;
                    }

                    UUID uuid = player.getUniqueId();
                    Vector direction = wallRunDirections.get(uuid);
                    BlockFace cardinal = RailGrind.convertVectorToCardinal(wallRunDirections.get(uuid));

                    // give the player constant falling velocity while wall running
                    int time = wallRunTimes.get(uuid);
                    double speed = wallRunSpeeds.get(uuid) * .05;
                    double downwardsSpeed = Math.pow(time, 2) / -1750;
                    Vector playerDirection = player.getLocation().getDirection();

                    player.setVelocity(player.getVelocity().add(playerDirection.multiply(speed).setY(downwardsSpeed)));
                    wallRunTimes.put(uuid, time + 1);

                    // if the player isnt touching a wall, then stop wall running
                    if (!isTouchingWall(player)) {
                        ArrayList<Block> wallBlocks = getWall(player, cardinal);

                        // pull up when the head block isn't solid (but the bottom one is)
                        // have to specify that last bit so this doesn't trigger when a player runs off a wall
                        if (!wallBlocks.getLast().isSolid() && wallBlocks.getFirst().isSolid()) {
                            player.setVelocity(new Vector(0, .66, 0).add(direction.multiply(.2)));
                            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.setVelocity(player.getVelocity().add(direction.multiply(.2)));
                                }
                            }.runTaskLater(nmlAcrobatics, 5);
                        }

                        stopWallRunning(player);
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startWallRunning(Player player, BlockFace cardinal) {
        UUID uuid = player.getUniqueId();

        player.setMetadata(wallRunString, new FixedMetadataValue(nmlAcrobatics, true));
        wallRunDirections.put(uuid, cardinal.getDirection());
        wallRunSpeeds.put(uuid, Speedometer.getSpeedPerTick(player));
        wallRunTimes.put(uuid, 0);
        player.playSound(player.getLocation(), pingSound, 1, 1);
        player.setAllowFlight(true); // flight is how we make the player stay on the wall
        player.setFlying(true);
    }

    public static void stopWallRunning(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata(wallRunString, nmlAcrobatics);
        wallRunDirections.remove(uuid);
        wallRunSpeeds.remove(uuid);
        wallRunTimes.remove(uuid);
        player.setFlying(false);

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
        }
    }

    public static boolean isWallRunning(Player player) {
        return player.hasMetadata(wallRunString);
    }

    // returns t/f if the player is still touching the wall that they started wall running from
    private static boolean isTouchingWall(Player player) {
        for (Block block : getWall(player, RailGrind.convertVectorToCardinal(wallRunDirections.get(player.getUniqueId())))) {
            if (!block.isSolid()) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidWall(List<Block> blocks) {
        for (Block block : blocks) {
            if (!block.isSolid()) {
                return false;
            }
        }

        return true;
    }

    // returns which of the walls the player is gonna wall run off of
    // checking from the front, right, then left
    public static BlockFace getClosetWallCardinal(Player player) {
        BlockFace facing = player.getFacing();
        BlockFace left = RailGrind.getLeftCardinal(player.getFacing());
        BlockFace right = RailGrind.getRightCardinal(player.getFacing());

        if (isValidWall(getWall(player, facing))) {
            return facing;
        } else if (isValidWall(getWall(player, left))) {
            return left;
        } else if (isValidWall(getWall(player, right))) {
            return right;
        } else {
            return null;
        }
    }

    // returns an arraylist of blocks that's to the (CARDINAL) of a player
    private static ArrayList<Block> getWall(Player player, BlockFace cardinal) {
        return new ArrayList<>(){{
            World world = player.getWorld();

            add(world.getBlockAt(player.getLocation().add(cardinal.getDirection())));
            add(world.getBlockAt(player.getLocation().add(cardinal.getDirection()).add(0, 1, 0)));
        }};
    }
}
