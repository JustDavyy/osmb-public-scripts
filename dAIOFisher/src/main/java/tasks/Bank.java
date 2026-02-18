package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import data.FishingLocation;
import data.FishingMethod;
import utils.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.dAIOFisher.*;

public class Bank extends Task {
    private final Set<Integer> ignoreItems = new HashSet<>();

    public Bank(Script script) {
        super(script);

        ignoreItems.addAll(
                expandWithEquivalents(fishingMethod.getRequiredTools())
        );

        Collections.addAll(ignoreItems,
                ItemID.SPIRIT_FLAKES,
                ItemID.FISH_BARREL,
                ItemID.OPEN_FISH_BARREL
        );
    }

    public boolean activate() {
        Set<Integer> allFish = new HashSet<>(fishingMethod.getAllFish());
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(allFish);
        if (inventorySnapshot == null) {
            script.log("Bank", "Inventory not visible.");
            return false;
        }

        WorldPosition myPos = script.getWorldPosition();

        return bankMode && inventorySnapshot.isFull() || bankMode && script.getWidgetManager().getDepositBox().isVisible() || myPos != null && isAtBank(myPos) && inventorySnapshot.containsAny(allFish);
    }

    public boolean execute() {
        task = "Bank";

        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        // Check if we're inside a cooking building
        if (cookMode && fishingMethod.getCookingObjectArea() != null && fishingMethod.getCookingObjectArea().contains(myPos)) {
            script.log("Bank", "We're still inside the cooking area, moving to bank!");

            boolean success = moveToArea(fishingLocation.getBankArea());

            if (!success && fishingMethod.getCookingHasDoor()) {
                script.log("Bank", "Handling door logic first!");
                RSObject door = getSpecificObjectAt("Door", fishingMethod.getCookingDoorPosition().getX(), fishingMethod.getCookingDoorPosition().getY(), fishingMethod.getCookingDoorPosition().getPlane());

                if (door == null) {
                    script.log("Bank", "Door is null, we need to move closer!");
                    return script.getWalker().walkTo(fishingMethod.getCookingDoorPosition());
                }

                return door.interact("Open");
            }

            return success;
        }

        // Move to bank first if we're not there yet (or can interact with it from where we are)
        if (!isAtBank(myPos)) {
            return walkToBankOrDeposit(fishingLocation);
        }

        Set<Integer> allFishAndBarrels = new HashSet<>(fishingMethod.getAllFish());
        allFishAndBarrels.add(ItemID.FISH_BARREL);
        allFishAndBarrels.add(ItemID.OPEN_FISH_BARREL);

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(allFishAndBarrels);
        if (!alreadyCountedFish) {
            if (inv == null) return false;

            for (int i = 0; i < fishingMethod.getAllFish().size(); i++) {
                int id = fishingMethod.getAllFish().get(i);
                int count = inv.getAmount(id);

                switch (i) {
                    case 0 -> fish1Caught += count;
                    case 1 -> fish2Caught += count;
                    case 2 -> fish3Caught += count;
                    case 3 -> fish4Caught += count;
                    case 4 -> fish5Caught += count;
                    case 5 -> fish6Caught += count;
                    case 6 -> fish7Caught += count;
                    case 7 -> fish8Caught += count;
                }
            }

            alreadyCountedFish = true;
        }

        // Open the correct bank type
        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            if (!script.getWidgetManager().getBank().isVisible()) {
                openBank();
                return false;
            } else {
                script.log("Bank", "Bank interface is visible.");
            }
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            if (!script.getWidgetManager().getDepositBox().isVisible()) {
                openDepositBox();
                return false;
            } else {
                script.log("Bank", "Deposit box interface is visible.");
            }
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.NPC)) {
            return false;
        }

        // Deposit items
        task = "Deposit items";
        if (usingBarrel && inv.containsAny(Set.copyOf(fishingMethod.getAllFish()))) {
            task = "Empty fish barrel";
            script.log("Bank", "Emptying fish barrel in the bank");
            if (!inv.getItem(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL).interact("Empty")) {
                return false;
            }

            fish1Caught += 28;
        }

        boolean usesTexturedItem = fishingMethod.getRequiredTools().contains(ItemID.SMALL_FISHING_NET)
                || fishingMethod.getRequiredTools().contains(ItemID.BIG_FISHING_NET);

        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            if (usesTexturedItem) {
                script.log("Bank", "Using textured item (small/big net). Excluding slot 0 from bank deposit.");
                if (!script.getWidgetManager().getBank().depositAll(Set.copyOf(ignoreItems), Set.of(0))) {
                    script.log("Bank", "Deposit items failed (slot 0 excluded).");
                    return false;
                } else {
                    script.log("Bank", "Deposit items successful (slot 0 excluded).");
                }
            } else {
                if (!script.getWidgetManager().getBank().depositAll(Set.copyOf(ignoreItems))) {
                    script.log("Bank", "Deposit items failed.");
                    return false;
                } else {
                    script.log("Bank", "Deposit items successful.");
                }
            }
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            if (usesTexturedItem) {
                script.log("Bank", "Using textured item (small/big net). Excluding slot 0 from deposit box deposit.");
                if (!script.getWidgetManager().getDepositBox().depositAll(Set.copyOf(ignoreItems), Set.of(0))) {
                    script.log("Bank", "Deposit items failed (slot 0 excluded).");
                    return false;
                } else {
                    script.log("Bank", "Deposit items successful (slot 0 excluded).");
                }
            } else {
                if (!script.getWidgetManager().getDepositBox().depositAll(Set.copyOf(ignoreItems))) {
                    script.log("Bank", "Deposit items failed.");
                    return false;
                } else {
                    script.log("Bank", "Deposit items successful.");
                }
            }
        }

        task = "Close bank";
        if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK)) {
            script.getWidgetManager().getBank().close();
            script.pollFramesHuman(() -> !script.getWidgetManager().getBank().isVisible(), RandomUtils.uniformRandom(4000, 6000));
        } else if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {
            script.getWidgetManager().getDepositBox().close();
            script.pollFramesHuman(() -> !script.getWidgetManager().getDepositBox().isVisible(), RandomUtils.uniformRandom(4000, 6000));
        }

        return false;
    }

    private void openBank() {
        task = "Open bank";
        script.log("Bank", "Searching for a bank...");

        task = "Get bank name/action";
        String bankName = fishingMethod.getBankObjectName();
        String bankAction = fishingMethod.getBankObjectAction();

        if (bankName == null || bankAction == null) {
            script.log("Bank", "Bank name or action is not defined in fishingMethod.");
            return;
        }

        task = "Get bank objects";
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(bankName)
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && action.equalsIgnoreCase(bankAction))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log("Bank", "No bank objects found matching name: " + bankName + " and action: " + bankAction);
            return;
        }

        task = "Interact with bank object";
        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(bankAction)) {
            script.log("Bank", "Failed to interact with bank object.");
            return;
        }

        // Wait for banking UI to appear or player to stop moving
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for bank to open";
        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, RandomUtils.uniformRandom(14000, 16000));
    }

    private void openDepositBox() {
        task = "Open Deposit box";
        script.log("Bank", "Searching for a deposit box...");

        task = "Get bank name/action";
        String bankName = fishingMethod.getBankObjectName();
        String bankAction = fishingMethod.getBankObjectAction();

        if (bankName == null || bankAction == null) {
            script.log("Bank", "Bank name or action is not defined in fishingMethod.");
            return;
        }

        task = "Get deposit box objects";
        List<RSObject> banksFound = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(bankName)
                    && Arrays.stream(gameObject.getActions()).anyMatch(action ->
                    action != null && action.equalsIgnoreCase(bankAction))
                    && gameObject.canReach();
        });

        if (banksFound.isEmpty()) {
            script.log("Bank", "No deposit box objects found matching name: " + bankName + " and action: " + bankAction);
            return;
        }

        task = "Interact with deposit box object";
        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(bankAction)) {
            script.log("Bank", "Failed to interact with deposit box object.");
            return;
        }

        // Wait for deposit box UI to appear or player to stop moving
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for deposit box to open";
        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getDepositBox().isVisible() || positionChangeTimer.get().timeElapsed() > 3000;
        }, RandomUtils.uniformRandom(14000, 16000));
    }

    private RSObject getClosestBankOrDeposit() {
        String bankName = fishingMethod.getBankObjectName();
        String bankAction = fishingMethod.getBankObjectAction();

        if (bankName == null || bankAction == null) {
            return null;
        }

        List<RSObject> objects = script.getObjectManager().getObjects(gameObject -> {
            if (gameObject.getName() == null || gameObject.getActions() == null) return false;
            return gameObject.getName().equalsIgnoreCase(bankName)
                    && Arrays.stream(gameObject.getActions())
                    .anyMatch(action -> action != null && action.equalsIgnoreCase(bankAction))
                    && gameObject.canReach();
        });

        return objects.isEmpty() ? null : (RSObject) script.getUtils().getClosest(objects);
    }

    private boolean walkToBankOrDeposit(FishingLocation fishingLocation) {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (!fishingLocation.getBankArea().contains(myPos)) {
            task = "Moving to bank area";

            if (fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.BANK) ||
                    fishingMethod.getBankObjectType().equals(FishingMethod.BankObjectType.DEPOSIT_BOX)) {

                WalkConfig cfg = new WalkConfig.Builder()
                        .enableRun(true)
                        .breakCondition(() -> {
                            RSObject bank = getClosestBankOrDeposit();
                            return bank != null && bank.isInteractableOnScreen();
                        })
                        .build();

                return script.getWalker().walkTo(fishingLocation.getBankArea().getRandomPosition(), cfg);
            } else {
                return script.getWalker().walkTo(fishingLocation.getBankArea().getRandomPosition());
            }
        }

        return true;
    }

    private boolean isAtBank(WorldPosition myPos) {
        // True if inside the defined bank area
        if (fishingLocation.getBankArea().contains(myPos)) {
            return true;
        }

        // Or if bank/deposit is already visible & interactable on screen
        RSObject bank = getClosestBankOrDeposit();
        return bank != null && bank.isInteractableOnScreen();
    }

    public static Set<Integer> expandWithEquivalents(Collection<Integer> baseIds) {
        Set<Integer> expanded = new HashSet<>();

        for (int id : baseIds) {
            expanded.add(id);

            switch (id) {
                case ItemID.HARPOON ->
                        expanded.addAll(TOOL_EQUIVALENTS.get("harpoon"));

                case ItemID.FISHING_ROD ->
                        expanded.addAll(TOOL_EQUIVALENTS.get("fishingrod"));

                case ItemID.FLY_FISHING_ROD ->
                        expanded.addAll(TOOL_EQUIVALENTS.get("flyfishingrod"));

                case ItemID.OILY_FISHING_ROD ->
                        expanded.addAll(TOOL_EQUIVALENTS.get("oilyfishingrod"));

                case ItemID.BARBARIAN_ROD ->
                        expanded.addAll(TOOL_EQUIVALENTS.get("barbarianrod"));

                case ItemID.FEATHER ->
                        expanded.addAll(BAIT_EQUIVALENTS.get("barbbait"));

                case ItemID.SANDWORMS ->
                        expanded.addAll(BAIT_EQUIVALENTS.get("sandworm"));
            }
        }

        return expanded;
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
