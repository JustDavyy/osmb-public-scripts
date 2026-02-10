package tasks;

import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import static main.dMLMv2.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        paintTask = "Setup";
        script.log("Setup", "We are now inside the Setup task logic");

        // Check required mining level
        paintTask = "Get mining level";
        SkillsTabComponent.SkillLevel miningSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MINING);
        if (miningSkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }
        startLevel = miningSkillLevel.getLevel();
        currentLevel = miningSkillLevel.getLevel();
        script.log("Setup", "Start mining level: " + startLevel);

        if (currentLevel < 30) {
            script.log("Setup", "30 mining is required to do this activity. Mining level: " + currentLevel);
            script.log("Setup", "Stopping script!");
            script.stop();
            return false;
        }

        paintTask = "Open inventory tab";
        script.log("Setup", "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        paintTask = "Update flags";
        setupDone = true;
        return false;
    }
}
