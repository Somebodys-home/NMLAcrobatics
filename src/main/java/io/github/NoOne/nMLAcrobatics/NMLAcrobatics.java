package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLAcrobatics.maneuvers.ManeuverCombos;
import io.github.NoOne.nMLAcrobatics.maneuvers.Maneuvers;
import io.github.NoOne.nMLAcrobatics.maneuvers.longJump.LongJumpListener;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrindListener;
import io.github.NoOne.nMLAcrobatics.maneuvers.roll.RollListener;
import io.github.NoOne.nMLPlayerStats.NMLPlayerStats;
import io.github.NoOne.nMLPlayerStats.profileSystem.ProfileManager;
import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NMLAcrobatics extends JavaPlugin {
    private static NMLAcrobatics instance;
    private ProfileManager profileManager;
    private SkillSetManager skillSetManager;
    private Maneuvers maneuvers;
    private ManeuverCombos maneuverCombos;

    @Override
    public void onEnable() {
        instance = this;
        profileManager = JavaPlugin.getPlugin(NMLPlayerStats.class).getProfileManager();
        skillSetManager = JavaPlugin.getPlugin(NMLSkills.class).getSkillSetManager();

        maneuvers = new Maneuvers(this);
        maneuvers.startWallRunTask();

        maneuverCombos = new ManeuverCombos(this);

        Speedometer.startSpeedometer();
        RailGrind.startRailGrindTask();

        getServer().getPluginManager().registerEvents(new AcrobaticsListener(this), this);
        getServer().getPluginManager().registerEvents(new RollListener(this), this);
        getServer().getPluginManager().registerEvents(new LongJumpListener(this), this);
        getServer().getPluginManager().registerEvents(new RailGrindListener(), this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata("fishing_combo", this);
        }

        maneuvers.stopTasks();
//        maneuverCombos.stop();
    }

    public static NMLAcrobatics getInstance() {
        return instance;
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
