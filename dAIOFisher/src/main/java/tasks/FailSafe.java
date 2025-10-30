package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dAIOFisher.*;

public class FailSafe extends Task {

    public FailSafe(Script script) {
        super(script);
    }

    public boolean activate() {
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getAllFish()));
        if (inventorySnapshot == null) return false;

        boolean hasFish = inventorySnapshot.containsAny(Set.copyOf(fishingMethod.getAllFish()));
        boolean inventoryFull = inventorySnapshot.isFull();
        boolean inFishingArea = fishingLocation.getFishingArea().contains(myPos);
        boolean inBankArea = fishingLocation.getBankArea().contains(myPos);

        if (inBankArea && hasFish) {
            return false;
        }

        return !inFishingArea && !inventoryFull;
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        WorldPosition myPos = script.getWorldPosition();

        if (!fishingLocation.getFishingArea().contains(myPos)) {
            script.log(getClass(), "Moving to fishing area.");
            task = "Moving to fishing area";
            return script.getWalker().walkTo(fishingLocation.getFishingArea().getRandomPosition());
        }

        return false;
    }
}
