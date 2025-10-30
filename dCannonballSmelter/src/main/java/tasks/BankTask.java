package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCannonballSmelter.*;

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

        task = "Withdraw bars";
        boolean withdrawed = script.getWidgetManager().getBank().withdraw(ItemID.STEEL_BAR, 27);
        task = "Close bank";
        script.getWidgetManager().getBank().close();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(5000, 7500));

        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR));
        if (!inventorySnapshot.contains(ItemID.STEEL_BAR)) {
            if (!withdrawed) {
                script.log(getClass(), "No steel bars in inventory. Ran out of supplies. Stopping script!");
                script.stop();
            } else {
                script.log(getClass(), "No steel bars in inventory. Exiting banking logic.");
            }
            return false;
        }

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Opening bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (!banksFound.isEmpty()) {
            RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
            bank.interact(BANK_ACTIONS);
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
        }, script.random(15000, 17000));
    }
}
