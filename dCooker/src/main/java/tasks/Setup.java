package tasks;

import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

import utils.Task;
import static main.dCooker.*;


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

        // Check required cooking level
        task = "Get cooking level";
        SkillsTabComponent.SkillLevel cookingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.COOKING);
        if (cookingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = cookingSkillLevel.getLevel();
        currentLevel = cookingSkillLevel.getLevel();

        task = "Open Inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        setupDone = true;
        return false;
    }
}
