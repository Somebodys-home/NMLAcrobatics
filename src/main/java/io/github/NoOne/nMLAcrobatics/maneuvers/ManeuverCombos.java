package io.github.NoOne.nMLAcrobatics.maneuvers;

import io.github.NoOne.nMLAcrobatics.NMLAcrobatics;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
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
        BossBar comboBar = Bukkit.createBossBar(startingManeuver, BarColor.WHITE, BarStyle.SOLID);

        comboBars.put(player.getUniqueId(), comboBar);
        maneuverCombos.put(player.getUniqueId(), new ArrayList<>(List.of(startingManeuver)));
        startComboDepleteTask(player);
    }

    public void addToCombo(Player player, String maneuver) {
        BossBar comboBar = comboBars.get(player.getUniqueId());

        if (!comboBar.getPlayers().contains(player)) {
            comboBar.addPlayer(player);
        }

        maneuverCombos.get(player.getUniqueId()).add(maneuver);
        comboBar.setProgress(1);
        comboBar.setTitle(getComboTitleString(player));
        ongoingDepletingComboTasks.remove(player.getUniqueId()).cancel();
        startComboDepleteTask(player);
    }

    private void startComboDepleteTask(Player player) {
        UUID uuid = player.getUniqueId();

        BossBar comboBar = comboBars.get(uuid);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(nmlAcrobatics, () -> { // actual task
            if (comboBar.getProgress() == 0) { // combo is fully gone
                maneuverCombos.remove(uuid);
                comboBar.removePlayer(player);
                comboBars.remove(uuid);
                ongoingDepletingComboTasks.remove(uuid).cancel();

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
        String previous = maneuverCombo.get(0);
        int count = 1;

        if (comboAmount < 5) {
            comboAmountColor = "§e";
        } else if (comboAmount < 10) {
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

    private String compactRepeats(String maneuver, int count) {
        if (count > 1) {
            return maneuver + " x" + count;
        }
        return maneuver;
    }
}
