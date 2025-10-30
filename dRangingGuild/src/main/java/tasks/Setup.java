package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import main.dRangingGuild;
import utils.Task;

import java.util.Set;

import static main.dRangingGuild.*;

public class Setup extends Task {
    public static final Area minigameArea = new RectangleArea(2670, 3417, 3, 2, 0);
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass().getSimpleName(), "We are now inside the Setup task logic");

        // Check required ranged level
        task = "Get ranged level";
        SkillsTabComponent.SkillLevel rangedSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.RANGE);
        if (rangedSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = rangedSkillLevel.getLevel();
        currentLevel = rangedSkillLevel.getLevel();

        // Open inventory
        task = "Open inventory tab";
        script.log(dRangingGuild.class, "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if we have coins
        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.COINS_995));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (!inventorySnapshot.contains(ItemID.COINS_995)) {
            script.log(getClass().getSimpleName(), "No coins found in inventory, stopping script!");
            script.stop();
            return false;
        }

        // Check if we're in the right area
        task = "Check character position";
        if (!minigameArea.contains(script.getWorldPosition())) {
            script.log(getClass().getSimpleName(), "Not at the right area to shoot, moving there!");
            return script.getWalker().walkTo(minigameArea.getRandomPosition());
        }

        setupDone = true;
        return false;
    }
}
