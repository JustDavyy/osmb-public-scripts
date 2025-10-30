package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dOffering.*;

public class Bank extends Task {
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public Bank(Script script) {
        super(script);
    }

    private int withdrawFailCount = 0;

    @Override
    public boolean activate() {
        return needToBank;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inv == null) return false;

        if (inv.isFull()) {
            script.log(getClass(), "Inventory already full in bank process, marking need to bank false and returning!");
            needToBank = false;
            withdrawFailCount = 0;
            return false;
        }

        task = "Withdraw items";
        if (!script.getWidgetManager().getBank().withdraw(selectedItem, castsPerInvent * 3)) {
            withdrawFailCount++;
            script.log(getClass(), "Failed to withdraw " + selectedItem + ". Fail count: " + withdrawFailCount);

            if (withdrawFailCount >= 3) {
                script.log(getClass(), "Withdraw failed 3 times in a row. Stopping script.");
                closeBankWithRetry();
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
            }

            return false;
        }

        // Reset fail counter on successful withdraw
        withdrawFailCount = 0;

        task = "Close bank";
        closeBankWithRetry();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), 5000);
        needToBank = false;

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass().getSimpleName(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
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

    private void closeBankWithRetry() {
        if (!script.getWidgetManager().getBank().close()) {
            script.log(getClass().getSimpleName(), "Bank close failed, retrying...");
            if (!script.getWidgetManager().getBank().close()) {
                script.log(getClass().getSimpleName(), "Bank close failed, retrying...");
                if (!script.getWidgetManager().getBank().close()) {
                    script.log(getClass().getSimpleName(), "Bank close failed, retrying...");
                    script.getWidgetManager().getBank().close();
                }
            }
        }
    }
}
