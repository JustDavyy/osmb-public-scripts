package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import utils.Task;
import static main.dTeleporter.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass(), "We are now inside the Setup task logic");

        // Check required magic level
        task = "Get magic level";
        SkillsTabComponent.SkillLevel magicSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MAGIC);
        if (magicSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = magicSkillLevel.getLevel();
        currentLevel = magicSkillLevel.getLevel();

        task = "Open magic tab";
        script.log(getClass().getSimpleName(), "Opening magic tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.SPELLBOOK);

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
