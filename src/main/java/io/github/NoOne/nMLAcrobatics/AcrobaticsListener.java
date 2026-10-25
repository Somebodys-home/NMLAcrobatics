package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLAcrobatics.maneuvers.Maneuvers;
import io.github.NoOne.nMLAcrobatics.maneuvers.PerformedManeuverEvent;
import io.github.NoOne.nMLAcrobatics.maneuvers.roll.Roll;
import io.github.NoOne.nMLPlayerStats.profileSystem.ProfileManager;
import io.github.NoOne.nMLPlayerStats.statSystem.StatChangeEvent;
import io.github.NoOne.nMLPlayerStats.statSystem.Stats;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import io.github.NoOne.nMLSkills.skillSystem.SkillChangeEvent;
import io.github.NoOne.nMLSkills.skillSystem.Skills;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class AcrobaticsListener implements Listener {
    private NMLAcrobatics nmlAcrobatics;
    private SkillSetManager skillSetManager;
    private ProfileManager profileManager;

    public AcrobaticsListener(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
        skillSetManager = nmlAcrobatics.getSkillSetManager();
        profileManager = nmlAcrobatics.getProfileManager();
    }

    @EventHandler
    public void acrobaticsSkillLevelUp(SkillChangeEvent event) {
        if (event.getChange() > 0 && event.getSkill().equals("acrobatics")) {
            Player player = event.getPlayer();
            Skills skills = skillSetManager.getSkillSet(player.getUniqueId()).getSkills();
            Stats stats = profileManager.getPlayerProfile(player.getUniqueId()).getStats();
            int change = (int) event.getChange();
            int prevlevel = Math.max(skills.getAcrobaticsLevel() - change, 1);
            int newlevel = skills.getAcrobaticsLevel();

            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            /// chat message
            player.sendMessage("§f§l---------------------------");
            player.sendMessage("§f§lACROBATICS SKILL LEVEL UP!");
            player.sendMessage("§fLv. §r§8" + prevlevel + " -> §r§b" + newlevel + " §r§fAcrobat");
            player.sendMessage("");

            if (newlevel >= 10 && prevlevel < 10) {
                player.sendMessage("§b§lMANEUVER LEARNED: LONG JUMP");
                player.sendMessage("§8§o(Run + Shift + Jump)");
                player.sendMessage("§f§oIt's a bigger jump! Neato!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
                player.sendMessage("");
            }

            if (newlevel >= 20 && prevlevel < 20) {
                player.sendMessage("§b§lMANEUVER LEARNED: RAIL GRIND");
                player.sendMessage("§f§oGrind on rails after a long jump!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
                player.sendMessage("");
            }

            if (newlevel >= 30 && prevlevel < 30) {
                player.sendMessage("§b§lMANEUVER LEARNED: WALL RUN");
                player.sendMessage("§f§oRun across walls after a long jump!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
                player.sendMessage("");
            }

            if (newlevel % 2 == 0) {
                int speedChange = Math.max((newlevel - prevlevel) / 2, 1);

                if (prevlevel < 2 && newlevel >= 2) {
                    speedChange++;
                }

                player.sendMessage("§7§lREWARDS:");
                player.sendMessage("§8" + stats.getSpeed() + " -> §r§f" + (stats.getSpeed() + speedChange) + " §r§fSpeed ✦ §r§b(+" + speedChange + ")");
                Bukkit.getPluginManager().callEvent(new StatChangeEvent(player, "speed", speedChange));
            }

            player.sendMessage("§f§l---------------------------");

            // firework
            Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation().add(0, 2, 0), EntityType.FIREWORK_ROCKET);
            FireworkMeta fireworkMeta = firework.getFireworkMeta();

            fireworkMeta.addEffect(FireworkEffect.builder()
                    .withColor(Color.fromRGB(255, 255, 255))
                    .withFade(Color.AQUA)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .flicker(true)
                    .withTrail()
                    .build());
            fireworkMeta.setPower(0);
            firework.setFireworkMeta(fireworkMeta);
            firework.setMetadata("no_damage", new FixedMetadataValue(nmlAcrobatics, true));
            firework.detonate();
        }
    }

    // listener for setting the sneak start for rolls and long jump
    @EventHandler
    public void sneakStart(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();

        if (!player.hasMetadata("sneak_start")) {
            player.setMetadata("sneak_start", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("sneak_start", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 5);
        } else if (!player.hasMetadata("roll cooldown")) { // have to put the logic for rolling here cuz if it was in its own class the listeners would clash
            player.removeMetadata("sneak_start", nmlAcrobatics);

            if (player.isOnGround()) {
                Roll.roll(player);
            } else { // roll brace
                player.setMetadata("roll brace", new FixedMetadataValue(nmlAcrobatics, true));
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void wallJump(PlayerInputEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("wall run") && event.getInput().isJump()) {
            Maneuvers.wallJump(player);
        }
    }

    @EventHandler
    public void stopWallRunning(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking() && player.hasMetadata("wall run")) {
            Maneuvers.stopWallRunning(player);
        }
    }

    @EventHandler
    public void onPerformManeuver(PerformedManeuverEvent event) {
//        Player player = event.getPlayer();
//        ManeuverCombos maneuverCombos = nmlAcrobatics.getManeuverCombos();
//
//        if (!maneuverCombos.alreadyStartedCombo(player)) {
//            maneuverCombos.startCombo(player, event.getManeuver());
//        } else {
//            maneuverCombos.addToCombo(player, event.getManeuver());
//        }
    }
}
