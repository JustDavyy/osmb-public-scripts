package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static main.dLooter.*;

public class GOTR extends Task {

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank chest"};
    public static final String[] BANK_ACTIONS = {"use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Object stuff
    public static final String[] GUARDIAN_NAMES = {"Rewards Guardian", "<col=ffff00>Rewards Guardian</col>"};
    public static final String[] GUARDIAN_ACTIONS = {"big-search"};
    public static final Predicate<RSObject> guardianQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(GUARDIAN_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(GUARDIAN_ACTIONS).anyMatch(guardianAction -> guardianAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Areas/Pathing/Mapping
    private static final Area bankArea = new RectangleArea(3616, 9474, 3, 3, 0);
    private static final Area guardianArea = new RectangleArea(3612, 9478, 2, 3, 0);

    // Chat history stuff
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    public GOTR(Script script) {
        super(script);

        // init totals to 0 for all tracked items
        for (int id : getTrackedItems()) {
            totalGained.put(id, 0);
        }
    }

    public boolean activate() {
        return setupDone;
    }

    public boolean execute() {

        // If bank is already open, close it.
        if (script.getWidgetManager().getBank().isVisible()) {
            task = "Close bank";
            return script.getWidgetManager().getBank().close();
        }

        // 1. Check if we have inventory spots free > start searching if so
        ItemGroupResult invSlots = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (invSlots == null) return false;
        if (invSlots.getFreeSlots() >= 2) {
            if (!startSearch()) {
                return false;
            }
            onSearchStartBaseline();
            waitUntilDoneLooting();
            if (needToStop) return false;
        }

        // 2. Open bank if inventory is filled
        invSlots = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (invSlots == null) return false;
        if (invSlots.getFreeSlots() <= 1) {
            updateLootCountsIfNeeded("pre-bank safety");
            openBank();
        }

        // 3. Deposit all and close the bank after
        if (script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
            task = "Close bank";
            boolean closeSuccess = script.getWidgetManager().getBank().close();
            if (!closeSuccess) {
                script.log(getClass(), "Bank close failed, retrying...");
                lastSeenInventory.clear();
                return script.getWidgetManager().getBank().close();
            } else {
                lastSeenInventory.clear();
                return false;
            }
        } else {
            return false;
        }
    }

    private void waitUntilDoneLooting() {

        script.log(getClass(), "Waiting until we are done looting...");
        task = "Wait till done looting";

        BooleanSupplier condition = () -> {
            DialogueType type = null;
            var dialogue = script.getWidgetManager().getDialogue();
            if (dialogue != null) {
                type = dialogue.getDialogueType();
            }

            if (type != null && type.equals(DialogueType.TAP_HERE_TO_CONTINUE)) {
                task = "Read dialogue";
                script.pollFramesHuman(() -> false, script.random(1000, 3000));

                UIResult<String> textResult = dialogue.getText();
                if (textResult == null || textResult.isNotFound() || textResult.isNotVisible()) {
                    script.log(getClass(), "Dialogue text is missing or not visible");
                    monitorChatbox();
                    return false;
                }

                String dialogueText = textResult.get().toLowerCase();
                script.log(getClass(), "Dialogue text: " + dialogueText);

                if (dialogueText.contains("anything in the rift")) {
                    script.log(getClass(), "Detected phrase: There doesn't seem to be anything in the rift for you.");
                    script.log(getClass(), "We're out of searches, marking script to stop!");
                    needToStop = true;
                    return true;
                }

                if (dialogueText.contains("two free inventory")) {
                    script.log(getClass(), "Detected phrase: You'll need two free inventory spaces to keep searching the rift.");
                    script.log(getClass(), "Flagging bank!");
                    return true;
                }

                return true;
            }
            monitorChatbox();

            ItemGroupResult invSlots = script.getWidgetManager().getInventory().search(Collections.emptySet());
            if (invSlots == null) return false;

            return invSlots.isFull();
        };

        script.pollFramesHuman(condition, script.random(150000, 250000));

        // Update all counts here of loot gained
        script.log(getClass(), "Updating all loot counts.");
        task = "Update loot counts";
        updateLootCountsIfNeeded("post-search completion");

        // Additional random delay sometimes
        if (script.random(10) < 3) {
            task = "Add extra random delay";
            script.log(getClass(), "Adding extra randomized delay");
            script.pollFramesHuman(() -> false, script.random(150, 500));
        }
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Opening bank...");

        if (script.getWidgetManager().getBank().isVisible()) return;

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found nearby, walking to bank area instead...");
            script.getWalker().walkTo(Objects.requireNonNull(bankArea.getRandomPosition()));
            return;
        }

        RSObject closestBank = (RSObject) script.getUtils().getClosest(banksFound);

        WalkConfig config = new WalkConfig.Builder()
                .breakCondition(closestBank::isInteractableOnScreen)
                .enableRun(true)
                .build();

        boolean walked = script.getWalker().walkTo(closestBank, config);
        if (!walked) {
            script.log(getClass(), "Walking to bank failed, attempting fallback walk to bank area...");
            script.getWalker().walkTo(Objects.requireNonNull(bankArea.getRandomPosition()));
            return;
        }

        if (!closestBank.interact(BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object after walking.");
            return;
        }

        script.pollFramesHuman(() -> script.getWidgetManager().getBank().isVisible(), script.random(5000, 8000));
    }

    private boolean startSearch() {
        script.log(getClass(), "Get Rewards guardian object...");

        List<RSObject> objectsFound = script.getObjectManager().getObjects(guardianQuery);
        if (objectsFound.isEmpty()) {
            script.log(getClass(), "Rewards Guardian object not found.");
            return false;
        }

        RSObject guardian = (RSObject) script.getUtils().getClosest(objectsFound);

        WalkConfig config = new WalkConfig.Builder()
                .breakCondition(guardian::isInteractableOnScreen)
                .enableRun(true)
                .build();

        boolean walked = script.getWalker().walkTo(guardianArea.getRandomPosition(), config);
        if (!walked) {
            script.log(getClass(), "Walking to Rewards Guardian failed.");
            return false;
        }

        if (!guardian.interact(getMenuHook())) {
            script.log(getClass(), "Failed to interact with Rewards Guardian object after walking.");
            return false;
        }

        return true;
    }

    private Set<Integer> getTrackedItems() {
        return Set.of(
                // Rare reward table
                ItemID.ATLAXS_DIARY,
                ItemID.CATALYTIC_TALISMAN,
                ItemID.ABYSSAL_NEEDLE,
                ItemID.ABYSSAL_LANTERN,
                ItemID.ABYSSAL_RED_DYE,
                ItemID.ABYSSAL_GREEN_DYE,
                ItemID.ABYSSAL_BLUE_DYE,

                // Essence pouches
                ItemID.SMALL_POUCH,
                ItemID.MEDIUM_POUCH,
                ItemID.LARGE_POUCH,
                ItemID.GIANT_POUCH,

                // Runes
                ItemID.AIR_RUNE,
                ItemID.WATER_RUNE,
                ItemID.EARTH_RUNE,
                ItemID.FIRE_RUNE,
                ItemID.MIND_RUNE,
                ItemID.BODY_RUNE,
                ItemID.CHAOS_RUNE,
                ItemID.COSMIC_RUNE,
                ItemID.NATURE_RUNE,
                ItemID.LAW_RUNE,
                ItemID.DEATH_RUNE,
                ItemID.BLOOD_RUNE,

                // Talismans
                ItemID.ELEMENTAL_TALISMAN,
                ItemID.LAW_TALISMAN,
                ItemID.DEATH_TALISMAN +1,

                // Other
                ItemID.ABYSSAL_PEARLS,
                ItemID.INTRICATE_POUCH,
                ItemID.ABYSSAL_ASHES,
                ItemID.NEEDLE,

                // Tertiary
                ItemID.ABYSSAL_PROTECTOR
        );
    }

    private ItemGroupResult searchTracked() {
        return script.getWidgetManager().getInventory().search(getTrackedItems());
    }

    private Map<Integer, Integer> snapshot(ItemGroupResult inv) {
        Map<Integer, Integer> snap = new HashMap<>();
        if (inv == null) return snap;
        for (int id : getTrackedItems()) {
            snap.put(id, inv.getAmount(id));
        }
        return snap;
    }

    private void onSearchStartBaseline() {
        // take a baseline snapshot and mark this cycle as not yet processed
        ItemGroupResult inv = searchTracked();
        lastSeenInventory = snapshot(inv);
        inventoryProcessedThisCycle = false;
    }

    private void updateLootCountsIfNeeded(String reason) {
        if (inventoryProcessedThisCycle) return;  // already processed this search cycle
        ItemGroupResult inv = searchTracked();
        if (inv == null) return;

        // compute deltas vs baseline and add to totals
        for (int id : getTrackedItems()) {
            int current = inv.getAmount(id);
            int last = lastSeenInventory.getOrDefault(id, 0);
            int delta = current - last;
            if (delta > 0) {
                totalGained.merge(id, delta, Integer::sum);
            }
        }

        // mark as processed and refresh baseline to current (optional)
        inventoryProcessedThisCycle = true;
        lastSeenInventory = snapshot(inv);

        script.log(getClass(), "Updated loot counts (" + reason + ").");
    }

    private MenuHook getMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String text = entry.getRawText().toLowerCase();
                if (text.equals("big-search rewards guardian")) {
                    return entry;
                }
            }
            return null;
        };
    }

    private void monitorChatbox() {
        task = "Read chatbox";
        // Make sure game filter tab is selected
        if (script.getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            script.getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
            return;
        }

        UIResultList<String> chatResult = script.getWidgetManager().getChatbox().getText();
        if (!chatResult.isFound() || chatResult.isEmpty()) {
            return;
        }

        List<String> currentLines = chatResult.asList();
        if (currentLines.isEmpty()) return;

        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return;
            }

            int currSize = currentLines.size();
            int prevSize = PREVIOUS_CHATBOX_LINES.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;

                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(PREVIOUS_CHATBOX_LINES.get(j))) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        }

        List<String> newMessages = currentLines.subList(0, firstDifference);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);

        task = "Process new chatbox messages";
        processNewChatboxMessages(newMessages);
    }

    private void processNewChatboxMessages(List<String> newLines) {
        for (String message : newLines) {
            String lower = message.toLowerCase();

            if (lower.contains("funny feeling")) {
                // Increment the Abyssal Protector count by 1
                totalGained.merge(ItemID.ABYSSAL_PROTECTOR, 1, Integer::sum);
                script.log(getClass(), "Abyssal Protector drop detected via chat message!");
                return;
            }

            if (lower.contains("more rewards in")) {
                // Extract the number from the message
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(message);
                if (matcher.find()) {
                    try {
                        lootsLeft = Integer.parseInt(matcher.group());
                        script.log(getClass(), "Updated lootsLeft to " + lootsLeft);
                    } catch (NumberFormatException e) {
                        script.log(getClass(), "Failed to parse lootsLeft from message: " + message);
                    }
                }
            }
        }
    }
}