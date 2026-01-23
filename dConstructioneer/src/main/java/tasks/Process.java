package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dConstructioneer.*;

public class Process extends Task {
    private static int failCount = 0;

    public Process(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(selectedBaseMaterialId));
        if (inventorySnapshot == null) {
            script.log("PROCESS", "Inventory not visible.");
            script.log("PROCESS", "Opening inventory tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            return false;
        }

        int amount = 0;
        if (inventorySnapshot.contains(selectedBaseMaterialId)) {
            amount = inventorySnapshot.getAmount(selectedBaseMaterialId);
        }

        int needed;

        // Ironwood & Rosewood planks → only 1 needed
        if (selectedBaseMaterialId == 31435 || selectedBaseMaterialId == 31438) {
            needed = 1;
        } else if (selectedType.equalsIgnoreCase("repair kits")) {
            needed = 2;
        } else {
            needed = 5;
        }

        return amount >= needed;
    }

    @Override
    public boolean execute() {
        task = "PROCESS";
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return script.getWidgetManager().getBank().close();
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(selectedBaseMaterialId, ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND, ItemID.SAW, ItemID.AMYS_SAW, ItemID.AMYS_SAW_OFFHAND, ItemID.CRYSTAL_SAW, ItemID.SWAMP_PASTE, ItemID.SWAMP_PASTE_22095, ItemID.LAMP, ItemID.BOOK_OF_KNOWLEDGE, ItemID.BRONZE_NAILS, ItemID.IRON_NAILS, ItemID.STEEL_NAILS, ItemID.MITHRIL_NAILS, ItemID.ADAMANTITE_NAILS, ItemID.RUNE_NAILS, 31406));
        if (inventorySnapshot == null) {
            script.log("PROCESS", "Inventory not visible.");
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (selectedType.equalsIgnoreCase("repair kits")) {
            if (!inventorySnapshot.containsAll(Set.of(selectedBaseMaterialId, selectedNail))) {
                if (failCount >= 8) {
                    script.log("PROCESS", "We don't have the required planks, paste and nails to continue for 8 times in a row.");
                    script.stop();
                    return false;
                }
                script.log("PROCESS", "Required planks, paste and nails not found in inventory, returning!");
                failCount++;
                return false;
            }
        } else {
            if (!inventorySnapshot.contains(selectedBaseMaterialId)) {
                if (failCount >= 8) {
                    script.log("PROCESS", "We don't have the required planks");
                    script.stop();
                    return false;
                }
                failCount++;
                return false;
            }
        }


        failCount = 0;

        task = "Find workbench object";
        RSObject benchObject = getClosestbenchObject();
        if (benchObject == null) {
            script.log("PROCESS", "No shipwrights workbench object found nearby.");
            return false;
        }

        task = "Interact with object";
        if (!benchObject.interact("Craft")) {
            script.log("PROCESS", "Failed to interact with workbench object. Retrying...");
            if (!benchObject.interact("Craft")) {
                return false;
            }
        }

        task = "Start process action";
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.TEXT_OPTION;
        };

        script.pollFramesHuman(condition, RandomUtils.uniformRandom(4000, 6000));

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.TEXT_OPTION) {
            boolean selected;

            if (selectedType.equalsIgnoreCase("repair kits")) {
                selected = script.getWidgetManager().getDialogue().selectOption("Repair kits");
            } else {
                selected = script.getWidgetManager().getDialogue().selectOption("Hull parts");
            }

            if (!selected) {
                script.log("PROCESS", "Failed to select construction category in TEXT_OPTION.");
                return false;
            }

            script.log("PROCESS", "Selected construction category successfully.");

            BooleanSupplier waitItemOption = () ->
                    script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;

            script.pollFramesHuman(waitItemOption, RandomUtils.uniformRandom(4000, 6000));

            // Once that appear, select the correct option, then wait
            DialogueType afterCategory = script.getWidgetManager().getDialogue().getDialogueType();
            if (afterCategory != DialogueType.ITEM_OPTION) {
                script.log("PROCESS", "Expected ITEM_OPTION, but it did not appear.");
                return false;
            }

            selected = script.getWidgetManager().getDialogue().selectItem(selectedItemId);

            if (!selected) {
                script.log("PROCESS", "Failed to select: " + selectedItemId);
                return false;
            }

            // Check for dialogue that we don't have enough items
            DialogueType diag = script.getWidgetManager().getDialogue().getDialogueType();
            if (diag != null) {
                task = "Dialogue detected";
                if (diag.equals(DialogueType.TAP_HERE_TO_CONTINUE)) {
                    task = "Continue dialogue detected";
                    script.log("PROCESS", "Continue dialogue detected, out of resources?");
                    task = "Get dialogue text";

                    String text = script.getWidgetManager().getDialogue().getText().get();
                    if (text != null && !text.isBlank()) {
                        script.log("PROCESS", "Dialogue text detected: " + text.toLowerCase());

                        if (text.toLowerCase().contains("you need")) {
                            script.log("PROCESS", "You need dialogue detected, we are out of resources. Stopping script");
                            script.stop();
                            return false;
                        }
                    }
                }
            }

            waitUntilFinishedProcessing();
        } else {
            script.log("PROCESS", "Expected TEXT_OPTION but did not receive it.");
            return false;
        }

        return false;
    }

    private RSObject getClosestbenchObject() {
        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) {
                return false;
            }
            return Objects.equals(gameObject.getName(), "Shipwrights' Workbench");
        });

        if (objects.isEmpty()) {
            script.log(Process.class, "No objects found matching query...");
            return null;
        }

        objects.removeIf(object -> !object.canReach());
        if (objects.isEmpty()) {
            script.log(Process.class, "No reachable objects inside the loaded scene..");
            return null;
        }

        RSObject closest = (RSObject) script.getUtils().getClosest(objects);
        if (closest == null) {
            script.log(Process.class, "Closest object is null.");
            return null;
        }

        return closest;
    }

    private void waitUntilFinishedProcessing() {
        task = "Wait until finished";
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            int timeout = 30000;

            if (amountChangeTimer.timeElapsed() > timeout) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager()
                    .getInventory()
                    .search(Set.of(selectedBaseMaterialId));

            if (inventorySnapshot == null) {
                return false;
            }

            // Ironwood & Rosewood only need 1 plank
            int threshold = (selectedBaseMaterialId == 31435 || selectedBaseMaterialId == 31438)
                    ? 1
                    : 2;

            if (inventorySnapshot.contains(selectedBaseMaterialId)) {
                int amount = inventorySnapshot.getAmount(selectedBaseMaterialId);
                return amount < threshold;
            } else {
                return true;
            }
        };

        script.log("PROCESS", "Using human task to wait until processing finishes.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(66000, 70000));
    }

    private MenuHook getFireMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String text = entry.getRawText().toLowerCase();
                if (text.startsWith("use ") && text.endsWith("-> fire")) {
                    return entry;
                }
            }
            return null;
        };
    }
}
