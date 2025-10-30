package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dCamTorumMiner.*;

public class DropTask extends Task {

    public static final Set<Integer> ITEM_IDS_TO_NOT_DEPOSIT2 = new HashSet<>(Set.of(
            ItemID.PAYDIRT, ItemID.BRONZE_PICKAXE, ItemID.IRON_PICKAXE,
            ItemID.STEEL_PICKAXE, ItemID.BLACK_PICKAXE, ItemID.MITHRIL_PICKAXE,
            ItemID.ADAMANT_PICKAXE, ItemID.RUNE_PICKAXE, ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_OR, ItemID.CRYSTAL_PICKAXE, ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR, ItemID.ANTIQUE_LAMP, ItemID.GILDED_PICKAXE,
            ItemID.HAMMER, ItemID.IMCANDO_HAMMER, ItemID.IMCANDO_HAMMER_OFFHAND,
            ItemID.BLESSED_BONE_SHARDS, ItemID.CALCIFIED_MOTH, ItemID.CALCIFIED_DEPOSIT,
            ItemID.GEM_BAG, ItemID.GEM_BAG_12020, ItemID.GEM_BAG_25628, ItemID.OPEN_GEM_BAG
    ));

    private int currentBankThreshold = getNewBankThreshold();

    public DropTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!dropMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        WorldPosition myPos = script.getWorldPosition();
        return inv != null && inv.isFull() || myPos != null && anvilPlusBankArea.contains(myPos);
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(getTrackedItemIDs());
        if (inv == null) return false;

        int deposits = safeAmount(inv.getAmount(ItemID.CALCIFIED_DEPOSIT));
        int sapphires = safeAmount(inv.getAmount(ItemID.UNCUT_SAPPHIRE));
        int emeralds = safeAmount(inv.getAmount(ItemID.UNCUT_EMERALD));
        int rubies = safeAmount(inv.getAmount(ItemID.UNCUT_RUBY));
        int diamonds = safeAmount(inv.getAmount(ItemID.UNCUT_DIAMOND));
        int loopKeys = safeAmount(inv.getAmount(ItemID.LOOP_HALF_OF_KEY_30107));

        int totalBankable = sapphires + emeralds + rubies + diamonds + loopKeys;

        if (!dropAllMode) {
            script.log(getClass(), String.format("Detected: Sapphires=%d, Emeralds=%d, Rubies=%d, Diamonds=%d, LoopKeys=%d (Total bankable = %d, Threshold = %d)",
                    sapphires, emeralds, rubies, diamonds, loopKeys, totalBankable, currentBankThreshold));

            // Bank if total bankable meets/exceeds threshold
            task = "Check bankable items";
            if (totalBankable >= currentBankThreshold) {
                task = "Check position";
                WorldPosition myPos = script.getWorldPosition();
                if (myPos != null && !bankArea.contains(myPos)) {
                    task = "Walk to bank area";
                    return script.getWalker().walkTo(bankWalkArea.getRandomPosition());
                }

                boolean success = bankItems(inv);
                if (success) {
                    currentBankThreshold = getNewBankThreshold();
                    return true;
                }
            }
        }

        // Drop deposits
        if (deposits > 0) {
            script.log(getClass(), "Dropping " + deposits + " calcified deposit(s).");
            script.getWidgetManager().getInventory().dropItems(ItemID.CALCIFIED_DEPOSIT);
        }

        // Drop other stuff
        if (dropAllMode) {
            if (sapphires > 0 || emeralds > 0 || rubies > 0 || diamonds > 0 || loopKeys > 0) {
                script.log(getClass(), "Dropping gems/keys.");
                script.getWidgetManager().getInventory().dropItems(ItemID.UNCUT_SAPPHIRE, ItemID.UNCUT_EMERALD, ItemID.UNCUT_RUBY, ItemID.UNCUT_DIAMOND, ItemID.LOOP_HALF_OF_KEY_30107);
            }
        }

        inv = script.getWidgetManager().getInventory().search(getTrackedItemIDs());

        if (!dropAllMode) {
            if (!inv.containsAny(getTrackedItemIDs())) {
                script.log(getClass().getSimpleName(), "Nothing to drop/deposit anymore, walking back!");
                return script.getWalker().walkTo(miningArea.getRandomPosition());
            } else {
                script.log(getClass().getSimpleName(), "Not enough to bank, walking back to mining area.");
                script.getWalker().walkTo(miningArea.getRandomPosition());
            }
        } else {
            script.log(getClass(), "Drop task completed, returning...");
        }

        return true;
    }

    private boolean bankItems(ItemGroupResult inv) {
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

        var snapshot = script.getWidgetManager().getDepositBox().search(ITEM_IDS_TO_NOT_DEPOSIT2);
        if (snapshot == null) {
            script.log(getClass(), "Deposit box not open.");
            return false;
        }

        if (!script.getWidgetManager().getDepositBox().depositAll(ITEM_IDS_TO_NOT_DEPOSIT2)) {
            script.log(getClass(), "Failed to deposit items.");
            return false;
        }

        script.getWidgetManager().getDepositBox().close();
        script.log(getClass(), "Banked items and closed deposit box.");
        return true;
    }

    private int getNewBankThreshold() {
        return script.random(15, 21);
    }

    private int safeAmount(int amount) {
        return Math.max(amount, 0);
    }

    private Set<Integer> getTrackedItemIDs() {
        return Set.of(
                ItemID.UNCUT_SAPPHIRE,
                ItemID.UNCUT_EMERALD,
                ItemID.UNCUT_RUBY,
                ItemID.UNCUT_DIAMOND,
                ItemID.CALCIFIED_DEPOSIT,
                ItemID.CLUE_GEODE_BEGINNER,
                ItemID.CLUE_GEODE_EASY,
                ItemID.CLUE_GEODE_MEDIUM,
                ItemID.CLUE_GEODE_HARD,
                ItemID.CLUE_GEODE_ELITE,
                ItemID.LOOP_HALF_OF_KEY_30107
        );
    }
}