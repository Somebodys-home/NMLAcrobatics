package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLEnergySystem.EnergyManager;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
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
    private final SkillSetManager skillSetManager;
    private BukkitTask railGrindTask;
    private static final HashMap<UUID, Vector> rollDirection = new HashMap<>();
    private static final HashMap<UUID, Vector> railGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Double> railGrindSpeed = new HashMap<>();
    private static final HashMap<UUID, String> lastSuccessfulRailGrindDirection = new HashMap<>();
    private static final HashMap<UUID, Integer> lastDirectionChangeTick = new HashMap<>();

    public Maneuvers(NMLAcrobatics nmlAcrobatics) {
        Maneuvers.nmlAcrobatics = nmlAcrobatics;
        skillSetManager = nmlAcrobatics.getSkillSetManager();
    }

    public void startRailGrindTask() {
        railGrindTask = new BukkitRunnable() {
            int tickCounter = 0;

            @Override
            public void run() {
                tickCounter++;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    int acrobaticsLevel = skillSetManager.getSkillSet(uuid).getSkills().getAcrobaticsLevel();

                    if (acrobaticsLevel >= 20 && player.hasMetadata("rail grind")) {
                        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
                        if (isGrindable(below)) {
                            String priorDirection = lastSuccessfulRailGrindDirection.get(uuid);
                            int lastChangeTick = lastDirectionChangeTick.getOrDefault(uuid, -10);
                            double speed = railGrindSpeed.get(uuid);
                            String newDirection = null;
                            Vector velocity = null;

                            Block base = player.getLocation().getBlock();
                            Block northBlock = base.getRelative(BlockFace.NORTH).getRelative(BlockFace.DOWN);
                            Block eastBlock  = base.getRelative(BlockFace.EAST).getRelative(BlockFace.DOWN);
                            Block southBlock = base.getRelative(BlockFace.SOUTH).getRelative(BlockFace.DOWN);
                            Block westBlock  = base.getRelative(BlockFace.WEST).getRelative(BlockFace.DOWN);

                            if (speed < .1) {
                                player.removeMetadata("rail grind", nmlAcrobatics);
                                lastSuccessfulRailGrindDirection.remove(uuid);
                                lastDirectionChangeTick.remove(uuid);
                            }

                            if (isGrindable(northBlock) && !"south".equals(priorDirection)) {
                                newDirection = "north";
                                velocity = new Vector(0, 0, -0.5);
                            } else if (isGrindable(eastBlock) && !"west".equals(priorDirection)) {
                                newDirection = "east";
                                velocity = new Vector(0.5, 0, 0);
                            } else if (isGrindable(southBlock) && !"north".equals(priorDirection)) {
                                newDirection = "south";
                                velocity = new Vector(0, 0, 0.5);
                            } else if (isGrindable(westBlock) && !"east".equals(priorDirection)) {
                                newDirection = "west";
                                velocity = new Vector(-0.5, 0, 0);
                            }

                            if (newDirection != null && !newDirection.equals(priorDirection) && tickCounter - lastChangeTick >= 20) {
                                centerPlayer(player);
                                lastSuccessfulRailGrindDirection.put(uuid, newDirection);
                                lastDirectionChangeTick.put(uuid, tickCounter); // reset cooldown
                            }

                            if (velocity != null) {
                                velocity.multiply(speed);
                                player.setVelocity(velocity);
                            }

                            railGrindSpeed.put(uuid, speed * .98);

                        } else {
                            player.removeMetadata("rail grind", nmlAcrobatics);
                            lastSuccessfulRailGrindDirection.remove(uuid);
                            lastDirectionChangeTick.remove(uuid);
                        }
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public void stopRailGrindTask() {
        railGrindTask.cancel();
    }

    public static void roll(Player player) {
        Vector direction = rollDirection.get(player.getUniqueId());

        if (direction == null && direction.lengthSquared() < 0.0001) return;

        Vector roll = direction.normalize().multiply(2);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        EnergyManager.useEnergy(player, 5);
    }

    public static void rollBrace(Player player) {
        Vector roll = player.getLocation().getDirection().normalize().multiply(3);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    public static void longJump(Player player) {
        Vector longJump = player.getLocation().getDirection().normalize().multiply(1.5).setY(.5);
        double speed = longJump.length();

        player.setVelocity(longJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // start rail grind
                    if (isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {
                        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
                        railGrindDirection.put(player.getUniqueId(), player.getLocation().getDirection());
                        railGrindSpeed.put(player.getUniqueId(), speed);

                        float yaw = player.getLocation().getYaw();
                        // Normalize yaw to 0–360
                        yaw = (yaw % 360 + 360) % 360;

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

                    player.removeMetadata("longjump", nmlAcrobatics);
                    cancel();
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void railJump(Player player, double speed) {
        Vector railJump = player.getLocation().getDirection().normalize().multiply(1.5).setY(.5);

        if (speed < 1) speed = 1;

        railJump.multiply(speed);
        double speed2 = railJump.length();

        player.setVelocity(railJump);
        EnergyManager.useEnergy(player, 10);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) { // start rail grind
                    if (isGrindable(player.getLocation().getBlock().getRelative(BlockFace.DOWN))) {
                        player.setMetadata("rail grind", new FixedMetadataValue(nmlAcrobatics, true));
                        railGrindDirection.put(player.getUniqueId(), player.getLocation().getDirection());
                        railGrindSpeed.put(player.getUniqueId(), speed2);

                        float yaw = player.getLocation().getYaw();
                        // Normalize yaw to 0–360
                        yaw = (yaw % 360 + 360) % 360;

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

                    player.removeMetadata("longjump", nmlAcrobatics);
                    cancel();
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void setRollDirection(Player player, Vector direction) {
        rollDirection.put(player.getUniqueId(), direction);
    }

    private static boolean isGrindable(Block block) {
        if (block.getType() == Material.OAK_FENCE || block.getType() == Material.OAK_FENCE_GATE ||
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
            block.getType() == Material.NETHER_BRICK_FENCE || block.getType() == Material.IRON_BARS) {

            return true;
        } else {
            return false;
        }
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
}
