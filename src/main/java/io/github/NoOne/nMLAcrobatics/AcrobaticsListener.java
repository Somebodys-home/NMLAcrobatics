package io.github.NoOne.nMLAcrobatics;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.NoOne.expertiseStylePlugin.abilitySystem.cooldownSystem.CooldownManager;
import io.github.NoOne.nMLAcrobatics.maneuvers.ManeuverCombos;
import io.github.NoOne.nMLAcrobatics.maneuvers.Maneuvers;
import io.github.NoOne.nMLAcrobatics.maneuvers.PerformedManeuverEvent;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import io.github.NoOne.nMLSkills.skillSystem.SkillChangeEvent;
import io.github.NoOne.nMLSkills.skillSystem.Skills;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Input;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class AcrobaticsListener implements Listener {
    private final NMLAcrobatics nmlAcrobatics;
    private final SkillSetManager skillSetManager;
    private static final HashMap<UUID, Long> lastSneakToggle = new HashMap<>();
    private static final long inputThreshold = 300;

    public AcrobaticsListener(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
        skillSetManager = nmlAcrobatics.getSkillSetManager();
    }

    @EventHandler
    public void acrobaticsSkillLevelUp(SkillChangeEvent event) {
        if (event.getChange() > 0 && event.getSkill().equals("acrobatics")) {
            Player player = event.getPlayer();
            Skills skills = skillSetManager.getSkillSet(player.getUniqueId()).getSkills();
            int change = (int) event.getChange();
            int prevlevel = Math.max(skills.getAcrobaticsLevel() - change, 1);
            int newlevel = skills.getAcrobaticsLevel();

            player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

            /// chat message
            player.sendMessage("§f§l---------------------------");
            player.sendMessage("§f§lACROBATICS SKILL LEVEL UP!");
            player.sendMessage("§fLv. §r§8" + prevlevel + " -> §r§b" + newlevel + " §r§fAcrobat");
            player.sendMessage("");

            if (newlevel == 10) {
                player.sendMessage("§b§lMANEUVER LEARNED: LONG JUMP");
                player.sendMessage("§8§o(Run + Shift + Jump)");
                player.sendMessage("§f§oIt's a bigger jump! Neato!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
            } else if (newlevel == 20) {
                player.sendMessage("§b§lMANEUVER LEARNED: RAIL GRIND");
                player.sendMessage("§f§oGrind on rails after a long jump!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
            } else if (newlevel == 30) {
                player.sendMessage("§b§lMANEUVER LEARNED: WALL JUMP");
                player.sendMessage("§f§oRun across walls!");
                player.sendMessage("");
                player.sendMessage("§7§oYou can view this maneuver in");
                player.sendMessage("§7§othe §n/skills§r§7§o menu");
            } else {
                player.sendMessage("§7§lREWARDS:");
                player.sendMessage("§8§o(Nothing yet lmao)");
            }

            player.sendMessage("§f§l---------------------------");

            /// firework
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
            firework.setMetadata("ability_firework", new FixedMetadataValue(nmlAcrobatics, true));
            firework.detonate();
        }
    }

    @EventHandler
    public void roll(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long lastTime = lastSneakToggle.getOrDefault(player.getUniqueId(), 0L);

        if (now - lastTime <= inputThreshold && (!player.hasMetadata("roll cooldown") && !player.hasMetadata("roll brace"))) {
            if (player.isOnGround()) {
                Maneuvers.roll(player);
            }
            else { // roll brace
                player.setMetadata("roll brace", new FixedMetadataValue(nmlAcrobatics, true));
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
            }
        }

        lastSneakToggle.put(player.getUniqueId(), now);
    }

    @EventHandler
    public void removeRollBraceMetadata(PlayerMoveEvent event) { // remove roll brace metadata when hitting the ground
        Player player = event.getPlayer();

        if (!player.hasMetadata("roll brace")) return;

        if (player.isOnGround()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("roll brace", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 1);
        }
    }

    @EventHandler
    public void actualRollBraceFromDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (player.hasMetadata("roll brace")) {
                event.setDamage(event.getDamage() / 2);
                Maneuvers.rollBrace(player);
                CooldownManager.putAllAbilitiesOnCooldown(player, 1.5);
                player.setMetadata("roll cooldown", new FixedMetadataValue(nmlAcrobatics, true));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.removeMetadata("roll cooldown", nmlAcrobatics);
                    }
                }.runTaskLater(nmlAcrobatics, 30);
            }
        }
    }

    @EventHandler
    public void startLongJumpMetadata(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        int acrobaticsLvl = skillSetManager.getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

        if (!event.isSneaking()) return;
        if (player.isSprinting() && acrobaticsLvl >= 10) {
            player.setMetadata("start long jump", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("start long jump", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 5L);
        }
    }

    @EventHandler
    public void actualLongJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("start long jump")) {
            player.removeMetadata("start long jump", nmlAcrobatics);
            Maneuvers.longJump(player);
        }

    }

    @EventHandler
    public void getOffGrinding(PlayerInputEvent event) {
        Player player = event.getPlayer();
        Input input = event.getInput();

        if (player.hasMetadata("rail grind")) {
            if (input.isJump()) {
                Maneuvers.stopRailGrinding(player);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Maneuvers.railJump(player, player.getVelocity().length());
                    }
                }.runTaskLater(nmlAcrobatics, 1);
            } else if (input.isSneak()) {
                Maneuvers.stopRailGrinding(player);
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
        Player player = event.getPlayer();
        ManeuverCombos maneuverCombos = nmlAcrobatics.getManeuverCombos();

        if (!maneuverCombos.alreadyStartedCombo(player)) {
            maneuverCombos.startCombo(player, event.getManeuver());
        } else {
            maneuverCombos.addToCombo(player, event.getManeuver());
        }
    }
}
