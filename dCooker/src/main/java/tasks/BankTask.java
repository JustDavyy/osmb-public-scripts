package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import data.CookingItem;
import main.dCooker;
import utils.Task;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dCooker.*;

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
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(cookingItemID));
        if (!script.getWidgetManager().getBank().isVisible()) {
            script.log(getClass(), "Bank is not visible, opening bank!");
            openBank();
            return false;
        }

        task = "Deposit items";
        if (bankMethod.equals("Deposit all")) {
            if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
                script.log(getClass().getSimpleName(), "Deposit all action failed.");
                return false;
            }
            script.log(getClass(), "Deposit all succeeded.");
        } else {
            if (!script.getWidgetManager().getBank().depositAll(Set.of(cookingItemID))) {
                script.log(getClass().getSimpleName(), "Deposit item by item action failed.");
                return false;
            }
            script.log(getClass(), "Deposit item by item succeeded.");
        }

        int targetAmount = script.getWidgetManager().getInventory().getGroupSize();
        if (cookingItemID == ItemID.GIANT_SEAWEED) {
            targetAmount /= 6;
        }

        int amountNeeded = targetAmount - inventorySnapshot.getAmount(cookingItemID);

        if (amountNeeded == 0) {
            task = "Close bank";
            script.getWidgetManager().getBank().close();
            script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else if (amountNeeded > 0) {
            ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(Set.of(cookingItemID));
            if (!bankSnapshot.contains(cookingItemID)) {
                if (isMultipleMode && selectedItemIndex + 1 < selectedItemIDs.size()) {
                    selectedItemIndex++;
                    cookingItemID = selectedItemIDs.get(selectedItemIndex);
                    assert CookingItem.fromRawItemId(cookingItemID) != null;
                    cookedItemID = CookingItem.fromRawItemId(cookingItemID).getCookedItemId();
                    script.log(getClass(), "Switching to next fish: " + script.getItemManager().getItemName(cookingItemID));
                    return false;
                } else {
                    script.log(getClass(), "Out of all food to cook. Stopping.");
                    script.stop();
                    return false;
                }
            }

            if (!script.getWidgetManager().getBank().withdraw(cookingItemID, targetAmount)) {
                script.log(getClass(), "Withdraw failed for item id: " + cookingItemID);
                return false;
            }

            script.getWidgetManager().getBank().close();
            script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), script.random(4000, 6000));
        } else {
            if (bankMethod.equals("Deposit all")) {
                if (!script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
                    script.log(getClass().getSimpleName(), "Deposit all action failed.");
                    return false;
                }
            } else {
                if (!script.getWidgetManager().getBank().deposit(cookingItemID, Math.abs(targetAmount))) {
                    script.log(getClass().getSimpleName(), "Deposit item by item action failed.");
                    return false;
                }
            }
        }
        return false;
    }

    private void openBank() {
        script.log(getClass(), "Searching for a bank...");
        task = "Open bank";

        if (script.getWorldPosition() != null && script.getWorldPosition().getRegionID() == 12109) {
            // We are in 12109, use the NPC tile
            WorldPosition npcTilePosition = new WorldPosition(3042, 4972, 1);

            RSTile npcTile = script.getSceneManager().getTile(npcTilePosition);
            if (npcTile == null) {
                script.log(getClass(), "NPC tile is null, cannot open bank via NPC.");
                return;
            }

            if (!npcTile.isOnGameScreen()) {
                script.log(getClass(), "NPC tile not on screen, walking...");
                script.getWalker().walkTo(npcTilePosition);
                return;
            }

            script.log(getClass(), "Interacting with NPC tile for banking...");
            if (!script.getFinger().tap(npcTile.getTileCube(110).getResized(0.4), "Bank")) {
                script.log(getClass(), "Failed to long-press 'Bank' on NPC tile.");
                return;
            }
        } else {
            // Regular bank object
            List<RSObject> banksFound = script.getObjectManager().getObjects(dCooker.bankQuery);
            if (banksFound.isEmpty()) {
                script.log(getClass(), "No bank objects found.");
                return;
            }

            RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
            if (!bank.interact(dCooker.BANK_ACTIONS)) {
                script.log(getClass(), "Failed to interact with bank object.");
                return;
            }
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
