package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import data.FishingLocation;
import utils.Task;
import java.util.*;

import static main.dAIOFisher.*;

public class Travel extends Task {

    public Travel(Script script) {
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

        return !inFishingArea && !inventoryFull && !hasFish;
    }

    public boolean execute() {
        task = "Travel";

        WorldPosition myPos = script.getWorldPosition();

        if (!fishingLocation.getFishingArea().contains(myPos)) {
            task = "Moving to fishing area";
            return script.getWalker().walkTo(fishingLocation.getFishingArea().getRandomPosition());
        }

        return false;
    }
}
