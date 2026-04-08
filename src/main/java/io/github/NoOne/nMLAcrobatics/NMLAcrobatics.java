package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLAcrobatics.maneuvers.ManeuverCombos;
import io.github.NoOne.nMLAcrobatics.maneuvers.Maneuvers;
import io.github.NoOne.nMLPlayerStats.NMLPlayerStats;
import io.github.NoOne.nMLPlayerStats.profileSystem.ProfileManager;
import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class NMLAcrobatics extends JavaPlugin {
    private ProfileManager profileManager;
    private SkillSetManager skillSetManager;
    private Maneuvers maneuvers;
    private ManeuverCombos maneuverCombos;

    @Override
    public void onEnable() {
        profileManager = JavaPlugin.getPlugin(NMLPlayerStats.class).getProfileManager();
        skillSetManager = JavaPlugin.getPlugin(NMLSkills.class).getSkillSetManager();

        maneuvers = new Maneuvers(this);
        maneuvers.rollTask();
        maneuvers.railGrindTask();
        maneuvers.wallRunTask();

        maneuverCombos = new ManeuverCombos(this);

        getServer().getPluginManager().registerEvents(new AcrobaticsListener(this), this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata("fishing_combo", this);
        }

        maneuvers.stopTasks();
        maneuverCombos.stop();
    }

    public SkillSetManager getSkillSetManager() {
        return skillSetManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public ManeuverCombos getManeuverCombos() {
        return maneuverCombos;
    }
}
