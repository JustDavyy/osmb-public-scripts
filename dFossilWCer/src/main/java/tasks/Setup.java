package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dFossilWCer.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        // Check required agility level
        task = "Get agility level";
        SkillsTabComponent.SkillLevel agilitySkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.AGILITY);
        if (agilitySkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }

        if (agilitySkillLevel.getLevel() < 70) {
            script.log(getClass(), "Agility level is below 70 (" + agilitySkillLevel.getLevel() + "). Disabling usage of the shortcut.");
            useShortcut = false;
        }

        // Check required woodcutting level
        task = "Get woodcutting level";
        SkillsTabComponent.SkillLevel woodcuttingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.WOODCUTTING);
        if (woodcuttingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = woodcuttingSkillLevel.getLevel();
        currentLevel = woodcuttingSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using log basket
        task = "Check log basket usage";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
        if (inv == null) return false;

        if (inv.containsAny(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET))) {
            script.log(getClass(), "Log basket detected in inventory. Marking usage as true.");
            useLogBasket = true;
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
