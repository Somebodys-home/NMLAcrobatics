package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLAcrobatics.maneuvers.ManeuverCombos;
import io.github.NoOne.nMLAcrobatics.maneuvers.Maneuvers;
import io.github.NoOne.nMLPlayerStats.NMLPlayerStats;
import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NMLAcrobatics extends JavaPlugin {
    private NMLPlayerStats nmlPlayerStats;
    private SkillSetManager skillSetManager;
    private Maneuvers maneuvers;
    private ManeuverCombos maneuverCombos;

    @Override
    public void onEnable() {
        nmlPlayerStats = JavaPlugin.getPlugin(NMLPlayerStats.class);
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
        maneuvers.stopTasks();
        maneuverCombos.stop();
    }

    public SkillSetManager getSkillSetManager() {
        return skillSetManager;
    }

    public NMLPlayerStats getNmlPlayerStats() {
        return nmlPlayerStats;
    }

    public ManeuverCombos getManeuverCombos() {
        return maneuverCombos;
    }
}
