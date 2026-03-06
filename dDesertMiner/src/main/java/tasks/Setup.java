package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import main.dDesertMiner;
import utils.Task;

import static main.dDesertMiner.*;

public class Setup extends Task {
    public Setup(Script script) {
        super((dDesertMiner) script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = "Setup";
        script.log("Setup", "We are now inside the Setup task logic");

        // Check mining level
        task = "Get mining level";
        SkillsTabComponent.SkillLevel miningSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MINING);
        if (miningSkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }
        startLevel = miningSkillLevel.getLevel();
        currentLevel = miningSkillLevel.getLevel();

        if (currentLevel < 35) {
            script.log("Setup", "You need at least 35 mining to mind sandstone. Stopping script.");
            script.stop();
            return false;
        }
        
        task = "Update flags";
        setupDone = true;
        return false;
    }
}
