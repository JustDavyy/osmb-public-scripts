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

public class WT extends Task {

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank chest"};
    public static final String[] BANK_ACTIONS = {"bank"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Object stuff
    public static final String[] CART_NAMES = {"Reward Cart"};
    public static final String[] CART_ACTIONS = {"big-search"};
    public static final Predicate<RSObject> cartQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(CART_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(CART_ACTIONS).anyMatch(guardianAction -> guardianAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Areas/Pathing/Mapping
    private static final Area bankArea = new RectangleArea(1638, 3943, 2, 2, 0);
    private static final Area cartArea = new RectangleArea(1633, 3944, 3, 2, 0);

    // Chat history stuff
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    public WT(Script script) {
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
        if (!invSlots.isFull()) {
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
        if (invSlots.isFull()) {
            updateLootCountsIfNeeded("pre-bank safety");
            openBank();
        }

        // 3. Deposit all and close the bank after
        task = "Deposit all";
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

                if (dialogueText.contains("taken as much as")) {
                    script.log(getClass(), "Detected phrase: You think you've taken as much as you're owed from the reward cart.");
                    script.log(getClass(), "We're out of searches, marking script to stop!");
                    needToStop = true;
                    return true;
                }

                if (dialogueText.contains("one free inventory")) {
                    script.log(getClass(), "Detected phrase: You need at least one free inventory space to take from the reward cart.");
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
        script.log(getClass(), "Get Reward Cart object...");

        List<RSObject> objectsFound = script.getObjectManager().getObjects(cartQuery);
        if (objectsFound.isEmpty()) {
            script.log(getClass(), "Reward Cart object not found.");
            return false;
        }

        RSObject cart = (RSObject) script.getUtils().getClosest(objectsFound);

        WalkConfig config = new WalkConfig.Builder()
                .breakCondition(cart::isInteractableOnScreen)
                .enableRun(true)
                .build();

        boolean walked = script.getWalker().walkTo(cartArea.getRandomPosition(), config);
        if (!walked) {
            script.log(getClass(), "Walking to Reward Cart failed.");
            return false;
        }

        if (!cart.interact("big-search")) {
            script.log(getClass(), "Failed to interact with Reward Cart object after walking.");
            return false;
        }

        return true;
    }

    private Set<Integer> getTrackedItems() {
        return Set.of(
                // Rare reward table
                ItemID.BURNT_PAGE,
                ItemID.WARM_GLOVES,
                ItemID.BRUMA_TORCH,
                ItemID.PYROMANCER_HOOD,
                ItemID.PYROMANCER_GARB,
                ItemID.PYROMANCER_ROBE,
                ItemID.PYROMANCER_BOOTS,
                ItemID.TOME_OF_FIRE_EMPTY,
                ItemID.PHOENIX,
                ItemID.DRAGON_AXE,

                // Logs
                ItemID.OAK_LOGS +1,
                ItemID.WILLOW_LOGS +1,
                ItemID.TEAK_LOGS +1,
                ItemID.MAPLE_LOGS +1,
                ItemID.MAHOGANY_LOGS +1,
                ItemID.YEW_LOGS +1,
                ItemID.MAGIC_LOGS +1,

                // Gems
                ItemID.UNCUT_SAPPHIRE +1,
                ItemID.UNCUT_EMERALD +1,
                ItemID.UNCUT_RUBY +1,
                ItemID.UNCUT_DIAMOND +1,

                // Ores
                ItemID.PURE_ESSENCE +1,
                ItemID.LIMESTONE +1,
                ItemID.SILVER_ORE +1,
                ItemID.IRON_ORE +1,
                ItemID.COAL +1,
                ItemID.GOLD_ORE +1,
                ItemID.MITHRIL_ORE +1,
                ItemID.ADAMANTITE_ORE +1,
                ItemID.RUNITE_ORE +1,

                // Herbs
                ItemID.GRIMY_RANARR_WEED +1,
                ItemID.GRIMY_IRIT_LEAF +1,
                ItemID.GRIMY_AVANTOE +1,
                ItemID.GRIMY_KWUARM +1,
                ItemID.GRIMY_CADANTINE +1,
                ItemID.GRIMY_LANTADYME +1,
                ItemID.GRIMY_DWARF_WEED +1,
                ItemID.GRIMY_TORSTOL +1,

                // Seeds
                ItemID.ACORN,
                ItemID.WILLOW_SEED,
                ItemID.MAPLE_SEED,
                ItemID.BANANA_TREE_SEED,
                ItemID.TEAK_SEED,
                ItemID.MAHOGANY_SEED,
                ItemID.YEW_SEED,
                ItemID.WATERMELON_SEED,
                ItemID.SNAPE_GRASS_SEED,
                ItemID.SPIRIT_SEED,
                ItemID.MAGIC_SEED,

                // Fish
                ItemID.RAW_ANCHOVIES +1,
                ItemID.RAW_TROUT +1,
                ItemID.RAW_SALMON +1,
                ItemID.RAW_TUNA +1,
                ItemID.RAW_LOBSTER +1,
                ItemID.RAW_SWORDFISH +1,
                ItemID.RAW_SHARK +1,

                // Other
                ItemID.COINS_995,
                ItemID.SALTPETRE +1,
                ItemID.DYNAMITE +1
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
                // Increment the Phoenix count by 1
                totalGained.merge(ItemID.PHOENIX, 1, Integer::sum);
                script.log(getClass(), "Phoenix drop detected via chat message!");
                return;
            }

            if (lower.contains("you are owed")) {
                // Extract the number from the message
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(message);
                if (matcher.find()) {
                    try {
                        lootsLeft = Integer.parseInt(matcher.group());
                        script.log(getClass(), "Updated lootsLeft to " + lootsLeft);
                    } catch (NumberFormatException e) {
                        script.log(getClass(), "Failed to parse lootsLeft from message: " + message);
                    }
                } else {
                    script.log(getClass(), "Message did not match reward: " + lower);
                }
            }
        }
    }
}