package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static main.dGemstoneCrabber.*;

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

        // Check location
        task = "Check location";
        int regionID = script.getWorldPosition().getRegionID();

        // Allowed region IDs
        Set<Integer> allowedRegions = Set.of(
                4913, 4912, 4911,
                5169, 5168, 5167,
                5425, 5424, 5423
        );

        if (!allowedRegions.contains(regionID)) {
            script.log(getClass(), "Not in any of the allowed regions, stopping script!");
            script.stop();
        }

        // Check pots if using pots
        if (usePot) {
            List<Integer> drinkOrder = getPotionVariantOrder(potID);
            Set<Integer> idSet = new HashSet<>(drinkOrder);

            ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
            if (inv == null) return false;

            int potDoses = totalPotionAmount(inv, drinkOrder);

            needToBank = potDoses == 0;
        }

        // Check food if using food
        if (useFood) {
            // Build ordered list of candidate IDs (lowest → highest)
            List<Integer> eatOrder = getFoodVariantOrder(foodID);
            java.util.Set<Integer> idSet = new java.util.HashSet<>(eatOrder);

            ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
            if (inv == null) return false;

            // Count ALL variants before eating
            int foodTotal = totalAmount(inv, eatOrder);

            needToBank = foodTotal == 0;
        }

        // Check for dragon battleaxe if using it
        if (useDBAXE) {
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.DRAGON_BATTLEAXE));
            if (inv == null) return false;

            if (!inv.contains(ItemID.DRAGON_BATTLEAXE)) {
                script.log(getClass(), "Dragon battle axe usage enabled but not found in inventory, stopping script!");
                script.stop();
                return false;
            }
        }

        // Check for Imbued or Saturated heart if using it
        if (useHearts) {
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.IMBUED_HEART, ItemID.SATURATED_HEART));
            if (inv == null) return false;

            if (inv.contains(ItemID.IMBUED_HEART)) {
                heartID = ItemID.IMBUED_HEART;
                script.log(getClass(), "Imbued heart found!");
            } else if (inv.contains(ItemID.SATURATED_HEART)) {
                heartID = ItemID.SATURATED_HEART;
                script.log(getClass(), "Saturated heart found!");
            } else {
                script.log(getClass(), "Both hearts were not found, while usage is enabled. Stopping script!");
                script.stop();
                return false;
            }
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
