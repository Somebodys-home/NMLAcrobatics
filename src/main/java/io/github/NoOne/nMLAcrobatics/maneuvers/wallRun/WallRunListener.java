package io.github.NoOne.nMLAcrobatics.maneuvers.wallRun;

import io.github.NoOne.nMLAcrobatics.maneuvers.longJump.LongJump;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;

public class WallRunListener implements Listener {

    @EventHandler
    public void stopWallRunning(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();

        if (WallRun.isWallRunning(player)) {
            if (input.isJump()) {
                LongJump.wallJump(player);
            } else if (input.isSneak()) {
                WallRun.stopWallRunning(player);
            }
        }
    }

}
