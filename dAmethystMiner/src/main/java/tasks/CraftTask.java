package tasks;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dAmethystMiner.*;

public class CraftTask extends Task {
    private boolean startedCrafting = false;

    public CraftTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!craftMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull() || startedCrafting;
    }

    @Override
    public boolean execute() {
        task = "Check tapToDrop";
        UIResult<Boolean> tapToDropResult = script.getWidgetManager().getHotkeys().isTapToDropEnabled();
        if (tapToDropResult.isFound() && tapToDropResult.get()) {
            script.log(getClass(), "⚠️ Tap-to-drop is enabled — disabling it.");
            boolean success = script.getWidgetManager().getHotkeys().setTapToDropEnabled(false);
            if (!success) {
                script.log(getClass(), "❌ Failed to disable Tap-to-drop!");
                return false;
            }
        }

        task = "Get inventory snapshot";
        // tap the deposit first in our inventory
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST, ItemID.CHISEL, ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        task = "Unselect item if needed";
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (!inventorySnapshot.contains(ItemID.CHISEL)) {
            for (int i = 0; i < 20; i++) {
                script.log(getClass().getSimpleName(), "Chisel not found in inventory, fail-over to banking mode!");
                script.pollFramesHuman(() -> false, script.random(100, 250));
            }

            craftMode = false;
            bankMode = true;
            return false;
        }

        if (inventorySnapshot.contains(ItemID.AMETHYST)) {
            startedCrafting = true;
            task = "Interact with chisel";
            if (!inventorySnapshot.getItem(ItemID.CHISEL).interact()) {
                script.log(getClass().getSimpleName(), "Failed to use calcified deposit, returning.");
                return false;
            }

            task = "Interact with amethyst";
            if (!inventorySnapshot.getItem(ItemID.AMETHYST).interact()) {
                script.log(getClass().getSimpleName(), "Failed to use calcified deposit, returning.");
                return false;
            }

            task = "Wait for dialogue";
            BooleanSupplier condition = () -> {
                DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
                return type == DialogueType.ITEM_OPTION;
            };

            script.pollFramesHuman(condition, script.random(4000, 6000));

            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selected = script.getWidgetManager().getDialogue().selectItem(selectedAmethystItemId);
                if (!selected) {
                    task = "Retry interaction";
                    script.log(getClass(), "Initial amethyst craft option selection failed, retrying...");
                    script.pollFramesHuman(() -> false, script.random(150, 300));
                    selected = script.getWidgetManager().getDialogue().selectItem(selectedAmethystItemId);
                }

                if (!selected) {
                    script.log(getClass(), "Failed to select " + script.getItemManager().getItemName(selectedAmethystItemId) + " in dialogue after retry.");
                    return false;
                }

                script.log(getClass(), "Selected " + script.getItemManager().getItemName(selectedAmethystItemId) + " to craft/process.");

                task = "Wait until finished";
                waitUntilFinishedCrafting();
            } else {
                script.log(getClass(), "Failed to start item crafting.");
                return false;
            }
        }

        // Double check we no longer have any amethyst left
        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST, ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (inventorySnapshot.containsAny(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND)) {
            script.log(getClass().getSimpleName(), "Dropping uncut(s)!");
            return script.getWidgetManager().getInventory().dropItems(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND);
        }

        if (!inventorySnapshot.containsAny(ItemID.AMETHYST, ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND)) {
            script.log(getClass().getSimpleName(), "Nothing to craft/drop anymore, walking back!");
            startedCrafting = false;
        }

        return true;
    }

    private void waitUntilFinishedCrafting() {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            // This is the level level up check
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
                script.pollFramesHuman(() -> false, script.random(1000, 3000));
                return true;
            }

            // A timer to timeout
            if (amountChangeTimer.timeElapsed() > script.random(70000, 78000)) {
                return true;
            }

            // Check if we ran out of items
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(ItemID.AMETHYST);
        };

        script.log(getClass(), "Using human task to wait until crafting finishes.");
        script.pollFramesHuman(condition, script.random(70000, 78000));
    }
}