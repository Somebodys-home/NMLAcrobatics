package io.github.NoOne.nMLAcrobatics;

import io.github.Gabriel.expertiseStylePlugin.AbilitySystem.CooldownSystem.CooldownManager;
import io.github.NoOne.nMLEnergySystem.EnergyManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Maneuvers {
    private static NMLAcrobatics nmlAcrobatics;
    private BukkitTask railGrindTask;
    private BukkitTask rollTask;
    private BukkitTask climbTask;
    private static final HashMap<UUID, Location> lastLocations = new HashMap<>();
    private static final HashMap<UUID, Location> currentLocations = new HashMap<>();
    private static final HashMap<UUID, Vector> railGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Double> railGrindSpeed = new HashMap<>();
    private static final HashMap<UUID, String> lastSuccessfulRailGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Location> lastDirectionChangeLocation = new HashMap<>();
    private static final HashMap<UUID, Location> startClimbBlockLocation = new HashMap<>();
    private static final HashMap<UUID, Vector> climbCardinals = new HashMap<>();

    public Maneuvers(NMLAcrobatics nmlAcrobatics) {
        Maneuvers.nmlAcrobatics = nmlAcrobatics;
    }

    public void startRollTask() {
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

    public void startRailGrindTask() {
        railGrindTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (player.hasMetadata("rail grind")) {
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);

                        if (isGrindable(below)) {
                            String priorDirection = lastSuccessfulRailGrindDirection.get(uuid);
                            double speed = railGrindSpeed.get(uuid);
                            boolean ableToSwitchDirection = true;
                            String newDirection = null;
                            Vector velocity = null;

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

                        } else {
                            player.removeMetadata("rail grind", nmlAcrobatics);
                        }
                    } else if (!player.hasMetadata("rail grind")) {
                        lastSuccessfulRailGrindDirection.remove(uuid);
                        lastDirectionChangeLocation.remove(uuid);
                        railGrindSpeed.remove(uuid);
                        railGrindDirection.remove(uuid);
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public void startClimbTask() {
        climbTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("climb")) {
                        UUID uuid = player.getUniqueId();
                        Vector climbCardinal = climbCardinals.get(uuid);

                        if (climbCardinal == null) continue;

                        Location startingLocation = startClimbBlockLocation.get(uuid);
                        Location front = player.getLocation().add(climbCardinal).add(0, 1, 0);
                        Block frontBlock = front.getBlock();
                        Location aboveFront = player.getLocation().add(climbCardinal).add(0, 1, 0);
                        Block aboveFrontBlock = aboveFront.clone().add(0, 1, 0).getBlock();
                        Location playerLocation = player.getLocation();
                        Vector offset = playerLocation.toVector().subtract(startingLocation.toVector()); // Vector from wall to player
                        double perpendicularDistance = offset.dot(climbCardinal);

                        offset.setY(0); // (horizontal only)

                        // landing / running off
                        if (player.isOnGround() || frontBlock.getType().isAir()) {
                            stopClimbing(player);
                        }

                        // wall lock
                        if (Math.abs(perpendicularDistance) >= .8) {
                            Vector pushBack = climbCardinal.clone().multiply(-perpendicularDistance * 0.05);
                            player.setVelocity(player.getVelocity().add(pushBack));
                        }

                        // pull-up
                        if (frontBlock.getType().isSolid() && aboveFrontBlock.getType().isAir()) {
                            stopClimbing(player);
                            player.setVelocity(new Vector(0, 0.66, 0).add(climbCardinal.multiply(0.2)));
                            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.setVelocity(player.getVelocity().add(climbCardinal.multiply(.3)));
                                }
                            }.runTaskLater(nmlAcrobatics, 5);
                        }

                        // wall running or not
                        if (player.isSprinting()) {
                            player.setFlySpeed(0.05f);
                        } else {
                            player.setFlySpeed(0.025f);
                        }

                        if (tickCounter % 20 == 0) EnergyManager.useEnergy(player, 5); // use energy every second
                    }
                }

                if (tickCounter % 20 == 0) tickCounter = 0;
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public void stopTasks() {
        railGrindTask.cancel();
        rollTask.cancel();
        climbTask.cancel();
    }

    public static void roll(Player player) {
        UUID id = player.getUniqueId();
        Location last = lastLocations.get(id);
        Location now = currentLocations.get(id);

        if (last == null || now == null) return;

        Vector movement = now.toVector().subtract(last.toVector());

        if (movement.lengthSquared() > 0.01) {
            Vector roll = movement.normalize().multiply(2);

            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            EnergyManager.useEnergy(player, 5);
            CooldownManager.putAllAbilitiesOnCooldown(player, 1.5);
            player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));
            player.setVelocity(roll);

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("roll cooldown", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 30);
        }
    }

    public static void rollBrace(Player player) {
        Vector roll = player.getLocation().getDirection().normalize().multiply(3);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    public static void longJump(Player player) {
        Vector longJump = player.getLocation().getDirection().normalize().multiply(1.2).setY(.5);
        double speed = longJump.length();

        player.setVelocity(longJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                        isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, speed);
                        player.removeMetadata("longjump", nmlAcrobatics);
                        cancel();
                    }
                }
                else { // climbing logic
                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();
                    Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
                    Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        Vector wallNormal = player.getLocation().getDirection().setY(0).normalize();

                        // snap to cardinal direction
                        if (Math.abs(wallNormal.getX()) > Math.abs(wallNormal.getZ())) {
                            wallNormal.setX(wallNormal.getX() > 0 ? 1 : -1);
                            wallNormal.setZ(0);
                        } else {
                            wallNormal.setZ(wallNormal.getZ() > 0 ? 1 : -1);
                            wallNormal.setX(0);
                        }

                        player.setVelocity(new Vector());
                        setStartClimbBlockLocation(player, topLocation); // top or bottom location doesn't matter
                        setClimbCardinal(player, wallNormal);
                        climb(player, false);
                        player.removeMetadata("longjump", nmlAcrobatics);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void railJump(Player player, double speed) {
        Vector railJump = player.getLocation().getDirection().normalize().multiply(1.5).setY(.5);

        if (speed < 1) speed = 1;

        railJump.multiply(speed);
        player.setVelocity(railJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));
        player.removeMetadata("rail grind", nmlAcrobatics);

        double finalSpeed = speed;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                            isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, finalSpeed);
                        player.removeMetadata("longjump", nmlAcrobatics);
                        cancel();
                    }
                }
                else { // climbing logic
                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();
                    Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
                    Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        Vector wallNormal = player.getLocation().getDirection().setY(0).normalize();

                        // snap to cardinal direction
                        if (Math.abs(wallNormal.getX()) > Math.abs(wallNormal.getZ())) {
                            wallNormal.setX(wallNormal.getX() > 0 ? 1 : -1);
                            wallNormal.setZ(0);
                        } else {
                            wallNormal.setZ(wallNormal.getZ() > 0 ? 1 : -1);
                            wallNormal.setX(0);
                        }

                        player.setVelocity(new Vector());
                        setStartClimbBlockLocation(player, topLocation);
                        setClimbCardinal(player, wallNormal);
                        climb(player, false);
                        player.removeMetadata("longjump", nmlAcrobatics);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 5, 1);
    }

    private static void railGrind(Player player, double speed) {
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
        railGrindDirection.put(player.getUniqueId(), player.getLocation().getDirection());
        railGrindSpeed.put(player.getUniqueId(), speed);

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
    }

    public static void climb(Player player, boolean headStart) {
        int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();
        Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
        Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

        if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
            Vector wallCardinal = player.getLocation().getDirection().setY(0).normalize();

            // snap to cardinal direction
            if (Math.abs(wallCardinal.getX()) > Math.abs(wallCardinal.getZ())) {
                wallCardinal.setX(wallCardinal.getX() > 0 ? 1 : -1);
                wallCardinal.setZ(0);
            } else {
                wallCardinal.setZ(wallCardinal.getZ() > 0 ? 1 : -1);
                wallCardinal.setX(0);
            }

            setStartClimbBlockLocation(player, topLocation);
            setClimbCardinal(player, wallCardinal);
        }

        player.setMetadata("climb", new FixedMetadataValue(nmlAcrobatics, true));
        player.setAllowFlight(true);
        player.setFlying(true);

        if (headStart) player.setVelocity(player.getVelocity().add(new Vector(0 , .2, 0)));
    }

    public static void stopClimbing(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
        } else {
            player.setFlying(false);
        }

        player.removeMetadata("climb", nmlAcrobatics);
        startClimbBlockLocation.remove(player.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                climbCardinals.remove(player.getUniqueId());
                player.setFlySpeed(.1f);
            }
        }.runTaskLater(nmlAcrobatics, 1);
    }

    public static void wallJump(Player player) {
        Vector climbCardinal = climbCardinals.get(player.getUniqueId());
        Vector wallJump = player.getLocation().toVector().multiply(climbCardinal).multiply(-.00012).setY(.65);
        double speed = wallJump.length();

        stopClimbing(player);
        player.setVelocity(wallJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            boolean canceled = false;
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                            isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, speed);
                        player.removeMetadata("longjump", nmlAcrobatics);
                        canceled = true;
                        cancel();
                    }
                }
                else { // climbing logic
                    new BukkitRunnable() { // have to delay it a little bit so the data can clear from the existing wall
                        @Override
                        public void run() {
                            int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();
                            Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
                            Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

                            if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                                Vector wallCardinal = climbCardinals.get(player.getUniqueId());

                                player.setVelocity(new Vector());
                                setStartClimbBlockLocation(player, topLocation);
                                setClimbCardinal(player, wallCardinal);
                                climb(player, false);
                                player.removeMetadata("longjump", nmlAcrobatics);
                                canceled = true;
                                cancel();
                            }
                        }
                    }.runTaskLater(nmlAcrobatics, 3);
                }

                if (canceled) cancel();
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
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

    private static void centerPlayer(Player player) {
        Location loc = player.getLocation();
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();

        loc.setX(loc.getBlockX() + 0.5);
        loc.setY(loc.getBlockY()); // keep current Y
        loc.setZ(loc.getBlockZ() + 0.5);
        loc.setYaw(yaw);   // keep facing the same way
        loc.setPitch(pitch);

        player.teleport(loc.add(0, .5, 0));
    }

    public static void setStartClimbBlockLocation(Player player, Location location) {
        startClimbBlockLocation.put(player.getUniqueId(), location);
    }

    public static void setClimbCardinal(Player player, Vector vector) {
        climbCardinals.put(player.getUniqueId(), vector);
    }

    public static Block getBottomBlock(Player player) {
        Vector bottom = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
        Location bottomLocation = player.getLocation().clone().add(bottom).add(0, .75, 0);

        return bottomLocation.getBlock();
    }

    public static Block getTopBlock(Player player) {
        Vector top = player.getLocation().getDirection().setY(0).normalize().multiply(0.5);
        Location topLocation = player.getLocation().clone().add(top).add(0, 1.75, 0);

        return topLocation.getBlock();
    }
}
