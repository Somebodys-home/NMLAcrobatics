package io.github.NoOne.nMLAcrobatics;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Maneuvers {
    private static NMLAcrobatics nmlAcrobatics;
    private static final HashMap<UUID, Vector> rollDirection = new HashMap<>();

    public Maneuvers(NMLAcrobatics nmlAcrobatics) {
        Maneuvers.nmlAcrobatics = nmlAcrobatics;
    }

    public static void roll(Player player) {
        Vector direction = rollDirection.get(player.getUniqueId());

        if (direction == null && direction.lengthSquared() < 0.0001) return;

        Vector roll = direction.normalize().multiply(2);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    public static void rollBrace(Player player) {
        Vector roll = player.getLocation().getDirection().normalize().multiply(3);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
    }

    public static void longJump(Player player) {
        Vector roll = player.getLocation().getDirection().normalize().multiply(1.5).setY(.5);

        player.setVelocity(roll);
        player.playSound(player, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        player.setMetadata("longjump", new FixedMetadataValue(nmlAcrobatics, true));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnGround()) {
                    player.removeMetadata("longjump", nmlAcrobatics);
                    cancel();
                }
            }
        }.runTaskTimer(nmlAcrobatics, 0, 1);
    }

    public static void setRollDirection(Player player, Vector direction) {
        rollDirection.put(player.getUniqueId(), direction);
    }
}
