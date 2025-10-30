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

public class cWarsCrate extends Task {

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    private int withdrawFailCount = 0;

    public cWarsCrate(Script script) {
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
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CASTLE_WARS_SUPPLY_CRATE));
        if (inventorySnapshot == null) return false;

        if (!inventorySnapshot.contains(ItemID.CASTLE_WARS_SUPPLY_CRATE)) {
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
            ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.CASTLE_WARS_SUPPLY_CRATE));
            if (bankSnapshot == null) return false;
            if (!bankSnapshot.contains(ItemID.CASTLE_WARS_SUPPLY_CRATE)) {
                script.log(getClass(), "Out of supply crates, stopping script!");
                script.stop();
            } else {
                lootsLeft = bankSnapshot.getAmount(ItemID.CASTLE_WARS_SUPPLY_CRATE);
            }

            task = "Withdraw items";
            if (!script.getWidgetManager().getBank().withdraw(ItemID.CASTLE_WARS_SUPPLY_CRATE, 20)) {
                withdrawFailCount++;
                script.log(getClass(), "Failed to withdraw supply crates. Fail count: " + withdrawFailCount);

                if (withdrawFailCount >= 3) {
                    script.log(getClass(), "Withdraw failed 3 times in a row. Stopping script.");
                    script.getWidgetManager().getBank().close();
                    script.getWidgetManager().getLogoutTab().logout();
                    script.stop();
                }

                return false;
            }

            // Reset fail counter on successful withdraw
            withdrawFailCount = 0;

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

        // Open crates one-by-one with fresh re-queries every iteration
        int safety = 40;
        while (safety-- > 0) {
            // Get a fresh snapshot of crates each loop
            ItemGroupResult cratesNow = script.getWidgetManager().getInventory()
                    .search(Set.of(ItemID.CASTLE_WARS_SUPPLY_CRATE));
            if (cratesNow == null || !cratesNow.contains(ItemID.CASTLE_WARS_SUPPLY_CRATE)) break;

            // Build a list of available slots and randomize the order
            java.util.List<Integer> slotList = new java.util.ArrayList<>(cratesNow.getSlotsForItem(ItemID.CASTLE_WARS_SUPPLY_CRATE));
            if (slotList.isEmpty()) break;
            java.util.Collections.shuffle(slotList);

            // Open crates in this randomized order
            for (int slot : slotList) {
                // Take a baseline snapshot before interacting
                onSearchStartBaseline();
                int beforeCount = cratesNow.getAmount(ItemID.CASTLE_WARS_SUPPLY_CRATE);

                // Get fresh bounds for this slot
                var boundsRes = script.getWidgetManager().getInventory().getBoundsForSlot(slot);
                if (boundsRes == null || !boundsRes.isFound()) {
                    script.log(getClass(), "No bounds for crate slot " + slot + " (skipping this slot).");
                    continue;
                }

                Rectangle bounds = boundsRes.get();
                script.getFinger().tap(bounds);
                script.pollFramesUntil(() -> false, script.random(1, 200)); // slight jitter

                // Wait until: crate count decreases OR slot disappears OR loot delta detected
                boolean opened = script.pollFramesUntil(() -> {
                    ItemGroupResult invCheck = script.getWidgetManager().getInventory()
                            .search(Set.of(ItemID.CASTLE_WARS_SUPPLY_CRATE));
                    if (invCheck == null) return false;

                    int nowCount = invCheck.getAmount(ItemID.CASTLE_WARS_SUPPLY_CRATE);
                    if (nowCount < beforeCount) return true; // crate successfully opened

                    var stillThere = script.getWidgetManager().getInventory().getBoundsForSlot(slot);
                    boolean slotGone = (stillThere == null) || !stillThere.isFound();
                    if (slotGone) return true; // slot visually disappeared

                    return hasTrackedDelta(); // loot detected
                }, script.random(2000, 3500));

                // Update totals if a loot delta was detected
                updateLootCountsIfNeeded();

                // Human-like pause before moving to the next crate
                script.pollFramesUntil(() -> false, script.random(50, 150));
            }
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
                ItemID.BLIGHTED_MANTA_RAY +1,
                ItemID.BLIGHTED_ANGLERFISH +1,
                ItemID.BLIGHTED_KARAMBWAN +1,
                ItemID.BLIGHTED_SUPER_RESTORE4 +1,
                ItemID.BLIGHTED_ANCIENT_ICE_SACK,
                ItemID.BLIGHTED_VENGEANCE_SACK,
                ItemID.CASTLE_WARS_ARROW,
                ItemID.CASTLE_WARS_BOLTS,
                ItemID.RUNE_ARROW,
                ItemID.RUNE_JAVELIN,
                ItemID.CASTLE_WARS_TICKET
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

        script.log(getClass(), "Updated loot counts (" + "crates opened" + ").");
    }
}