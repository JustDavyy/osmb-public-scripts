package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dPublicAlcher.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log("DEBUG", "We are now inside the Setup task logic");

        task = "Verify config choices";
        if (alchItemID == -1 || alchItemID == ItemID.BANK_FILLER) {
            script.log("ERROR", "Item ID to alch is invalid. Stopping script.");
            script.stop();
        }

        task = "Set up items";
        setupItem(alchItemID);

        // Check required magic level
        task = "Get magic level";
        SkillsTabComponent.SkillLevel magicSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MAGIC);
        if (magicSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = magicSkillLevel.getLevel();
        currentLevel = magicSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        setupDone = true;
        return false;
    }

    private void setupItem(int itemId) {
        itemName = script.getItemManager().getItemName(itemId);
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(itemId));

        if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
            script.log("FAIL", "Could not locate " + itemName + " to alch.");
            script.stop();
        } else {
            int stack = inventorySnapshot.getAmount(itemId);
            script.log("INFO", "Item " + itemName + " found, stack: " + stack);
            stackSize = stack;
            hasReqs = true;
            itemRect = inventorySnapshot.getItem(itemId).getTappableBounds();
        }
    }
}
