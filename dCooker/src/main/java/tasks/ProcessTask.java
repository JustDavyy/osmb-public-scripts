package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import main.dCooker;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static main.dCooker.*;

public class ProcessTask extends Task {
    private static int failCount = 0;

    public ProcessTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            script.log(getClass().getSimpleName(), "Opening inventory tab");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
            return false;
        }

        return inventorySnapshot.contains(cookingItemID);
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return script.getWidgetManager().getBank().close();
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (getLevelForFood(cookingItemID) > currentLevel) {
            script.log(getClass(), "Current cooking level is not high enough to cook the selected food. Have: " + currentLevel + " Need: " + getLevelForFood(cookingItemID));
            script.log(getClass(), "Stopping script!");
            script.stop();
            return false;
        }

        if (!inventorySnapshot.contains(cookingItemID)) {
            if (failCount >= 3) {
                script.log(getClass(), "No cookable fish found for three times in a row, stopping script!");
                script.stop();
                return false;
            }
            script.log(getClass(), "No fish to cook found in inventory, returning!");
            failCount++;
            return false;
        }

        task = "Find cooking object";
        RSObject cookObject = getClosestCookObject();
        if (cookObject == null) {
            script.log(getClass(), "No cookable object found nearby (range/fire/clay oven).");
            return false;
        }

        task = "Interact with object";
        if (!cookObject.interact(COOKING_ACTIONS)) {
            script.log(getClass(), "Failed to interact with cooking object. Retrying...");
            if (!cookObject.interact(COOKING_ACTIONS)) {
                return false;
            }
        }

        task = "Start cooking action";
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        script.pollFramesHuman(condition, RandomUtils.uniformRandom(4000, 6000));

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selected = script.getWidgetManager().getDialogue().selectItem(cookingItemID)
                    || script.getWidgetManager().getDialogue().selectItem(cookedItemID);

            if (!selected) {
                script.log(getClass(), "Initial food selection failed, retrying...");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));

                selected = script.getWidgetManager().getDialogue().selectItem(cookingItemID)
                        || script.getWidgetManager().getDialogue().selectItem(cookedItemID);
            }

            if (!selected) {
                script.log(getClass(), "Failed to select food item in dialogue after retry.");
                return false;
            }

            script.log(getClass(), "Selected food to cook.");

            waitUntilFinishedCooking();

            Set<Integer> idsToSearch = (cookingItemID == cookedItemID)
                    ? Set.of(cookingItemID)
                    : Set.of(cookingItemID, cookedItemID);

            inventorySnapshot = script.getWidgetManager().getInventory().search(idsToSearch);

            if (inventorySnapshot == null || inventorySnapshot.isEmpty()) {
                script.log(getClass(), "No fish to cook could be located.");
            } else {
                int cookedNow = inventorySnapshot.getAmount(cookedItemID);

                if (cookingItemID != ItemID.GIANT_SEAWEED) {
                    totalCookCount += 28;
                    cookCount += cookedNow;
                    burnCount += (28 - cookedNow);
                } else {
                    totalCookCount += 6;
                    cookCount += cookedNow;
                }
            }
        }

        return false;
    }

    private RSObject getClosestCookObject() {
        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) {
                return false;
            }
            return Objects.equals(gameObject.getName(), "Range")
                    || Objects.equals(gameObject.getName(), "Fire")
                    || Objects.equals(gameObject.getName(), "Clay oven")
                    || Objects.equals(gameObject.getName(), "Clay Oven");
        });

        if (objects.isEmpty()) {
            script.log(ProcessTask.class, "No objects found matching query...");
            return null;
        }

        objects.removeIf(object -> !object.canReach());
        if (objects.isEmpty()) {
            script.log(ProcessTask.class, "No reachable objects inside the loaded scene..");
            return null;
        }

        RSObject closest = (RSObject) script.getUtils().getClosest(objects);
        if (closest == null) {
            script.log(ProcessTask.class, "Closest object is null.");
            return null;
        }

        return closest;
    }

    private void waitUntilFinishedCooking() {
        task = "Wait until cooking finish";
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            int timeout = (cookingItemID == ItemID.GIANT_SEAWEED) ? 8000 : 66000;

            if (amountChangeTimer.timeElapsed() > timeout) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(cookingItemID);
        };

        script.log(getClass(), "Using human task to wait until cooking finishes.");
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

    private int getLevelForFood(int itemId) {
        return switch (itemId) {
            case ItemID.RAW_HERRING -> 5;
            case ItemID.RAW_MACKEREL, ItemID.UNCOOKED_BERRY_PIE -> 10;
            case ItemID.RAW_TROUT -> 15;
            case ItemID.RAW_COD -> 18;
            case ItemID.RAW_PIKE, ItemID.UNCOOKED_MEAT_PIE -> 20;
            case ItemID.RAW_WILD_KEBBIT ->  23;
            case ItemID.RAW_SALMON, ItemID.UNCOOKED_STEW -> 25;
            case ItemID.RAW_TUNA, ItemID.UNCOOKED_APPLE_PIE, ItemID.RAW_KARAMBWAN -> 30;
            case ItemID.RAW_LARUPIA -> 31;
            case ItemID.RAW_BARBTAILED_KEBBIT -> 32;
            case ItemID.RAW_GARDEN_PIE -> 34;
            case ItemID.RAW_LOBSTER -> 40;
            case ItemID.RAW_GRAAHK -> 41;
            case ItemID.RAW_BASS -> 43;
            case ItemID.RAW_SWORDFISH -> 45;
            case ItemID.RAW_FISH_PIE -> 47;
            case ItemID.RAW_KYATT -> 51;
            case ItemID.UNCOOKED_BOTANICAL_PIE -> 52;
            case ItemID.RAW_PYRE_FOX -> 59;
            case ItemID.UNCOOKED_MUSHROOM_PIE -> 60;
            case ItemID.UNCOOKED_CURRY -> 60;
            case ItemID.RAW_MONKFISH -> 62;
            case ItemID.RAW_SUNLIGHT_ANTELOPE -> 68;
            case ItemID.RAW_ADMIRAL_PIE -> 70;
            case ItemID.UNCOOKED_DRAGONFRUIT_PIE -> 73;
            case ItemID.RAW_SHARK -> 80;
            case ItemID.RAW_SEA_TURTLE, ItemID.RAW_DASHING_KEBBIT -> 82;
            case ItemID.RAW_ANGLERFISH -> 84;
            case ItemID.RAW_WILD_PIE -> 85;
            case ItemID.RAW_DARK_CRAB -> 90;
            case ItemID.RAW_MANTA_RAY -> 91;
            case ItemID.RAW_MOONLIGHT_ANTELOPE -> 92;
            case ItemID.RAW_SUMMER_PIE -> 95;
            default -> 1;
        };
    }
}
