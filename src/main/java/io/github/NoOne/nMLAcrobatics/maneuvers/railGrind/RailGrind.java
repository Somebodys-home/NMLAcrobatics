package io.github.NoOne.nMLAcrobatics.maneuvers.railGrind;

import io.github.NoOne.nMLAbilities.abilitySystem.abilityEffects.AbilityEffectsHelper;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.Speedometer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RailGrind {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static HashMap<UUID, Vector> railGrindDirections = new HashMap<>(); // the direction that the player is grinding in
    private static HashMap<UUID, Double> railGrindSpeeds = new HashMap<>(); // the speed that the player is grinding at
    private static HashMap<UUID, Float> playerWalkSpeeds = new HashMap<>(); // set the players movement speed to 0 when rail grinding, so make sure to save the value it was before

    public static void startRailGrindTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // todo: make it so if you can turn but also can go straight, prioritize turning if the player is looking in that direction
                //  the plan for the wall check is to add another hashmap variable or metadata flag to store the players that just turned when rail grinding, that gets removed immediately on the next tick
                //  if they have that flag = they've turned on that last tick, then temporarily set their rail grind speed lower so they don't fall off from moving
                //  also go through the backup hit wall check (currently commented out)

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasMetadata("rail grind")) return;
                    if (!isGrindable(getBottomBlock(player))) { // player has fallen off the rail :T
                        stopRailGrind(player);
                        fellOffRailTask(player);

                        continue;
                    }

                    World world = player.getWorld();
                    Vector direction = railGrindDirections.get(player.getUniqueId());
                    BlockFace cardinal = convertVectorToCardinal(direction);

                    Block bottomBlock = getBottomBlock(player);
                    Block forwardsBottomBlock = bottomBlock.getRelative(cardinal);
                    Block forwardsBottomBottomBlock = forwardsBottomBlock.getRelative(BlockFace.DOWN);
                    Block forwardsBlock = forwardsBottomBlock.getRelative(BlockFace.UP);
                    boolean aboutToFall = !isGrindable(forwardsBottomBlock) && !isGrindable(forwardsBottomBottomBlock);
                    boolean aboutToHitWall = getIncomingWall(player, cardinal).isSolid();

                    // setting the player's velocity
                    player.setVelocity(getRailGrindVelocity(player));

                    // turning logic
                    if (aboutToHitWall || aboutToFall) {
                        BlockFace priorityCardinal = null;

                        if (player.getFacing().equals(getLeftCardinal(cardinal))) {
                            priorityCardinal = getLeftCardinal(cardinal);
                        } else if (player.getFacing().equals(getRightCardinal(cardinal))) {
                            priorityCardinal = getRightCardinal(cardinal);
                        }

                        // prioritize checking if the player can turn in the direction they're looking
                        if (priorityCardinal != null) {

                            // the block that the player would be going on in the direction they're looking is grindable
                            // crank that shit sojaboy
                            if (isTurningDirectionValid(player, priorityCardinal)) {
                                turnPlayer(player, priorityCardinal);
                            } else { // if not, repeat that same check to the opposite cardinal block
                                BlockFace oppositeCardinal = getOppositeCardinal(priorityCardinal);

                                if (isTurningDirectionValid(player, oppositeCardinal)) {
                                    turnPlayer(player, oppositeCardinal);
                                }
                            }
                        } else { // if the player isn't looking in a valid direction, just check from right to left
                            BlockFace leftCardinal = getLeftCardinal(cardinal);
                            BlockFace rightCardinal = getRightCardinal(cardinal);

                            if (isTurningDirectionValid(player, rightCardinal)) {
                                turnPlayer(player, rightCardinal);
                            } else if (isTurningDirectionValid(player, leftCardinal)) {
                                turnPlayer(player, leftCardinal);
                            }
                        }
                    }

                    // repeat the about to hit a wall check cuz we potentially just changed directions
//                    aboutToHitWall = getIncomingWall(player, cardinal).isSolid();
//
//                    if (aboutToHitWall) {
//                        stopRailGrind(player);
//                    }

                    // scaling logic
                    if (forwardsBlock.isSolid()) {
                        if (isGrindable(forwardsBlock)) { // if you hit a block at your feet that is grindable, then go on top of it
                            centerPlayerOnBlock(player, forwardsBlock);
                        } else { // if it isn't grindable, then you stop there
                            stopRailGrind(player);
                        }
                    } else if (!isGrindable(forwardsBottomBlock) && isGrindable(forwardsBottomBottomBlock)) { // if youre gonna fall on a grindable block, go on top of it
                        centerPlayerOnBlock(player, forwardsBottomBottomBlock);
                    }

                    // rail jump indicator
                    Location landingLocation = getRailJumpLandingLocation(player);

                    if (isGrindable(world.getBlockAt(landingLocation))) {
                        AbilityEffectsHelper.makeWireFrameRectangle(landingLocation, .5, .5, .5);
                    } else {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, landingLocation, 1, 0, 0, 0, 0);
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startRailGrind(Player player) {
        UUID uuid = player.getUniqueId();

        player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));

        centerPlayerOnBlock(player);
        railGrindDirections.put(uuid, getCardinalDirection(player));
        playerWalkSpeeds.put(uuid, player.getWalkSpeed());
        railGrindSpeeds.put(uuid, Speedometer.getSpeed(player));
        player.setWalkSpeed(0);
    }

    public static void stopRailGrind(Player player) {
        player.removeMetadata("rail grind", nmlAcrobatics);
        player.setWalkSpeed(playerWalkSpeeds.get(player.getUniqueId()));
    }

    // when the player falls off a rail, they can continue a rail grind if they land on a rail
    private static void fellOffRailTask(Player player) {
        player.setVelocity(getRailGrindVelocity(player));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    if (isGrindable(getBottomBlock(player))) {
                        startRailGrind(player);
                    }

                    cancel();
                }
            }
        }.runTaskTimer(nmlAcrobatics, 1, 1);
    }

    private static void centerPlayerOnBlock(Player player) {
        Location location = player.getLocation();

        location.setX(location.getBlockX() + .5);
        location.setY(location.getBlockY() + .5);
        location.setZ(location.getBlockZ() + .5);
        player.teleport(location);
    }

    private static void centerPlayerOnBlock(Player player, Block block) {
        Location location = block.getLocation();

        location.setX(location.getBlockX() + .5);
        location.setY(location.getBlockY() + 1.5);
        location.setZ(location.getBlockZ() + .5);
        location.setRotation(player.getYaw(), player.getPitch());
        player.teleport(location);
    }

    private static void turnPlayer(Player player, BlockFace newDirection) {
        centerPlayerOnBlock(player);
        railGrindDirections.put(player.getUniqueId(), getCardinalDirection(newDirection));
    }

    public static boolean isGrindable(Block block) {
        String blockType = block.getType().name();

        return blockType.contains("FENCE") || blockType.contains("GATE") || blockType.contains("BARS") || blockType.contains("WALL");
    }

    public static boolean isTurningDirectionValid(Player player, BlockFace cardinal) {
        Block bottomBlock = getBottomBlock(player);
        Block frontBottomBlock = bottomBlock.getRelative(cardinal);
        Block frontBottomTopBlock = frontBottomBlock.getRelative(BlockFace.UP);
        Block frontBottomBottomBlock = frontBottomBlock.getRelative(BlockFace.DOWN);
        Block incomingWall = getIncomingWall(player, cardinal);

        return (isGrindable(frontBottomBlock) || isGrindable(frontBottomBottomBlock) || isGrindable(frontBottomTopBlock)) && !incomingWall.isSolid();
    }

    private static Vector getCardinalDirection(Player player) {
        return player.getFacing().getDirection();
    }

    private static Vector getCardinalDirection(BlockFace blockFace) {
        return blockFace.getDirection();
    }

    private static Vector getRailGrindVelocity(Player player) {
        return railGrindDirections.get(player.getUniqueId()).clone().multiply(.7);
    }

    public static Block getBottomBlock(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }

    public static Block getIncomingWall(Player player, BlockFace cardinal) {
        return player.getWorld().getBlockAt(player.getLocation().add(cardinal.getDirection().setY(1)));
    }

    private static BlockFace convertVectorToCardinal(Vector vector) {
        List<BlockFace> cardinals = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

        for (BlockFace blockFace : cardinals) {
            if (vector.equals(blockFace.getDirection())) {
                return blockFace;
            }
        }

        return BlockFace.NORTH;
    }

    // returns what the left cardinal is to a given cardinal
    private static BlockFace getLeftCardinal(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> null;
        };
    }

    // returns what the right cardinal is to a given cardinal
    private static BlockFace getRightCardinal(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> null;
        };
    }

    // returns what the opposite cardinal is to a given cardinal
    private static BlockFace getOppositeCardinal(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.WEST;
            case SOUTH -> BlockFace.NORTH;
            case WEST -> BlockFace.EAST;
            default -> null;
        };
    }

    private static Location getRailJumpLandingLocation(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector planarDirection = playerLocation.getDirection().setY(0);
        Vector direction = planarDirection.clone().multiply(9).add(getRailGrindVelocity(player));

        if (player.isSprinting()) { // if the player is sprinting, add a flat extra 4 blocks in the direction
            Location location = playerLocation.clone();

            location.setPitch(0);

            Vector noPitchDirection = location.getDirection().setY(0);

            direction.add(noPitchDirection.clone().multiply(1.9));
        }

        Location finalLocation = playerLocation.clone().add(direction);
        Location finalBlockLocation = playerLocation.clone().add(direction).toBlockLocation().add(.5, -.5, .5);

        if (isGrindable(world.getBlockAt(finalBlockLocation))) {
            return finalBlockLocation;
        } else {
            return finalLocation;
        }
    }
}
