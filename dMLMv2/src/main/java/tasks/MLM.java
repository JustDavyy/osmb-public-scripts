package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.triangle.Triangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import data.MineArea;
import data.MLMAreaProvider;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import overlays.MLMSackOverlay;
import utils.Task;

import static main.dMLMv2.*;

public class MLM extends Task {
    public static final int RESPAWN_CIRCLE_HEIGHT = 160;
    public static final int BLACKLIST_TIMEOUT = 15000;
    public static final Set<Integer> ITEM_IDS_TO_NOT_DEPOSIT = new HashSet<>(Set.of(
            ItemID.PAYDIRT, ItemID.BRONZE_PICKAXE, ItemID.IRON_PICKAXE,
            ItemID.STEEL_PICKAXE, ItemID.BLACK_PICKAXE, ItemID.MITHRIL_PICKAXE,
            ItemID.ADAMANT_PICKAXE, ItemID.RUNE_PICKAXE, ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_12797, ItemID.DRAGON_PICKAXE_OR_25376, ItemID.DRAGON_PICKAXE_OR_30351,
            ItemID.DRAGON_PICKAXE_OR, ItemID.CRYSTAL_PICKAXE, ItemID.CRYSTAL_PICKAXE_INACTIVE, ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR, ItemID.ANTIQUE_LAMP
    ));
    public static final Predicate<RSObject> LADDER_QUERY = (rsObject) -> {
        String name = rsObject.getName();
        if (name == null) {
            return false;
        }
        if (!name.equalsIgnoreCase("ladder")) {
            return false;
        }
        return rsObject.canReach();
    };
    private static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);
    private static final int[] ORES = new int[]{ItemID.GOLDEN_NUGGET,ItemID.COAL, ItemID.GOLD_ORE, ItemID.MITHRIL_ORE, ItemID.ADAMANTITE_ORE, ItemID.RUNITE_ORE};
    private static final SearchablePixel FLOWING_WATER_PIXEL = new SearchablePixel(-6707525, new SingleThresholdComparator(3), ColorModel.HSL);
    private static final WorldPosition CRATE_POSITION = new WorldPosition(3752, 5674, 0);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.PAYDIRT, ItemID.HAMMER, ItemID.GOLDEN_NUGGET,ItemID.COAL, ItemID.GOLD_ORE, ItemID.MITHRIL_ORE, ItemID.ADAMANTITE_ORE, ItemID.RUNITE_ORE));
    private static final MenuHook SACK_MENU_HOOK = menuEntries -> {
        for (MenuEntry entry : menuEntries) {
            if (entry.getRawText().equalsIgnoreCase("search sack")) {
                return entry;
            }
        }
        return null;
    };
    /**
     * This is used as a failsafe to temporarily block interacting with a vein if the respawn circle isn't visible but the object is.
     * For example. The object is half on the game screen, but the respawn circle isn't (covered by a UI component etc.)
     */
    private final Map<WorldPosition, Long> objectPositionBlacklist = new HashMap<>();
    private ItemGroupResult inventorySnapshot;
    private boolean fixWaterWheelFlag = false;
    private boolean forceCollectFlag = false;
    private boolean firstTimeBack = false;
    private int amountChangeTimeout;
    private int animationTimeout;
    private MLMSackOverlay sackOverlay;
    private Task task;
    private Integer spaceLeft;
    private Integer deposited;
    private Stopwatch dropDelayTimer;
    private int failCount = 0;
    private boolean updatedCounts = false;
    private final Set<RSObject> skippedByPlayers = new HashSet<>();

    public MLM(Script script) {
        super(script);
        sackOverlay = new MLMSackOverlay(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {

        skippedByPlayers.clear();

        task = decideTask();
        if (task == null) {
            return false;
        }
        if ((task == Task.COLLECT || task == Task.HANDLE_BANK) && !firstTimeBack) {
            firstTimeBack = true;
        }
        script.log("MLM", "Executing task: " + task);
        executeTask(task);
        return false;
    }

    private Task decideTask() {
        paintTask = "Decide task";
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return Task.HANDLE_BANK;
        }
        spaceLeft = (Integer) sackOverlay.getValue(MLMSackOverlay.SPACE_LEFT);
        deposited = (Integer) sackOverlay.getValue(MLMSackOverlay.DEPOSITED);
        if (spaceLeft == null || deposited == null) {
            script.log("MLM", "Problem reading sack overlay... (space left: " + spaceLeft + ") (deposited: " + deposited + ")");
            return null;
        }

        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible
            script.log("MLM", "Inventory not visible...");
            return null;
        }
        if (outsideAccessibleAreaCheck()) {
            return null;
        }
        if (fixWaterWheelFlag) {
            return Task.REPAIR_WHEEL;
        } else if (inventorySnapshot.contains(ItemID.HAMMER)) {
            if (dropDelayTimer != null && dropDelayTimer.hasFinished()) {
                return Task.DROP_HAMMER;
            } else {
                if (dropDelayTimer == null) {
                    dropDelayTimer = new Stopwatch(RandomUtils.uniformRandom(0, 15000));
                }
            }
        }

        // === Mixed inventory ===
        if (inventorySnapshot.contains(ItemID.PAYDIRT)
                && inventorySnapshot.containsAny(ORES)) {

            script.log("MLM", "Inventory contains both pay-dirt and ores. Forcing bank.");
            return Task.OPEN_BANK;
        }

        if (deposited == 0 && !inventorySnapshot.contains(ItemID.PAYDIRT)) {
            forceCollectFlag = false;
        } else if (shouldCollect(spaceLeft) || forceCollectFlag) {
            forceCollectFlag = true;
            return Task.COLLECT;
        }

        int oresToMine = spaceLeft - inventorySnapshot.getAmount(ItemID.PAYDIRT);

        if (inventorySnapshot.isFull() || oresToMine <= 0) {
            script.log("MLM", "Inventory full? " + inventorySnapshot.isFull() + " Ores to mine: " + oresToMine);
            // If we have too much payDirt drop it
            if (oresToMine < 0) {
                // too many ores drop some
                return Task.DROP_PAYDIRT;
            }
            // If NO free slots AND we have paydirt in our inv deposit to mine cart
            else if (inventorySnapshot.contains(ItemID.PAYDIRT)) {
                WorldPosition currentPos = script.getWorldPosition();
                if (useUpperHopper && currentPos != null && !MLMAreaProvider.TOP_FLOOR_AREA.contains(currentPos)) {
                    return Task.WALK_TO_VEIN_AREA;
                }
                return Task.DEPOSIT_PAY_DIRT;
            } else {
                // if no spaces & no paydirt, open the bank to deposit and make room...
                return Task.OPEN_BANK;
            }
        } else {
            if (inventorySnapshot.containsAny(ORES)) {
                script.log("MLM", "Inventory contains ores, banking them...");
                return Task.OPEN_BANK;
            }
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return null;
            }
            if (needsToWalkToMineArea(myPosition)) {
                return Task.WALK_TO_VEIN_AREA;
            }
            return Task.MINE_VEIN;
        }
    }

    private void executeTask(Task task) {
        switch (task) {
            case MINE_VEIN -> mineVein();
            case DEPOSIT_PAY_DIRT -> depositPayDirt();
            case DROP_PAYDIRT -> dropPayDirt();
            case COLLECT -> collectPayDirt();
            case WALK_TO_VEIN_AREA -> walkToVeinArea();
            case HANDLE_BANK -> handleBank();
            case OPEN_BANK -> openBank();
            case REPAIR_WHEEL -> repairWheel();
            case DROP_HAMMER -> dropHammer();
        }
    }

    private void dropHammer() {
        paintTask = "Drop hammer";
        if (inventorySnapshot.contains(ItemID.HAMMER)) {
            if (inventorySnapshot.getItem(ItemID.HAMMER).interact("Drop")) {
                dropDelayTimer = null;
            }
        }
    }

    private void repairWheel() {
        paintTask = "Repair wheel";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return;
        }
        if (MLMAreaProvider.TOP_FLOOR_AREA.contains(myPosition)) {
            // climb down ladder if we are on the top floor
            climbDownLadder();
            return;
        }

        // scan the water to see if its flowing
        scanWater();

        if (!fixWaterWheelFlag) {
            return;
        }
        if (!inventorySnapshot.contains(ItemID.HAMMER)) {
            // grab a hammer
            takeHammer();
            return;
        }
        // find water wheel objects
        RSObject brokenStrut = script.getObjectManager().getClosestObject(myPosition, "broken strut");

        if (brokenStrut == null) {
            script.log("MLM", "Can't find Strut in scene...");
            return;
        }
        script.log("MLM", "Interact with water wheel...");
        boolean interactResult = brokenStrut.interact(null, menuEntries -> {
            script.log("MLM", menuEntries.toString());
            for (MenuEntry entry : menuEntries) {
                String entryText = entry.getRawText().toLowerCase();
                if (entryText.startsWith("hammer broken strut")) {
                    return entry;
                } else if (entryText.startsWith("examine strut")) {
                    fixWaterWheelFlag = false;
                }
            }
            return null;
        });

        if (interactResult) {
            script.pollFramesHuman(() -> {
                // scan the water to check if repaired
                scanWater();
                // flag will switch to false inside scanWater method if water is flowing
                return !fixWaterWheelFlag;
            }, RandomUtils.uniformRandom(6000, 11000));
        }

    }

    private void takeHammer() {
        paintTask = "Take hammer";
        if (inventorySnapshot.isFull()) {
            // if no free slots, drop a paydirt
            dropPayDirt();
            return;
        }
        RSObject crate = script.getObjectManager().getRSObject(object -> object.getWorldPosition().equals(CRATE_POSITION));
        if (crate == null) {
            // walk to crate
            script.getWalker().walkTo(CRATE_POSITION, new WalkConfig.Builder().breakDistance(2).tileRandomisationRadius(2).build());
            return;
        }
        if (!crate.interact("Search")) {
            return;
        }
        // wait for hammer to appear in the inventory
        script.pollFramesHuman(() -> {
            inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            return inventorySnapshot.contains(ItemID.HAMMER);
        }, RandomUtils.uniformRandom(5000, 8000));
    }

    private void openBank() {
        paintTask = "Open deposit box";
        updatedCounts = false;
        // === If we have ores, ALWAYS bank. Never touch hopper ===
        if (inventorySnapshot.containsAny(ORES)) {
            script.log("MLM", "Ores present. Skipping hopper logic and opening bank.");
        } else if (inventorySnapshot.contains(ItemID.PAYDIRT)) {
            if (spaceLeft >= inventorySnapshot.getAmount(ItemID.PAYDIRT)) {
                depositPayDirt();
            } else {
                dropPayDirt();
            }
            return;
        }
        script.log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        Predicate<RSObject> bankQuery = gameObject -> {
            if (gameObject.getName() == null || !gameObject.getName().equalsIgnoreCase("bank deposit box")) {
                return false;
            }
            return gameObject.canReach();
        };
        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) script.getUtils().getClosest(banksFound);
        if (!object.interact("deposit")) {
            // if we fail to interact with the bank
            return;
        }

        long positionChangeTimeout = RandomUtils.uniformRandom(800, 2500);
        script.pollFramesHuman(() -> {
            WorldPosition position = script.getWorldPosition();
            if (position == null) {
                return false;
            }
            if (object.getTileDistance(position) > 1 && script.getLastPositionChangeMillis() >= positionChangeTimeout) {
                return true;
            }

            return script.getWidgetManager().getDepositBox().isVisible();
        }, 15000);
    }

    private void handleBank() {
        paintTask = "Deposit gained items";
        ItemGroupResult depositBoxSnapshot = script.getWidgetManager().getDepositBox().search(ITEM_IDS_TO_NOT_DEPOSIT);
        if (depositBoxSnapshot == null) {
            return;
        }

        if (depositBoxSnapshot.containsAny(ITEM_IDS_TO_NOT_DEPOSIT)) {
            if (!script.getWidgetManager().getDepositBox().depositAll(ITEM_IDS_TO_NOT_DEPOSIT)) {
                script.log("MLM", "Failed depositing items...");
                return;
            }
        } else {
            // deposit all button
            if (!script.getWidgetManager().getDepositBox().depositAll(Collections.emptySet())) {
                script.log("MLM", "Failed depositing all");
                return;
            }
        }

        script.log("MLM", "Closing deposit box...");
        script.getWidgetManager().getDepositBox().close();
    }

    private void walkToVeinArea() {
        paintTask = "Walk to vein area";
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(3);
        builder.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return selectedMineArea.getArea().contains(myPosition);
        });
        if (selectedMineArea == MineArea.TOP) {
            if (climbUpLadder()) {
                return;
            }
        }
        script.getWalker().walkTo(selectedMineArea.getArea().getRandomPosition(), builder.build());
    }

    private boolean climbUpLadder() {
        paintTask = "Climb up ladder";
        RSObject ladder = script.getObjectManager().getRSObject(LADDER_QUERY);
        if (ladder == null) {
            // walk to ladder
            script.getWalker().walkTo(MLMAreaProvider.LADDER_AREA.getRandomPosition());
            return true;
        } else {
            if (ladder.interact("Climb")) {
                script.pollFramesHuman(() -> {
                    WorldPosition worldPosition = script.getWorldPosition();
                    if (worldPosition == null) {
                        return false;
                    }
                    return MLMAreaProvider.TOP_FLOOR_AREA.contains(worldPosition);
                }, RandomUtils.uniformRandom(7000, 12000));
                return true;
            }
        }
        return false;
    }

    private void collectPayDirt() {
        paintTask = "Collect pay dirt";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log("MLM", "Position is null...");
            return;
        }
        if (outsideAccessibleAreaCheck()) {
            return;
        }
        if (MLMAreaProvider.TOP_FLOOR_AREA.contains(myPosition)) {
            climbDownLadder();
            return;
        }
        if (inventorySnapshot.isFull() || deposited == 0) {
            openBank();
            return;
        }
        RSObject sack = getSack();
        if (sack == null) {
            script.log("MLM", "Can't find object Sack inside our loaded scene.");
            return;
        }

        if (sack.interact(null, SACK_MENU_HOOK)) {
            int initialSlotsFree = inventorySnapshot.getFreeSlots();
            script.pollFramesHuman(() -> {
                inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    // not visible
                    return false;
                }

                if (inventorySnapshot.getFreeSlots() < initialSlotsFree && !updatedCounts) {

                    if (inventorySnapshot.contains(ItemID.GOLDEN_NUGGET)) {
                        int amount = inventorySnapshot.getAmount(ItemID.GOLDEN_NUGGET);
                        nuggetsGained += amount;
                        script.log("MLM",
                                "Gold nugget count increased by " + amount +
                                        " to a total of " + nuggetsGained);
                    }

                    if (inventorySnapshot.contains(ItemID.COAL)) {
                        int amount = inventorySnapshot.getAmount(ItemID.COAL);
                        coalGained += amount;
                        script.log("MLM",
                                "Coal count increased by " + amount +
                                        " to a total of " + coalGained);
                    }

                    if (inventorySnapshot.contains(ItemID.GOLD_ORE)) {
                        int amount = inventorySnapshot.getAmount(ItemID.GOLD_ORE);
                        goldGained += amount;
                        script.log("MLM",
                                "Gold ore count increased by " + amount +
                                        " to a total of " + goldGained);
                    }

                    if (inventorySnapshot.contains(ItemID.MITHRIL_ORE)) {
                        int amount = inventorySnapshot.getAmount(ItemID.MITHRIL_ORE);
                        mithrilGained += amount;
                        script.log("MLM",
                                "Mithril ore count increased by " + amount +
                                        " to a total of " + mithrilGained);
                    }

                    if (inventorySnapshot.contains(ItemID.ADAMANTITE_ORE)) {
                        int amount = inventorySnapshot.getAmount(ItemID.ADAMANTITE_ORE);
                        adamantGained += amount;
                        script.log("MLM",
                                "Adamantite ore count increased by " + amount +
                                        " to a total of " + adamantGained);
                    }

                    if (inventorySnapshot.contains(ItemID.RUNITE_ORE)) {
                        int amount = inventorySnapshot.getAmount(ItemID.RUNITE_ORE);
                        runeGained += amount;
                        script.log("MLM",
                                "Runite ore count increased by " + amount +
                                        " to a total of " + runeGained);
                    }

                    updateGPGained();
                    updatedCounts = true;
                }

                return inventorySnapshot.getFreeSlots() < initialSlotsFree;
            }, RandomUtils.uniformRandom(15000, 25000));
        }
    }

    private boolean climbDownLadder() {
        paintTask = "Climb down ladder";
        RSObject ladder = script.getObjectManager().getRSObject(LADDER_QUERY);
        if (ladder == null) {
            script.log("MLM", "Can't find ladder in scene...");
            return true;
        }
        if (ladder.interact("Climb")) {
            script.pollFramesHuman(() -> {
                WorldPosition worldPosition = script.getWorldPosition();
                if (worldPosition == null) {
                    return false;
                }
                return !MLMAreaProvider.TOP_FLOOR_AREA.contains(worldPosition);
            }, RandomUtils.uniformRandom(6000, 12000));
            return true;
        }

        return false;
    }

    private void dropPayDirt() {
        paintTask = "Drop pay dirt";
        if (!inventorySnapshot.contains(ItemID.PAYDIRT)) {
            return;
        }
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return;
        }
        ItemSearchResult payDirt = inventorySnapshot.getRandomItem(ItemID.PAYDIRT);
        if (payDirt == null) {
            return;
        }
        payDirt.interact("Drop");
    }

    private void depositPayDirt() {
        paintTask = "Deposit pay dirt";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log("MLM", "Position is null...");
            return;
        }
        if (MLMAreaProvider.TOP_FLOOR_AREA.contains(myPosition)
                && !MLMAreaProvider.TOP_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {

            script.log("MLM", "Detected inaccessible top-floor area, attempting recovery.");
            recoverFromInaccessibleTopArea();
            return;
        }
        if (!MLMAreaProvider.TOP_FLOOR_AREA.contains(myPosition)
            && !MLMAreaProvider.TOP_FLOOR_ACCESSIBLE_AREA.contains(myPosition)
            && !MLMAreaProvider.BOTTOM_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {
            script.log("MLM", "Detected inaccessible bottom-floor area, attempting recovery.");
            recoverFromInaccessibleBottomArea();
            return;
        }
        RSObject hopper = getHopper();
        if (hopper == null) {
            script.log("MLM", "Can't find the hopper in our loaded scene...");
            return;
        }

        if (!hopper.interact("deposit")) {
            // failed to interact with the hopper
            return;
        }
        // wait until paydirt is deposited
        int payDirtBefore = inventorySnapshot.getAmount(ItemID.PAYDIRT);
        script.pollFramesHuman(() -> {
            inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inventory not visible
                return false;
            }
            int payDirtNow = inventorySnapshot.getAmount(ItemID.PAYDIRT);

            if (payDirtNow < payDirtBefore) {
                if (spaceLeft != null && spaceLeft - payDirtBefore <= 0) {
                    script.log("MLM", "Forcing collect.");
                    forceCollectFlag = true;
                }
                return true;
            }
            if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                UIResult<String> dialogueText = script.getWidgetManager().getDialogue().getText();
                if (dialogueText.isFound()) {
                    if (dialogueText.get().toLowerCase().startsWith("you've already got some pay-dirt in the machine")) {
                        fixWaterWheelFlag = true;
                    }
                    // only fix in this case if our sack is full
                    else if (dialogueText.get().toLowerCase().startsWith("the machine will need to be repaired") && forceCollectFlag) {
                        fixWaterWheelFlag = true;
                    }
                    return true;
                }
            }
            return false;
        }, RandomUtils.uniformRandom(10000, 15000));
    }

    private RSObject getHopper() {
        paintTask = "Get hopper object";
        // === Upper hopper logic (TOP floor only) ===
        WorldPosition currentLoc = null;
        if (useUpperHopper) {
            currentLoc = script.getWorldPosition();
            if (currentLoc == null) return null;
        }
        if (useUpperHopper && selectedMineArea == MineArea.TOP && MLMAreaProvider.TOP_FLOOR_AREA.contains(currentLoc)) {
            WorldPosition upperHopperPos = new WorldPosition(3755, 5677, 0);

            RSObject upperHopper = script.getObjectManager().getRSObject(obj ->
                    obj.getWorldPosition().equals(upperHopperPos)
                            && obj.getName() != null
                            && obj.getName().equalsIgnoreCase("hopper")
                            && obj.canReach()
            );

            return upperHopper;
        }

        // === Default behavior: closest reachable hopper ===
        return script.getObjectManager().getRSObject(rsObject -> {
            if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("hopper")) {
                return false;
            }
            return rsObject.canReach();
        });
    }

    private RSObject getSack() {
        paintTask = "Get sack object";
        return script.getObjectManager().getRSObject(rsObject -> {
            // name needs to be default name
            if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("empty sack")) {
                return false;
            }
            return rsObject.canReach();
        });
    }

    private void scanWater() {
        paintTask = "Scan water";
        List<WorldPosition> waterTiles = MLMAreaProvider.WATER_INNER_AREA.getSurroundingPositions(1);
        boolean found = false;
        for (WorldPosition worldPosition : waterTiles) {
            // create a polygon for the tile
            Polygon polygon = script.getSceneProjector().getTilePoly(worldPosition, true);
            if (polygon == null || script.getWidgetManager().insideGameScreenFactor(polygon, Collections.emptyList()) < 0.4) {
                // not on the screen or blocked by ui
                continue;
            }

            List<Point> pixels = script.getPixelAnalyzer().findPixelsOnGameScreen(polygon, FLOWING_WATER_PIXEL);
            boolean hasFlowingWaterPixels = !pixels.isEmpty();
            int color = pixels.isEmpty() ? Color.GREEN.getRGB() : Color.RED.getRGB();
            // draw the polygon and fill it with color
            script.getScreen().getDrawableCanvas().fillPolygon(polygon, color, 0.5);
            // apply outline
            script.getScreen().getDrawableCanvas().drawPolygon(polygon, color, 1);

            if (hasFlowingWaterPixels && !found) {
                found = true;
                // we continue the loop just to draw the tiles as it looks fancy
                //     new ImagePanel(script.getScreen().getDrawableCanvas().toImage().toBufferedImage()).showInFrame("");
            }
        }
        if (found) {
            script.log("Water appears to be flowing... disabling fix water wheel flag");
            fixWaterWheelFlag = false;
        }
    }

    private void mineVein() {
        paintTask = "Mine vein";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log("MLM", "World position is null...");
            return;
        }
        List<RSObject> veins = getVeins();
        List<RSObject> activeVeinsOnScreen = getActiveVeinsOnScreen(veins, myPosition);
        UIResultList<WorldPosition> playersResult =
                script.getWidgetManager().getMinimap().getPlayerPositions();

        List<WorldPosition> playerPositions =
                playersResult.isFound() ? playersResult.asList() : Collections.emptyList();

        List<RSObject> availableVeins = new ArrayList<>();

        for (RSObject vein : activeVeinsOnScreen) {
            if (isVeinBeingMinedByPlayer(vein, playerPositions)) {
                skippedByPlayers.add(vein);
            } else {
                availableVeins.add(vein);
            }
        }
        script.log("MLM", "Active veins on screen: " + activeVeinsOnScreen.size());
        if (activeVeinsOnScreen.isEmpty()) {
            script.log("MLM", "Walking to closest vein off screen...");
            // walk to the closest vein which isn't on screen
            walkToClosestVeinOffScreen(veins, activeVeinsOnScreen);
            return;
        }

        int index = 0;
        if (firstTimeBack) {
            // first time running back to the mining area, we choose a random between the first 3 closest. this helps a lot with the top floor, as if we always get the closest first time around
            // it tends to go towards the south side, with this it makes it more random
            index = Math.min(activeVeinsOnScreen.size() - 1, 3);
        }
        RSObject closestVein;

        if (!availableVeins.isEmpty()) {
            closestVein = availableVeins.get(0);
        } else {
            script.log("MLM", "All veins occupied by players — mining closest anyway.");
            closestVein = activeVeinsOnScreen.get(0);
        }
        // draw the active veins
        drawActiveVeins(activeVeinsOnScreen, closestVein);

        // interact with the object
        // We aren't using RSObject#interact here because it tries multiple times to interact if the given menu entry options aren't visible.
        Polygon veinPolygon = closestVein.getConvexHull();
        if (veinPolygon == null) {
            return;
        }

        MenuHook veinMenuHook = getVeinMenuHook(closestVein);
        if (!script.getFinger().tapGameScreen(veinPolygon, veinMenuHook)) {
            // if we fail to interact with the object
            failCount++;
            if (failCount > 1) {
                script.log("MLM", "Failed to interact with the closest vein multiple times, ignoring it.");
                objectPositionBlacklist.put(closestVein.getWorldPosition(), System.currentTimeMillis());
            }
            return;
        }

        myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log("MLM", "World position is null after interacting with the vein...");
            return;
        }

        failCount = 0;

        if (firstTimeBack) {
            firstTimeBack = false;
        }
        long positionChangeTime = script.getLastPositionChangeMillis();
        if (closestVein.getTileDistance(myPosition) > 1) {
            WorldPosition myPosition_ = script.getWorldPosition();
            if (myPosition_ == null) {
                script.log("MLM", "World position is null after interacting with the vein...");
                return;
            }
            // if not in interactable distance, wait a little so we start moving.
            // This is just to detect a dud action (when you click a menu entry but nothing happens)
            if (!script.pollFramesUntil(() -> closestVein.getTileDistance(myPosition_) <= 1 || script.getLastPositionChangeMillis() < positionChangeTime, RandomUtils.uniformRandom(2000, 4000))) {
                // if we don't move after interacting and we aren't next to the object
                script.log("MLM", "We're not moving... trying again.");
                return;
            }
        }
        waitUntilFinishedMining(closestVein);
    }

    private void walkToClosestVeinOffScreen(List<RSObject> veins, List<RSObject> activeVeinsOnScreen) {
        paintTask = "Walk to closest vein";
        veins.removeAll(activeVeinsOnScreen);
        RSObject closestOffScreen = (RSObject) script.getUtils().getClosest(veins);
        if (closestOffScreen == null) {
            script.log("MLM", "Closest object off screen is null.");
            return;
        }
        script.getWalker().walkTo(closestOffScreen);
    }

    private void waitUntilFinishedMining(RSObject closestVein) {
        paintTask = "Wait till done mining";
        // wait until respawn circle appears in closestVein's position, or any other general conditions met
        Timer animatingTimer = new Timer();
        animationTimeout = RandomUtils.uniformRandom(4000, 6000);
        WorldPosition veinPosition = closestVein.getWorldPosition();
        script.log("MLM", "Entering waiting task...");
        AtomicBoolean failed = new AtomicBoolean(false);
        AtomicInteger previousAmount = new AtomicInteger(inventorySnapshot.getAmount(ItemID.PAYDIRT));
        Timer amountChangeTimer = new Timer();
        amountChangeTimeout = RandomUtils.uniformRandom(14000, 22000);
        long positionChangeTimeout = RandomUtils.uniformRandom(800, 2000);
        script.pollFramesHuman(() -> {
            WorldPosition myPosition_ = script.getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                UIResult<String> text = script.getWidgetManager().getDialogue().getText();
                if (text.isFound()) {
                    if (text.get().toLowerCase().contains("you need a pickaxe")) {
                        script.log("MLM", "No pickaxe, stopping script.");
                        script.stop();
                        return true;
                    }
                }
                return true;
            }
            int tileDistance = closestVein.getTileDistance(myPosition_);
            if (tileDistance > 1) {
                // still traversing to the rock
                amountChangeTimer.reset();
                script.log("MLM", "Still walking to rock. Tile distance: " + tileDistance);
                if (script.getLastPositionChangeMillis() > positionChangeTimeout) {
                    failed.set(true);
                    return true;
                } else {
                    return false;
                }
            }


            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                script.log("MLM", "Amount change timeout");
                this.amountChangeTimeout = RandomUtils.uniformRandom(14000, 22000);
                failed.set(true);
                return true;
            }

            if (animatingTimer.timeElapsed() > animationTimeout) {
                script.log("MLM", "Animation timeout");
                this.animationTimeout = RandomUtils.uniformRandom(4000, 6000);
                failed.set(true);
                return true;
            }

            Polygon polygon = script.getSceneProjector().getTileCube(myPosition_, 120);
            if (polygon == null) {
                return false;
            }
            if (script.getPixelAnalyzer().isPlayerAnimating(0.15)) {
                animatingTimer.reset();
            }

            List<WorldPosition> respawnCircles = getRespawnCirclePositions();
            if (respawnCircles.contains(veinPosition)) {
                script.log("MLM", "Respawn circle detected in the objects position.");
                return true;
            }

            inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inv full
                return false;
            }

            if (inventorySnapshot.isFull()) {
                return true;
            }
            spaceLeft = (Integer) sackOverlay.getValue(MLMSackOverlay.SPACE_LEFT);
            deposited = (Integer) sackOverlay.getValue(MLMSackOverlay.DEPOSITED);
            if (spaceLeft == null || deposited == null) {
                script.log("MLM", "Problem reading sack overlay... (space left: " + spaceLeft + ") (deposited: " + deposited + ")");
                return false;
            }
            int payDirtAmount = inventorySnapshot.getAmount(ItemID.PAYDIRT);
            if (spaceLeft - payDirtAmount <= 0) {
                // sack full
                return true;
            }
            if (payDirtAmount > previousAmount.get()) {
                // gained paydirt, reset item amount change timer
                script.log("MLM", "Gained Paydirt!");
                int amountGained = payDirtAmount - previousAmount.get();
                paydirtMined += amountGained;
                amountChangeTimer.reset();
                previousAmount.set(payDirtAmount);
            }
            return false;
        }, RandomUtils.uniformRandom(60000, 90000));

        if (!failed.get()) {
            // extra response time so we aren't instantly reacting
            script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(0, 4000));
        }
    }

    private MenuHook getVeinMenuHook(RSObject closestVein) {
        return menuEntries -> {
            boolean foundDepleted = false;
            for (MenuEntry entry : menuEntries) {
                String rawText = entry.getRawText();
                if (rawText.equalsIgnoreCase("mine ore vein")) {
                    return entry;
                } else if (rawText.equalsIgnoreCase("examine depleted vein")) {
                    script.log("MLM", "Depleted vein found");
                    foundDepleted = true;
                }
            }
            if (foundDepleted) {
                script.log("MLM", "Adding to blacklist");
                WorldPosition veinPosition = closestVein.getWorldPosition();
                objectPositionBlacklist.put(veinPosition, System.currentTimeMillis());
            }
            return null;
        };
    }

    private void drawActiveVeins(List<RSObject> veins, RSObject targetVein) {
        paintTask = "Draw active veins";
        script.getScreen().queueCanvasDrawable("ActiveVeins", canvas -> {
            for (RSObject vein : veins) {
                if (vein.getFaces() == null) continue;

                Color color = Color.GREEN;

                if (vein.equals(targetVein)) {
                    color = Color.CYAN;
                } else if (skippedByPlayers.contains(vein)) {
                    color = Color.ORANGE;
                }

                for (Triangle t : vein.getFaces()) {
                    canvas.drawPolygon(
                            t.getXPoints(),
                            t.getYPoints(),
                            3,
                            color.getRGB()
                    );
                }
            }

            // Draw blacklisted / depleted veins in RED
            for (Map.Entry<WorldPosition, Long> entry : objectPositionBlacklist.entrySet()) {
                WorldPosition pos = entry.getKey();

                RSObject match = veins.stream()
                        .filter(v -> pos.equals(v.getWorldPosition()) && v.getFaces() != null)
                        .findFirst()
                        .orElse(null);

                if (match != null) {
                    for (Triangle t : match.getFaces()) {
                        canvas.drawPolygon(
                                t.getXPoints(),
                                t.getYPoints(),
                                3,
                                Color.RED.getRGB()
                        );
                    }
                }
            }
        });
    }

    private List<RSObject> getVeins() {
        return script.getObjectManager().getObjects(rsObject -> {
            WorldPosition position = rsObject.getWorldPosition();
            Long time = objectPositionBlacklist.get(position);
            if (time != null) {
                if ((System.currentTimeMillis() - time) < BLACKLIST_TIMEOUT) {
                    return false;
                } else {
                    objectPositionBlacklist.remove(position);
                }
            }
            if (selectedMineArea == MineArea.TOP) {
                if (!MLMAreaProvider.TOP_FLOOR_ACCESSIBLE_AREA.contains(rsObject.getWorldPosition())) {
                    return false;
                }
            }
            return rsObject.getName() != null && rsObject.getName().equalsIgnoreCase("depleted vein") && rsObject.canReach();
        });
    }

    private List<WorldPosition> getRespawnCirclePositions() {
        List<Rectangle> respawnCircles = script.getPixelAnalyzer().findRespawnCircles();
        return script.getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
    }

    public List<RSObject> getActiveVeinsOnScreen(List<RSObject> veins, WorldPosition myPosition) {
        List<RSObject> activeVeinsOnScreen = new ArrayList<>(veins);
        List<Rectangle> respawnCircles = script.getPixelAnalyzer().findRespawnCircles();
        List<WorldPosition> circlePositions = script.getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
        // remove objects what aren't interactable on screen OR if there is a respawn circle in that position
        activeVeinsOnScreen.removeIf(rsObject -> !rsObject.isInteractableOnScreen() || circlePositions.contains(rsObject.getWorldPosition()));
        // sort by distance
        activeVeinsOnScreen.sort(Comparator.comparingDouble(
                vein -> vein.getWorldPosition().distanceTo(myPosition))
        );
        return activeVeinsOnScreen;
    }

    private boolean shouldCollect(int freeSackSpaces) {
        return !inventorySnapshot.contains(ItemID.PAYDIRT) && freeSackSpaces <= (selectedMineArea != MineArea.TOP ? 14 : 0);
    }

    private boolean outsideAccessibleAreaCheck() {
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return false;
        }
        if (MLMAreaProvider.TOP_FLOOR_AREA.contains(myPosition)) {
            if (!MLMAreaProvider.TOP_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {
                script.log("MLM", "Outside accessible top area, returning to accessible area.");
                recoverFromInaccessibleTopArea();
            }
        } else {
            if (!MLMAreaProvider.BOTTOM_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {
                script.log("MLM", "Outside accessible bottom area, returning to accessible area.");
                recoverFromInaccessibleBottomArea();
            }
        }
        return false;
    }

    private boolean needsToWalkToMineArea(WorldPosition worldPosition) {
        for (Area area : MLMAreaProvider.MINING_MINE_AREAS) {
            if (area == null) {
                continue;
            }
            if (area.contains(worldPosition)) {
                return false;
            }
        }
        return !MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_AREA.contains(worldPosition) && !MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_AREA2.contains(worldPosition) && !MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_AREA.contains(worldPosition) && !MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_AREA3.contains(worldPosition);
    }

    enum Task {
        MINE_VEIN,
        WALK_TO_VEIN_AREA,
        DEPOSIT_PAY_DIRT,
        COLLECT,
        DROP_PAYDIRT,
        HANDLE_BANK,
        OPEN_BANK,
        REPAIR_WHEEL,
        DROP_HAMMER;
    }

    private void updateGPGained() {
        int nuggetValue = nuggetsGained * 1250;
        int coalValue = coalGained * 268;
        int goldValue = goldGained * 116;
        int mithrilValue = mithrilGained * 119;
        int adamantvalue = adamantGained * 651;
        int runevalue = runeGained * 10302;

        gpGained = nuggetValue + coalValue
                + goldValue + mithrilValue
                + adamantvalue + runevalue;
    }

    private boolean isVeinBeingMinedByPlayer(RSObject vein, List<WorldPosition> playerPositions) {
        WorldPosition veinPos = vein.getWorldPosition();
        int plane = veinPos.getPlane();

        for (WorldPosition playerPos : playerPositions) {
            if (playerPos.getPlane() != plane) continue;

            // N, E, S, W adjacency
            if (
                    playerPos.equals(new WorldPosition(veinPos.getX(), veinPos.getY() + 1, plane)) ||
                            playerPos.equals(new WorldPosition(veinPos.getX(), veinPos.getY() - 1, plane)) ||
                            playerPos.equals(new WorldPosition(veinPos.getX() + 1, veinPos.getY(), plane)) ||
                            playerPos.equals(new WorldPosition(veinPos.getX() - 1, veinPos.getY(), plane))
            ) {
                return true;
            }
        }
        return false;
    }

    private void recoverFromInaccessibleTopArea() {
        paintTask = "Recover inaccessibility";
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return;

        if (MLMAreaProvider.TOP_FLOOR_NORTHEAST_AREA.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.TOP_FLOOR_NORTHEAST_ROCK,
                    MLMAreaProvider.TOP_FLOOR_NORTHEAST_WALKAREA
            );
            return;
        }

        if (MLMAreaProvider.TOP_FLOOR_NORTHWEST_AREA.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.TOP_FLOOR_NORTHWEST_ROCK,
                    MLMAreaProvider.TOP_FLOOR_NORTHWEST_WALKAREA
            );
            return;
        }

        if (MLMAreaProvider.TOP_FLOOR_SOUTHEAST_AREA.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.TOP_FLOOR_SOUTHEAST_ROCK,
                    MLMAreaProvider.TOP_FLOOR_SOUTHEAST_WALKAREA
            );
            return;
        }

        script.log("MLM", "In unknown inaccessible top area. Stopping script.");
        script.stop();
    }

    private void recoverFromInaccessibleBottomArea() {
        paintTask = "Recover inaccessibility";
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return;

        if (MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_AREA.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_ROCK,
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_WALKAREA
            );
            return;
        }

        if (MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_AREA2.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_ROCK2,
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_SOUTH_WALKAREA2
            );
            return;
        }

        if (MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_AREA.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_ROCK1,
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_WALKAREA1
            );
            handleRockfallRecovery(
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_ROCK2,
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_WALKAREA2
            );
            return;
        }

        if (MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_AREA3.contains(myPos)) {
            handleRockfallRecovery(
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_ROCK3,
                    MLMAreaProvider.BOTTOM_FLOOR_WEST_NORTH_WALKAREA3
            );
            return;
        }

        script.log("MLM", "In unknown inaccessible bottom area. Stopping script.");
        script.stop();
    }

    private void handleRockfallRecovery(WorldPosition rockPos, Area walkArea) {
        paintTask = "Handle rockfall";
        Predicate<RSObject> rockQuery = obj -> {
            if (obj == null) return false;
            if (!MLMAreaProvider.rockName.equalsIgnoreCase(obj.getName())) return false;

            String[] actions = obj.getActions();
            if (actions == null) return false;

            boolean hasMine = Arrays.stream(actions)
                    .anyMatch(a -> a != null && a.equalsIgnoreCase(MLMAreaProvider.rockAction));

            return hasMine
                    && rockPos.equals(obj.getWorldPosition())
                    && obj.canReach();
        };

        RSObject rock = script.getObjectManager().getRSObject(rockQuery);

        if (rock == null) {
            script.log("MLM", "Rockfall not found at " + rockPos + ", walking closer.");
            script.getWalker().walkTo(rockPos, new WalkConfig.Builder().breakDistance(2).tileRandomisationRadius(2).build());
            return;
        }

        script.log("MLM", "Mining rockfall at " + rockPos);

        if (!rock.interact(MLMAreaProvider.rockAction)) {
            script.log("MLM", "Failed to interact with rockfall, already gone?");
            // Minimap escape
            WorldPosition escapeTile = walkArea.getRandomPosition();
            WorldPosition myPos = script.getWorldPosition();
            if (myPos == null) return;

            Rectangle tapRect = script.getWidgetManager()
                    .getMinimap()
                    .positionToMinimap(myPos, escapeTile);

            if (tapRect != null) {
                script.log("MLM", "Escaping via minimap to " + escapeTile);
                script.getFinger().tap(tapRect);
            }

            // Wait until we are back in accessible area
            script.pollFramesHuman(() -> {
                WorldPosition pos = script.getWorldPosition();
                return pos != null && MLMAreaProvider.TOP_FLOOR_ACCESSIBLE_AREA.contains(pos);
            }, RandomUtils.uniformRandom(8000, 14000));

            script.log("MLM", "Recovered from inaccessible area.");
            return;
        }

        // Wait until we are next to the rock
        boolean reachedRock = script.pollFramesHuman(() -> {
            WorldPosition pos = script.getWorldPosition();
            return pos != null && pos.distanceTo(rockPos) <= 1;
        }, RandomUtils.uniformRandom(7000, 11000));

        if (!reachedRock) {
            script.log("MLM", "Failed to reach rockfall.");
            return;
        }

        // Extra human delay (3–5s)
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(3000, 5000));

        // Minimap escape
        WorldPosition escapeTile = walkArea.getRandomPosition();
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return;

        Rectangle tapRect = script.getWidgetManager()
                .getMinimap()
                .positionToMinimap(myPos, escapeTile);

        if (tapRect != null) {
            script.log("MLM", "Escaping via minimap to " + escapeTile);
            script.getFinger().tap(tapRect);
        }

        // Wait until we are back in walk area
        script.pollFramesHuman(() -> {
            WorldPosition pos = script.getWorldPosition();
            return pos != null && walkArea.contains(pos);
        }, RandomUtils.uniformRandom(8000, 14000));

        script.log("MLM", "Recovered from inaccessible area.");
    }
}