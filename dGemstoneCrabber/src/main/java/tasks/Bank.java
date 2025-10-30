package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dGemstoneCrabber.*;

public class Bank extends Task {
    // Areas
    private static final Area bankArea = new RectangleArea(1233, 3113, 18, 15, 0);
    private static final Area bankWalkArea = new RectangleArea(1235, 3116, 14, 10, 0);

    public Bank(Script script) {
        super(script);
    }

    public boolean activate() {
        return needToBank;
    }

    public boolean execute() {

        task = "Get and cache position";
        currentPos = script.getWorldPosition();
        if (currentPos == null) return false;

        // 1) Go to bank if not there
        if (!isAtBank(currentPos)) {
            task = "Navigate to bank";
            script.log(getClass(), "Not at bank area, moving there!");

            WalkConfig cfg = new WalkConfig.Builder()
                    .enableRun(true)
                    .disableWalkScreen(true)
                    .breakCondition(() -> {
                        RSObject bank = getClosestBank();
                        return bank != null && bank.isInteractableOnScreen();
                    })
                    .build();

            return script.getWalker().walkTo(bankWalkArea.getRandomPosition(), cfg);
        }

        // 2) Open bank if not open
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        // 3) Deposit all except runes / rune pouches
        boolean deposited = script.getWidgetManager().getBank().depositAll(Set.of(
                ItemID.AIR_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE, ItemID.FIRE_RUNE, ItemID.MIND_RUNE,
                ItemID.CHAOS_RUNE, ItemID.DEATH_RUNE, ItemID.BLOOD_RUNE, ItemID.WRATH_RUNE,
                ItemID.RUNE_POUCH, ItemID.RUNE_POUCH_23650, ItemID.RUNE_POUCH_27086, ItemID.RUNE_POUCH_L,
                ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_L, ItemID.SUNFIRE_RUNE,
                ItemID.MIST_RUNE, ItemID.DUST_RUNE, ItemID.MUD_RUNE, ItemID.SMOKE_RUNE,
                ItemID.STEAM_RUNE, ItemID.LAVA_RUNE, 30843, ItemID.SOUL_RUNE, foodID, potID,
                ItemID.DRAGON_BATTLEAXE
        ));
        if (!deposited) {
            script.log(getClass(), "Failed to deposit items, retrying later...");
            return false;
        }

        // Track whether we changed anything / still need bank open
        boolean didAnyWithdraw = false;

        // 4) POTIONS (if enabled)
        if (usePot) {
            task = "Get inventory snapshot (pots)";
            ItemGroupResult invPot = script.getWidgetManager().getInventory().search(Set.of(potID));
            if (invPot == null) return false;

            int have = invPot.getAmount(potID);
            int need = Math.max(0, potAmount - have);

            if (need > 0) {
                task = "Withdraw pots (" + need + ")";
                // up to 3 attempts
                boolean success = false;
                for (int attempt = 1; attempt <= 3 && !success; attempt++) {
                    // Ensure it exists in bank
                    ItemGroupResult bankPot = script.getWidgetManager().getBank().search(Set.of(potID));
                    if (bankPot == null || !bankPot.contains(potID)) {
                        if (attempt == 3) {
                            script.log(getClass(), "Cannot locate potion after 3 tries: " +
                                    script.getItemManager().getItemName(potID) + ". Stopping script.");
                            script.stop();
                            return false;
                        }
                        script.log(getClass(), "Potion not found (attempt " + attempt + "/3). Retrying...");
                        script.pollFramesHuman(() -> false, script.random(250, 600));
                        continue;
                    }

                    // Try to withdraw
                    if (!script.getWidgetManager().getBank().withdraw(potID, need)) {
                        if (attempt == 3) {
                            script.log(getClass(), "Withdraw failed for potion after 3 tries: " +
                                    script.getItemManager().getItemName(potID) + ". Stopping script.");
                            script.stop();
                            return false;
                        }
                        script.log(getClass(), "Withdraw failed (attempt " + attempt + "/3) for pot: " +
                                script.getItemManager().getItemName(potID) + ". Retrying...");
                        script.pollFramesHuman(() -> false, script.random(250, 600));
                    } else {
                        success = true;
                        didAnyWithdraw = true;
                    }
                }
            }
        }

        // 5) FOOD (if enabled)
        if (useFood) {
            task = "Get inventory snapshot (food)";
            ItemGroupResult invFood = script.getWidgetManager().getInventory().search(Set.of(foodID));
            if (invFood == null) return false;

            int have = invFood.getAmount(foodID);
            int need = Math.max(0, foodAmount - have);

            if (need > 0) {
                task = "Withdraw food (" + need + ")";
                // up to 3 attempts
                boolean success = false;
                for (int attempt = 1; attempt <= 3 && !success; attempt++) {
                    // Ensure it exists in bank
                    ItemGroupResult bankFood = script.getWidgetManager().getBank().search(Set.of(foodID));
                    if (bankFood == null || !bankFood.contains(foodID)) {
                        if (attempt == 3) {
                            script.log(getClass(), "Cannot locate food after 3 tries: " +
                                    script.getItemManager().getItemName(foodID) + ". Stopping script.");
                            script.stop();
                            return false;
                        }
                        script.log(getClass(), "Food not found (attempt " + attempt + "/3). Retrying...");
                        script.pollFramesHuman(() -> false, script.random(250, 600));
                        continue;
                    }

                    // Try to withdraw
                    if (!script.getWidgetManager().getBank().withdraw(foodID, need)) {
                        if (attempt == 3) {
                            script.log(getClass(), "Withdraw failed for food after 3 tries: " +
                                    script.getItemManager().getItemName(foodID) + ". Stopping script.");
                            script.stop();
                            return false;
                        }
                        script.log(getClass(), "Withdraw failed (attempt " + attempt + "/3) for food: " +
                                script.getItemManager().getItemName(foodID) + ". Retrying...");
                        script.pollFramesHuman(() -> false, script.random(250, 600));
                    } else {
                        success = true;
                        didAnyWithdraw = true;
                    }
                }
            }
        }

        // 6) Close the bank
        if (script.getWidgetManager().getBank().isVisible()) {
            closeBank();
        }

        // 7) Only clear needToBank if we truly have what we need
        boolean haveEnoughPots = true;
        if (usePot) {
            ItemGroupResult invPot = script.getWidgetManager().getInventory().search(Set.of(potID));
            haveEnoughPots = invPot != null && invPot.getAmount(potID) >= potAmount;
        }

        boolean haveEnoughFood = true;
        if (useFood) {
            ItemGroupResult invFood = script.getWidgetManager().getInventory().search(Set.of(foodID));
            haveEnoughFood = invFood != null && invFood.getAmount(foodID) >= foodAmount;
        }

        task = "Update flags";
        needToBank = !(haveEnoughPots && haveEnoughFood);
        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Opening bank...");

        RSObject bankFound = getClosestBank();
        if (bankFound != null) {
            bankFound.interact("bank");
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for open bank";
        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            boolean success = script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
            if (success) {
                script.pollFramesHuman(() -> false, script.random(1, 1500));
            }
            return success;
        }, script.random(15000, 17000));
    }

    private boolean closeBank() {
        task = "Close bank";
        script.getWidgetManager().getBank().close();
        return script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
    }

    private boolean isAtBank(WorldPosition myPos) {
        // True if inside the defined bank area
        if (bankArea.contains(myPos)) {
            return true;
        }

        // Or if bank is already visible & interactable on screen
        RSObject bank = getClosestBank();
        return bank != null && bank.isInteractableOnScreen();
    }

    private RSObject getClosestBank() {
        List<RSObject> objs = script.getObjectManager().getObjects(obj ->
                obj != null &&
                        obj.getName() != null &&
                        (obj.getName().equalsIgnoreCase("Bank booth"))
                        && obj.canReach());
        if (objs == null || objs.isEmpty()) return null;
        return (RSObject) script.getUtils().getClosest(objs);
    }
}
