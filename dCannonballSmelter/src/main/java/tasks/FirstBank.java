package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.utils.RandomUtils;
import main.dCannonballSmelter;
import utils.Task;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCannonballSmelter.*;

public class FirstBank extends Task {

    public FirstBank(Script script) {
        super(script);
    }

    private boolean hasMould = false;

    @Override
    public boolean activate() {
        return !bankSetupDone;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        task = "Check inventory";
        if (inventorySnapshot.containsAny(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD))) {
            hasMould = true;
            inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.STEEL_BAR));
            if (inventorySnapshot.contains(ItemID.STEEL_BAR)) {
                script.log(getClass().getSimpleName(), "We already have a mould and bars in our inventory, marking bank setup as done.");
                bankSetupDone = true;
                return false;
            }
        }

        if (!script.getWidgetManager().getBank().isVisible()) {
            openBank();
            return false;
        }

        ItemGroupResult bankSnapshot;
        if (!hasMould) {
            task = "Deposit all";
            script.getWidgetManager().getBank().depositAll(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD));
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(300, 600));
            bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.DOUBLE_AMMO_MOULD));
            if (bankSnapshot.contains(ItemID.DOUBLE_AMMO_MOULD)) {
                task = "Withdraw double mould";
                script.getWidgetManager().getBank().withdraw(ItemID.DOUBLE_AMMO_MOULD, 1);
            } else {
                bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.AMMO_MOULD));
                if (bankSnapshot.contains(ItemID.AMMO_MOULD)) {
                    task = "Withdraw single mould";
                    script.getWidgetManager().getBank().withdraw(ItemID.AMMO_MOULD, 1);
                } else {
                    script.log(getClass(), "No ammo moulds available. Stopping script.");
                    script.stop();
                    return false;
                }
            }
        }

        bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.STEEL_BAR));

        task = "Check bank supplies";
        if (!bankSnapshot.contains(ItemID.STEEL_BAR)) {
            script.log(getClass(), "Ran out of supplies. Stopping script.");
            script.stop();
            return false;
        }

        task = "Withdraw bars";
        script.getWidgetManager().getBank().withdraw(ItemID.STEEL_BAR, 27);
        task = "Close bank";
        script.getWidgetManager().getBank().close();
        script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), RandomUtils.uniformRandom(5000, 7500));

        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD, ItemID.STEEL_BAR));

        task = "Check inventory";
        if (inventorySnapshot.containsAny(Set.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD)) && inventorySnapshot.contains(ItemID.STEEL_BAR)) {
            bankSetupDone = true;
            script.log(getClass(), "Bank setup completed successfully.");
        }

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log(getClass(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(dCannonballSmelter.bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass(), "No bank objects found.");
            return;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(dCannonballSmelter.BANK_ACTIONS)) {
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
        }, RandomUtils.uniformRandom(15000, 17000));
    }
}