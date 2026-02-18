package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import data.FishingLocation;
import utils.Task;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static main.dAIOFisher.*;

public class Cook extends Task {

    public Cook(Script script) {
        super(script);
    }

    public boolean activate() {
        if (!cookMode) return false;
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }
        ItemGroupResult inv;
        inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getCatchableFish()));
        return inv != null && inv.isFull() && inv.containsAny(Set.copyOf(fishingMethod.getCatchableFish()));
    }

    public boolean execute() {
        task = "Find and cook raw fish";

        List<Integer> rawFish = fishingMethod.getCatchableFish();
        ItemGroupResult inventory = script.getWidgetManager().getInventory().search(Set.copyOf(rawFish));
        if (inventory == null || inventory.isEmpty()) {
            script.log("Cook", "No raw fish found in inventory.");
            return false;
        }

        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (fishingMethod.getCookingObjectArea() != null) {
            script.log("Cook", "Cooking area is defined, we need to move there!");
            if (!fishingMethod.getCookingObjectArea().contains(myPos)) {
                script.log("Cook", "Not inside cooking area, moving there!");

                if (fishingMethod.getCookingHasDoor()) {
                    script.log("Cook", "Cooking area has door, we might need to handle that.");
                }

                boolean success = moveToArea(fishingMethod.getCookingObjectArea());

                if (!success && fishingMethod.getCookingHasDoor()) {
                    script.log("Cook", "Handling door logic first!");
                    RSObject door = getSpecificObjectAt("Door", fishingMethod.getCookingDoorPosition().getX(), fishingMethod.getCookingDoorPosition().getY(), fishingMethod.getCookingDoorPosition().getPlane());

                    if (door == null) {
                        script.log("Cook", "Door is null, we need to move closer!");
                        return script.getWalker().walkTo(fishingMethod.getCookingDoorPosition());
                    }

                    return door.interact("Open");
                }

                return success;
            }
        }

        // Different cooking method for eels
        if (fishingLocation.equals(FishingLocation.Zul_Andra) || fishingLocation.equals(FishingLocation.Mor_Ul_Rek_East) || fishingLocation.equals(FishingLocation.Mor_Ul_Rek_West)) {
            // Select which tool/fish to use
            int toolToUse = 0;
            int fishToUse = 0;
            if (fishingLocation.equals(FishingLocation.Zul_Andra)) {
                toolToUse = ItemID.KNIFE;
                fishToUse = ItemID.SACRED_EEL;
            } else {
                toolToUse = ItemID.HAMMER;
                fishToUse = ItemID.INFERNAL_EEL;
            }

            task = "Start cooking action";
            inventory = script.getWidgetManager().getInventory().search(Set.of(fishToUse, toolToUse));
            if (inventory == null) {
                script.log("Cook", "Inventory could not be found");
                return false;
            }

            if (!alreadyCountedFish) {
                fish1Caught += inventory.getAmount(fishToUse);
                alreadyCountedFish = true;
            }

            boolean clickSuccess;
            int clickOrder = RandomUtils.uniformRandom(1, 2);
            if (clickOrder == 1) {
                // Click fish first, then tool
                clickSuccess = inventory.getItem(fishToUse).interact() &&
                        inventory.getItem(toolToUse).interact();
            } else {
                // Click tool first, then fish
                clickSuccess = inventory.getItem(toolToUse).interact() &&
                        inventory.getItem(fishToUse).interact();
            }

            if (!clickSuccess) {
                return false;
            }

            // We're now cutting the eels, wait to complete.
            waitUntilFinishedCutting(fishToUse);
        }

        for (int rawId : rawFish) {
            if (!inventory.contains(rawId)) continue;

            if (rawId == ItemID.RAW_SALMON && currentCookingLevel < 25) {
                script.log("Cook", "Not high enough cooking level to cook salmon. Have: " + currentCookingLevel + " need: 25");
                script.log("Cook", "Dropping raw salmon since we cannot cook them yet.");
                script.getWidgetManager().getInventory().dropItems(rawId);
            }
            if (rawId == ItemID.RAW_TROUT && currentCookingLevel < 15) {
                script.log("Cook", "Not high enough cooking level to cook trout. Have: " + currentCookingLevel + " need: 25");
                script.log("Cook", "Dropping raw trout since we cannot cook them yet.");
                script.getWidgetManager().getInventory().dropItems(rawId);
            }

            RSObject cookObject = getClosestCookObject(fishingMethod.getCookingObjectName(), fishingMethod.getCookingObjectAction());
            if (cookObject == null) {
                script.log("Cook", "No cookable object found nearby (" + fishingMethod.getCookingObjectName() + ").");
                return false;
            }

            task = "Interact with object";
            if (!cookObject.interact(fishingMethod.getCookingObjectAction())) {
                script.log("Cook", "Failed to interact with cooking object. Retrying...");
                if (!cookObject.interact(fishingMethod.getCookingObjectAction())) {
                    return false;
                }
            }

            task = "Start cooking action";
            BooleanSupplier condition = () -> script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
            script.pollFramesHuman(condition, RandomUtils.uniformRandom(4000, 6000));

            if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION) {
                int cookedId = fishingMethod.getCookedFish().get(rawFish.indexOf(rawId));

                boolean selected = script.getWidgetManager().getDialogue().selectItem(rawId)
                        || script.getWidgetManager().getDialogue().selectItem(cookedId);

                if (!selected) {
                    script.log("Cook", "Initial food selection failed, retrying...");
                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));

                    selected = script.getWidgetManager().getDialogue().selectItem(rawId)
                            || script.getWidgetManager().getDialogue().selectItem(cookedId);
                }

                if (!selected) {
                    script.log("Cook", "Failed to select food item in dialogue after retry.");
                    continue;
                }

                script.log("Cook", "Selected food to cook: " + rawId + "/" + cookedId);
                waitUntilFinishedCooking(Set.of(rawId), cookedId);

                return false; // let next execution handle next type
            }
        }

        return false;
    }

    private RSObject getClosestCookObject(String name, String requiredAction) {
        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(name)
                    && Arrays.stream(gameObject.getActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(requiredAction))
                    && gameObject.canReach();
        });

        if (objects.isEmpty()) {
            script.log("Cook", "No objects found matching query for: " + name + " with action: " + requiredAction);
            return null;
        }

        RSObject closest = (RSObject) script.getUtils().getClosest(objects);
        if (closest == null) {
            script.log("Cook", "Closest object is null.");
        }
        return closest;
    }

    private void waitUntilFinishedCooking(Set<Integer> itemIdsToWatch, int cookedId) {
        task = "Wait until cooking finish";
        Timer amountChangeTimer = new Timer();

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute. Resetting as we just performed an action.");
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
        }

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 66000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(itemIdsToWatch);
            if (inventorySnapshot == null) return false;

            return itemIdsToWatch.stream().noneMatch(inventorySnapshot::contains);
        };

        script.log("Cook", "Using human task to wait until cooking finishes.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(66000, 70000));
    }

    private void waitUntilFinishedCutting(int itemIdToWatch) {
        task = "Wait until cooking finish";

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute. Resetting as we just performed an action.");
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
        }

        Timer amountChangeTimer = new Timer();

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 66000) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(itemIdToWatch));
            if (inventorySnapshot == null) return false;

            return !inventorySnapshot.contains(itemIdToWatch);
        };

        script.log("Cook", "Using human task to wait until cooking finishes.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(66000, 70000));
    }

    private boolean moveToArea(Area destinationArea) {

        WorldPosition currentPos = script.getWorldPosition();
        if (currentPos == null) {
            script.log("Cook", "Player position is null, cannot move to area.");
            return false;
        }

        // Already inside the area
        if (destinationArea.contains(currentPos)) {
            waitTillStopped();
            return true;
        }

        WalkConfig cfg = new WalkConfig.Builder()
                .breakCondition(() -> {
                    WorldPosition pos = script.getWorldPosition();
                    return pos != null && destinationArea.contains(pos);
                })
                .disableWalkScreen(true)
                .enableRun(true)
                .build();

        boolean walking = script.getWalker().walkTo(
                destinationArea.getRandomPosition(),
                cfg
        );

        if (!walking) {
            script.log("Cook", "Failed to initiate walk to destination area.");
            return false;
        }

        // Ensure we fully stop after entering the area
        waitTillStopped();

        return true;
    }

    private void waitTillStopped() {
        task = "Wait till stopped";
        script.log("Cook", "Waiting until player stops moving...");

        AtomicReference<WorldPosition> lastPos =
                new AtomicReference<>(script.getWorldPosition());

        long[] stillStart = { System.currentTimeMillis() };
        long[] animClearSince = { -1 };

        int delay = RandomUtils.uniformRandom(750, 1050);

        java.util.function.BooleanSupplier stopCondition = () -> {

            WorldPosition now = script.getWorldPosition();
            WorldPosition prev = lastPos.get();

            if (now == null || prev == null) {
                stillStart[0] = System.currentTimeMillis();
                animClearSince[0] = -1;
                lastPos.set(now);
                return false;
            }

            boolean sameTile =
                    now.getX() == prev.getX() &&
                            now.getY() == prev.getY() &&
                            now.getPlane() == prev.getPlane();

            if (!sameTile) {
                stillStart[0] = System.currentTimeMillis();
                animClearSince[0] = -1;
                lastPos.set(now);
                return false;
            }

            long nowMs = System.currentTimeMillis();

            return nowMs - stillStart[0] >= delay;
        };

        script.pollFramesUntil(
                stopCondition,
                RandomUtils.uniformRandom(4000, 7500));
    }

    private RSObject getSpecificObjectAt(String name, int worldX, int worldY, int plane) {
        task = "Get RSObject";
        return script.getObjectManager().getRSObject(obj ->
                obj != null
                        && obj.getName() != null
                        && name.equalsIgnoreCase(obj.getName())
                        && obj.getWorldX() == worldX
                        && obj.getWorldY() == worldY
                        && obj.getPlane() == plane
        );
    }
}
