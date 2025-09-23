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

import java.util.ArrayList;
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
                        Location playerLocation = player.getLocation();
                        UUID uuid = player.getUniqueId();
                        Vector climbCardinal = getClosestWallCardinal(player);

                        if (climbCardinal == null) {
                            climbCardinal = climbCardinals.get(uuid);
                        }

                        climbCardinals.put(uuid, climbCardinal);

                        Location front = playerLocation.clone().add(climbCardinal).add(0, 1, 0);
                        Location aboveFront = playerLocation.clone().add(climbCardinal).add(0, 1, 0);
                        Block frontBlock = front.getBlock();
                        Block aboveFrontBlock = aboveFront.clone().add(0, 1, 0).getBlock();
                        Block moreAboveFrontBlock = aboveFront.clone().add(0, 2, 0).getBlock();

                        Location wallFaceLoc = frontBlock.getLocation().add(0.5, 0.5, 0.5).toVector().subtract(climbCardinal.clone().multiply(0.5))
                                                .toLocation(frontBlock.getWorld());
                        Vector toPlayer = playerLocation.toVector().subtract(wallFaceLoc.toVector());
                        double distanceFromWall = Math.abs(toPlayer.dot(climbCardinal));

                        // running off wall / landing on the ground stops player from climbing
                        if (player.isOnGround() || !hasNeighboringBlocks(wallFaceLoc)) stopClimbing(player);

                        // wall lock
                        if (!isFalling(player) && distanceFromWall >= .7) { // max distance
                            Vector pushBack = climbCardinal.clone().multiply(Math.pow(distanceFromWall, 2) / 25);
                            player.setVelocity(pushBack);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.setVelocity(player.getVelocity().add(pushBack));
                                }
                            }.runTaskLater(nmlAcrobatics, 1);
                        }

                        // pull-up
                        if (frontBlock.getType().isSolid() && (aboveFrontBlock.getType().isAir() || !aboveFrontBlock.isSolid()) &&
                            (moreAboveFrontBlock.getType().isAir() || !moreAboveFrontBlock.isSolid())) {

                            stopClimbing(player);
                            player.setVelocity(new Vector(0, 0.66, 0).add(climbCardinal.multiply(0.2)));
                            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

                            Vector finalClimbCardinal = climbCardinal;
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.setVelocity(player.getVelocity().add(finalClimbCardinal.multiply(.3)));
                                }
                            }.runTaskLater(nmlAcrobatics, 5);
                        }

                        // wall running or not
                        if (player.isSprinting()) {
                            player.setFlySpeed(0.04f);
                        } else {
                            player.setFlySpeed(0.025f);
                        }

                        if (tickCounter % 20 == 0) EnergyManager.useEnergy(player, 5);
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

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        player.setVelocity(new Vector());
                        setClimbCardinal(player, getClosestWallCardinal(player));
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

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        player.setVelocity(new Vector());
                        setClimbCardinal(player, getClosestWallCardinal(player));
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

        if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
            setClimbCardinal(player, getClosestWallCardinal(player));
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
        Vector wallJump;

        // vector changes depending on if the player is looking at the wall before jumping
        if (getTopBlock(player).getType().isAir()) {
            wallJump = player.getLocation().getDirection().multiply(.75).setY(.65);
        } else {
            wallJump = player.getLocation().toVector().multiply(climbCardinal).multiply(-.00012).setY(.65);
        }

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

                            if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                                player.setVelocity(new Vector());
                                setClimbCardinal(player, getClosestWallCardinal(player));
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

    public static boolean hasNeighboringBlocks(Location loc) {
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

    private static boolean isFalling(Player player) {
        return !player.isOnGround() && player.getVelocity().getY() < 0;
    }
}
