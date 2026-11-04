package io.github.NoOne.nMLAcrobatics.maneuvers.longJump;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.maneuvers.PerformedManeuverEvent;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import io.github.NoOne.nMLAcrobatics.maneuvers.wallRun.WallRun;
import io.github.NoOne.nMLEnergy.EnergyManager;
import io.github.NoOne.nMLPlayerStats.profileSystem.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LongJump {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();
    private static ProfileManager profileManager = NMLAcrobatics.getInstance().getProfileManager();

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

    public static void wallJump(Player player) {
        Vector direction = WallRun.getWallRunDirection(player);
        Vector wallJump;
        int delay;

        if (player.getTargetBlock(null, 1).isSolid()) { // if the player is looking at a wall, then jump straight up
            wallJump = new Vector(0, .6, 0);
            delay = 10;
        } else { // if they're not, then jump in the direction they're looking + the opposite direction of their cardinal
            wallJump = player.getVelocity().add(player.getLocation().getDirection().multiply(.5)).add(direction.multiply(-.25)).setY(.65);
            delay = 5;
        }

        WallRun.stopWallRunning(player);

        if (profileManager.getPlayerProfile(player.getUniqueId()).getStats().getCurrentEnergy() > 10) {
            player.setVelocity(wallJump);
            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Wall Jump"));
            EnergyManager.useEnergy(player, 7);

            new BukkitRunnable() {
                @Override
                public void run() {
                    startLongJumpTask(player);
                }
            }.runTaskLater(nmlAcrobatics, delay);
        }
    }

    public static void startLongJumpTask(Player player) {
        new BukkitRunnable() {
            int acrobaticsLevel = nmlAcrobatics.getSkillSetManager().getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

            @Override
            public void run() {
                if (player.isInWater() || player.isInLava() || player.isInPowderedSnow()) cancel();
                if (acrobaticsLevel >= 30 && !RailGrind.isRailGrinding(player) && WallRun.getClosetWallCardinal(player) != null) {
                    WallRun.startWallRunning(player, WallRun.getClosetWallCardinal(player));
                    cancel();
                }

                if (player.isOnGround()) { // rail grind logic
                    cancel();

                    if (acrobaticsLevel >= 20 && RailGrind.isGrindable(RailGrind.getBottomBlock(player))) {
                        RailGrind.startRailGrind(player);
                    }
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }
}
