package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import main.dCamTorumMiner;
import utils.Task;
import static main.dCamTorumMiner.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        // Check required mining level
        task = "Get mining level";
        SkillsTabComponent.SkillLevel miningSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MINING);
        if (miningSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = miningSkillLevel.getLevel();
        currentLevel = miningSkillLevel.getLevel();

        task = "Open inventory";
        script.log(dCamTorumMiner.class, "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Update flags";
        setupDone = true;
        hasReqs = true;
        return false;
    }
}
