package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import utils.Task;
import static main.dAmethystMiner.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        // Check mining level
        task = "Get mining level";
        SkillsTabComponent.SkillLevel miningSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.FISHING);
        if (miningSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startMiningLevel = miningSkillLevel.getLevel();
        currentMiningLevel = miningSkillLevel.getLevel();

        // Check crafting level
        task = "Get crafting level";
        SkillsTabComponent.SkillLevel craftingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.COOKING);
        if (craftingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startCraftingLevel = craftingSkillLevel.getLevel();
        currentCraftingLevel = craftingSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        
        task = "Update flags";
        setupDone = true;
        return false;
    }
}
