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
import java.util.Map;
import java.util.UUID;

public class RailGrind {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static HashMap<UUID, Vector> railGrindDirections = new HashMap<>(); // the cardinal direction that the player is grinding in
    private static HashMap<UUID, Double> railGrindSpeeds = new HashMap<>(); // the speed that the player is grinding at
    private static HashMap<UUID, Float> playerWalkSpeeds = new HashMap<>(); // we set the players movement speed to 0 when rail grinding, so make sure to save the value it was before
    private static HashMap<UUID, Block> pivotedBlocks = new HashMap<>(); // stores the blocks that a player previously turned on
    private static HashMap<UUID, Block> heightBlocks = new HashMap<>(); // stores the blocks that a player previously changed their height on
    private static HashMap<UUID, Integer> soundTimers = new HashMap<>(); // stores the blocks that a player previously changed their height on
    private static Sound railGrindSound = Sound.BLOCK_GRINDSTONE_USE;
    private static Sound pingSound = Sound.ITEM_TRIDENT_RETURN;

    public static void startRailGrindTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isRailGrinding(player)) continue;
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
                    } else if (isGrindable(forwardsBottomBottomBlock) && !isGrindable(forwardsBottomBlock)) {
                        scaleBlock(player, forwardsBottomBottomBlock);
                    }

                    // rail jump indicator
                    Location landingLocation = getRailJumpLandingLocation(player);

                    if (isGrindable(world.getBlockAt(landingLocation))) {
                        AbilityEffectsHelper.makeWireFrameRectangle(landingLocation, .5, .5, .5);
                    } else {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, landingLocation, 1, 0, 0, 0, 0);
                    }

                    // bonked
                    aboutToHitWall = getIncomingWall(player, cardinal).isSolid();

                    if (aboutToHitWall) {
                        stopRailGrind(player);
                    }

                    // setting the player's velocity
                    player.setVelocity(getRailGrindVelocity(player));
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startSoundTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int timer = 230;

                for (Map.Entry<UUID, Integer> entry : soundTimers.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    int timeLeft = entry.getValue();

                    if (timeLeft % 5 == 0) player.playSound(player.getLocation(), railGrindSound, .33f, .33f);
                    if (timeLeft == 0) {
                        soundTimers.put(player.getUniqueId(), timer);
                        player.playSound(player.getLocation(), Sound.ENTITY_MINECART_INSIDE, .6f, .5f);
                    } else {
                        soundTimers.put(player.getUniqueId(), timeLeft - 1);
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startRailGrind(Player player) {
        UUID uuid = player.getUniqueId();

        player.playSound(player, pingSound, 1f, 1f);

        railGrindDirections.put(uuid, player.getFacing().getDirection());
        playerWalkSpeeds.put(uuid, player.getWalkSpeed());
        railGrindSpeeds.put(uuid, Speedometer.getSpeedPerSecond(player));
        centerPlayerOnBlock(player);
        player.setWalkSpeed(0);
        startPlayingSound(player);
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
    }

    public static void stopRailGrind(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata("rail grind", nmlAcrobatics);
        player.setWalkSpeed(playerWalkSpeeds.get(uuid));
        stopPlayingSound(player);
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
                        heightBlocks.put(player.getUniqueId(), getBottomBlock(player)); // so that the slowdown effect happens when you land, cuz its cool B]
                        playPingSound(player);
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
        playPingSound(player);
    }

    private static void turnPlayer(Player player, BlockFace newDirection) {
        UUID uuid = player.getUniqueId();

        if (!tooCloseToTurn(player)) {
            centerPlayerOnBlock(player);
            railGrindDirections.put(uuid, newDirection.getDirection());
            pivotedBlocks.put(uuid, getBottomBlock(player));
            playPingSound(player);
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
                BlockFace oppositeCardinal = priorityCardinal.getOppositeFace();

                if (isTurningDirectionValid(player, oppositeCardinal)) {
                    turnPlayer(player, oppositeCardinal);
                }
            }
        }
    }

    private static void startPlayingSound(Player player) {
        soundTimers.put(player.getUniqueId(), 0);
    }

    private static void stopPlayingSound(Player player) {
        soundTimers.remove(player.getUniqueId());
        player.stopSound(railGrindSound);
        player.stopSound(Sound.ENTITY_MINECART_INSIDE);
    }

    private static void playPingSound(Player player) {
        stopPlayingSound(player);
        player.playSound(player, pingSound, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!soundTimers.containsKey(player.getUniqueId()) && isRailGrinding(player)) {
                    startPlayingSound(player);
                }
            }
        }.runTaskLater(nmlAcrobatics, 5);
    }

    public static boolean isGrindable(Block block) {
        String blockType = block.getType().name();

        return blockType.contains("FENCE") || blockType.contains("GATE") || blockType.contains("BARS") || blockType.contains("WALL");
    }

    public static boolean isRailGrinding(Player player) {
        return player.hasMetadata("rail grind");
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
        double multiplier = Math.max((railGrindSpeeds.get(player.getUniqueId()) / 20) * 1.2, .65); // 1x speed multiplier = 20 m/s

        // if the player just turned / scaled, then massively lower their speed until they're a block away from where they did it from
        if (tooCloseToTurn(player) || tooCloseToStep(player)) {
            if (multiplier <= 1 && multiplier > .1) {
                multiplier = .1;
            } else {
                multiplier = .2;
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

    public static BlockFace convertVectorToCardinal(Vector vector) {
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
