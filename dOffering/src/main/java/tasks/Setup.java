package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dOffering.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = "Open inventory";
        script.log(getClass(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using a rune pouch
        task = "Check rune pouch usage";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.RUNE_POUCH, ItemID.RUNE_POUCH_23650, ItemID.RUNE_POUCH_27086, ItemID.RUNE_POUCH_L, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_L, selectedItem));
        if (inv == null) return false;

        if (inv.containsAny(Set.of(ItemID.RUNE_POUCH, ItemID.RUNE_POUCH_23650, ItemID.RUNE_POUCH_27086, ItemID.RUNE_POUCH_L, ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_L))) {
            script.log(getClass(), "A rune pouch detected in inventory. We can do 9 casts/inventory");
            castsPerInvent = 9;
        } else {
            script.log(getClass(), "No rune pouch detected in inventory. We can do 8 casts/inventory");
            castsPerInvent = 8;
        }

        if (inv.contains(selectedItem)) {
            int amount = inv.getAmount(selectedItem);
            script.log(getClass(), amount + " bones/ashes found in inventory.");

            int maxCastsFromItems = Math.floorDiv(amount, 3);
            int castsToDo = Math.min(maxCastsFromItems, castsPerInvent);
            castsThisInvent = castsPerInvent - castsToDo;

            script.log(getClass(), "We can do " + castsToDo + " casts this inventory, set castsThisInvent to: " + castsThisInvent);
        } else {
            needToBank = true;
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
