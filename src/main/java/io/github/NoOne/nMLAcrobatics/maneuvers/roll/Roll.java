package io.github.NoOne.nMLAcrobatics.maneuvers.roll;

import io.github.NoOne.nMLAbilities.abilitySystem.cooldownSystem.CooldownManager;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLAcrobatics.Speedometer;
import io.github.NoOne.nMLAcrobatics.maneuvers.PerformedManeuverEvent;
import io.github.NoOne.nMLEnergy.EnergyManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class Roll {
    private static NMLAcrobatics nmlAcrobatics = NMLAcrobatics.getInstance();

    public static void roll(Player player) {
        Location previousLocation = Speedometer.getPreviousLocation(player);

        if (previousLocation != null && Speedometer.getSpeedPerSecond(player) > 0.5) { // if you're actually moving
            Vector roll = Speedometer.getRollDirection(player).multiply(2).setY(0);

            player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("roll cooldown", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 30);

            player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            EnergyManager.useEnergy(player, 10);
            CooldownManager.putOnHardCooldown(player, 1.5);
            player.setVelocity(roll);
            Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Roll"));
        }
    }

    public static void rollBrace(Player player) {
        Vector direction = player.getLocation().getDirection();

        direction.setY(0);
        direction.normalize();

        player.setVelocity(direction.multiply(2));
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        Bukkit.getPluginManager().callEvent(new PerformedManeuverEvent(player, "Roll Brace"));
        CooldownManager.putOnHardCooldown(player, 1.5);
        player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                player.removeMetadata("roll cooldown", nmlAcrobatics);
            }
        }.runTaskLater(nmlAcrobatics, 30);
    }
}
