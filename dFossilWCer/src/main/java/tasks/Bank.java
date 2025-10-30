package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.dFossilWCer.*;

public class Bank extends Task {

    private static final Area bankArea = new RectangleArea(3731, 3799, 13, 13, 0);
    private static final Area bankWalkArea = new RectangleArea(3739, 3802, 3, 3, 0);
    private static final Area choppingArea = new RectangleArea(3700, 3828, 19, 13, 0);
    private static final Area holeWalkArea = new RectangleArea(3710, 3832, 3, 3, 0);

    public static final Set<Integer> ITEMS_NOT_TO_DEPOSIT = new HashSet<>(Set.of(
            ItemID.BRONZE_AXE, ItemID.BRONZE_FELLING_AXE,
            ItemID.IRON_AXE, ItemID.IRON_FELLING_AXE,
            ItemID.STEEL_AXE, ItemID.STEEL_FELLING_AXE,
            ItemID.BLACK_AXE, ItemID.BLACK_FELLING_AXE,
            ItemID.MITHRIL_AXE, ItemID.MITHRIL_FELLING_AXE, ItemID.BLESSED_AXE,
            ItemID.ADAMANT_AXE, ItemID.ADAMANT_FELLING_AXE,
            ItemID.RUNE_AXE, ItemID.RUNE_FELLING_AXE,
            ItemID.GILDED_AXE,
            ItemID.DRAGON_AXE, ItemID.DRAGON_FELLING_AXE,
            ItemID._3RD_AGE_AXE, ItemID._3RD_AGE_FELLING_AXE,
            ItemID.CRYSTAL_AXE, ItemID.CRYSTAL_FELLING_AXE,

            // Inactives
            ItemID.CRYSTAL_AXE_INACTIVE, ItemID.CRYSTAL_FELLING_AXE_INACTIVE,
            ItemID.INFERNAL_AXE_UNCHARGED, ItemID.INFERNAL_AXE_UNCHARGED_25371,
            ItemID.INFERNAL_AXE_UNCHARGED_30348,

            // Ornament items
            ItemID.DRAGON_AXE_OR, ItemID.DRAGON_AXE_OR_30352,
            ItemID.INFERNAL_AXE_OR, ItemID.INFERNAL_AXE_OR_30347,

            // Additional tools
            ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET
    ));

    public Bank(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!bankMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull() || script.getWidgetManager().getBank().isVisible();
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();

        if (script.getWidgetManager().getBank().isVisible()) {
            return bankItems();
        }

        WorldPosition myPos = script.getWorldPosition();
        if (myPos != null && !bankArea.contains(myPos) && !isBankChestInteractable()) {
            task = "Walk to bank area";
            if (useShortcut) {
                script.log(getClass(), "Walking to bank area (shortcut)");
                return walkWithShortcut();
            } else {
                script.log(getClass(), "Walking to bank area");
                return walkWithoutShortcut();
            }
        }

        // Move to / open bank first
        if (!openBank()) {
            return false;
        }

        // Handle banking
        return bankItems();
    }

    private boolean walkWithShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (choppingArea.contains(myPos)) {
            task = "Use shortcut";

            // Search for Hole object at base tile 3712, 3828 with action "Climb-through"
            List<RSObject> holes = script.getObjectManager().getObjects(obj -> {
                if (obj.getName() == null || obj.getActions() == null) {
                    return false;
                }
                return obj.getName().equals("Hole")
                        && Arrays.asList(obj.getActions()).contains("Climb through")
                        && obj.getWorldPosition().getX() == 3712
                        && obj.getWorldPosition().getY() == 3828;
            });

            if (holes.isEmpty()) {
                script.log(getClass(), "No Hole object found at base tile (3712,3828).");
                return false;
            }

            RSObject hole = (RSObject) script.getUtils().getClosest(holes);
            if (hole == null) {
                script.log(getClass(), "Closest Hole object is null.");
                return false;
            }

            // Walk to hole if not interactable on screen
            if (!hole.isInteractableOnScreen()) {
                script.log(getClass(), "Hole not on screen, walking closer...");
                WalkConfig config = new WalkConfig.Builder()
                        .breakCondition(hole::isInteractableOnScreen)
                        .enableRun(true)
                        .build();
                script.getWalker().walkTo(holeWalkArea.getRandomPosition(), config);
                return false;
            }

            // Interact with Hole
            task = "Climbing through hole";
            if (!hole.interact("Climb through")) {
                script.log(getClass(), "Failed to climb through Hole.");
                return false;
            }

            boolean done = script.pollFramesHuman(() -> {
                WorldPosition currentPos = script.getWorldPosition();
                return currentPos != null && !choppingArea.contains(currentPos);
            }, script.random(7000, 12000));
            if (done) {
                return walkWithoutShortcut();
            } else {
                return false;
            }
        } else {
            return walkWithoutShortcut();
        }
    }

    private boolean walkWithoutShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        task = "Walk to bank and check Chest pieces";

        // Search for Chest pieces object at (3742, 3805) with "Build" action
        List<RSObject> chests = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) {
                return false;
            }
            return obj.getName().equals("Chest pieces")
                    && Arrays.asList(obj.getActions()).contains("Build")
                    && obj.getWorldPosition().getX() == 3742
                    && obj.getWorldPosition().getY() == 3805;
        });

        if (chests.isEmpty()) {
            script.log(getClass(), "No bank chest object found at (3742,3805). Walking to bank area...");
            script.getWalker().walkTo(bankWalkArea.getRandomPosition());
            return false;
        }

        RSObject chest = (RSObject) script.getUtils().getClosest(chests);
        if (chest == null) {
            script.log(getClass(), "Closest bank chest object is null.");
            return false;
        }

        // Walk to chest if not interactable on screen
        if (!chest.isInteractableOnScreen()) {
            script.log(getClass(), "Bank chest not on screen, walking closer...");
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(chest::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            script.getWalker().walkTo(chest.getWorldPosition(), config);
            return false; // Re-poll once it's on screen
        }

        script.pollFramesHuman(() -> false, script.random(600, 1200));
        return true;
    }

    private boolean openBank() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        task = "Open bank";
        script.log(getClass(), "Searching for bank chest (Chest pieces)...");

        // Search for Chest pieces object at (3742, 3805) with "Build" action
        List<RSObject> chests = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) {
                return false;
            }
            return obj.getName().equals("Chest pieces")
                    && Arrays.asList(obj.getActions()).contains("Build")
                    && obj.getWorldPosition().getX() == 3742
                    && obj.getWorldPosition().getY() == 3805;
        });

        if (chests.isEmpty()) {
            script.log(getClass(), "No bank chest object found at (3742,3805). Walking to bank area...");
            script.getWalker().walkTo(bankWalkArea.getRandomPosition());
            return false;
        }

        RSObject chest = (RSObject) script.getUtils().getClosest(chests);
        if (chest == null) {
            script.log(getClass(), "Closest bank chest object is null.");
            return false;
        }

        // Walk to chest if not interactable on screen
        if (!chest.isInteractableOnScreen()) {
            script.log(getClass(), "Bank chest not on screen, walking closer...");
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(chest::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            script.getWalker().walkTo(chest.getWorldPosition(), config);
            return false; // Re-poll once it's on screen
        }

        // Interact with chest
        if (!script.getFinger().tap(chest.getConvexHull(), "Use")) {
            script.log(getClass(), "Failed to interact with bank chest.");
            return false;
        }

        // Wait until bank is visible or position changes for > 4s (fail-safe)
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;
            if (pos.get() == null || !current.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(current);
            }
            return script.getWidgetManager().getBank().isVisible()
                    || positionChangeTimer.get().timeElapsed() > 4000;
        }, 20000);

        // Return whether bank is open
        boolean bankOpen = script.getWidgetManager().getBank().isVisible();
        script.log(getClass(), "Bank open status: " + bankOpen);
        return bankOpen;
    }

    private boolean bankItems() {
        task = "Deposit items";

        if (useLogBasket && !usedBasketAlready) {
            task = "Empty log basket";
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
            if (inv == null) return false;

            if (!inv.getItem(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET).interact("Empty")) {
                script.log(getClass(), "Failed to empty log basket, returning!");
                return false;
            } else {
                usedBasketAlready = true;
                logsChopped += 28;
            }
        }

        if (!script.getWidgetManager().getBank().depositAll(ITEMS_NOT_TO_DEPOSIT)) {
            script.log(getClass(), "Banking failed (partially?), returning!");
            return false;
        }

        script.getWidgetManager().getBank().close();
        script.log(getClass(), "Banked items and closed bank.");
        return true;
    }

    private boolean isBankChestInteractable() {
        List<RSObject> chests = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) {
                return false;
            }
            return obj.getName().equals("Chest pieces")
                    && Arrays.asList(obj.getActions()).contains("Build")
                    && obj.getWorldPosition().getX() == 3742
                    && obj.getWorldPosition().getY() == 3805;
        });

        if (chests.isEmpty()) {
            return false;
        }

        RSObject chest = (RSObject) script.getUtils().getClosest(chests);
        return chest != null && chest.isInteractableOnScreen();
    }
}