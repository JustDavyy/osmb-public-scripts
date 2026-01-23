package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
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
        task = "BANK";
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log("BANK", "Bank is not visible, opening bank!");
            openBank();
            return false;
        }

        task = "Deposit items";
        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND, ItemID.SAW, ItemID.AMYS_SAW, ItemID.AMYS_SAW_OFFHAND, ItemID.CRYSTAL_SAW, ItemID.SWAMP_PASTE, ItemID.SWAMP_PASTE_22095, ItemID.LAMP, ItemID.BOOK_OF_KNOWLEDGE, selectedNail))) {
            script.log("BANK", "Deposit item by item action failed.");
            return false;
        }
        script.log("BANK", "Deposit items succeeded.");

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log("BANK", "Inventory not visible.");
            return false;
        }

        int withdrawAmount = inventorySnapshot.getFreeSlots();

        if (selectedType.equalsIgnoreCase("large hull parts")) {
            withdrawAmount = 25;
        }

        // Ironwood (31435) and Rosewood (31438) planks:
        // 1 plank makes 3 repair kits → divide free slots by 3 and round down
        if (selectedBaseMaterialId == 31435 || selectedBaseMaterialId == 31438) {
            withdrawAmount = withdrawAmount / 3;
        }

        if (withdrawAmount <= 0) {
            script.log("BANK", "No space to withdraw base material: " + selectedBaseMaterialId);
            return false;
        }

        if (!script.getWidgetManager().getBank().withdraw(selectedBaseMaterialId, withdrawAmount)) {
            script.log("BANK", "Withdraw failed for " + withdrawAmount + "x " + selectedBaseMaterialId);
            return false;
        }
        script.log("BANK", "Withdraw succeeded for " + withdrawAmount + "x " + selectedBaseMaterialId);

        script.getWidgetManager().getBank().close();
        return script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), RandomUtils.uniformRandom(4000, 6000));
    }

    private void openBank() {
        script.log("BANK", "Searching for a bank...");
        task = "Open bank";

        // Regular bank object
        List<RSObject> banksFound = script.getObjectManager().getObjects(dConstructioneer.bankQuery);
        if (banksFound.isEmpty()) {
            script.log("BANK", "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dConstructioneer.BANK_ACTIONS)) {
            script.log("BANK", "Failed to interact with bank object.");
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
        }, RandomUtils.uniformRandom(14000, 16000));
    }
}
