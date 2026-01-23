package tasks;

import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import utils.Task;
import static main.dCannonballSmelter.*;


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

        // Check our smithing level
        task = "Get smithing level";
        SkillsTabComponent.SkillLevel smithingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.SMITHING);
        if (smithingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = smithingSkillLevel.getLevel();
        currentLevel = smithingSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(500, 1250));

        setupDone = true;
        return false;
    }
}
