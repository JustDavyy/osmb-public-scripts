package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.*;
import java.util.function.BooleanSupplier;

import static main.dAIOFisher.*;

public class Cut extends Task {

    public Cut(Script script) {
        super(script);
    }

    public boolean activate() {
        if (!cutMode) return false;
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(31553, 31561));
        return inv != null && inv.isFull() && inv.containsAny(31553, 31561);
    }

    public boolean execute() {
        task = "Cut";

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(31553, 31561, ItemID.KNIFE));
        if (inv == null) return false;

        if (!inv.containsAny(31553, 31561)) {
            script.log("Cut", "Inventory did not contain any squids... which shouldn't happen. Stopping script!");
            script.stop();
            return false;
        }

        if (!inv.contains(ItemID.KNIFE)) {
            script.log("Cut", "Inventory does not contain a knife, we can't perform the cut task. Stopping script!");
            script.stop();
            return false;
        }

        List<Integer> cooked = cookMode ? fishingMethod.getCookedFish() : fishingMethod.getCatchableFish();

        for (int i = 0; i < cooked.size(); i++) {
            int id = cooked.get(i);
            int count = inv.getAmount(id);

            switch (i) {
                case 0 -> fish1Caught += count;
                case 1 -> fish2Caught += count;
                case 2 -> fish3Caught += count;
                case 3 -> fish4Caught += count;
                case 4 -> fish5Caught += count;
                case 5 -> fish6Caught += count;
                case 6 -> fish7Caught += count;
                case 7 -> fish8Caught += count;
            }
        }

        List<Integer> squidIds = new ArrayList<>();

        if (inv.contains(31553)) squidIds.add(31553);
        if (inv.contains(31561)) squidIds.add(31561);

        if (squidIds.isEmpty()) {
            return false; // nothing to do
        }

        Collections.shuffle(squidIds);

        Random random = new Random();

        for (int squidId : squidIds) {

            boolean knifeFirst = random.nextBoolean();

            if (knifeFirst) {
                if (!useKnife(inv)) return false;
                if (!useSquid(inv, squidId)) return false;
                waitTillDone(squidId);
            } else {
                if (!useSquid(inv, squidId)) return false;
                if (!useKnife(inv)) return false;
                waitTillDone(squidId);
            }
        }

        return false;
    }

    private boolean useKnife(ItemGroupResult inv) {
        boolean success = inv.getItem(ItemID.KNIFE).interact();
        if (!success) {
            script.log("Cut", "Failed to interact with knife, re-polling...");
        }
        return success;
    }

    private boolean useSquid(ItemGroupResult inv, int squidId) {
        boolean success = inv.getItem(squidId).interact();
        if (!success) {
            script.log("Cut", "Failed to interact with squid " + squidId + ", re-polling...");
        }
        return success;
    }

    private boolean waitTillDone(int squidId) {
        com.osmb.api.utils.timing.Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 66000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(squidId));
            if (inventorySnapshot == null) return false;

            return !inventorySnapshot.contains(squidId);
        };

        script.log("Cut", "Using human task to wait until cutting finishes.");
        return script.pollFramesHuman(condition, RandomUtils.uniformRandom(66000, 70000));
    }
}
