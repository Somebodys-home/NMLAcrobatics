package io.github.NoOne.nMLAcrobatics.maneuvers;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLEnergy.EnergyManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Maneuvers {
    private static NMLAcrobatics nmlAcrobatics;
    private BukkitTask wallRunTask;
    private static HashMap<UUID, Vector> wallCardinals = new HashMap<>(); // only to know where to push the player back to when on a wall
    private static HashMap<UUID, Double> wallRunSpeeds = new HashMap<>(); // speed that you start wall running at
    private static HashMap<UUID, Integer> wallRunTimes = new HashMap<>(); // the amount of time that you spend wall running

    public Maneuvers(NMLAcrobatics nmlAcrobatics) {
        Maneuvers.nmlAcrobatics = nmlAcrobatics;
    }

    public void startWallRunTask() {
        wallRunTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("wall run")) {
                        Location playerLocation = player.getLocation();
                        UUID uuid = player.getUniqueId();

                        wallRunTimes.put(uuid, wallRunTimes.get(uuid) + 1);

                        Vector climbCardinal = getClosestWallCardinal(player);
                        double currentEnergy = nmlAcrobatics.getProfileManager().getPlayerProfile(uuid).getStats().getCurrentEnergy();
                        double energyMinimum = nmlAcrobatics.getProfileManager().getPlayerProfile(uuid).getStats().getMaxEnergy() / 20;
                        double speed = wallRunSpeeds.get(uuid);

                        if (climbCardinal == null) {
                            climbCardinal = wallCardinals.get(uuid);
                        }

                        wallCardinals.put(uuid, climbCardinal);

                        Location front = playerLocation.clone().add(climbCardinal).add(0, 1, 0);
                        Location aboveFront = playerLocation.clone().add(climbCardinal).add(0, 1, 0);
                        Block frontBlock = front.getBlock();
                        Block aboveFrontBlock = aboveFront.clone().add(0, 1, 0).getBlock();
                        Block moreAboveFrontBlock = aboveFront.clone().add(0, 2, 0).getBlock();
                        Location wallFaceLoc = frontBlock.getLocation().add(0.5, 0.5, 0.5).toVector().subtract(climbCardinal.clone().multiply(0.5)).toLocation(frontBlock.getWorld());

                        // constant wall run velocity
                        Vector wallRun = player.getVelocity().add(player.getLocation().getDirection().multiply(speed / 7.5));
                        wallRun.setY(Math.pow(wallRunTimes.get(uuid), 2) / -450); // fall-off rate
                        player.setVelocity(wallRun);

                        // running off wall / landing on the ground stops player from climbing
                        if (player.isOnGround() || !hasNeighboringBlocks(wallFaceLoc) || currentEnergy < energyMinimum) stopWallRunning(player);

                        // pull-up
                        if (frontBlock.getType().isSolid() && (aboveFrontBlock.getType().isAir() || !aboveFrontBlock.isSolid()) &&
                            (moreAboveFrontBlock.getType().isAir() || !moreAboveFrontBlock.isSolid())) {

                            Vector finalClimbCardinal = climbCardinal;

                            stopWallRunning(player);
                            player.setVelocity(new Vector(0, 0.66, 0).add(climbCardinal.multiply(0.2)));
                            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.setVelocity(player.getVelocity().add(finalClimbCardinal.multiply(.3)));
                                }
                            }.runTaskLater(nmlAcrobatics, 5);
                        }

                        if (tickCounter == 10) {
                            EnergyManager.useEnergy(player, 5);
                            tickCounter = 0;
                        }
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 2);
    }

    public void stopTasks() {
        wallRunTask.cancel();
    }

    public static void startWallRun(Player player, Vector wallCardinal) {
        if (!wallRunSpeeds.containsKey(player.getUniqueId())) {
            wallRunSpeeds.put(player.getUniqueId(), player.getVelocity().length());
            wallRunTimes.put(player.getUniqueId(), 0);
            wallCardinals.put(player.getUniqueId(), wallCardinal);
            player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
            player.setMetadata("wall run", new FixedMetadataValue(nmlAcrobatics, true));
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(.04f);
            EnergyManager.pauseEnergyRegen(player);
            Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Wall Run"));
        }
    }

    public static void startWallRun(Player player, Vector wallCardinal, double speed) {
        if (!wallRunSpeeds.containsKey(player.getUniqueId())) {
            wallRunSpeeds.put(player.getUniqueId(), speed);
            wallRunTimes.put(player.getUniqueId(), 0);
            wallCardinals.put(player.getUniqueId(), wallCardinal);
            player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
            player.setMetadata("wall run", new FixedMetadataValue(nmlAcrobatics, true));
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setFlySpeed(.04f);
            EnergyManager.pauseEnergyRegen(player);
            Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Wall Run"));
        }
    }

    public static void stopWallRunning(Player player) {
        EnergyManager.resumeEnergyRegen(player);
        player.removeMetadata("wall run", nmlAcrobatics);
        wallRunSpeeds.remove(player.getUniqueId());
        wallRunTimes.remove(player.getUniqueId());

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
        } else {
            player.setFlying(false);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                wallCardinals.remove(player.getUniqueId());
                player.setFlySpeed(.1f);
            }
        }.runTaskLater(nmlAcrobatics, 1);
    }

    public static void wallJump(Player player) {
        double speed = wallRunSpeeds.get(player.getUniqueId());
        Vector reverseWallCardinal = reverseWallCardinal(wallCardinals.get(player.getUniqueId()));
        Vector wallJump;

        // vector changes depending on if the player is looking at the wall before jumping
        if (getTopBlock(player, 1).getType().isAir()) { // looking away from wall
            wallJump = player.getVelocity().add(new Vector(.5, 0, .5).multiply(reverseWallCardinal)).setY(.75);
        } else { // looking at wall
            wallJump = new Vector(0, .5, 0);
        }

        stopWallRunning(player);
        player.setVelocity(wallJump);
        EnergyManager.resumeEnergyRegen(player);
        EnergyManager.useEnergy(player, 5);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Wall Jump"));

        // slightly different from the postJumpRunnable();
        new BukkitRunnable() {
            boolean canceled = false;
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20) {
                        player.removeMetadata("long jump", nmlAcrobatics);
                        canceled = true;
                        cancel();
                    }
                }
                else if (player.isInWater() || player.isUnderWater()) {
                    cancel();
                }
                else { // wall running logic
                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

                    if (acrobaticsLvl >= 30) {
                        Vector playerDir = player.getLocation().getDirection().clone().setY(0);

                        if (playerDir.lengthSquared() < 1e-6) {
                            double yawRad = Math.toRadians(player.getLocation().getYaw());
                            playerDir = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
                        }

                        Vector forward = playerDir.clone().normalize().multiply(1.2);
                        Vector right = playerDir.clone().crossProduct(new Vector(0, 1, 0)).setY(0).normalize();
                        Vector left = right.clone().multiply(-1);
                        Location leftLoc = player.getLocation().clone().add(left);
                        Location forwardLoc = player.getLocation().clone().add(forward);
                        Location rightLoc = player.getLocation().clone().add(right);

                        Block leftTop = leftLoc.clone().add(0, 1, 0).getBlock();
                        Block leftBottom = leftLoc.getBlock();
                        Block forwardTop = forwardLoc.clone().add(0, 1, 0).getBlock();
                        Block forwardBottom = forwardLoc.getBlock();
                        Block rightTop = rightLoc.clone().add(0, 1, 0).getBlock();
                        Block rightBottom = rightLoc.getBlock();

                        if (!leftTop.getType().isAir() && !leftBottom.getType().isAir()) {
                            startWallRun(player, snapToCardinal(left), speed);
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                        else if (!forwardTop.getType().isAir() && !forwardBottom.getType().isAir()) {
                            startWallRun(player, snapToCardinal(forward), speed);
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                        else if (!rightTop.getType().isAir() && !rightBottom.getType().isAir()) {
                            startWallRun(player, snapToCardinal(right), speed);
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 10, 1);
    }

    private static Block getTopBlock(Player player, double distance) {
        Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(distance);
        Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

        return topLocation.getBlock();
    }

    private static Vector getClosestWallCardinal(Player player) {
        int radius = 2;
        Location head = player.getLocation().add(0, 1.25, 0);

        for (int dist = 0; dist <= radius; dist++) {
            Location north = head.clone().add(0, 0, -dist);
            Block northBlock = head.clone().add(0, 0, -dist).getBlock();

            Location south = head.clone().add(0, 0, dist);
            Block southBlock = south.getBlock();

            Location east = head.clone().add(dist, 0, 0);
            Block eastBlock = east.getBlock();

            Location west = head.clone().add(-dist, 0, 0);
            Block westBlock = west.getBlock();

            if (!northBlock.getType().isAir() && hasNeighboringBlocks(north)) return new Vector(0, 0, -1);
            if (!southBlock.getType().isAir() && hasNeighboringBlocks(south)) return new Vector(0, 0, 1);
            if (!eastBlock.getType().isAir() && hasNeighboringBlocks(east)) return new Vector(1, 0, 0);
            if (!westBlock.getType().isAir() && hasNeighboringBlocks(west)) return new Vector(-1, 0, 0);
        }

        return null; // no wall nearby
    }

    private static Vector reverseWallCardinal(Vector vector) {
        if (Objects.equals(vector, new Vector(0, 0, 1))) return new Vector(0, 0, -1);
        if (Objects.equals(vector, new Vector(0, 0, -1))) return new Vector(0, 0, 1);
        if (Objects.equals(vector, new Vector(1, 0, 0))) return new Vector(-1, 0, 0);
        if (Objects.equals(vector, new Vector(-1, 0, 0))) return new Vector(1, 0, 0);

        return new Vector();
    }

    private static Vector snapToCardinal(Vector vec) {
        if (Math.abs(vec.getX()) > Math.abs(vec.getZ())) {
            return vec.getX() > 0 ? new Vector(1, 0, 0) : new Vector(-1, 0, 0);
        } else {
            return vec.getZ() > 0 ? new Vector(0, 0, 1) : new Vector(0, 0, -1);
        }
    }

    private static boolean hasNeighboringBlocks(Location loc) {
        ArrayList<Block> blocks = new ArrayList<>();
        World world = loc.getWorld();

        for (double x = -1.5; x <= 1.5; x += .5) {
            for (double z = -1.5; z <= 1.5; z += .5) {
                if (x * x + z * z <= 1.5 * 1.5) { // circle check
                    blocks.add(world.getBlockAt(loc.getBlockX() + (int) Math.round(x), loc.getBlockY(), loc.getBlockZ() + (int) Math.round(z)));
                }
            }
        }

        blocks.removeIf(block -> block.getType().isAir());

        return !blocks.isEmpty();
    }
}
