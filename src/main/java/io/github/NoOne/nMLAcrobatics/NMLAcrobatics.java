package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLAcrobatics.maneuvers.ManeuverCombos;
import io.github.NoOne.nMLAcrobatics.maneuvers.longJump.LongJumpListener;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrind;
import io.github.NoOne.nMLAcrobatics.maneuvers.railGrind.RailGrindListener;
import io.github.NoOne.nMLAcrobatics.maneuvers.roll.RollListener;
import io.github.NoOne.nMLAcrobatics.maneuvers.wallRun.WallRun;
import io.github.NoOne.nMLAcrobatics.maneuvers.wallRun.WallRunListener;
import io.github.NoOne.nMLPlayerStats.NMLPlayerStats;
import io.github.NoOne.nMLPlayerStats.StatConfigManager;
import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.SaveSkillsManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NMLAcrobatics extends JavaPlugin {
    private static NMLAcrobatics instance;
    private StatConfigManager statConfigManager;
    private SaveSkillsManager saveSkillsManager;
    private ManeuverCombos maneuverCombos;

    @Override
    public void onEnable() {
        instance = this;
        statConfigManager = JavaPlugin.getPlugin(NMLPlayerStats.class).getStatConfigManager();
        saveSkillsManager = JavaPlugin.getPlugin(NMLSkills.class).getSaveSkillsManager();
        maneuverCombos = new ManeuverCombos(this);

        Speedometer.startSpeedometer();
        RailGrind.startRailGrindTask();
        RailGrind.startEffectsTask();
        WallRun.startWallRunTask();

        getServer().getPluginManager().registerEvents(new AcrobaticsListener(this), this);
        getServer().getPluginManager().registerEvents(new RollListener(this), this);
        getServer().getPluginManager().registerEvents(new LongJumpListener(this), this);
        getServer().getPluginManager().registerEvents(new RailGrindListener(), this);
        getServer().getPluginManager().registerEvents(new WallRunListener(), this);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata("fishing_combo", this);
        }

//        maneuverCombos.stop();
    }

    public static NMLAcrobatics getInstance() {
        return instance;
    }

    public SaveSkillsManager getSaveSkillsManager() {
        return saveSkillsManager;
    }

    public StatConfigManager getStatConfigManager() {
        return statConfigManager;
    }

    public ManeuverCombos getManeuverCombos() {
        return maneuverCombos;
    }
}
