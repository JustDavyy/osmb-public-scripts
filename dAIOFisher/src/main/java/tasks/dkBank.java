package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import data.FishingLocation;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dAIOFisher.*;

public class dkBank extends Task {
    private int missingKarambwanjiCount = 0;

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public dkBank(Script script) {
        super(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI, ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159, ItemID.RAW_KARAMBWAN, ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL, ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (!inventorySnapshot.contains(ItemID.RAW_KARAMBWANJI)) {
            missingKarambwanjiCount++;
            script.log(getClass().getSimpleName(), "❌ Missing Karambwanji (" + missingKarambwanjiCount + "/3)");
            script.pollFramesHuman(() -> false, script.random(2000, 4000));

            if (!script.getWidgetManager().getBank().isVisible()) {
                openBank();
                return false;
            }

            if (missingKarambwanjiCount >= 3) {
                script.log(getClass().getSimpleName(), "‼ Karambwanji missing 3 times in a row. Stopping script...");
                script.stop();
            }

            return false;
        } else {
            missingKarambwanjiCount = 0; // Reset counter if found
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        if (usingBarrel && inventorySnapshot.contains(ItemID.RAW_KARAMBWAN)) {
            task = "Empty fish barrel";
            script.log(getClass().getSimpleName(), "Emptying fish barrel in the bank");
            if (!inventorySnapshot.getItem(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL).interact("Empty")) {
                return false;
            }

            // Add inventory karambwan amount + 28 from fishing barrel
            fish1Caught += inventorySnapshot.getAmount(ItemID.RAW_KARAMBWAN) + 28;
        }

        if (!usingBarrel && inventorySnapshot.contains(ItemID.RAW_KARAMBWAN)) {
            fish1Caught += inventorySnapshot.getAmount(ItemID.RAW_KARAMBWAN);
        }

        task = "Deposit karambwans";
        if (!script.getWidgetManager().getBank().depositAll(Set.of(ItemID.RAW_KARAMBWANJI, ItemID.KARAMBWAN_VESSEL, ItemID.KARAMBWAN_VESSEL_3159, ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL, ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET, ItemID.SPIRIT_FLAKES))) {
            return false;
        }

        task = "Close bank";
        script.getWidgetManager().getBank().close();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));

        doneBanking = true;
        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Searching for a bank...");

        // Regular bank object
        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

        // Same waiting logic after interaction
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, script.random(14000, 16000));
    }
}
