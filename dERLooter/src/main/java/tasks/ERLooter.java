package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.spellbook.ArceuusSpellbook;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.utils.*;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import utils.Task;
import static main.dERLooter.*;
import com.osmb.api.script.Script;

import java.awt.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ERLooter extends Task {

    // Areas
    private final Area groundArea = new RectangleArea(1539, 3030, 24, 15, 0);
    private final Area firstfloor = new RectangleArea(1551, 3032, 7, 5, 1);
    private final Area secondfloor = new RectangleArea(1551, 3032, 7, 4, 2);

    // Ladders
    private final WorldPosition groundFloorLadder = new WorldPosition(1552, 3033, 0);
    private final WorldPosition firstFloorUPLadder = new WorldPosition(1557, 3034, 1);
    private final WorldPosition firstFloorDOWNLadder = new WorldPosition(1552, 3033, 1);
    private final WorldPosition secondFloorLadder = new WorldPosition(1557, 3034, 2);

    // Tiles
    private final WorldPosition redEclipseSpawn = new WorldPosition(1555, 3035, 2);

    // Bank queries
    private static final String[] BANK_NAMES = {"Bank chest"};
    private static final String[] BANK_ACTIONS = {"bank"};
    private static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    // Position
    private WorldPosition myPosition;

    public ERLooter(Script script) {
        super(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {

        // Get position and store it
        task = "Get position";
        myPosition = script.getWorldPosition();

        if (myPosition == null) {
            script.log("ERLooter", "Position is null, returning!");
            return false;
        }

        task = "Get inventory snapshot";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.ECLIPSE_RED));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        // Handle if we're at the base floor
        if (groundArea.contains(myPosition)) {
            task = "Handle ground floor";

            // Handle inventory full (we need to bank)
            if (inventorySnapshot.isFull()) {
                task = "Bank";
                script.log("ERLooter", "Inventory full, banking!");

                // Open bank if needed
                if (!script.getWidgetManager().getBank().isVisible()) {
                    openBank();
                    return false;
                }

                // Deposit all items
                if (script.getWidgetManager().getBank().depositAll(Collections.emptySet())) {
                    script.log("ERLooter", "Deposit successful");
                    script.getWidgetManager().getBank().close();
                } else {
                    script.log("ERLooter", "Deposit failed, returning!");
                    return false;
                }
            }

            // Handle inventory not full (we need to go loot eclipse reds)
            RSObject groundLadder = getSpecificObjectAt("Ladder", groundFloorLadder.getX(), groundFloorLadder.getY(), groundFloorLadder.getPlane());
            if (groundLadder == null) {
                script.log("ERLooter", "Ground object ladder is null, returning!");
                return false;
            }
            if (groundLadder.isInteractableOnScreen()) {
                if (groundLadder.interact("climb-up")) {
                    // Wait till we are on the first floor
                    BooleanSupplier condition = () -> {
                        myPosition = script.getWorldPosition();
                        if (myPosition == null) return false;

                        return firstfloor.contains(myPosition);
                    };

                    task = "Wait till first floor arrival";
                    return script.pollFramesHuman(condition, RandomUtils.uniformRandom(8500, 13000));
                }
            } else {
                task = "Walk to ground ladder";
                return script.getWalker().walkTo(groundLadder);
            }
        }

        // Handle if we're on the first floor
        if (firstfloor.contains(myPosition)) {
            task = "Handle first floor";

            // Handle inventory full (we need to bank)
            if (inventorySnapshot.isFull()) {
                task = "Go down";

                RSObject firstFloorDownLadder = getSpecificObjectAt("Ladder", firstFloorDOWNLadder.getX(), firstFloorDOWNLadder.getY(), firstFloorDOWNLadder.getPlane());
                if (firstFloorDownLadder == null) {
                    script.log("ERLooter", "first floor object ladder is null, returning!");
                    return false;
                }
                if (firstFloorDownLadder.isInteractableOnScreen()) {
                    if (firstFloorDownLadder.interact("climb-down")) {
                        // Wait till we are on the groundfloor
                        BooleanSupplier condition = () -> {
                            myPosition = script.getWorldPosition();
                            if (myPosition == null) return false;

                            return groundArea.contains(myPosition);
                        };

                        task = "Wait till ground floor arrival";
                        return script.pollFramesHuman(condition, RandomUtils.uniformRandom(8500, 13000));
                    }
                } else {
                    task = "Walk to first floor down ladder";
                    return script.getWalker().walkTo(firstFloorDownLadder);
                }
            }

            // Inventory not full, we need to go up to loot
            task = "Go up";

            RSObject firstFloorUpLadder = getSpecificObjectAt("Ladder", firstFloorUPLadder.getX(), firstFloorUPLadder.getY(), firstFloorUPLadder.getPlane());
            if (firstFloorUpLadder == null) {
                script.log("ERLooter", "first floor object ladder is null, returning!");
                return false;
            }
            if (firstFloorUpLadder.isInteractableOnScreen()) {
                if (firstFloorUpLadder.interact("climb-up")) {
                    // Wait till we are on the second floor
                    BooleanSupplier condition = () -> {
                        myPosition = script.getWorldPosition();
                        if (myPosition == null) return false;

                        return secondfloor.contains(myPosition);
                    };

                    task = "Wait till second floor arrival";
                    return script.pollFramesHuman(condition, RandomUtils.uniformRandom(8500, 13000));
                }
            } else {
                task = "Walk to first floor up ladder";
                return script.getWalker().walkTo(firstFloorUpLadder);
            }
        }

        // Handle if we're on the second floor
        if (secondfloor.contains(myPosition)) {
            task = "Handle second floor";

            // Handle inventory full (we need to bank)
            if (inventorySnapshot.isFull()) {
                task = "Go down";

                RSObject secondFloorLadderObj = getSpecificObjectAt("Ladder", secondFloorLadder.getX(), secondFloorLadder.getY(), secondFloorLadder.getPlane());
                if (secondFloorLadderObj == null) {
                    script.log("ERLooter", "second floor object ladder is null, returning!");
                    return false;
                }
                if (secondFloorLadderObj.isInteractableOnScreen()) {
                    if (secondFloorLadderObj.interact("climb-down")) {
                        // Wait till we are on the first floor
                        BooleanSupplier condition = () -> {
                            myPosition = script.getWorldPosition();
                            if (myPosition == null) return false;

                            return firstfloor.contains(myPosition);
                        };

                        task = "Wait till first floor arrival";
                        return script.pollFramesHuman(condition, RandomUtils.uniformRandom(8500, 13000));
                    }
                } else {
                    task = "Walk to second floor up ladder";
                    return script.getWalker().walkTo(secondFloorLadderObj);
                }
            }

            // Handle inventory not full, we need to loot red eclipse
            if (isRedEclipseOnFloor()) {
                task = "Pick up eclipse red";

                previousEclipseRed = inventorySnapshot.getAmount(ItemID.ECLIPSE_RED);
                script.log("ERLooter", "Current eclipse red in inventory (before loot): " + previousEclipseRed);

                RSTile eclipseTile = script.getSceneManager().getTile(redEclipseSpawn);
                if (eclipseTile == null) {
                    script.log("ERLooter", "Eclipse spawn tile is null, returning!");
                    return false;
                }

                if (!eclipseTile.isOnGameScreen()) {
                    script.log("ERLooter", "Red eclipse spawn is not on screen, moving there!");
                    return script.getWalker().walkTo(redEclipseSpawn);
                }

                script.log("ERLooter", "Interacting with eclipse spawn tile");

                // randomize left tap and long press
                int roll = RandomUtils.uniformRandom(1, 100);
                boolean tapped;

                if (roll <= 10) { // ~10% chance long press
                    tapped = script.getFinger().tap(eclipseTile.getTileCube(0).getResized(0.45), "Take");
                } else {
                    tapped = script.getFinger().tap(eclipseTile.getTileCube(0).getResized(0.45));
                }

                if (!tapped) {
                    script.log("ERLooter", "Failed to pick up eclipse red, returning!");
                    return false;
                }

                // Wait until item shows in inventory
                BooleanSupplier condition = () -> {
                    ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(Set.of(ItemID.ECLIPSE_RED));

                    if (invCheck == null) {
                        return false;
                    }

                    currentEclipseRed = invCheck.getAmount(ItemID.ECLIPSE_RED);
                    if (currentEclipseRed > previousEclipseRed) {
                        int gained = currentEclipseRed - previousEclipseRed;
                        totalEclipseRed += gained;
                        script.log("ERLooter", "Looted " + gained + "x eclipse red. Total so far: " + totalEclipseRed);
                    }

                    boolean success = currentEclipseRed > previousEclipseRed;
                    if (success) script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1, 400));
                    return success;
                };
                boolean eclipseLooted = script.pollFramesUntil(condition, RandomUtils.uniformRandom(3500, 5000));

                if (eclipseLooted) {
                    script.getProfileManager().forceHop();
                    return false;
                }

                return false;
            } else {
                script.log("ERLooter", "Eclipse red not found in this world, hopping!");
                script.getProfileManager().forceHop();
                return false;
            }
        }

        return false;
    }

    private boolean isRedEclipseOnFloor() {
        UIResultList<WorldPosition> itemPositions = script.getWidgetManager().getMinimap().getItemPositions();
        if (itemPositions.isNotVisible() || itemPositions.isNotFound()) {
            return false;
        }

        for (WorldPosition pos : itemPositions) {
            if (pos.equals(redEclipseSpawn)) {
                return true;
            }
        }

        return false;
    }

    private RSObject getSpecificObjectAt(String name, int worldX, int worldY, int plane) {
        return script.getObjectManager().getRSObject(obj ->
                obj != null
                        && obj.getName() != null
                        && name.equalsIgnoreCase(obj.getName())
                        && obj.getWorldX() == worldX
                        && obj.getWorldY() == worldY
                        && obj.getPlane() == plane
        );
    }

    private void openBank() {
        task = "Open bank";
        script.log("ERLooter", "Opening bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (!banksFound.isEmpty()) {
            RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
            bank.interact(BANK_ACTIONS);
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for open bank";
        script.pollFramesUntil(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 4000;
        }, RandomUtils.uniformRandom(15000, 17000));
    }
}