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
    private static HashMap<UUID, Block> pivotedBlocks = new HashMap<>(); // stores the blocks that a player previously turned on
    private static HashMap<UUID, Block> heightBlocks = new HashMap<>(); // stores the blocks that a player previously changed their height on

    public static void startRailGrindTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // todo: add sounds to rail grinding, turning, and scaling
                //  also make everything scale with the speed the player starts the rail on, instead of a fixed number
                //  including the forwards blocks check, which the forwards distance should increase when your speed multiplier is greater
                //  than 1

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
                    if (!aboutToFall && !aboutToHitWall) { // if you can still go forwards, then check if you can turn based on where the player is looking
                        if (player.getFacing() != cardinal) {
                            priorityCardinalTurn(player, cardinal);
                        }
                    } else { // if you cant go forwards, then turn logic is based on how many ways you can turn
                        BlockFace leftCardinal = getLeftCardinal(cardinal);
                        BlockFace rightCardinal = getRightCardinal(cardinal);
                        boolean canTurnLeft = isTurningDirectionValid(player, leftCardinal);
                        boolean canTurnRight = isTurningDirectionValid(player, rightCardinal);

                        if (canTurnLeft && canTurnRight) { // if you can turn both ways, then turn based on where the player is looking
                            priorityCardinalTurn(player, cardinal);
                        } else if (canTurnLeft) { // if you cant then just turn right or left
                            turnPlayer(player, leftCardinal);
                        } else if (canTurnRight) {
                            turnPlayer(player, getRightCardinal(cardinal));
                        }
                    }

                    // scaling logic
                    if (isGrindable(forwardsBlock)) {
                        scaleBlock(player, forwardsBlock);
                    } else if (isGrindable(forwardsBottomBottomBlock)) {
                        scaleBlock(player, forwardsBottomBottomBlock);
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
        railGrindDirections.put(uuid, player.getFacing().getDirection());
        playerWalkSpeeds.put(uuid, player.getWalkSpeed());
        railGrindSpeeds.put(uuid, Speedometer.getSpeed(player));
        player.setWalkSpeed(0);
    }

    public static void stopRailGrind(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata("rail grind", nmlAcrobatics);
        player.setWalkSpeed(playerWalkSpeeds.get(uuid));
        pivotedBlocks.remove(uuid);
        heightBlocks.remove(uuid);
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

    // for centering players when they start rail grinding or turn on a corner
    private static void centerPlayerOnBlock(Player player) {
        Location location = player.getLocation();

        location.setX(location.getBlockX() + .5);
        location.setY(location.getBlockY() + .5);
        location.setZ(location.getBlockZ() + .5);
        player.teleport(location);
    }

    // for centering players when they go up or down a block
    private static void scaleBlock(Player player, Block block) {
        Location location = block.getLocation();

        location.setX(location.getBlockX() + .5);
        location.setY(location.getBlockY() + 1.5);
        location.setZ(location.getBlockZ() + .5);
        location.setRotation(player.getYaw(), player.getPitch());
        player.teleport(location);
        heightBlocks.put(player.getUniqueId(), block);
    }

    private static void turnPlayer(Player player, BlockFace newDirection) {
        UUID uuid = player.getUniqueId();

        if (!tooCloseToTurn(player)) {
            centerPlayerOnBlock(player);
            railGrindDirections.put(uuid, newDirection.getDirection());
            pivotedBlocks.put(uuid, getBottomBlock(player));
        }
    }

    // the method to turn the player based on where they're looking
    private static void priorityCardinalTurn(Player player, BlockFace originalCardinalDirection) {
        BlockFace priorityCardinal = null;

        // if the player is looking to the left of their direction, set the priority to turn left
        if (player.getFacing().equals(getLeftCardinal(originalCardinalDirection))) {
            priorityCardinal = getLeftCardinal(originalCardinalDirection);
        } else if (player.getFacing().equals(getRightCardinal(originalCardinalDirection))) { // if the player is looking to the right of their direction, set the priority to turn right
            priorityCardinal = getRightCardinal(originalCardinalDirection);
        }

        if (priorityCardinal != null) { // if the player isnt looking a proper way fall off then, nerd
            if (isTurningDirectionValid(player, priorityCardinal)) {
                turnPlayer(player, priorityCardinal);
            } else { // if not, repeat that same check to the opposite cardinal block
                BlockFace oppositeCardinal = switch (priorityCardinal) {
                    case NORTH -> BlockFace.SOUTH;
                    case EAST -> BlockFace.WEST;
                    case SOUTH -> BlockFace.NORTH;
                    case WEST -> BlockFace.EAST;
                    default -> null;
                };

                if (isTurningDirectionValid(player, oppositeCardinal)) {
                    turnPlayer(player, oppositeCardinal);
                }
            }
        }
    }

    public static boolean isGrindable(Block block) {
        String blockType = block.getType().name();

        return blockType.contains("FENCE") || blockType.contains("GATE") || blockType.contains("BARS") || blockType.contains("WALL");
    }

    private static boolean isTurningDirectionValid(Player player, BlockFace cardinal) {
        Block bottomBlock = getBottomBlock(player);
        Block frontBottomBlock = bottomBlock.getRelative(cardinal);
        Block frontBottomTopBlock = frontBottomBlock.getRelative(BlockFace.UP);
        Block frontBottomBottomBlock = frontBottomBlock.getRelative(BlockFace.DOWN);
        Block incomingWall = getIncomingWall(player, cardinal);

        return (isGrindable(frontBottomBlock) || isGrindable(frontBottomBottomBlock) || isGrindable(frontBottomTopBlock)) && !incomingWall.isSolid();
    }

    // returns t/f if the player has moved from the block they previously turned on to be able to turn again
    private static boolean tooCloseToTurn(Player player) {
        return getBottomBlock(player).equals(pivotedBlocks.get(player.getUniqueId()));
    }

    // returns t/f if the player has moved from the block they previously stepped up/down on to be able to step again
    private static boolean tooCloseToStep(Player player) {
        return getBottomBlock(player).equals(heightBlocks.get(player.getUniqueId()));
    }

    private static Vector getRailGrindVelocity(Player player) {
        double multiplier = 1;

        // if the player just turned / scaled, then massively lower their speed until they're a block away from where they did it from
        if (tooCloseToTurn(player) || tooCloseToStep(player)) {
            if (multiplier <= 1) {
                multiplier = .2;
            } else {
                multiplier = Math.max(multiplier / 10.0, .2);
            }
        }

        return railGrindDirections.get(player.getUniqueId()).clone().multiply(multiplier);
    }

    public static Block getBottomBlock(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }

    private static Block getIncomingWall(Player player, BlockFace cardinal) {
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

    private static Location getRailJumpLandingLocation(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        Vector planarDirection = playerLocation.getDirection().setY(0);
        Vector direction = planarDirection.clone().multiply(9).add(getRailGrindVelocity(player));

        if (player.isSprinting()) { // if the player is sprinting, add a flat extra ~2 blocks in the direction
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
