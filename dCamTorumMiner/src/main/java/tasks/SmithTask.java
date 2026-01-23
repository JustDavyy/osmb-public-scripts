package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static main.dCamTorumMiner.*;

public class SmithTask extends Task {
    private final Area smithWalkArea = new RectangleArea(1447, 9582, 4, 3, 1);
    private final Area smithArea = new RectangleArea(1443, 9581, 10, 6, 1);

    public SmithTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!smithMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        WorldPosition myPos = script.getWorldPosition();
        return inv != null && inv.isFull() || myPos != null && anvilPlusBankArea.contains(myPos);
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();

        if (!anvilPlusBankArea.contains(script.getWorldPosition())) {
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.CALCIFIED_DEPOSIT));
            if (inv == null) {return false;}
            if (inv.contains(ItemID.CALCIFIED_DEPOSIT)) {
                task = "Walk to smithing area";
                script.log(getClass().getSimpleName(), "Walk to smithing area");
                return script.getWalker().walkTo(smithWalkArea.getRandomPosition());
            } else {
                task = "Walk to bank area";
                script.log(getClass().getSimpleName(), "Walk to bank area");
                return script.getWalker().walkTo(bankWalkArea.getRandomPosition());
            }
        }

        task = "Get inventory snapshot";
        // tap the deposit first in our inventory
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CALCIFIED_DEPOSIT));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        task = "Unselect item if needed";
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (inventorySnapshot.contains(ItemID.CALCIFIED_DEPOSIT)) {
            task = "Interact with deposit";
            if (!inventorySnapshot.getItem(ItemID.CALCIFIED_DEPOSIT).interact()) {
                script.log(getClass().getSimpleName(), "Failed to use calcified deposit, returning.");
                return false;
            }

            RSObject anvil = getClosestAnvil();
            if (anvil != null && anvil.canReach()) {
                task = "Use deposit on anvil";

                MenuHook hook = menuEntries -> {
                    for (MenuEntry menuEntry : menuEntries) {
                        if (menuEntry.getRawText().equalsIgnoreCase("use calcified deposit -> anvil")) {
                            return menuEntry;
                        }
                    }
                    return null;
                };

                boolean clicked = anvil.interact(null, hook);

                if (clicked) {
                    task = "Wait for dialogue";
                    BooleanSupplier condition = () -> {
                        DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
                        return type == DialogueType.ITEM_OPTION;
                    };

                    script.pollFramesHuman(condition, RandomUtils.uniformRandom(4000, 6000));

                    DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
                    if (dialogueType == DialogueType.ITEM_OPTION) {
                        boolean selected = script.getWidgetManager().getDialogue().selectItem(ItemID.CALCIFIED_DEPOSIT);
                        if (!selected) {
                            task = "Retry interaction";
                            script.log(getClass(), "Initial calcified deposit selection failed, retrying...");
                            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                            selected = script.getWidgetManager().getDialogue().selectItem(ItemID.CALCIFIED_DEPOSIT);
                        }

                        if (!selected) {
                            script.log(getClass(), "Failed to select calcified deposit in dialogue after retry.");
                            return false;
                        }

                        script.log(getClass(), "Selected calcified deposit to smith/process.");

                        task = "Wait until finished";
                        waitUntilFinishedSmithing();
                    }
                } else {
                    script.log(getClass(), "Failed to click anvil with correct menu entry.");
                    return false;
                }
            } else {
                script.log(getClass(), "No reachable anvil found.");
                return false;
            }
        }

        // Double check we no longer have any deposits left
        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CALCIFIED_DEPOSIT, ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND, ItemID.LOOP_HALF_OF_KEY_30107, ItemID.CLUE_GEODE_BEGINNER, ItemID.CLUE_GEODE_EASY, ItemID.CLUE_GEODE_MEDIUM, ItemID.CLUE_GEODE_HARD, ItemID.CLUE_GEODE_ELITE));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (!inventorySnapshot.contains(ItemID.CALCIFIED_DEPOSIT) && inventorySnapshot.containsAny(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND, ItemID.LOOP_HALF_OF_KEY_30107, ItemID.CLUE_GEODE_BEGINNER, ItemID.CLUE_GEODE_EASY, ItemID.CLUE_GEODE_MEDIUM, ItemID.CLUE_GEODE_HARD, ItemID.CLUE_GEODE_ELITE)) {
            WorldPosition myPos = script.getWorldPosition();
            if (myPos != null && !bankArea.contains(myPos)) {
                return script.getWalker().walkTo(bankWalkArea.getRandomPosition());
            }
            if (!script.getWidgetManager().getDepositBox().isVisible()) {
                task = "Bank at deposit box";
                script.log(getClass(), "Searching for deposit box...");

                Predicate<RSObject> bankQuery = obj ->
                        obj.getName() != null &&
                                obj.getName().equalsIgnoreCase("Bank Deposit Box") &&
                                obj.canReach();

                List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
                if (banksFound.isEmpty()) {
                    script.log(getClass(), "Can't find any banks matching criteria...");
                    return false;
                }

                RSObject depositBox = (RSObject) script.getUtils().getClosest(banksFound);
                if (!depositBox.interact("Deposit")) {
                    script.log(getClass(), "Failed to interact with deposit box.");
                    return false;
                }

                AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
                AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
                script.pollFramesHuman(() -> {
                    WorldPosition current = script.getWorldPosition();
                    if (current == null) return false;
                    if (pos.get() == null || !current.equals(pos.get())) {
                        positionChangeTimer.get().reset();
                        pos.set(current);
                    }
                    return script.getWidgetManager().getDepositBox().isVisible() || positionChangeTimer.get().timeElapsed() > 4000;
                }, 20000);
            }

            // Now deposit all except the protected items
            ItemGroupResult depositBoxSnapshot = script.getWidgetManager().getDepositBox().search(ITEM_IDS_TO_NOT_DEPOSIT);
            if (depositBoxSnapshot == null) {
                script.log(getClass(), "Deposit box not visible after opening.");
                return false;
            }

            if (depositBoxSnapshot.containsAny(ITEM_IDS_TO_NOT_DEPOSIT)) {
                if (!script.getWidgetManager().getDepositBox().depositAll(ITEM_IDS_TO_NOT_DEPOSIT)) {
                    script.log(getClass(), "Failed depositing items...");
                    return false;
                }
            }

            script.log(getClass(), "Closing deposit box...");
            script.getWidgetManager().getDepositBox().close();
        }

        if (!inventorySnapshot.containsAny(ItemID.CALCIFIED_DEPOSIT, ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND, ItemID.LOOP_HALF_OF_KEY_30107, ItemID.CLUE_GEODE_BEGINNER, ItemID.CLUE_GEODE_EASY, ItemID.CLUE_GEODE_MEDIUM, ItemID.CLUE_GEODE_HARD, ItemID.CLUE_GEODE_ELITE)) {
            script.log(getClass().getSimpleName(), "Nothing to smith/deposit anymore, walking back!");
            return script.getWalker().walkTo(miningArea.getRandomPosition());
        }

        return true;
    }

    private RSObject getClosestAnvil() {
        List<RSObject> objects = script.getObjectManager().getObjects(obj ->
                obj.getName() != null &&
                        (obj.getName().equalsIgnoreCase("Anvil")) &&
                        obj.canReach()
        );

        if (objects.isEmpty()) {
            script.log(getClass().getSimpleName(), "No reachable anvils found.");
            return null;
        }

        return (RSObject) script.getUtils().getClosest(objects);
    }

    private void waitUntilFinishedSmithing() {
        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            // This is the level level up check
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            // A timer to timeout
            if (amountChangeTimer.timeElapsed() > RandomUtils.uniformRandom(70000, 78000)) {
                return true;
            }

            // Check if we ran out of items
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CALCIFIED_DEPOSIT));
            if (inventorySnapshot == null) {return false;}
            return !inventorySnapshot.contains(ItemID.CALCIFIED_DEPOSIT);
        };

        script.log(getClass(), "Using human task to wait until smithing finishes.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(70000, 78000));
    }
}