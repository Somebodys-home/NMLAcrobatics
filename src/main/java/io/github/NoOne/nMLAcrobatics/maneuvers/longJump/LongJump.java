package io.github.NoOne.nMLAcrobatics.maneuvers.longJump;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.maneuvers.PerformedManeuverEvent;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import io.github.NoOne.nMLEnergy.EnergyManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LongJump {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();

    public static void longJump(Player player) {
        double speedMultiplier = nmlAcrobatics.getProfileManager().getPlayerProfile(player.getUniqueId()).getStats().getSpeed() / 100.0;
        Vector longJump = player.getLocation().getDirection().multiply(.9).multiply(speedMultiplier).setY(.5);

        player.setVelocity(longJump);
        startLongJumpTask(player);
        EnergyManager.useEnergy(player, 15);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Long Jump"));
    }

    public static void railJump(Player player) {
        Vector jump = player.getLocation().getDirection().multiply(Math.sqrt(1.22)).setY(.5);

        RailGrind.stopRailGrind(player);
        player.setVelocity(jump);

        new BukkitRunnable() {
            @Override
            public void run() {
                startLongJumpTask(player);
                EnergyManager.useEnergy(player, 15);
                player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
                Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Rail Jump"));
            }
        }.runTaskLater(nmlAcrobatics, 2);
    }

    public static void startLongJumpTask(Player player) {
        new BukkitRunnable() {
            int acrobaticsLevel = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

            @Override
            public void run() {
                if (player.isOnGround()) { // rail grind logic
                    if (acrobaticsLevel >= 20 && RailGrind.isGrindable(RailGrind.getBottomBlock(player))) {
                        RailGrind.startRailGrind(player);
                    }

                    cancel();
                }
//                else { // climbing logic
//                    int acrobaticsLvl = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();
//
//                    if (acrobaticsLvl >= 30) {
//                        Vector playerDir = player.getLocation().getDirection().clone().setY(0);
//
//                        if (playerDir.lengthSquared() < 1e-6) {
//                            double yawRad = Math.toRadians(player.getLocation().getYaw());
//                            playerDir = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
//                        }
//
//                        Vector forward = playerDir.clone().normalize().multiply(1.2);
//                        Vector right = playerDir.clone().crossProduct(new Vector(0, 1, 0)).setY(0).normalize();
//                        Vector left = right.clone().multiply(-1);
//                        Location leftLoc = player.getLocation().clone().add(left);
//                        Location forwardLoc = player.getLocation().clone().add(forward);
//                        Location rightLoc = player.getLocation().clone().add(right);
//
//                        Block leftTop = leftLoc.clone().add(0, 1, 0).getBlock();
//                        Block leftBottom = leftLoc.getBlock();
//                        Block forwardTop = forwardLoc.clone().add(0, 1, 0).getBlock();
//                        Block forwardBottom = forwardLoc.getBlock();
//                        Block rightTop = rightLoc.clone().add(0, 1, 0).getBlock();
//                        Block rightBottom = rightLoc.getBlock();
//
//                        if (leftTop.isSolid() && leftBottom.isSolid()) {
//                            startWallRun(player, snapToCardinal(left));
//                            player.removeMetadata("long jump", nmlAcrobatics);
//                            cancel();
//                        }
//                        else if (forwardTop.isSolid() && forwardBottom.isSolid()) {
//                            startWallRun(player, snapToCardinal(forward));
//                            player.removeMetadata("long jump", nmlAcrobatics);
//                            cancel();
//                        }
//                        else if (rightTop.isSolid() && rightBottom.isSolid()) {
//                            startWallRun(player, snapToCardinal(right));
//                            player.removeMetadata("long jump", nmlAcrobatics);
//                            cancel();
//                        }
//                    }
//                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }
}
