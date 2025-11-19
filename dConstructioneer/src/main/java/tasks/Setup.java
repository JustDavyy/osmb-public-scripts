package tasks;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

import utils.Task;
import static main.dConstructioneer.*;


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

        // Check required construction level
        task = "Get construction level";
        SkillsTabComponent.SkillLevel constructionSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.CONSTRUCTION);
        if (constructionSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = constructionSkillLevel.getLevel();
        currentLevel = constructionSkillLevel.getLevel();

        task = "Open Inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check location
        WorldPosition ourPos = script.getWorldPosition();
        if (ourPos == null) return false;

        if (ourPos.getRegionID() != 10545) {
            script.log(getClass(), "Not at port khazard, stopping script.");
            script.stop();
            return false;
        }

        setupDone = true;
        return false;
    }
}
