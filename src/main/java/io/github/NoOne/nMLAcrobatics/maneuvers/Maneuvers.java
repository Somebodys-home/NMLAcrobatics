package io.github.NoOne.nMLAcrobatics.maneuvers;

import io.github.NoOne.nMLAbilities.abilitySystem.cooldown.CooldownManager;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLEnergySystem.EnergyManager;
import io.github.NoOne.nMLPlayerStats.statSystem.Stats;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class Maneuvers {
    private static NMLAcrobatics nmlAcrobatics;
    private BukkitTask railGrindTask;
    private BukkitTask rollTask;
    private BukkitTask wallRunTask;
    private static final HashMap<UUID, Location> lastLocations = new HashMap<>(); // what
    private static final HashMap<UUID, Location> currentLocations = new HashMap<>();
    private static final HashMap<UUID, Double> railGrindSpeed = new HashMap<>();
    private static final HashMap<UUID, Integer> railGrindSoundTicks = new HashMap<>();
    private static final HashMap<UUID, String> lastSuccessfulRailGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Location> lastDirectionChangeLocation = new HashMap<>();
    private static final HashMap<UUID, Location> railGrindParticleLocation = new HashMap<>();
    private static final HashMap<UUID, Vector> wallCardinals = new HashMap<>(); // only to know where to push the player back to when on a wall
    private static final HashMap<UUID, Double> wallRunSpeeds = new HashMap<>(); // speed that you start wall running at
    private static final HashMap<UUID, Integer> wallRunTimes = new HashMap<>(); // the amount of time that you spend wall running

    public Maneuvers(NMLAcrobatics nmlAcrobatics) {
        Maneuvers.nmlAcrobatics = nmlAcrobatics;
    }

    public void rollTask() {
        rollTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID id = player.getUniqueId();

                    if (currentLocations.containsKey(id)) lastLocations.put(id, currentLocations.get(id));

                    currentLocations.put(id, player.getLocation().clone());
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 2);
    }

    public void railGrindTask() {
        railGrindTask = new BukkitRunnable() {
            int soundTicks = 0;

            @Override
            public void run() {
                soundTicks++;

                /// sound
                ArrayList<UUID> soundUUIDs = new ArrayList<>();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("rail grind")) {
                        soundUUIDs.add(player.getUniqueId());
                    }
                }

                if (soundUUIDs.isEmpty()) {
                    soundTicks = 31;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("rail grind")) {
                        UUID uuid = player.getUniqueId();
                        Stats stats = nmlAcrobatics.getProfileManager().getPlayerProfile(uuid).getStats();
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);

                        if (isGrindable(below)) {
                            int soundTicks = railGrindSoundTicks.getOrDefault(uuid, 0) + 1;
                            String priorDirection = lastSuccessfulRailGrindDirection.get(uuid);
                            double speed = railGrindSpeed.get(uuid);
                            boolean ableToSwitchDirection = true;
                            String newDirection = null;
                            Vector velocity = null;
                            double speedMultiplier = stats.getSpeed() / 100.0;
                            Location railJumpLandingPosition = calculateRailJumpLandingPosition(player, speedMultiplier);
                            Particle.DustOptions particleDust;

                            if (speedMultiplier < 1) {
                                particleDust = new Particle.DustOptions(Color.RED, 1.0F);
                            } else if (speedMultiplier < 1.3) {
                                particleDust = new Particle.DustOptions(Color.FUCHSIA, 1.0F);
                            } else {
                                particleDust = new Particle.DustOptions(Color.AQUA, 1.0F);
                            }

                            Block base = player.getLocation().getBlock();
                            Block northBlock = base.getRelative(BlockFace.NORTH).getRelative(BlockFace.DOWN);
                            Block eastBlock  = base.getRelative(BlockFace.EAST).getRelative(BlockFace.DOWN);
                            Block southBlock = base.getRelative(BlockFace.SOUTH).getRelative(BlockFace.DOWN);
                            Block westBlock  = base.getRelative(BlockFace.WEST).getRelative(BlockFace.DOWN);

                            // boolean for if the player is more than 1 block away from the block where they last switched directions
                            if (lastDirectionChangeLocation.get(uuid) != null && lastDirectionChangeLocation.get(uuid).distance(player.getLocation().getBlock().getLocation()) < 1) {
                                ableToSwitchDirection = false;
                            }

                            // what direction to grind in / changing directions
                            if (isGrindable(northBlock) && !"south".equals(priorDirection) && ableToSwitchDirection) {
                                newDirection = "north";
                                velocity = new Vector(0, 0, -0.5);
                            }
                            else if (isGrindable(eastBlock) && !"west".equals(priorDirection) && ableToSwitchDirection) {
                                newDirection = "east";
                                velocity = new Vector(0.5, 0, 0);
                            }
                            else if (isGrindable(southBlock) && !"north".equals(priorDirection) && ableToSwitchDirection) {
                                newDirection = "south";
                                velocity = new Vector(0, 0, 0.5);
                            }
                            else if (isGrindable(westBlock) && !"east".equals(priorDirection) && ableToSwitchDirection) {
                                newDirection = "west";
                                velocity = new Vector(-0.5, 0, 0);
                            }

                            // if we've changed directions, save it
                            if (newDirection != null && !newDirection.equals(priorDirection)) {
                                centerPlayer(player); // center the player
                                lastSuccessfulRailGrindDirection.put(uuid, newDirection); // save the direction we're now going in
                                lastDirectionChangeLocation.put(uuid, player.getLocation().getBlock().getLocation()); // save the location of the block that we changed direction from
                            }

                            // apply velocity, multiplied by speed, to player
                            if (velocity != null) player.setVelocity(velocity.multiply(speed));

                            // sound
                            if (soundTicks == 30) {
                                player.playSound(player, Sound.ENTITY_MINECART_RIDING, 1f, 1f);
                                soundTicks = 0;
                            }

                            // particle helper
                            if (!railJumpLandingPosition.getBlock().getType().isAir()) {
                                List<Location> particleWireFrame = createHollowCube(
                                        railJumpLandingPosition.clone().add(.5, .5, .5),
                                        railJumpLandingPosition.clone().add(-.5, -.5, -.5),
                                        .25
                                );

                                for (Location location : particleWireFrame) {
                                    player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, particleDust);
                                }
                            } else {
                                player.getWorld().spawnParticle(Particle.DUST, railJumpLandingPosition, 1, 0, 0, 0, particleDust);
                            }

                            railGrindParticleLocation.put(uuid, railJumpLandingPosition);
                            railGrindSoundTicks.put(uuid, soundTicks);
                        } else {
                            stopRailGrinding(player);
                        }
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public void wallRunTask() {
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
        railGrindTask.cancel();
        rollTask.cancel();
        wallRunTask.cancel();
    }

    public static void roll(Player player) {
        UUID id = player.getUniqueId();
        Location last = lastLocations.get(id);
        Location now = currentLocations.get(id);

        if (last == null || now == null) return;

        Vector movement = now.toVector().subtract(last.toVector());

        if (movement.lengthSquared() > 0.01) {
            double speedMultiplier = nmlAcrobatics.getProfileManager().getPlayerProfile(player.getUniqueId()).getStats().getSpeed() / 100.0;
            Vector roll = movement.normalize().multiply(2).multiply(speedMultiplier);

            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            EnergyManager.useEnergy(player, 10);
            CooldownManager.putOnHardCooldown(player, 1.5);
            player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));
            player.setVelocity(roll);
            Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Roll"));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("roll cooldown", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 30);
        }
    }

    public static void rollBrace(Player player) {
        double speedMultiplier = nmlAcrobatics.getProfileManager().getPlayerProfile(player.getUniqueId()).getStats().getSpeed() / 100.0;
        Vector roll = player.getLocation().getDirection().normalize().multiply(3).multiply(speedMultiplier);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Roll Brace"));
        CooldownManager.putOnHardCooldown(player, 1.5);
        player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("roll cooldown", nmlAcrobatics);
            }
        }.runTaskLater(nmlAcrobatics, 30);
    }

    public static void longJump(Player player) {
        double speedMultiplier = nmlAcrobatics.getProfileManager().getPlayerProfile(player.getUniqueId()).getStats().getSpeed() / 100.0;
        Vector longJump = player.getLocation().getDirection().normalize().multiply(.9).multiply(speedMultiplier).setY(.5);
        double speed = longJump.length();

        player.setVelocity(longJump);
        EnergyManager.useEnergy(player, 15);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.stopSound(Sound.ENTITY_MINECART_RIDING);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Long Jump"));
        postJumpRunnable(player, speed).runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void railGrind(Player player, double speed) {
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
        railGrindSpeed.put(player.getUniqueId(), speed);
        player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);

        float yaw = player.getLocation().getYaw();
        yaw = (yaw % 360 + 360) % 360; // Normalize yaw to 0–360

        if (yaw >= 45 && yaw < 135) {
            lastSuccessfulRailGrindDirection.put(player.getUniqueId(), "west");
        } else if (yaw >= 135 && yaw < 225) {
            lastSuccessfulRailGrindDirection.put(player.getUniqueId(), "north");
        } else if (yaw >= 225 && yaw < 315) {
            lastSuccessfulRailGrindDirection.put(player.getUniqueId(), "east");
        } else {
            lastSuccessfulRailGrindDirection.put(player.getUniqueId(), "south");
        }

        centerPlayer(player);
        railGrindSoundTicks.put(player.getUniqueId(), 29);
        EnergyManager.pauseEnergyRegen(player);
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Rail Grind"));
        player.setMetadata("no jumping", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("no jumping", nmlAcrobatics);
            }
        }.runTaskLater(nmlAcrobatics, 1);
    }

    public static void stopRailGrinding(Player player) {
        UUID uuid = player.getUniqueId();
        ManeuverCombos maneuverCombos = nmlAcrobatics.getManeuverCombos();

        player.removeMetadata("rail grind", nmlAcrobatics);
        player.stopSound(Sound.ENTITY_MINECART_RIDING);
        lastSuccessfulRailGrindDirection.remove(uuid);
        lastDirectionChangeLocation.remove(uuid);
        railGrindSpeed.remove(uuid);
        maneuverCombos.startComboDepleteTask(player);
        EnergyManager.resumeEnergyRegen(player);
    }

    public static void railJump(Player player, double speed) {
        Location particleLocation = railGrindParticleLocation.get(player.getUniqueId());
        Vector railJump = particleLocation.subtract(player.getLocation()).toVector().multiply(.12).setY(.55);

        player.setVelocity(railJump.multiply(Math.max(1, speed)));
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.stopSound(Sound.ENTITY_MINECART_RIDING);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));
        player.removeMetadata("rail grind", nmlAcrobatics);
        postJumpRunnable(player, speed).runTaskTimer(nmlAcrobatics, 5, 1);
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Rail Jump"));
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
        double speedMultiplier = nmlAcrobatics.getProfileManager().getPlayerProfile(player.getUniqueId()).getStats().getSpeed() / 100.0;
        Vector wallJump;

        // vector changes depending on if the player is looking at the wall before jumping
        if (getTopBlock(player, 1).getType().isAir()) { // looking away from wall
            wallJump = player.getVelocity().add(new Vector(.5, 0, .5).multiply(reverseWallCardinal)).setY(.75);
        } else { // looking at wall
            wallJump = new Vector(0, .5, 0);
        }

        wallJump.multiply(speedMultiplier);
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
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                            isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, speed);
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

    private static void centerPlayer(Player player) {
        Location loc = player.getLocation();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        loc.setX(loc.getBlockX() + 0.5);
        loc.setY(loc.getBlockY()); // keep current Y
        loc.setZ(loc.getBlockZ() + 0.5);
        loc.setYaw(yaw); // keep facing the same way
        loc.setPitch(pitch);

        player.teleport(loc.add(0, .5, 0));
    }

    private static BukkitRunnable postJumpRunnable(Player player, double speed) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                            isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, speed);
                        player.removeMetadata("long jump", nmlAcrobatics);
                    }

                    cancel();
                }
                else { // climbing logic
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

                        if (leftTop.isSolid() && leftBottom.isSolid()) {
                            startWallRun(player, snapToCardinal(left));
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                        else if (forwardTop.isSolid() && forwardBottom.isSolid()) {
                            startWallRun(player, snapToCardinal(forward));
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                        else if (rightTop.isSolid() && rightBottom.isSolid()) {
                            startWallRun(player, snapToCardinal(right));
                            player.removeMetadata("long jump", nmlAcrobatics);
                            cancel();
                        }
                    }
                }
            }
        };
    }

    private static Block getTopBlock(Player player, double distance) {
        Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(distance);
        Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

        return topLocation.getBlock();
    }

    private static Location calculateRailJumpLandingPosition(Player player, double speedMultiplier) {
        Location playerLocation = player.getLocation();
        Vector playerDirection = playerLocation.getDirection();
        double y = playerLocation.getY() - 1;
        double pitch = playerLocation.getPitch();
        double multiplier = (((-1 / 540.0) * Math.pow(pitch, 2)) + ((1 / 90.0) * pitch) + 10) * speedMultiplier;

        playerLocation.add(playerDirection.multiply(multiplier)).setY(y);
        return playerLocation;
    }

    private static List<Location> createHollowCube(Location corner1, Location corner2, double particleDistance) {
        List<Location> result = new ArrayList<>();
        World world = corner1.getWorld();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        for (double x = minX; x <= maxX; x+=particleDistance) {
            for (double y = minY; y <= maxY; y+=particleDistance) {
                for (double z = minZ; z <= maxZ; z+=particleDistance) {
                    int components = 0;
                    if (x == minX || x == maxX) components++;
                    if (y == minY || y == maxY) components++;
                    if (z == minZ || z == maxZ) components++;
                    if (components >= 2) {
                        result.add(new Location(world, x, y, z));
                    }
                }
            }
        }

        return result;
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

    private static boolean isGrindable(Block block) {
        return block.getType() == Material.OAK_FENCE || block.getType() == Material.OAK_FENCE_GATE ||
                block.getType() == Material.SPRUCE_FENCE || block.getType() == Material.SPRUCE_FENCE_GATE ||
                block.getType() == Material.BIRCH_FENCE || block.getType() == Material.BIRCH_FENCE_GATE ||
                block.getType() == Material.JUNGLE_FENCE || block.getType() == Material.JUNGLE_FENCE_GATE ||
                block.getType() == Material.ACACIA_FENCE || block.getType() == Material.ACACIA_FENCE_GATE ||
                block.getType() == Material.DARK_OAK_FENCE || block.getType() == Material.DARK_OAK_FENCE_GATE ||
                block.getType() == Material.MANGROVE_FENCE || block.getType() == Material.MANGROVE_FENCE_GATE ||
                block.getType() == Material.CHERRY_FENCE || block.getType() == Material.CHERRY_FENCE_GATE ||
                block.getType() == Material.BAMBOO_FENCE || block.getType() == Material.BAMBOO_FENCE_GATE ||
                block.getType() == Material.CRIMSON_FENCE || block.getType() == Material.CRIMSON_FENCE_GATE ||
                block.getType() == Material.WARPED_FENCE || block.getType() == Material.WARPED_FENCE_GATE ||
                block.getType() == Material.NETHER_BRICK_FENCE || block.getType() == Material.IRON_BARS;
    }
}
