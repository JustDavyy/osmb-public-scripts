package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static main.dSunbleakWCer.*;

public class Bank extends Task {

    private final Area bankWalkArea = new RectangleArea(2194, 2316, 3, 3, 0);

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
            ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET, ItemID.RATIONS
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
        task = "BANK";

        if (script.getWidgetManager().getBank().isVisible()) {
            return bankItems();
        }

        // Move to / open bank first
        if (!openBank()) {
            return false;
        }

        // Handle banking
        return bankItems();
    }

    private boolean openBank() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        task = "Open bank";
        script.log("BANK", "Searching for bank chest (Bank chest space)...");

        List<RSObject> chests = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) return false;
            return obj.getName().equals("Bank chest space")
                    && Arrays.asList(obj.getActions()).contains("Build")
                    && obj.getWorldPosition().getX() == 2194
                    && obj.getWorldPosition().getY() == 2314;
        });

        if (chests.isEmpty()) {
            script.log("BANK", "No bank chest object found at (2194, 2314). Walking to bank area...");
            script.getWalker().walkTo(bankWalkArea.getRandomPosition());
            return false;
        }

        RSObject chest = (RSObject) script.getUtils().getClosest(chests);
        if (chest == null) {
            script.log("BANK", "Closest bank chest object is null.");
            return false;
        }

        Polygon hull = chest.getConvexHull();
        if (hull == null) {
            script.log("BANK", "Chest convex hull is null, re-polling.");
            return false;
        }

        double insideFactor = script.getWidgetManager().insideGameScreenFactor(
                hull, List.of(ChatboxComponent.class));

        // Walk closer if not fully visible
        if (insideFactor < 1.0) {
            script.log("BANK", String.format(
                    "Bank chest not fully on screen (factor=%.2f). Walking closer...",
                    insideFactor));

            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(() -> {
                        Polygon h = chest.getConvexHull();
                        return h != null &&
                                script.getWidgetManager().insideGameScreenFactor(
                                        h, List.of(ChatboxComponent.class)) >= 1.0;
                    })
                    .enableRun(true)
                    .build();

            script.getWalker().walkTo(chest.getWorldPosition(), config);
            return false;
        }

        // Now safe to tap (insideFactor ≥ 1.0)
        if (!script.getFinger().tap(hull, "Bank")) {
            script.log("BANK", "Failed to interact with bank chest.");
            return false;
        }

        // Wait until bank widget visible OR fail after 4 seconds idle
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!current.equals(pos.get())) {
                pos.set(current);
                positionChangeTimer.get().reset();
            }

            return script.getWidgetManager().getBank().isVisible()
                    || positionChangeTimer.get().timeElapsed() > 4000;
        }, 20000);

        boolean bankOpen = script.getWidgetManager().getBank().isVisible();
        script.log("BANK", "Bank open status: " + bankOpen);
        return bankOpen;
    }

    private boolean bankItems() {
        task = "Deposit items";

        if (useLogBasket && !usedBasketAlready) {
            task = "Empty log basket";
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
            if (inv == null) return false;

            if (!inv.getItem(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET).interact("Empty")) {
                script.log("BANK", "Failed to empty log basket, returning!");
                return false;
            } else {
                usedBasketAlready = true;
                logsChopped += 28;
            }
        }

        if (!script.getWidgetManager().getBank().depositAll(ITEMS_NOT_TO_DEPOSIT)) {
            script.log("BANK", "Banking failed (partially?), returning!");
            return false;
        }

        script.getWidgetManager().getBank().close();
        script.log("BANK", "Banked items and closed bank.");
        return true;
    }
}