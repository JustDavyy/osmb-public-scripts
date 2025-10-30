package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dSawmillRunner.*;

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

        if (useRingOfElements) {
            task = "Open equipment tab";
            script.log(getClass().getSimpleName(), "Opening equipment tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.EQUIPMENT);

            // Check if Ring of Elements is equipped
            Equipment equipment = script.getWidgetManager().getEquipment();
            boolean equipped = equipment.isEquipped(ItemID.RING_OF_THE_ELEMENTS_26818).isFound();

            if (!equipped) {
                script.log(getClass(), "Ring of Elements not equipped, while marked as use = TRUE. Stopping script.");
                script.stop();
                return false;
            }
        }

        task = "Open inventory tab";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using sawmill vouchers
        task = "Check for Sawmill vouchers";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.SAWMILL_VOUCHER));
        if (inv == null) return false;
        if (inv.contains(ItemID.SAWMILL_VOUCHER)) {
            useVouchers = true;
            script.log(getClass(), "Sawmill vouchers detected, marking usage as true.");
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
