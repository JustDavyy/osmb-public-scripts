package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Set;
import java.util.function.BooleanSupplier;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import utils.Task;

import static main.dWinemaker.*;

public class ProcessTask extends Task {
    private final long startTime;

    public ProcessTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        return hasReqs && !shouldBank;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(grapeID, ItemID.JUG_OF_WATER));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        task = "Check inventory";
        if (!inventorySnapshot.contains(grapeID) || !inventorySnapshot.contains(ItemID.JUG_OF_WATER)) {
            script.log(getClass().getSimpleName(), "Missing ingredients. Flagging bank.");
            task = "Flag bank";
            shouldBank = true;
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        boolean interacted = interactAndWaitForDialogue(inventorySnapshot);

        if (!interacted) {
            script.log(getClass().getSimpleName(), "Failed to interact with items.");
            return false;
        }

        task = "Select dialogue item";
        // Dialogue opened - select item to produce
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(ItemID.JUG_OF_WINE, grapeID, ItemID.WINE_OF_ZAMORAK, ItemID.UNFERMENTED_WINE, ItemID.UNFERMENTED_WINE_1996, ItemID.ZAMORAKS_UNFERMENTED_WINE);
            if (!selected) {
                script.log(getClass().getSimpleName(), "Initial selection failed, retrying...");
                script.sleep(script.random(150, 300)); // slight delay before retry
                selected = script.getWidgetManager().getDialogue().selectItem(ItemID.JUG_OF_WINE, grapeID, ItemID.WINE_OF_ZAMORAK, ItemID.UNFERMENTED_WINE, ItemID.UNFERMENTED_WINE_1996, ItemID.ZAMORAKS_UNFERMENTED_WINE);
            }

            if (!selected) {
                script.log(getClass().getSimpleName(), "Failed to select wine in dialogue after retry.");
                return false;
            }
            script.log(getClass().getSimpleName(), "Selected wine to produce.");
            waitUntilFinishedProducing();
            task = "Update stats";
            craftCount += 14;
        }

        return false;
    }

    private boolean interactAndWaitForDialogue(ItemGroupResult inventSnapshot) {
        boolean firstIsGrape = script.random(2) == 0;

        int firstID = firstIsGrape ? grapeID : ItemID.JUG_OF_WATER;
        int secondID = firstIsGrape ? ItemID.JUG_OF_WATER : grapeID;

        task = "Interact with item 1";
        // First interaction with retry
        if (!inventSnapshot.getRandomItem(firstID).interact()) {
            script.log(getClass(), "First item interaction failed, retrying...");
            if (!inventSnapshot.getRandomItem(firstID).interact()) {
                return false;
            }
        }

        script.sleep(script.random(150, 300));

        // Second interaction with retry
        if (!inventSnapshot.getRandomItem(secondID).interact()) {
            script.log(getClass(), "Second item interaction failed, retrying...");
            if (!inventSnapshot.getRandomItem(secondID).interact()) {
                return false;
            }
        }

        task = "Interact with item 2";
        // Wait for dialogue
        boolean useHumanTask = script.random(10) < 3; // 30% chance
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        task = "Wait for dialogue";
        return script.pollFramesHuman(condition, script.random(3000, 5000));
    }

    private void waitUntilFinishedProducing() {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, script.random(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 18000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(grapeID, ItemID.JUG_OF_WATER));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.containsAny(grapeID, ItemID.JUG_OF_WATER);
        };

        task = "Checking wait condition";
        script.pollFramesHuman(condition, script.random(18000, 20000));
    }
}