package io.github.NoOne.nMLAcrobatics;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.github.Gabriel.expertiseStylePlugin.AbilitySystem.CooldownSystem.CooldownManager;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
    public void shiftingMetadata(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        int acrobaticsLvl = skillSetManager.getSkillSet(player.getUniqueId()).getSkills().getAcrobaticsLevel();

        if (!event.isSneaking()) return;

        if (player.hasMetadata("climb")) {
            player.setMetadata("start wall jump", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("start wall jump", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 5L);

            return;
        }
        if (!Maneuvers.getBottomBlock(player).getType().isAir() && !Maneuvers.getTopBlock(player).getType().isAir() && acrobaticsLvl >= 30) {
            player.setMetadata("start climb", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("start climb", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 5L);
        }
        else if (player.isSprinting() && acrobaticsLvl >= 10) {
            player.setMetadata("start longjump", new FixedMetadataValue(nmlAcrobatics, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    player.removeMetadata("start longjump", nmlAcrobatics);
                }
            }.runTaskLater(nmlAcrobatics, 5L);
        }
    }

    @EventHandler
    public void shiftJumpManeuver(PlayerJumpEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("start longjump")) {
            player.removeMetadata("start longjump", nmlAcrobatics);
            Maneuvers.longJump(player);
        }
        else if (player.hasMetadata("start climb")) {
            player.removeMetadata("start climb", nmlAcrobatics);
            Maneuvers.climb(player, true);
        }
    }

    @EventHandler
    public void getOffGrinding(PlayerInputEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("rail grind")) {
            if (event.getInput().isJump()) {
                player.removeMetadata("rail grind", nmlAcrobatics);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Maneuvers.railJump(player, player.getVelocity().length());
                    }
                }.runTaskLater(nmlAcrobatics, 1);
            } else if (event.getInput().isSneak()) {
                player.removeMetadata("rail grind", nmlAcrobatics);
                player.stopSound(Sound.ENTITY_MINECART_RIDING);
            }
        }
    }

    @EventHandler
    public void wallJump(PlayerInputEvent event) {
        Player player = event.getPlayer();

        if (event.getInput().isJump() && player.hasMetadata("start wall jump")) {
            player.removeMetadata("start wall jump", nmlAcrobatics);
            Maneuvers.wallJump(player);
        }
    }
}
