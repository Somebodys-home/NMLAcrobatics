package io.github.NoOne.nMLAcrobatics.maneuvers.wallRun;

import io.github.NoOne.nMLAbilities.abilitySystem.abilityEffects.AbilityEffectsHelper;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.Speedometer;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import io.github.NoOne.nMLEnergy.EnergyManager;
import io.github.NoOne.nMLPlayerStats.profileSystem.ProfileManager;
import io.github.NoOne.nMLPlayerStats.statSystem.Stats;
import org.bukkit.*;
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
    private static ProfileManager profileManager = NMLAcrobatics.getInstance().getProfileManager();
    private static HashMap<UUID, Vector> wallRunDirections = new HashMap<>(); // the cardinal direction that the player is wall running in
    private static HashMap<UUID, Double> wallRunSpeeds = new HashMap<>(); // the speeds that the players started wall running in
    private static HashMap<UUID, Integer> wallRunTimes = new HashMap<>(); // the amount of time that you spend wall running
    private static String wallRunString = "wall running";

    public static void startWallRunTask() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!WallRun.isWallRunning(player)) continue;

                    UUID uuid = player.getUniqueId();
                    Vector direction = wallRunDirections.get(uuid);
                    BlockFace cardinal = RailGrind.convertVectorToCardinal(wallRunDirections.get(uuid));
                    Block bottomWallBlock = getWall(player, cardinal).getFirst();

                    // stop wall running if the player hits the ground or isnt touching the wall anymore
                    if (player.isOnGround() || !isTouchingWall(player)) {
                        stopWallRunning(player);
                        continue;
                    }

                    // give the player constant falling velocity while wall running
                    int time = wallRunTimes.get(uuid);
                    double speed = wallRunSpeeds.get(uuid) * .085;
                    double downwardsSpeed = Math.pow(time, 2) / -1750;
                    Vector playerDirection = player.getLocation().getDirection();

                    player.setVelocity(player.getVelocity().add(playerDirection.multiply(speed).setY(downwardsSpeed)));

                    // sound & effects
                    int interval;

                    try {
                        interval = (int) (3 / Speedometer.getSpeedPerTick(player));
                    } catch (Exception e) {
                        interval = 0;
                    }

                    if (interval == 0 || time % interval == 0) player.playSound(player, bottomWallBlock.getBlockSoundGroup().getStepSound(), .5f, 1f);
                    if (time % 7 == 0 && Speedometer.getSpeedPerSecond(player) >= 18) {
                        AbilityEffectsHelper.verticalParticleCircleInFrontOfEntity(new Particle.DustOptions(Color.WHITE, 1f), player, 1.25, 60, 3);
                    }

                    // spawn block particle at the players feet
                    player.spawnParticle(Particle.BLOCK, player.getLocation().add(new Vector(0, .5, 0)), 5, .05, .05, .05, bottomWallBlock.getBlockData());
                    wallRunTimes.put(uuid, time + 1);

                    // pull up when the head block is solid and the block above the head block isn't solid and youre looking in the direction of the wall
                    // have to specify that last bit so this doesn't trigger when a player runs off a wall
                    Block pullUpBlock = player.getWorld().getBlockAt(player.getLocation().add(direction.clone().add(new Vector(0, 2, 0))));

                    if (!pullUpBlock.isSolid() && bottomWallBlock.isSolid() && player.getFacing() == cardinal) {
                        stopWallRunning(player);
                        player.setVelocity(new Vector(0, .66, 0));
                        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.setVelocity(player.getVelocity().add(direction.multiply(.075)));
                            }
                        }.runTaskLater(nmlAcrobatics, 3);
                    }

                    // energy use
                    Stats stats = profileManager.getPlayerProfile(uuid).getStats();

                    if (stats.getCurrentEnergy() < 5) {
                        stopWallRunning(player);
                    } else {
                        EnergyManager.useEnergy(player, .25); // 5 energy/s
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
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1, 1);
        EnergyManager.pauseEnergyRegen(player);
        AbilityEffectsHelper.putOnInfiniteCooldown(player);
        player.setAllowFlight(true); // flight is how we make the player stay on the wall
        player.setFlying(true);
        player.setFlySpeed(.075f);
    }

    public static void stopWallRunning(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata(wallRunString, nmlAcrobatics);
        wallRunDirections.remove(uuid);
        wallRunSpeeds.remove(uuid);
        wallRunTimes.remove(uuid);
        EnergyManager.resumeEnergyRegen(player);
        AbilityEffectsHelper.removeInfiniteCooldown(player);
        player.setFlying(false);
        player.setFlySpeed(.1f);

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

    public static Vector getWallRunDirection(Player player) {
        return wallRunDirections.get(player.getUniqueId());
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
