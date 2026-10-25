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
                // todo: when the player is rail grinding, and theyre gonna fall off (theres not a grindable block ahead of them) go to the turn logic
                //  check the blocks that are to the cardinal left and right of the block that the player is on, and center on the block and change directional vector accordingly
                //  prioritize whichever way the player is looking

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Block bottomBlock = getBottomBlock(player);

                    if (!player.hasMetadata("rail grind")) return;
                    if (!isGrindable(bottomBlock)) { // player has fallen off the rail :T
                        stopRailGrind(player);
                        continue;
                    }

                    Vector direction = railGrindDirections.get(player.getUniqueId());
                    BlockFace cardinalDirection = convertVectorToCardinal(direction);
                    player.setVelocity(direction.clone());

                    // turning logic
                    boolean aboutToFall = !isGrindable(bottomBlock.getRelative(cardinalDirection));

                    if (aboutToFall) {
                        BlockFace priorityCardinal = null;

                    }
                    Block rightBlock = bottomBlock.getRelative(getRightCardinal(convertVectorToCardinal(direction)));
                    player.sendMessage(rightBlock.getType().name());
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

    public static void railJump(Player player) {

    }

    private static void centerPlayerOnBlock(Player player) {
        Location location = player.getLocation();

        location.setX(location.getBlockX() + .5);
        location.setY(location.getBlockY() + .5);
        location.setZ(location.getBlockZ() + .5);
        player.teleport(location);
    }

    public static boolean isGrindable(Block block) {
        String blockType = block.getType().name();

        return blockType.contains("FENCE") || blockType.contains("GATE") || blockType.contains("BARS") || blockType.contains("WALL");
    }

    public static Block getBottomBlock(Player player) {
        return player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    }

    private static Vector getCardinalDirection(Player player) {
        return player.getFacing().getDirection();
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
}
