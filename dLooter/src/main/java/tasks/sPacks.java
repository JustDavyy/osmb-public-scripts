package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import utils.Task;

import java.util.*;
import java.util.function.Predicate;

import static main.dLooter.*;

public class sPacks extends Task {

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public sPacks(Script script) {
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

        task = getClass().getSimpleName();
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.SEED_PACK));
        if (inventorySnapshot == null) return false;

        if (!inventorySnapshot.contains(ItemID.SEED_PACK)) {
            // Bank process here
            if (!script.getWidgetManager().getBank().isVisible()) {
                script.log(getClass(), "Bank is not visible, opening bank!");
                task = "Open bank";
                openBank();
                return false;
            }

            task = "Deposit items";
            if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
                script.log(getClass().getSimpleName(), "Deposit all action failed.");
                return false;
            }

            task = "Check item count";
            ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.SEED_PACK));
            if (bankSnapshot == null) return false;
            if (!bankSnapshot.contains(ItemID.SEED_PACK)) {
                script.log(getClass(), "Out of seed packs, stopping script!");
                script.stop();
            } else {
                lootsLeft = bankSnapshot.getAmount(ItemID.SEED_PACK);
            }

            task = "Withdraw items";
            var bankSearch = script.getWidgetManager().getBank().search(Set.of(ItemID.SEED_PACK));
            if (bankSearch != null) {
                int slot = bankSearch.getSlotForItem(ItemID.SEED_PACK);
                var boundsOpt = script.getWidgetManager().getBank().getBoundsForSlot(slot);
                if (boundsOpt != null && boundsOpt.isFound()) {
                    Rectangle bounds = boundsOpt.get();
                    if (script.getFinger().tap(bounds)) {
                        // success
                    } else {
                        script.log(getClass(), "Failed to tap on seed pack slot in bank.");
                        return false;
                    }
                } else {
                    script.log(getClass(), "No bounds found for seed pack slot in bank.");
                    return false;
                }
            } else {
                script.log(getClass(), "Bank search returned null.");
                return false;
            }

            task = "Close bank";
            script.getWidgetManager().getBank().close();
            return script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        }

        // Processing here
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (script.getWidgetManager().getBank().isVisible()) {
            return script.getWidgetManager().getBank().close();
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        // Open seed packs one-by-one and update loot each time
        int safety = 12; // hard cap per execute() tick (avoid infinite loops)
        while (safety-- > 0) {
            ItemGroupResult packs = script.getWidgetManager().getInventory().search(Set.of(ItemID.SEED_PACK));
            if (packs == null || !packs.contains(ItemID.SEED_PACK)) break;

            // pick one current slot (re-queried each iteration to avoid stale indices)
            Set<Integer> packSlots = packs.getSlotsForItem(ItemID.SEED_PACK);
            if (packSlots == null || packSlots.isEmpty()) break;
            int firstPackSlot = packSlots.iterator().next();

            // baseline before opening this pack
            onSearchStartBaseline();

            Set<Integer> seedPackSlots = inventorySnapshot.getSlotsForItem(ItemID.SEED_PACK);
            if (seedPackSlots != null) {
                for (int packSlot : seedPackSlots) {
                    var boundsOpt = script.getWidgetManager().getInventory().getBoundsForSlot(packSlot);
                    if (boundsOpt != null && boundsOpt.isFound()) {
                        Rectangle bounds = boundsOpt.get();
                        script.getFinger().tap(bounds, "Take-all");
                        script.pollFramesHuman(() -> false, script.random(1, 250));
                    } else {
                        script.log(getClass(), "No bounds found for seed pack slot " + packSlot + " in inventory.");
                    }
                }
            }

            // wait until either: tracked loot increases OR pack view changes OR pack count changes
            script.pollFramesHuman(() ->
                            hasTrackedDelta() ||
                                    (script.getWidgetManager().getInventory().getBoundsForSlot(firstPackSlot) != null &&
                                            !script.getWidgetManager().getInventory().getBoundsForSlot(firstPackSlot).isFound()) ||
                                    (script.getWidgetManager().getInventory().search(Set.of(ItemID.SEED_PACK)) != null &&
                                            !script.getWidgetManager().getInventory().search(Set.of(ItemID.SEED_PACK)).contains(ItemID.SEED_PACK)),
                    script.random(800, 1800)
            );

            // add deltas to totals and mark processed
            updateLootCountsIfNeeded();

            // tiny human jitter
            script.pollFramesHuman(() -> false, script.random(80, 160));
        }

        return false;
    }

    private void openBank() {
        script.log(getClass(), "Opening bank...");

        if (script.getWidgetManager().getBank().isVisible()) return;

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found nearby, stopping script...");
            script.stop();
            return;
        }

        RSObject closestBank = (RSObject) script.getUtils().getClosest(banksFound);

        if (!closestBank.interact(BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object after walking.");
            return;
        }

        script.pollFramesHuman(() -> script.getWidgetManager().getBank().isVisible(), script.random(5000, 8000));
    }

    private Set<Integer> getTrackedItems() {
        return Set.of(
                // Low Rewards
                ItemID.ACORN,
                ItemID.APPLE_TREE_SEED,
                ItemID.BANANA_TREE_SEED,
                ItemID.ORANGE_TREE_SEED,
                ItemID.CURRY_TREE_SEED,

                // Medium Rewards
                ItemID.LIMPWURT_SEED,
                ItemID.WATERMELON_SEED,
                ItemID.SNAPE_GRASS_SEED,
                ItemID.POTATO_CACTUS_SEED,
                ItemID.WILLOW_SEED,
                ItemID.PINEAPPLE_SEED,
                ItemID.CALQUAT_TREE_SEED,
                ItemID.TEAK_SEED,
                ItemID.WHITE_LILY_SEED,

                // High Rewards
                ItemID.PAPAYA_TREE_SEED,
                ItemID.PALM_TREE_SEED,
                ItemID.HESPORI_SEED,
                ItemID.MAPLE_SEED,
                ItemID.MAHOGANY_SEED,
                ItemID.YEW_SEED,
                ItemID.DRAGONFRUIT_TREE_SEED,
                ItemID.CELASTRUS_SEED,
                ItemID.MAGIC_SEED,
                ItemID.SPIRIT_SEED,
                ItemID.REDWOOD_TREE_SEED
        );
    }

    private boolean hasTrackedDelta() {
        ItemGroupResult inv = searchTracked();
        if (inv == null) return false;
        for (int id : getTrackedItems()) {
            int current = inv.getAmount(id);
            int last = lastSeenInventory.getOrDefault(id, 0);
            if (current > last) return true;
        }
        return false;
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

    private void updateLootCountsIfNeeded() {
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

        script.log(getClass(), "Updated loot counts (" + "seed pack opened" + ").");
    }
}