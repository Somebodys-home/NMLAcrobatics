package io.github.NoOne.nMLAcrobatics.maneuvers.railGrind;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
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
    private static HashMap<UUID, Vector> railGrindDirections = new HashMap<>();
    private static HashMap<UUID, Float> playerWalkSpeeds = new HashMap<>(); // set the players movement speed to 0 when rail grinding, so make sure to save the value it was before

    public static void startRailGrindTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // todo: make some logic for falling off the rail so that you can continue rail grinding when you fall off unti you hit the ground
                //  also work on rail jumping
                //  and add back sound

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Block bottomBlock = getBottomBlock(player);

                    if (!player.hasMetadata("rail grind")) return;
                    if (!isGrindable(bottomBlock)) { // player has fallen off the rail :T
                        stopRailGrind(player);
                        continue;
                    }

                    // setting the player's velocity
                    Vector direction = railGrindDirections.get(player.getUniqueId());
                    BlockFace cardinal = convertVectorToCardinal(direction);

                    player.setVelocity(direction.clone());

                    // turning logic
                    boolean hasTurned = false; // have to hold this variable cuz scaling downwards logic semi clashes with turning logic

                    if (isAboutToFall(player, cardinal)) {
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
                                centerPlayerOnBlock(player);
                                railGrindDirections.put(player.getUniqueId(), getCardinalDirection(priorityCardinal));
                                hasTurned = true;
                            } else { // if not, repeat that same check to the opposite cardinal block
                                BlockFace oppositeCardinal = getOppositeCardinal(priorityCardinal);

                                if (isTurningDirectionValid(player, oppositeCardinal)) {
                                    centerPlayerOnBlock(player);
                                    railGrindDirections.put(player.getUniqueId(), getCardinalDirection(oppositeCardinal));
                                    hasTurned = true;
                                }
                            }
                        } else { // if the player isn't looking in a valid direction, just check from left to right
                            BlockFace leftCardinal = getLeftCardinal(cardinal);
                            BlockFace rightCardinal = getRightCardinal(cardinal);

                            if (isTurningDirectionValid(player, leftCardinal)) {
                                centerPlayerOnBlock(player);
                                railGrindDirections.put(player.getUniqueId(), getCardinalDirection(leftCardinal));
                                hasTurned = true;
                            } else if (isTurningDirectionValid(player, rightCardinal)) {
                                centerPlayerOnBlock(player);
                                railGrindDirections.put(player.getUniqueId(), getCardinalDirection(rightCardinal));
                                hasTurned = true;
                            }
                        }
                    }

                    // scaling logic
                    Block frontTopBlock = player.getLocation().getBlock().getRelative(cardinal);
                    Block frontBottomBlock = bottomBlock.getRelative(cardinal).getRelative(BlockFace.DOWN);

                    if (frontTopBlock.isSolid()) {
                        if (isGrindable(frontTopBlock)) { // if you hit a block at your feet that is grindable, then go on top of it
                            centerPlayerOnBlock(player, frontTopBlock);
                            player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
                        } else { // if it isnt grindable, then you stop there
                            stopRailGrind(player);
                        }
                    } else if (!hasTurned && isAboutToFall(player, cardinal) && frontBottomBlock.isSolid()) {
                        if (isGrindable(frontBottomBlock)) { // if youre gonna fall on a grindable block, go on top of it
                            centerPlayerOnBlock(player, frontBottomBlock);
                            player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
                        }
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void startRailGrind(Player player) {
        player.playSound(player, Sound.ITEM_TRIDENT_RETURN, 2f, 1f);
        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));

        centerPlayerOnBlock(player);
        railGrindDirections.put(player.getUniqueId(), getCardinalDirection(player));
        playerWalkSpeeds.put(player.getUniqueId(), player.getWalkSpeed());
        player.setWalkSpeed(0);
    }

    public static void stopRailGrind(Player player) {
        player.removeMetadata("rail grind", nmlAcrobatics);
        player.setWalkSpeed(playerWalkSpeeds.get(player.getUniqueId()));
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

    public static boolean isGrindable(Block block) {
        String blockType = block.getType().name();

        return blockType.contains("FENCE") || blockType.contains("GATE") || blockType.contains("BARS") || blockType.contains("WALL");
    }

    public static boolean isTurningDirectionValid(Player player, BlockFace cardinal) {
        Block bottomBlock = getBottomBlock(player);
        Block frontBottomBlock = bottomBlock.getRelative(cardinal);
        Block frontBottomTopBlock = frontBottomBlock.getRelative(BlockFace.UP);
        Block frontBottomBottomBlock = frontBottomBlock.getRelative(BlockFace.DOWN);

        return isGrindable(frontBottomBlock) ||  isGrindable(frontBottomBottomBlock) || isGrindable(frontBottomTopBlock);
    }

    public static boolean isAboutToFall(Player player, BlockFace cardinal) {
        Block bottomBlock = getBottomBlock(player);
        Block frontBottomBlock = bottomBlock.getRelative(cardinal);
        Block frontBottomBottomBlock = frontBottomBlock.getRelative(BlockFace.DOWN);

        return !isGrindable(frontBottomBlock) && !isGrindable(frontBottomBottomBlock);
    }

    public static Block getBottomBlock(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }

    private static Vector getCardinalDirection(Player player) {
        return player.getFacing().getDirection();
    }

    private static Vector getCardinalDirection(BlockFace blockFace) {
        return blockFace.getDirection();
    }

    private static BlockFace convertVectorToCardinal(Vector vector) {
        List<BlockFace> cardinals = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

        for (BlockFace blockFace : cardinals) {
            if (vector.equals(blockFace.getDirection())) {
                return blockFace;
            }
        }

        return null;
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
}
