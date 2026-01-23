package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static main.dSawmillRunner.*;

public class Sawmiller extends Task {

    // Woodcutting Guild
    private static final WorldPosition wcGuildOperatorTile = new WorldPosition(1623, 3500, 0);
    private static final Area wcGuildBankArea = new RectangleArea(1589, 3475, 4, 5, 0);

    // Varrock
    private static final WorldPosition varrockOperatorTile = new WorldPosition(3302, 3492, 0);
    private static final Area varrockBankArea = new RectangleArea(3250, 3419, 7, 4, 0);

    // Prifddinas North
    private static final WorldPosition prifNOperatorTile = new WorldPosition(3315, 6116, 0);
    private static final Area prifNBankArea = new RectangleArea(3255, 6105, 4, 4, 0);

    // Prifddinas South
    private static final WorldPosition prifSOperatorTile = new WorldPosition(3315, 6116, 0);
    private static final Area prifSBankArea = new RectangleArea(3292, 6056, 8, 8, 0);

    // Auburnvale
    private static final WorldPosition auburnvaleOperatorTile = new WorldPosition(1395, 3369, 0);
    private static final Area auburnvaleBankArea = new RectangleArea(1412, 3348, 8, 9, 0);

    // Banking stuff
    public static final String[] BANK_NAMES = {"Bank booth", "Bank chest"};
    public static final String[] BANK_ACTIONS = {"bank", "use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Other stuff
    private static final String operatorAction = "Buy-plank";
    private boolean plankCountChecked = false;
    private int ringTeleportFailCount = 0;
    private int bankFailCount = 0;
    private int failedOperatorAttempts = 0;
    private static final int MAX_OPERATOR_ATTEMPTS = 5;

    // Additional travel stuff
    private static final Area ringArrivalArea = new RectangleArea(3283, 3461, 16, 15, 0);

    public Sawmiller(Script script) {
        super(script);
    }

    public boolean activate() {
        return setupDone;
    }

    public boolean execute() {

        // 1. Check logs and coins in inventory
        if (hasLogs() && hasCoins() && !atOperator()) {
            // Default walking if not using ring or after teleport
            task = "Walking to operator";
            script.log(getClass(), "Walking to operator...");

            WorldPosition operatorTilePos = getOperatorTile();
            if (operatorTilePos == null) {
                script.log(getClass(), "Operator tile not found for current location. Stopping script.");
                script.stop();
                return false;
            }

            RSTile operatorTile = script.getSceneManager().getTile(operatorTilePos);

            if (operatorTile == null || !operatorTile.isOnGameScreen()) {
                script.log(getClass(), "Walking to operator tile with break condition when visible...");

                WalkConfig config = new WalkConfig.Builder()
                        .breakCondition(() -> {
                            RSTile tileCheck = script.getSceneManager().getTile(operatorTilePos);
                            return tileCheck != null && tileCheck.isOnGameScreen();
                        })
                        .enableRun(true)
                        .build();

                script.getWalker().walkTo(operatorTilePos, config);
            }

            return false;
        }

        // 2. If at operator, interact with operator tile
        if (hasLogs() && atOperator() && !isItemOptionDialogueOpen()) {
            if (useVouchers) {
                task = "Check vouchers";
                ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.SAWMILL_VOUCHER));
                if (inv == null) return false;
                if (!inv.contains(ItemID.SAWMILL_VOUCHER)) {
                    script.log(getClass(), "Out of sawmill vouchers, marking usage to false!");
                    useVouchers = false;
                } else {
                    script.log(getClass(), "Sawmill vouchers left: " + inv.getAmount(ItemID.SAWMILL_VOUCHER));
                }
            }

            task = "Interacting with operator";
            script.log(getClass(), "At operator. Interacting...");
            WorldPosition operatorTilePosition = getOperatorTile();
            RSTile operatorTile = script.getSceneManager().getTile(operatorTilePosition);

            if (operatorTile == null) {
                script.log(getClass(), "Operator tile is null, cannot interact.");
                return false;
            }

            if (!operatorTile.isOnGameScreen()) {
                script.log(getClass(), "Operator tile not on screen, walking...");
                script.getWalker().walkTo(operatorTilePosition);
                return false;
            }

            if (!script.getFinger().tap(operatorTile.getTileCube(120).getResized(0.7), operatorAction)) {
                script.log(getClass(), "Failed to tap '" + operatorAction + "' on operator tile. Attempt " + (failedOperatorAttempts + 1));
                failedOperatorAttempts++;

                if (failedOperatorAttempts >= MAX_OPERATOR_ATTEMPTS) {
                    script.log(getClass(), "Too many failed attempts. Walking to operator again to reset...");
                    failedOperatorAttempts = 0;

                    WalkConfig config = new WalkConfig.Builder()
                            .breakCondition(() -> {
                                RSTile tileCheck = script.getSceneManager().getTile(operatorTilePosition);
                                return tileCheck != null && tileCheck.isOnGameScreen();
                            })
                            .enableRun(true)
                            .build();

                    script.getWalker().walkTo(operatorTilePosition, config);
                }

                return false;
            }

            // Successful interaction, reset failed attempt counter
            failedOperatorAttempts = 0;

            // Wait until ITEM_OPTION dialogue is shown
            script.log(getClass(), "Waiting for item option dialogue after tapping operator...");
            script.pollFramesHuman(() -> {
                if (script.getWidgetManager().getDialogue() == null) return false;
                DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
                return type != null && type.equals(DialogueType.ITEM_OPTION);
            }, RandomUtils.uniformRandom(3000, 5000));

            return false;
        }

        // 3. Search for log ID in dialogue
        if (hasLogs()) {
            task = "Selecting logs in dialogue";
            DialogueType dialogueType = null;
            if (script.getWidgetManager().getDialogue() != null) {
                dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            }

            if (dialogueType != null && dialogueType.equals(DialogueType.ITEM_OPTION)) {
                task = "Selecting logs in dialogue";
                script.log(getClass(), "Dialogue open. Selecting log option...");

                boolean selected = script.getWidgetManager().getDialogue().selectItem(neededLogs);
                if (!selected) {
                    script.log(getClass(), "Initial log selection failed, retrying...");
                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                    selected = script.getWidgetManager().getDialogue().selectItem(neededLogs);
                }

                if (!selected) {
                    script.log(getClass(), "Failed to select log in dialogue after retry.");
                    return false;
                }

                script.log(getClass(), "Selected logs for plank conversion.");

                // Wait until planks produced
                waitUntilPlanksProduced();

                return false;
            }
        }

        // 5. If enough coins and no logs, go to bank
        if (!hasLogs() && hasCoins() && !atBank()) {
            task = "Walking to bank";
            script.log(getClass(), "No logs left, going to bank...");

            List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
            if (banksFound.isEmpty()) {
                script.log(getClass(), "No bank objects found nearby, walking to bank area instead...");
                script.getWalker().walkTo(Objects.requireNonNull(getBankArea()).getRandomPosition());
                return false;
            }

            RSObject closestBank = (RSObject) script.getUtils().getClosest(banksFound);

            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(closestBank::isInteractableOnScreen)
                    .disableWalkScreen(true)  // Disable screen walking
                    .enableRun(true)          // Enable run energy
                    .build();

            boolean walked = script.getWalker().walkTo(closestBank, config);

            if (!walked) {
                script.log(getClass(), "Walking to bank failed, attempting fallback walk to bank area...");
                script.getWalker().walkTo(Objects.requireNonNull(getBankArea()).getRandomPosition());
            }

            return false;
        }

        // 6. At the bank, deposit planks and withdraw logs
        if (atBank()) {
            task = "Banking: depositing and withdrawing logs";
            script.log(getClass(), "At bank. Starting deposit and withdraw sequence...");

            if (!script.getWidgetManager().getBank().isVisible()) {
                task = "Opening bank";
                openBank();
                return false;
            }

            // === DEPOSITING ===
            task = "Deposit planks";
            boolean depositSuccess = script.getWidgetManager().getBank().depositAll(Set.of(
                    neededLogs,
                    ItemID.COINS_995,
                    ItemID.LOG_BASKET,
                    ItemID.OPEN_LOG_BASKET,
                    ItemID.PLANK_SACK,
                    ItemID.PLANK_SACK_25629,
                    ItemID.SAWMILL_VOUCHER
            ));

            if (!depositSuccess) {
                script.log(getClass(), "Failed to deposit planks (excluding logs and coins). Incrementing bank fail count.");
                bankFailCount++;
                checkBankFailStop();
                return false;
            }

            // === WITHDRAWING ===
            int withdrawAmount = 27;
            if (useVouchers) {
                withdrawAmount = 13;
                script.log(getClass(), "Using sawmill vouchers. Adjusted withdraw amount to: " + withdrawAmount);
            }

            // Regular withdraw process if not using both log basket and plank sack
            task = "Withdraw logs (" + withdrawAmount + ")";
            boolean withdrawSuccess = script.getWidgetManager().getBank().withdraw(neededLogs, withdrawAmount);
            if (!withdrawSuccess) {
                script.log(getClass(), "Failed to withdraw logs. Incrementing bank fail count.");
                bankFailCount++;
                checkBankFailStop();
                return false;
            }

            plankCountChecked = false;

            // === CLOSING BANK ===
            task = "Close bank";
            boolean closeSuccess = script.getWidgetManager().getBank().close();
            if (!closeSuccess) {
                script.log(getClass(), "Bank close failed, retrying...");
                script.getWidgetManager().getBank().close();
            }

            bankFailCount = 0;
            return false;
        }

        task = "Idle - nothing to do";
        return false;
    }

    private boolean hasLogs() {
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(neededLogs));
        return inv != null && inv.contains(neededLogs);
    }

    private boolean hasPlanks() {
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(selectedPlank));
        return inv != null && inv.contains(selectedPlank);
    }

    private boolean hasCoins() {
        int plankId = selectedPlank;
        int requiredCoins;

        switch (plankId) {
            case ItemID.PLANK -> requiredCoins = 100;
            case ItemID.OAK_PLANK -> requiredCoins = 250;
            case ItemID.TEAK_PLANK -> requiredCoins = 500;
            case ItemID.MAHOGANY_PLANK -> requiredCoins = 1500;
            case 31432 -> requiredCoins = 2500;
            case 31435 -> requiredCoins = 5000;
            case 31438 -> requiredCoins = 7500;
            default -> {
                script.log(getClass(), "Unknown plank ID selected: " + plankId + ". Stopping script.");
                script.stop();
                return false;
            }
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.COINS_995));
        if (inv == null || !inv.contains(ItemID.COINS_995)) {
            script.log(getClass(), "No coins found in inventory. Stopping script.");
            script.stop();
            return false;
        }

        long coinsAmount = inv.getAmount(ItemID.COINS_995);
        if (coinsAmount < requiredCoins) {
            script.log(getClass(), "Not enough coins. Required: " + requiredCoins + ", found: " + coinsAmount + ". Stopping script.");
            script.stop();
            return false;
        }

        return true;
    }

    private boolean atOperator() {
        WorldPosition operatorPos = getOperatorTile();
        if (operatorPos == null) return false;

        RSTile operatorTile = script.getSceneManager().getTile(operatorPos);
        if (operatorTile == null) return false;

        return operatorTile.isOnGameScreen();
    }

    private boolean atRingArrival() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        return ringArrivalArea.contains(myPos);
    }

    private boolean atBank() {
        // Check if bank interface is already open
        if (script.getWidgetManager().getBank().isVisible()) {
            return true;
        }

        // First, check if any bank object is visible on screen anywhere
        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        for (RSObject bank : banksFound) {
            boolean visible = bank.isInteractableOnScreen();
            if (visible) {
                return true;
            }
        }

        // Get current position
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) {
            script.log(getClass(), "Unable to get current position for atBank check.");
            return false;
        }

        // Check if within bank area
        Area bankArea = getBankArea();
        if (bankArea != null && bankArea.contains(myPos)) {
            script.log(getClass(), "Within bank area, checking for reachable/interactable bank object...");

            for (RSObject bank : banksFound) {
                boolean reachable = bank.canReach();
                boolean visible = bank.isInteractableOnScreen();
                script.log(getClass(), "Bank object area check: reachable=" + reachable + ", visible=" + visible);
                if (reachable || visible) {
                    return true;
                }
            }
        }

        return false;
    }

    private WorldPosition getOperatorTile() {
        if (location == null) {
            script.log(getClass(), "No location set. Stopping script.");
            script.stop();
            return null;
        }

        if (location.equalsIgnoreCase("VARROCK")) {
            return varrockOperatorTile;
        } else if (location.equalsIgnoreCase("PRIFDDINAS_NORTH")) {
            return prifNOperatorTile;
        } else if (location.equalsIgnoreCase("PRIFDDINAS_SOUTH")) {
            return prifSOperatorTile;
        } else if (location.equalsIgnoreCase("WOODCUTTING_GUILD")) {
            return wcGuildOperatorTile;
        } else if (location.equalsIgnoreCase("AUBURNVALE")) {
            return auburnvaleOperatorTile;
        } else {
            script.log(getClass(), "Unknown location selected: " + location + ". Stopping script.");
            script.stop();
            return null;
        }
    }

    private void waitUntilPlanksProduced() {
        Timer amountChangeTimer = new Timer();

        script.log(getClass(), "Waiting until logs are processed into planks...");

        BooleanSupplier condition = () -> {
            DialogueType type = null;
            var dialogue = script.getWidgetManager().getDialogue();
            if (dialogue != null) {
                type = dialogue.getDialogueType();
            }

            if (type != null && type.equals(DialogueType.TAP_HERE_TO_CONTINUE)) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > 6000) {
                script.log(getClass(), "Timeout reached while waiting for planks.");
                return true;
            }

            return !hasLogs() || hasPlanks();
        };

        script.pollFramesHuman(condition, RandomUtils.uniformRandom(6000, 10000));

        if (!plankCountChecked) {
            // Count how many planks we have in inventory and increment totalPlankCount
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(selectedPlank));
            int amount = 0;

            if (inv != null && inv.contains(selectedPlank)) {
                amount = inv.getAmount(selectedPlank);
            }

            plankCount += amount;
            script.log(getClass(), "Incremented plank count by " + amount + ". Total now: " + plankCount);

            plankCountChecked = true;
        }

        if (RandomUtils.uniformRandom(10) < 3) {
            script.log(getClass(), "Adding extra randomized delay");
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 500));
        }
    }

    private Area getBankArea() {
        if (location == null) {
            script.log(getClass(), "No location set. Stopping script.");
            script.stop();
            return null;
        }

        String loc = location.toUpperCase();

        if (loc.equalsIgnoreCase("VARROCK")) {
            return varrockBankArea;
        } else if (loc.equalsIgnoreCase("PRIFDDINAS_NORTH")) {
            return prifNBankArea;
        } else if (loc.equalsIgnoreCase("PRIFDDINAS_SOUTH")) {
            return prifSBankArea;
        } else if (loc.equalsIgnoreCase("WOODCUTTING_GUILD")) {
            return wcGuildBankArea;
        } else if (location.equalsIgnoreCase("AUBURNVALE")) {
            return auburnvaleBankArea;
        }else {
            script.log(getClass(), "Unknown location selected for bank: " + location + ". Stopping script.");
            script.stop();
            return null;
        }
    }

    private void checkBankFailStop() {
        if (bankFailCount >= 3) {
            script.log(getClass(), "Banking failed 3 times in a row. Stopping script.");
            script.stop();
        } else {
            script.log(getClass(), "Banking failed " + bankFailCount + " time(s). Will retry...");
        }
    }

    private void openBank() {
        script.log(getClass(), "Opening bank...");

        if (script.getWidgetManager().getBank().isVisible()) return;

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

        script.pollFramesHuman(() -> script.getWidgetManager().getBank().isVisible(), RandomUtils.uniformRandom(5000, 8000));
    }

    private boolean isItemOptionDialogueOpen() {
        return script.getWidgetManager().getDialogue() != null &&
                script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
    }
}