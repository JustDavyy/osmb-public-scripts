package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import java.util.Set;
import java.util.function.BooleanSupplier;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import static main.dAmethystCutter.*;

public class Process extends Task {
    private final long startTime;

    public Process(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST));
        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        return inventorySnapshot.contains(ItemID.AMETHYST);
    }

    @Override
    public boolean execute() {
        if (script.getWidgetManager().getBank().isVisible()) {
            script.log(getClass(), "Bank still open, closing it.");
            return script.getWidgetManager().getBank().close();
        }

        task = "Take invent snapshot";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST, ItemID.CHISEL));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        boolean interacted = interactAndWaitForDialogue(inventorySnapshot);

        if (!interacted) {
            script.log(getClass().getSimpleName(), "Failed to interact with items. Re-polling...");
            return false;
        }

        task = "Select dialogue item";
        // Dialogue opened - select item to produce
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(itemID);
            if (!selected) {
                script.log(getClass().getSimpleName(), "Selection failed, re-polling...");
                return false;
            }

            script.log(getClass().getSimpleName(), "Selected item to produce.");
            waitUntilFinishedCrafting();
        }

        return false;
    }

    private boolean interactAndWaitForDialogue(ItemGroupResult inventSnapshot) {
        boolean firstIsChisel = RandomUtils.uniformRandom(2) == 0;

        int firstID = firstIsChisel ? ItemID.AMETHYST : ItemID.CHISEL;
        int secondID = firstIsChisel ? ItemID.CHISEL : ItemID.AMETHYST;

        task = "Interact with item 1";
        // First interaction with retry
        if (!inventSnapshot.getRandomItem(firstID).interact()) {
            script.log(getClass(), "First item interaction failed, re-polling...");
            return false;
        }

        script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(150, 300));

        task = "Interact with item 2";
        // Second interaction with retry
        if (!inventSnapshot.getRandomItem(secondID).interact()) {
            script.log(getClass(), "Second item interaction failed, re-polling...");
            return false;
        }

        // Wait for dialogue
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        task = "Wait for dialogue";
        return script.pollFramesHuman(condition, RandomUtils.uniformRandom(3000, 5000));
    }

    private void waitUntilFinishedCrafting() {
        task = "Wait till done processing";
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            // This is the level level up check
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
                script.getWidgetManager().getDialogue().continueChatDialogue();
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            // A timer to timeout
            if (amountChangeTimer.timeElapsed() > RandomUtils.uniformRandom(70000, 78000)) {
                return true;
            }

            // Check if we ran out of items
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(ItemID.AMETHYST);
        };

        script.log(getClass(), "Using human task to wait until crafting finishes.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(70000, 78000));
    }
}