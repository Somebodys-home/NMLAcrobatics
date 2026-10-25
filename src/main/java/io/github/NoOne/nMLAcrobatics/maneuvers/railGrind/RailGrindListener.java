package io.github.NoOne.nMLAcrobatics.maneuvers.railGrind;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;

public class RailGrindListener implements Listener {
    private NMLAcrobatics nmlAcrobatics;

    public RailGrindListener(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
    }

    @EventHandler
    public void getOffGrinding(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();

        if (player.hasMetadata("rail grind")) {
            if (input.isJump() || input.isSneak()) {
                RailGrind.stopRailGrind(player);
            }
        }
    }
}
