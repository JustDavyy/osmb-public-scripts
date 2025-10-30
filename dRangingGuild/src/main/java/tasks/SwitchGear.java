package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Tab;
import main.ScriptUI;
import utils.Task;

import java.util.Set;

import static main.dRangingGuild.*;

public class SwitchGear extends Task {

    public SwitchGear(Script script) {
        super(script);
    }

    public boolean activate() {
        return needsToSwitchGear;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        lastTaskRanAt = System.currentTimeMillis();

        script.log(getClass().getSimpleName(), "Switching gear at Ranged level: " + rangedLevel);

        // Open inventory tab
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check inventory visibility
        ItemGroupResult inventory = script.getWidgetManager().getInventory().search(Set.of(ItemID.MAGIC_LONGBOW, ItemID.MAGIC_SHORTBOW, ItemID.BLUE_DHIDE_VAMBRACES, ItemID.BLUE_DHIDE_BODY, ItemID.BLUE_DHIDE_CHAPS, ItemID.DARK_BOW, ItemID.RED_DHIDE_VAMBRACES, ItemID.RED_DHIDE_BODY, ItemID.RED_DHIDE_CHAPS, ItemID.BLACK_DHIDE_VAMBRACES, ItemID.BLACK_DHIDE_BODY, ItemID.BLACK_DHIDE_CHAPS, ItemID.SCORCHING_BOW));
        if (inventory == null || inventory.isEmpty()) {
            script.log(getClass().getSimpleName(), "Inventory not available.");
            return false;
        }

        boolean switchedSomething = false;
        String level = String.valueOf(rangedLevel);

        script.log(getClass().getSimpleName(), "Checking gear for level: " + level);
        for (String slot : new String[]{"weapon", "gloves", "body", "legs"}) {
            int itemId = ScriptUI.getSelectedGearItemId(level, slot);
            script.log(getClass().getSimpleName(), "Slot: " + slot + " => ItemID: " + itemId);
            if (itemId == -1) continue; // None selected

            if (inventory.contains(itemId)) {
                script.getWidgetManager().getInventory().unSelectItemIfSelected();
                script.log(getClass().getSimpleName(), "Equipping " + slot + " (" + script.getItemManager().getItemName(itemId) + ")");
                switchedSomething = inventory.getItem(itemId).interact();
                script.pollFramesHuman(() -> false, script.random(500, 700));
            }
        }

        // Check if any item that was equipped is no longer in inventory
        if (switchedSomething) {
            script.pollFramesHuman(() -> false, script.random(1250, 2000));
            ItemGroupResult updatedInventory = script.getWidgetManager().getInventory().search(Set.of(ItemID.MAGIC_LONGBOW, ItemID.MAGIC_SHORTBOW, ItemID.BLUE_DHIDE_VAMBRACES, ItemID.BLUE_DHIDE_BODY, ItemID.BLUE_DHIDE_CHAPS, ItemID.DARK_BOW, ItemID.RED_DHIDE_VAMBRACES, ItemID.RED_DHIDE_BODY, ItemID.RED_DHIDE_CHAPS, ItemID.BLACK_DHIDE_VAMBRACES, ItemID.BLACK_DHIDE_BODY, ItemID.BLACK_DHIDE_CHAPS, ItemID.SCORCHING_BOW));

            if (updatedInventory == null) {
                // Inventory not visible
                return false;
            }

            boolean allEquipped = true;
            for (String slot : new String[]{"weapon", "gloves", "body", "legs"}) {
                int itemId = ScriptUI.getSelectedGearItemId(level, slot);
                if (itemId == -1) continue;

                if (updatedInventory.contains(itemId)) {
                    allEquipped = false;
                    script.log(getClass().getSimpleName(), "Still holding: " + script.getItemManager().getItemName(itemId));
                }
            }

            if (allEquipped) {
                needsToSwitchGear = false;
                script.log(getClass().getSimpleName(), "Finished switching gear for level " + level);
                return true;
            }
        } else {
            script.log(getClass().getSimpleName(), "No gear to switch for level " + level);
            needsToSwitchGear = false; // Nothing to do
        }

        return true;
    }
}