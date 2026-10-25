package io.github.NoOne.nMLAcrobatics.maneuvers.longJump;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LongJumpListener implements Listener {
    private NMLAcrobatics nmlAcrobatics;
    private SkillSetManager skillSetManager;

    public LongJumpListener(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
        skillSetManager = nmlAcrobatics.getSkillSetManager();
    }

    @EventHandler
    public void longJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        int acrobaticsLvl = skillSetManager.getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

        if (player.hasMetadata("sneak_start") && player.isSprinting() && acrobaticsLvl >= 10) {
            player.removeMetadata("sneak_start", nmlAcrobatics);
            LongJump.longJump(player);
        }
    }
}
