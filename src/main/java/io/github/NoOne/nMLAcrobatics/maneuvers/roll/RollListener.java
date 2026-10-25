package io.github.NoOne.nMLAcrobatics.maneuvers.roll;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class RollListener implements Listener {
    private NMLAcrobatics nmlAcrobatics;

    public RollListener(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
    }

    @EventHandler
    public void removeRollBraceMetadata(PlayerMoveEvent event) { // remove roll brace metadata when hitting the ground
        Player player = event.getPlayer();

        if (player.hasMetadata("roll brace") && player.isOnGround()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("roll brace", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 1); // delay for a tick to give time for vv this listener vv to run
        }
    }

    @EventHandler
    public void actualRollBraceFromDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (player.hasMetadata("roll brace")) {
                event.setDamage(event.getDamage() / 2);
                Roll.rollBrace(player);
            }
        }
    }
}
