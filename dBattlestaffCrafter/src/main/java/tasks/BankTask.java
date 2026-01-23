package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import main.dBattlestaffCrafter;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dBattlestaffCrafter.*;

public class BankTask extends Task {

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        // Always activate this task, it's last and prevents getting stuck
        return shouldBank;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        task = "Deposit items";
        script.log(getClass().getSimpleName(), "Depositing full inventory...");
        script.getWidgetManager().getBank().depositAll(Collections.emptySet());

        // Determine the correct orb based on selected staff
        int orbId = getOrbIdForStaff(staffID);
        if (orbId == -1) {
            script.log(getClass().getSimpleName(), "Unknown orb for staff: " + staffID + ", stopping script.");
            script.stop();
            return false;
        }

        // Check if we have both orbs and battlestaffs
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(orbId, ItemID.BATTLESTAFF));

        if (!bankSnapshot.contains(orbId) || !bankSnapshot.contains(ItemID.BATTLESTAFF)) {
            script.log(getClass().getSimpleName(), "Ran out of orbs or battlestaffs. Stopping script.");
            script.stop();
            return false;
        }

        boolean randomOrder = RandomUtils.uniformRandom(2) == 0;
        int targetAmount = 14;

        task = "Withdraw items";
        if (randomOrder) {
            withdrawWithRetry(orbId, targetAmount);
            withdrawWithRetry(ItemID.BATTLESTAFF, targetAmount);
        } else {
            withdrawWithRetry(ItemID.BATTLESTAFF, targetAmount);
            withdrawWithRetry(orbId, targetAmount);
        }

        task = "Close bank";
        closeBankWithRetry();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), 5000);
        shouldBank = false;

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass().getSimpleName(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(dBattlestaffCrafter.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dBattlestaffCrafter.BANK_ACTIONS)) {
            script.log(getClass().getSimpleName(), "Failed to interact with bank object.");
            return;
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

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void withdrawWithRetry(int itemID, int amount) {
        if (!script.getWidgetManager().getBank().withdraw(itemID, amount)) {
            script.log(getClass().getSimpleName(), "Withdraw failed for " + itemID + ", retrying...");
            script.getWidgetManager().getBank().withdraw(itemID, amount);
        }
    }

    private void closeBankWithRetry() {
        if (!script.getWidgetManager().getBank().close()) {
            script.log(getClass().getSimpleName(), "Bank close failed, retrying...");
            script.getWidgetManager().getBank().close();
        }
    }

    private int getOrbIdForStaff(int staffId) {
        return switch (staffId) {
            case ItemID.AIR_BATTLESTAFF -> ItemID.AIR_ORB;
            case ItemID.WATER_BATTLESTAFF -> ItemID.WATER_ORB;
            case ItemID.EARTH_BATTLESTAFF -> ItemID.EARTH_ORB;
            case ItemID.FIRE_BATTLESTAFF -> ItemID.FIRE_ORB;
            default -> -1;
        };
    }
}
