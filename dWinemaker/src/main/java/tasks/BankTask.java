package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import main.dWinemaker;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dWinemaker.*;

public class BankTask extends Task {

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        task = "Deposit inventory";
        script.log(getClass(), "Depositing full inventory...");
        script.getWidgetManager().getBank().depositAll(Collections.emptySet());

        task = "Get bank snapshot";
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(grapeID, ItemID.JUG_OF_WATER));

        if (bankSnapshot == null) {
            // Bank not visible
            return false;
        }

        task = "Check bank items";
        if (!bankSnapshot.contains(grapeID) || !bankSnapshot.contains(ItemID.JUG_OF_WATER)) {
            script.log(getClass(), "Ran out of supplies. Stopping script.");
            script.stop();
            return false;
        }

        boolean randomOrder = RandomUtils.uniformRandom(2) == 0;
        int targetAmount = 14;

        task = "Withdraw items";
        if (randomOrder) {
            withdrawWithRetry(grapeID, targetAmount);
            withdrawWithRetry(ItemID.JUG_OF_WATER, targetAmount);
        } else {
            withdrawWithRetry(ItemID.JUG_OF_WATER, targetAmount);
            withdrawWithRetry(grapeID, targetAmount);
        }

        task = "Close bank";
        closeBankWithRetry();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), 5000);
        shouldBank = false;

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(dWinemaker.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dWinemaker.BANK_ACTIONS)) {
            script.log(getClass(), "Failed to interact with bank object.");
            return;
        }

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
        }, 15000);
    }


    private void withdrawWithRetry(int itemID, int amount) {
        if (!script.getWidgetManager().getBank().withdraw(itemID, amount)) {
            script.log(getClass(), "Withdraw failed for " + itemID + ", retrying...");
            script.getWidgetManager().getBank().withdraw(itemID, amount);
        }
    }

    private void closeBankWithRetry() {
        if (!script.getWidgetManager().getBank().close()) {
            script.log(getClass(), "Bank close failed, retrying...");
            script.getWidgetManager().getBank().close();
        }
    }
}
