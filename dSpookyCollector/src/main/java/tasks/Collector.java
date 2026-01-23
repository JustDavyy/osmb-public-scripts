package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.scene.RSObject;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static main.dSpookyCollector.*;

public class Collector extends Task {

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank booth"};
    public static final String[] BANK_ACTIONS = {"bank"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Cauldron stuff
    public static final String[] CAULDRON_NAMES = {"Spooky cauldron"};
    public static final String[] CAULDRON_ACTIONS = {"rummage"};
    public static final Predicate<RSObject> cauldronQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(CAULDRON_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(CAULDRON_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Additional travel stuff
    private static final Area cauldronArea = new RectangleArea(3200, 3438, 4, 4, 0);
    private static final Area bankArea = new RectangleArea(3181, 3436, 4, 3, 0);

    public Collector(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return setupDone;
    }

    @Override
    public boolean execute() {
        // 1. Check if inventory is full, bank if needed
        if (isInventoryFull()) {
            bankItems();
            return false;
        }

        // 2. Interact with cauldron
        task = "Searching cauldron";
        List<RSObject> cauldrons = script.getObjectManager().getObjects(cauldronQuery);

        if (cauldrons.isEmpty()) {
            script.log(getClass(), "No cauldron found nearby, walking to cauldron area...");
            walkToCauldronArea();
            return false;
        }

        RSObject cauldron = (RSObject) script.getUtils().getClosest(cauldrons);

        if (!cauldron.isInteractableOnScreen()) {
            script.log(getClass(), "Cauldron not on screen, walking closer...");
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(cauldron::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            script.getWalker().walkTo(cauldronArea.getRandomPosition(), config);
            return false;
        }

        // Interact with cauldron
        task = "Interacting with cauldron";
        if (!cauldron.interact("Rummage")) {
            script.log(getClass(), "Failed to rummage cauldron.");
            return false;
        }

        // 3. Handle ITEM_OPTION dialogue via static rectangle taps
        script.log(getClass(), "Waiting for ITEM_OPTION dialogue...");
        script.pollFramesHuman(() -> {
            var dlg = script.getWidgetManager().getDialogue();
            DialogueType type = dlg != null ? dlg.getDialogueType() : null;
            return type != null && type.equals(DialogueType.ITEM_OPTION);
        }, RandomUtils.uniformRandom(4000, 7500));

        var dialogue = script.getWidgetManager().getDialogue();
        if (dialogue != null && dialogue.getDialogueType() == DialogueType.ITEM_OPTION) {
            Rectangle rectToTap = null;

            switch (itemToCollect.toLowerCase()) {
                case "smelly sock":
                    rectToTap = new Rectangle(213, 39, 328 - 213, 53 - 39);
                    break;

                case "bruised banana":
                    rectToTap = new Rectangle(211, 63, 333 - 211, 78 - 63);
                    break;

                case "old wool":
                    rectToTap = new Rectangle(213, 87, 330 - 213, 99 - 87);
                    break;

                case "spooky egg":
                    // Tap More options first
                    Rectangle moreOptionsRect = new Rectangle(223, 111, 322 - 223, 123 - 111);
                    script.log(getClass(), "Tapping 'More options' for spooky egg.");
                    if (!script.getFinger().tap(moreOptionsRect)) {
                        script.log(getClass(), "Failed to tap 'More options' rectangle.");
                        return false;
                    }

                    // Wait for next ITEM_OPTION dialogue
                    script.pollFramesHuman(() -> {
                        var dlg2 = script.getWidgetManager().getDialogue();
                        DialogueType type2 = dlg2 != null ? dlg2.getDialogueType() : null;
                        return type2 != null && type2.equals(DialogueType.ITEM_OPTION);
                    }, RandomUtils.uniformRandom(1500, 2500));

                    // Wait between 600–1400ms before spooky tap
                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(600, 1400));

                    // Tap spooky egg option (same as smelly sock coords)
                    rectToTap = new Rectangle(213, 39, 328 - 213, 53 - 39);
                    break;
            }

            if (rectToTap != null) {
                script.log(getClass(), "Tapping rectangle for: " + itemToCollect + " => " + rectToTap);
                if (!script.getFinger().tap(rectToTap)) {
                    script.log(getClass(), "Failed to tap rectangle for: " + itemToCollect);
                    return false;
                }
            } else {
                script.log(getClass(), "No rectangle defined for: " + itemToCollect);
            }
        } else {
            script.log(getClass(), "No ITEM_OPTION dialogue found after rummage.");
        }

        // Wait after interaction until TAP_HERE_TO_CONTINUE dialogue appears (item received)
        script.pollFramesHuman(() -> {
            var dlg = script.getWidgetManager().getDialogue();
            DialogueType type = dlg != null ? dlg.getDialogueType() : null;
            return type != null && type.equals(DialogueType.TAP_HERE_TO_CONTINUE);
        }, RandomUtils.uniformRandom(2500, 5500));

        return false;
    }

    private boolean isInventoryFull() {
        var inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull();
    }

    private void bankItems() {
        task = "Banking items";

        // Use the same bankQuery as atBank for consistency
        List<RSObject> banks = script.getObjectManager().getObjects(bankQuery);
        if (banks.isEmpty()) {
            script.log(getClass(), "No bank object found nearby.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banks);

        if (!atBank()) {
            script.log(getClass(), "Walking to bank area...");
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(bank::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            script.getWalker().walkTo(bankArea.getRandomPosition(), config);
            return;
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return;
        }

        script.log(getClass(), "Depositing all items...");
        if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
            script.log(getClass(), "Failed to deposit items.");
            return;
        }

        itemsCollected += 28;

        task = "Closing bank";
        if (!script.getWidgetManager().getBank().close()) {
            script.log(getClass(), "Failed to close bank, retrying...");
            script.getWidgetManager().getBank().close();
        }

        script.log(getClass(), "Banking done.");
    }

    private void walkToCauldronArea() {
        WalkConfig config = new WalkConfig.Builder()
                .enableRun(true)
                .build();
        script.getWalker().walkTo(cauldronArea.getRandomPosition(), config);
    }

    private void openBank() {
        script.log(getClass(), "Opening bank...");

        // Use the same bankQuery as atBank for consistency
        List<RSObject> banks = script.getObjectManager().getObjects(bankQuery);
        if (banks.isEmpty()) {
            script.log(getClass(), "No bank object found nearby.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banks);

        // If bank is interactable on screen, interact immediately
        if (bank.isInteractableOnScreen()) {
            script.log(getClass(), "Bank object is interactable on screen, attempting to open...");
            if (!bank.interact("Bank")) {
                script.log(getClass(), "Failed to interact with bank object.");
                return;
            }

            // Wait for bank interface to become visible
            script.pollFramesHuman(() -> script.getWidgetManager().getBank().isVisible(), RandomUtils.uniformRandom(8000, 11000));
        } else {
            // Otherwise walk towards it
            script.log(getClass(), "Bank object not on screen, walking towards it...");
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(bank::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            script.getWalker().walkTo(bank, config);
        }
    }

    private boolean atBank() {
        // Check if bank interface is already open
        if (script.getWidgetManager().getBank().isVisible()) {
            return true;
        }

        // Check if any bank object is interactable on screen
        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        for (RSObject bank : banksFound) {
            if (bank.isInteractableOnScreen()) {
                script.log(getClass(), "Bank object is visible and interactable on screen.");
                return true;
            }
        }

        // Check if within bank area
        WorldPosition myPos = script.getWorldPosition();
        if (myPos != null && bankArea.contains(myPos)) {
            script.log(getClass(), "Within bank area, checking for reachable/interactable bank object...");
            for (RSObject bank : banksFound) {
                boolean reachable = bank.canReach();
                boolean visible = bank.isInteractableOnScreen();
                if (reachable || visible) {
                    return true;
                }
            }
        }

        script.log(getClass(), "Not at bank.");
        return false;
    }
}