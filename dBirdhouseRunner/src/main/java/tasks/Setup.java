package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dBirdhouseRunner.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass().getSimpleName(), "We are now inside the Setup task logic");

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Checking inventory for BH materials";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(
                selectedSeedId, ItemID.HAMMER, usedLogsId, ItemID.CHISEL
        ));

        if (inventorySnapshot == null) return false;

        if (!inventorySnapshot.contains(selectedSeedId) ||
                !inventorySnapshot.contains(ItemID.HAMMER) ||
                inventorySnapshot.getAmount(usedLogsId) < 4 ||
                !inventorySnapshot.contains(ItemID.CHISEL)) {
            script.log(getClass().getSimpleName(), "Missing required BH items, flagging bank!");
            needToBank = true;
        }

        if (enableSeaweedRun) {
            task = "Checking inventory for SW materials";
            inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(
                    ItemID.SEAWEED_SPORE, ItemID.COMPOST, ItemID.SUPERCOMPOST, ItemID.ULTRACOMPOST, ItemID.SEED_DIBBER, ItemID.SPADE, ItemID.RAKE
            ));

            if (inventorySnapshot == null) return false;

            if (!inventorySnapshot.contains(ItemID.SEAWEED_SPORE) ||
                    !inventorySnapshot.contains(ItemID.SEED_DIBBER) ||
                    !inventorySnapshot.contains(ItemID.SPADE) ||
                    !inventorySnapshot.contains(ItemID.RAKE)) {
                script.log(getClass().getSimpleName(), "Missing required SW items, flagging bank!");
                needToBank = true;
            }

            if (!needToBank) {
                if (compostId != 0) {
                    if (!inventorySnapshot.containsAny(ItemID.COMPOST, ItemID.SUPERCOMPOST, ItemID.ULTRACOMPOST)) {
                        script.log(getClass().getSimpleName(), "Missing " + script.getItemManager().getItemName(compostId) + ", flagging bank!");
                        needToBank = true;
                    }
                }
            }
        }

        currentPos = script.getWorldPosition();

        if (currentPos == null) {
            script.log(getClass(), "⚠ World position is null, cannot verify current location. Returning!");
            return false;
        }

        setupDone = true;
        return false;
    }
}
