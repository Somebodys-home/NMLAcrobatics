package io.github.NoOne.nMLAcrobatics;

import io.github.NoOne.nMLSkills.NMLSkills;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetListener;
import io.github.NoOne.nMLSkills.skillSetSystem.SkillSetManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class NMLAcrobatics extends JavaPlugin {
    private NMLSkills nmlSkills;
    private SkillSetManager skillSetManager;

    @Override
    public void onEnable() {
        nmlSkills = JavaPlugin.getPlugin(NMLSkills.class);
        skillSetManager = nmlSkills.getSkillSetManager();

        new Maneuvers(this);

        getServer().getPluginManager().registerEvents(new AcrobaticsListener(this), this);
    }

    public NMLSkills getNmlSkills() {
        return nmlSkills;
    }

    public SkillSetManager getSkillSetManager() {
        return skillSetManager;
    }
}
