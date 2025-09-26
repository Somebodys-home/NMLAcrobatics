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
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final HashMap<UUID, Integer> railGrindSoundTicks = new HashMap<>();
    private static final HashMap<UUID, String> lastSuccessfulRailGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Location> lastDirectionChangeLocation = new HashMap<>();
    private static final HashMap<UUID, Vector> climbCardinals = new HashMap<>();

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
                ArrayList<UUID> soundUUIDs = new ArrayList<>();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasMetadata("rail grind")) {
                        soundUUIDs.add(player.getUniqueId());
                    }
                }

                if (soundUUIDs.isEmpty()) soundTicks = 31;
                // ^^ sound ^^

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (player.hasMetadata("rail grind")) {
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);

                        if (isGrindable(below)) {
                            int soundTicks = railGrindSoundTicks.getOrDefault(uuid, 0) + 1;
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

                            // sound
                            if (soundTicks == 30) {
                                player.playSound(player, Sound.ENTITY_MINECART_RIDING, 1f, 1f);
                                soundTicks = 0;
                            }

                            // particle helper
                            Location particleLocation = calculateRailJumpLandingPosition(player);
                            Particle.DustOptions green = new Particle.DustOptions(Color.FUCHSIA, 1.0F);

                            if (!particleLocation.getBlock().getType().isAir()) {
                                List<Location> particleWireFrame = getHollowCube(particleLocation.clone().add(.5, .5, .5),
                                        particleLocation.clone().add(-.5, -.5, -.5), .25);

                                for (Location location : particleWireFrame) {
                                    player.getWorld().spawnParticle(Particle.DUST, location, 1, 0, 0, 0, green);
                                }
                            } else {
                                player.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, 0, 0, 0, green);
                            }

                            railGrindSoundTicks.put(uuid, soundTicks);
                        } else {
                            player.removeMetadata("rail grind", nmlAcrobatics);
                            player.stopSound(Sound.ENTITY_MINECART_RIDING);
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

    public void climbTask() {
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

                            Vector finalClimbCardinal = climbCardinal;

                            stopClimbing(player);
                            player.setVelocity(new Vector(0, 0.66, 0).add(climbCardinal.multiply(0.2)));
                            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);

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
        player.stopSound(Sound.ENTITY_MINECART_RIDING);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));

        postJumpRunnable(player, speed).runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void railJump(Player player, double speed) {
        Vector railJump = player.getLocation().getDirection().normalize().multiply(1.5).setY(.5);

        if (speed < 1) speed = 1;

        railJump.multiply(speed);
        player.setVelocity(railJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.stopSound(Sound.ENTITY_MINECART_RIDING);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));
        player.removeMetadata("rail grind", nmlAcrobatics);
        postJumpRunnable(player, speed).runTaskTimer(nmlAcrobatics, 5, 1);
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
        railGrindSoundTicks.put(player.getUniqueId(), 29);
    }

    public static void climb(Player player, boolean tinyJump) {
        int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

        if (getBottomBlock(player).isSolid() && getTopBlock(player).isSolid() && acrobaticsLvl >= 30) {
            setClimbCardinal(player, getClosestWallCardinal(player));
            player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
        }

        player.setMetadata("climb", new FixedMetadataValue(nmlAcrobatics, true));
        player.setAllowFlight(true);
        player.setFlying(true);

        if (tinyJump) player.setVelocity(player.getVelocity().add(new Vector(0 , .2, 0)));
    }

    public static void stopClimbing(Player player) {
        player.removeMetadata("climb", nmlAcrobatics);

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setAllowFlight(false);
        } else {
            player.setFlying(false);
        }

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
        if (getTopBlock(player, 1).getType().isAir()) { // looking away from wall
            wallJump = player.getLocation().getDirection().multiply(.75).setY(.65);
        } else { // looking at wall
            wallJump = player.getLocation().toVector().multiply(climbCardinal).multiply(-.00012).setY(.65);
        }

        double speed = wallJump.length();
        stopClimbing(player);
        player.setVelocity(wallJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("long jump", new FixedMetadataValue(nmlAcrobatics, true));

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
                else { // climbing logic
                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        setClimbCardinal(player, getClosestWallCardinal(player));
                        player.setVelocity(new Vector());
                        climb(player, false);
                        player.removeMetadata("long jump", nmlAcrobatics);
                        canceled = true;
                        cancel();
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 3, 1);
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

    public List<Location> getHollowCube(Location corner1, Location corner2, double particleDistance) {
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

    private static BukkitRunnable postJumpRunnable(Player player, double speed) {
         return new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel() >= 20 &&
                            isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {

                        railGrind(player, speed);
                        player.removeMetadata("long jump", nmlAcrobatics);

                        cancel();
                    }
                }
                else { // climbing logic
                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

                    if (!getBottomBlock(player).getType().isAir() && !getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
                        player.setVelocity(new Vector());
                        setClimbCardinal(player, getClosestWallCardinal(player));
                        climb(player, false);
                        player.removeMetadata("long jump", nmlAcrobatics);
                        cancel();
                    }
                }
            }
        };
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

    public static Block getTopBlock(Player player, double distance) {
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

    // i gave up halfway through and just used AI to debug this im sorry but its 2:27 AM
    // i'm not as strong a man when i dont have enough sleep and i just wanted to get this done
    // i have a broken ukulele if that's whatchu want outta me
    public static Location calculateRailJumpLandingPosition(Player player) {
        Location startLoc = player.getLocation().clone();
        double startY = startLoc.getY();                 // plane we want to land on (player's feet Y)
        Location simLoc = startLoc.clone().add(0, player.getEyeHeight() * 0.75, 0); // start a little above feet (same as you had)

        // EXACT same initial velocity expression you use to launch the real player
        Vector v = player.getLocation().getDirection().clone().multiply(1.5);
        v.setY(0.5);

        World world = player.getWorld();

        final double g = 0.08;    // gravity per tick (same as before)
        final double d = 0.93;    // drag per tick (same as before)
        final int MAX_TICKS = 1000;
        final double EPS = 1e-9;

        double v0x = v.getX();
        double v0y = v.getY();
        double v0z = v.getZ();

        // iterate ticks (but use closed-form sums so we don't accumulate integration error)
        for (int n = 0; n < MAX_TICKS; n++) {
            double dPowN = Math.pow(d, n);                 // d^n
            double oneMinusDPowN = 1.0 - dPowN;
            double denom = 1.0 - d;                        // (1 - d), safe because d != 1

            // horizontal displacement after n whole ticks: v0x * (1 - d^n) / (1 - d)
            double sumX = (denom == 0.0) ? v0x * n : v0x * (oneMinusDPowN / denom);
            double sumZ = (denom == 0.0) ? v0z * n : v0z * (oneMinusDPowN / denom);

            // vertical displacement after n whole ticks (closed form)
            // term1 = v0y * (1 - d^n) / (1 - d)
            double term1 = (denom == 0.0) ? v0y * n : v0y * (oneMinusDPowN / denom);
            // term2 = (g * d / (1 - d)) * ( n - (1 - d^n) / (1 - d) )
            double term2 = (g * d / denom) * ( n - (oneMinusDPowN / denom) );
            double deltaY = term1 - term2;
            double yAfterN = simLoc.getY() + deltaY;

            // velocity at start of tick n (the velocity that will move the entity during tick n)
            double vAtN_y = v0y * dPowN - (g * d * (1.0 - dPowN) / (1.0 - d));
            // horizontal velocities at start of tick n
            double vAtN_x = v0x * dPowN;
            double vAtN_z = v0z * dPowN;

            // check if during this tick the Y crosses the landing plane:
            // movement during this tick is yAfterN -> yAfterN + vAtN_y
            if (yAfterN + vAtN_y <= startY + EPS) {
                // fraction along this tick where y == startY
                double t;
                if (Math.abs(vAtN_y) < EPS) {
                    t = 0.0;
                } else {
                    t = (startY - yAfterN) / vAtN_y;
                    if (Double.isNaN(t) || Double.isInfinite(t)) t = 0.0;
                }
                t = Math.max(0.0, Math.min(1.0, t));

                // X and Z at crossing:
                double x = simLoc.getX() + sumX + t * vAtN_x;
                double z = simLoc.getZ() + sumZ + t * vAtN_z;

                return new Location(world, x, startY - 1, z); // keep your -1 exactly as requested
            }
        }

        // fallback: didn't cross within MAX_TICKS (shouldn't happen); return last simLoc horizontally
        return new Location(world, simLoc.getX(), startY - 1, simLoc.getZ());
    }


}
