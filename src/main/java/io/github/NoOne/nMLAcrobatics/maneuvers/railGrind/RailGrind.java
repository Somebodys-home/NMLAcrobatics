package io.github.NoOne.nMLAcrobatics.maneuvers.railGrind;

import io.github.NoOne.nMLAbilities.abilitySystem.abilityEffects.AbilityEffectsHelper;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.Speedometer;
import io.github.NoOne.nMLEnergy.EnergyManager;
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
    private static HashMap<UUID, Double> railGrindSpeeds = new HashMap<>(); // the speed that the player started/will br grinding at
    private static HashMap<UUID, Float> playerWalkSpeeds = new HashMap<>(); // we set the players movement speed to 0 when rail grinding, so make sure to save the value it was before
    private static HashMap<UUID, Block> pivotedBlocks = new HashMap<>(); // stores the blocks that a player previously turned on
    private static HashMap<UUID, Block> heightBlocks = new HashMap<>(); // stores the blocks that a player previously changed their height on
    private static HashMap<UUID, Integer> effectTimers = new HashMap<>(); // stores a timer for making the players effects when rail grinding
    private static Sound railGrindSound = Sound.BLOCK_GRINDSTONE_USE;
    private static Sound pingSound = Sound.ITEM_TRIDENT_RETURN;

    public static void startRailGrindTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!isRailGrinding(player)) continue;

                    World world = player.getWorld();
                    Vector direction = railGrindDirections.get(player.getUniqueId());
                    BlockFace cardinal = convertVectorToCardinal(direction);
                    Block bottomBlock = getBottomBlock(player);
                    Block forwardsBottomBlock = bottomBlock.getRelative(cardinal, (int) Math.round(getEffectiveRailGrindSpeed(player)));
                    Block forwardsBottomBottomBlock = forwardsBottomBlock.getRelative(BlockFace.DOWN);
                    boolean aboutToFall = !isGrindable(forwardsBottomBlock) && !isGrindable(forwardsBottomBottomBlock);
                    boolean aboutToHitWall = getIncomingWall(player, cardinal).isSolid();

                    if (!isGrindable(bottomBlock)) { // player has fallen off the rail :T
                        stopRailGrind(player);
                        startFellOffRailTask(player);
                        continue;
                    }

                    // turning logic
                    // todo: this turning logic is bugged at high speeds
                    //  see the distance that you get the block in front of the player by scales with your speed per tick
                    //  ex if you were moving 1.5 blocks a tick, then the block that is in front of you is whatever block is 1.5 blocks away in the direction youre rail grinding in
                    //  the problem with that is how minecraft calculates what your block position is
                    //  because it takes the integer value of your coordinates only
                    //  so like if the x position of your coords was like 5.235 or something, the x coord of your block position would be 5~~.235~~
                    //  so for railgrinding, if youre going at a speed over a block a tick, and you approach a turn at the very end of one block but not quite at the start of a new one,
                    //  then the check regonizes that youre reaching a dead end
                    //  but fails in determining where you should turn to because the block that it gets below you is actually a block behind where you are
                    //  and by the time the railgrinding system regonizes that and tries to course correct on the next tick
                    //  youve already fallen off the rail
                    //  and i think how to fix it is if the player is maybe 3/4ths of the way past the block, then check the block in front of the player for turns instead
                    if (!aboutToFall && !aboutToHitWall) { // if you can still go forwards, then check if you can turn based on where the player is looking
                        if (player.getFacing() != cardinal) {
                            priorityCardinalTurn(player, cardinal);
                        }
                    } else { // if you cant go forwards, then turn logic is based on how many ways you can turn
                        BlockFace leftCardinal = getLeftCardinal(cardinal);
                        BlockFace rightCardinal = getRightCardinal(cardinal);
                        boolean canTurnLeft = isTurningDirectionValid(player, leftCardinal, bottomBlock);
                        boolean canTurnRight = isTurningDirectionValid(player, rightCardinal, bottomBlock);
                        // todo: so like here, instead of bottomblock if youre like 3/4th through the block youre on, it gets the block in front of you

                        if (canTurnLeft && canTurnRight) { // if you can turn both ways, then turn based on where the player is looking
                            priorityCardinalTurn(player, cardinal);
                        } else if (canTurnLeft) {
                            turnPlayer(player, leftCardinal);
                        } else if (canTurnRight) {
                            turnPlayer(player, getRightCardinal(cardinal));
                        }
                    }

                    // scaling logic
                    Block forwardsBlock = forwardsBottomBlock.getRelative(BlockFace.UP);

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

    public static void startEffectsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int timer = 230;

                for (Map.Entry<UUID, Integer> entry : effectTimers.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    Location playerLocation = player.getLocation();
                    int timeLeft = entry.getValue();
                    Vector direction = railGrindDirections.get(player.getUniqueId());

                    player.getWorld().spawnParticle(Particle.BLOCK, playerLocation, 10, direction.getX() * .25, .25, direction.getZ() * .25, getBottomBlock(player).getBlockData());

                    if (timeLeft % 5 == 0) player.playSound(playerLocation, railGrindSound, .33f, .33f);
                    if (timeLeft == 0) {
                        effectTimers.put(player.getUniqueId(), timer);
                        player.playSound(playerLocation, Sound.ENTITY_MINECART_INSIDE, .6f, .5f);
                    } else {
                        effectTimers.put(player.getUniqueId(), timeLeft - 1);
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
        railGrindSpeeds.put(uuid, Speedometer.getSpeedPerTick(player));
        centerPlayerOnBlock(player);
        player.setWalkSpeed(0);
        startPlayingSound(player);
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
        AbilityEffectsHelper.putOnInfiniteCooldown(player);
        EnergyManager.pauseEnergyRegen(player);
    }

    public static void stopRailGrind(Player player) {
        UUID uuid = player.getUniqueId();

        player.removeMetadata("rail grind", nmlAcrobatics);
        player.setWalkSpeed(playerWalkSpeeds.get(uuid));
        stopPlayingSound(player);
        pivotedBlocks.remove(uuid);
        heightBlocks.remove(uuid);
        AbilityEffectsHelper.removeInfiniteCooldown(player);
        EnergyManager.resumeEnergyRegen(player);
    }

    // when the player falls off a rail, they can continue a rail grind if they land on a rail
    private static void startFellOffRailTask(Player player) {
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
            if (isTurningDirectionValid(player, priorityCardinal, getBottomBlock(player))) {
                turnPlayer(player, priorityCardinal);
            } else { // if not, repeat that same check to the opposite cardinal block
                BlockFace oppositeCardinal = priorityCardinal.getOppositeFace();

                if (isTurningDirectionValid(player, oppositeCardinal, getBottomBlock(player))) {
                    turnPlayer(player, oppositeCardinal);
                }
            }
        }
    }

    private static void startPlayingSound(Player player) {
        effectTimers.put(player.getUniqueId(), 0);
    }

    private static void stopPlayingSound(Player player) {
        effectTimers.remove(player.getUniqueId());
        player.stopSound(railGrindSound);
        player.stopSound(Sound.ENTITY_MINECART_INSIDE);
    }

    private static void playPingSound(Player player) {
        stopPlayingSound(player);
        player.playSound(player, pingSound, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!effectTimers.containsKey(player.getUniqueId()) && isRailGrinding(player)) {
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

    // returns if, for a given cardinal direction:
    // the first block in that direction is grindable, and there's no wall there
    private static boolean isTurningDirectionValid(Player player, BlockFace cardinal, Block block) {
        Block frontBottomBlock = player.getWorld().getBlockAt(block.getLocation().add(cardinal.getDirection()));
        Block frontBlock = frontBottomBlock.getRelative(BlockFace.UP);
        Block frontBottomBottomBlock = frontBottomBlock.getRelative(BlockFace.DOWN);
        Block incomingWall = getIncomingWall(player, cardinal);

        return (isGrindable(frontBottomBlock) || isGrindable(frontBottomBottomBlock) || isGrindable(frontBlock)) && !incomingWall.isSolid();
    }

    // returns t/f if the player has moved from the block they previously turned on to be able to turn again
    private static boolean tooCloseToTurn(Player player) {
        return getBottomBlock(player).equals(pivotedBlocks.get(player.getUniqueId()));
    }

    // returns t/f if the player has moved from the block they previously stepped up/down on to be able to step again
    private static boolean tooCloseToStep(Player player) {
        return getBottomBlock(player).equals(heightBlocks.get(player.getUniqueId()));
    }

    private static double getEffectiveRailGrindSpeed(Player player) {
        // if the player just turned / scaled, then massively lower their speed until they're a block away from where they did it from
        if (tooCloseToTurn(player) || tooCloseToStep(player)) {
            return .1;
        } else {
            return Math.max(railGrindSpeeds.get(player.getUniqueId()) * 1.2, .65); // 1x speed multiplier = 20 m/s
        }
    }

    private static Vector getRailGrindVelocity(Player player) {
        return railGrindDirections.get(player.getUniqueId()).clone().multiply(getEffectiveRailGrindSpeed(player));
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
    public static BlockFace getLeftCardinal(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.SELF;
        };
    }

    // returns what the right cardinal is to a given cardinal
    public static BlockFace getRightCardinal(BlockFace blockFace) {
        return switch (blockFace) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SELF;
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
