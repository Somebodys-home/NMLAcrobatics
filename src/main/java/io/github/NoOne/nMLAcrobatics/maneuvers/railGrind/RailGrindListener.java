package io.github.NoOne.nMLAcrobatics.maneuvers.railGrind;

import io.github.NoOne.nMLAcrobatics.maneuvers.longJump.LongJump;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInputEvent;

public class RailGrindListener implements Listener {

    @EventHandler
    public void getOffGrinding(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();

        if (player.hasMetadata("rail grind")) {
            if (input.isJump()) {
                LongJump.railJump(player);
            } else if (input.isSneak()) {
                RailGrind.stopRailGrind(player);
            }
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void actualRollBraceFromDamage(EntityDamageEvent event) { // lower the damage if you fall on a rail grindable block so you dont die when rail grinding
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (RailGrind.isGrindable(RailGrind.getBottomBlock(player))) {
                event.setDamage(1);
            }
        }
    }
}
