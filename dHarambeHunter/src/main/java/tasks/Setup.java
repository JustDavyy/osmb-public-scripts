package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import utils.Task;

import static main.dHarambeHunter.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass(), "We are now inside the Setup task logic");

        // Check location
        task = "Check location";
        int regionID = script.getWorldPosition().getRegionID();

        if (regionID != 11662) {
            script.log(getClass(), "Not in the harambe hunting area, stopping script!");
            script.stop();
        }

        // Check our hunter level
        task = "Get hunter level";
        SkillsTabComponent.SkillLevel hunterSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.HUNTER);
        if (hunterSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = hunterSkillLevel.getLevel();
        currentLevel = hunterSkillLevel.getLevel();

        if (currentLevel < 60) {
            script.log(getClass(), "You need at least 60 hunter to hunt these creatures... You have:" + currentLevel);
            script.log(getClass(), "Stopping script!");
            return false;
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
