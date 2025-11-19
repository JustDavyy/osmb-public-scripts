package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import main.dConstructioneer;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dConstructioneer.*;

public class Bank extends Task {

    public Bank(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log(getClass(), "Bank is not visible, opening bank!");
            openBank();
            return false;
        }

        task = "Deposit items";
        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND, ItemID.SAW, ItemID.AMYS_SAW, ItemID.AMYS_SAW_OFFHAND, ItemID.CRYSTAL_SAW, ItemID.SWAMP_PASTE, ItemID.SWAMP_PASTE_22095, ItemID.LAMP, ItemID.BOOK_OF_KNOWLEDGE, selectedNail))) {
            script.log(getClass().getSimpleName(), "Deposit item by item action failed.");
            return false;
        }
        script.log(getClass(), "Deposit items succeeded.");

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        int withdrawAmount = inventorySnapshot.getFreeSlots();

        if (!script.getWidgetManager().getBank().withdraw(selectedPlank, withdrawAmount)) {
            script.log(getClass(), "Withdraw failed for " + withdrawAmount + "x " + selectedPlank);
            return false;
        }
        script.log(getClass(), "Withdraw succeeded for " + withdrawAmount + "x " + selectedPlank);

        script.getWidgetManager().getBank().close();
        return script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
    }

    private void openBank() {
        script.log(getClass(), "Searching for a bank...");
        task = "Open bank";

        // Regular bank object
        List<RSObject> banksFound = script.getObjectManager().getObjects(dConstructioneer.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dConstructioneer.BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

        // Same waiting logic after interaction
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, script.random(14000, 16000));
    }
}
