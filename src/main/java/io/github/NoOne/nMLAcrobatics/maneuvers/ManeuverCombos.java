package io.github.NoOne.nMLAcrobatics.maneuvers;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import io.github.NoOne.nMLSkills.skillSystem.SkillBars;
import io.github.NoOne.nMLSkills.skillSystem.SkillChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ManeuverCombos {
    private NMLAcrobatics nmlAcrobatics;
    private HashMap<UUID, ArrayList<String>> maneuverCombos;
    private HashMap<UUID, BossBar> comboBars;
    private final HashMap<UUID, BukkitTask> ongoingDepletingComboTasks = new HashMap<>();

    public ManeuverCombos(NMLAcrobatics nmlAcrobatics) {
        this.nmlAcrobatics = nmlAcrobatics;
        maneuverCombos = new HashMap<>();
        comboBars = new HashMap<>();
    }

    public void stop() {
        for (UUID uuid : comboBars.keySet()) {
            comboBars.get(uuid).removeAll();
        }
    }

    public void startCombo(Player player, String startingManeuver) {
        if (!player.hasMetadata("fishing_combo")) {
            BossBar comboBar = Bukkit.createBossBar(startingManeuver, BarColor.WHITE, BarStyle.SOLID);

            comboBars.put(player.getUniqueId(), comboBar);
            maneuverCombos.put(player.getUniqueId(), new ArrayList<>(List.of(startingManeuver)));
            startComboDepleteTask(player);
        }
    }

    public void addToCombo(Player player, String maneuver) {
        BossBar comboBar = comboBars.get(player.getUniqueId());

        if (!comboBar.getPlayers().contains(player)) {
            comboBar.addPlayer(player);
        }

        maneuverCombos.get(player.getUniqueId()).add(maneuver);
        comboBar.setProgress(1);
        comboBar.setTitle(getComboTitleString(player));

        if (ongoingDepletingComboTasks.containsKey(player.getUniqueId())) {
            ongoingDepletingComboTasks.remove(player.getUniqueId()).cancel();
        }

        if (!maneuver.equals("Rail Grind")) {
            startComboDepleteTask(player);
        }
    }

    public void startComboDepleteTask(Player player) {
        UUID uuid = player.getUniqueId();

        BossBar comboBar = comboBars.get(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(nmlAcrobatics, () -> { // actual task
            if (comboBar.getProgress() == 0) { // combo is done
                if (comboBar.getPlayers().contains(player)) {
                    endCombo(player);
                }

                return;
            }

            comboBar.setProgress(Math.max(comboBar.getProgress() - .025, 0));
        }, 0L, 1L);

        ongoingDepletingComboTasks.put(uuid, task); // put the task on the hashmap
    }

    public boolean alreadyStartedCombo(Player player) {
        return comboBars.containsKey(player.getUniqueId());
    }

    public String getComboTitleString(Player player) {
        ArrayList<String> maneuverCombo = maneuverCombos.get(player.getUniqueId());
        int comboAmount = maneuverCombo.size();
        String comboAmountColor;
        String comboTitle = "";
        String previous = maneuverCombo.getFirst();
        int count = 1;

        if (comboAmount < 10) {
            comboAmountColor = "§e";
        } else if (comboAmount < 20) {
            comboAmountColor = "§b";
        } else {
            comboAmountColor = "§d";
        }

        for (int i = 1; i < maneuverCombo.size(); i++) {
            String current = maneuverCombo.get(i);

            if (current.equals(previous)) {
                count++;
            } else {
                comboTitle += compactRepeats(previous, count) + " + ";
                previous = current;
                count = 1;
            }
        }

        comboTitle += compactRepeats(previous, count);

        if (comboTitle.length() > 28) {
            int substringStart = comboTitle.length() - 28;
            comboTitle = "..." + comboTitle.substring(substringStart);
        }

        return "(" + comboAmountColor + "§l" + comboAmount + "x§r) " + comboTitle;
    }

    private void endCombo(Player player) {
        UUID uuid = player.getUniqueId();
        ArrayList<String> maneuverCombo = maneuverCombos.get(uuid);
        int comboSize = maneuverCombo.size();
        String comboTitle;
        int totalRolls = 0;
        int totalLongJumps = 0;
        int totalRailGrinds = 0;
        int totalWallRuns = 0;

        // title
        if (comboSize > 1) {
            if (comboSize < 10) {
                comboTitle = "§e" + comboSize + "x Combo";
            } else if (comboSize < 20) {
                comboTitle = "§b" + comboSize + "x Combo!";
            } else {
                comboTitle = "§d" + comboSize + "x COMBO!";
            }
        } else {
            comboTitle = "";
        }

        // calculate exp gain
        for (String string : maneuverCombo) {
            switch (string) {
                case "Roll", "Roll Brace" -> totalRolls++;
                case "Long Jump", "Rail Jump", "Wall Jump" -> totalLongJumps++;
                case "Rail Grind" -> totalRailGrinds++;
                case "Wall Run" -> totalWallRuns++;
            }
        }

        int expGain = totalRolls * 7 + totalLongJumps * 10 + totalRailGrinds * 25 + totalWallRuns * 30;
            expGain += (int) Math.round((expGain / 2.0) * (comboSize / 100.0));
            expGain /= 3; /// temporary
        double expGainPerTick = Math.min(9, expGain / 20.0);
        int ticks = (int) Math.ceil(expGain / expGainPerTick);

        // actually finishing the combo
        player.sendTitle(comboTitle, "§7 + " + expGain + " exp!", 5, 50, 5);
        player.setMetadata("fishing_combo", new FixedMetadataValue(nmlAcrobatics, true));

        int finalExpGain = expGain;
        new BukkitRunnable() {
            double expLeft = finalExpGain;

            @Override
            public void run() {
                expLeft -= expGainPerTick;

                if (expLeft <= 0) {
                    expLeft = 0;
                }

                player.sendTitle(comboTitle, "§7 + " + Math.round(expLeft) + " exp!", 0, 30, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 2f, 1f);

                if (expLeft == 0) {
                    player.removeMetadata("fishing_combo", nmlAcrobatics);
                    cancel();
                }
            }
        }.runTaskTimer(nmlAcrobatics, 15L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().callEvent(new SkillChangeEvent(player, "acrobaticsexp", finalExpGain, ticks));
            }
        }.runTaskLater(nmlAcrobatics, 15L);

        // clearing data
        maneuverCombos.remove(uuid);
        comboBars.remove(uuid).removePlayer(player);
        ongoingDepletingComboTasks.remove(uuid).cancel();
    }

    private String compactRepeats(String maneuver, int count) {
        if (count > 1) {
            return maneuver + " x" + count;
        }

        return maneuver;
    }
}
