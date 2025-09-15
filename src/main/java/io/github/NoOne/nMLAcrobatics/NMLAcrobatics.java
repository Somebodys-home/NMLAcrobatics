package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NMLAcrobatics extends JavaPlugin {
    private NMLSkills nmlSkills;
    private SkillSetManager skillSetManager;
    private Maneuvers maneuvers;

    @Override
    public void onEnable() {
        nmlSkills = JavaPlugin.getPlugin(NMLSkills.class);
        skillSetManager = nmlSkills.getSkillSetManager();

        maneuvers = new Maneuvers(this);
        maneuvers.startRailGrindTask();


        getServer().getPluginManager().registerEvents(new AcrobaticsListener(this), this);
    }

    @Override
    public void onDisable() {
        maneuvers.stopRailGrindTask();
    }

    public NMLSkills getNmlSkills() {
        return nmlSkills;
    }

    public SkillSetManager getSkillSetManager() {
        return skillSetManager;
    }
}
