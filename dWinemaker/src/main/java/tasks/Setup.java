package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import main.dWinemaker;
import utils.Task;
import static main.dWinemaker.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {

        // Check required cooking level
        task = "Get cooking level";
        SkillsTabComponent.SkillLevel cookingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.COOKING);
        if (cookingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = cookingSkillLevel.getLevel();
        currentLevel = cookingSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Update flags";
        setupDone = true;
        hasReqs = true;
        shouldBank = true;
        return false;
    }
}
