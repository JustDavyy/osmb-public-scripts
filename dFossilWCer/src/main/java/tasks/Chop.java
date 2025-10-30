package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static main.dFossilWCer.*;

public class Chop extends Task {
    private int clusterFailCount = 0;
    private RSObject targetTree;

    private static final SearchablePixel[] MAHOGANY_PIXEL_CLUSTER = new SearchablePixel[]{
            new SearchablePixel(-11443436, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-15527164, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12432618, new SingleThresholdComparator(0), ColorModel.RGB),
            new SearchablePixel(-14603515, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-14342889, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-14605039, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-13946602, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10590171, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12563192, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12761064, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10063308, new SingleThresholdComparator(2), ColorModel.RGB)
    };

    private static final SearchablePixel[] TEAK_PIXEL_CLUSTER = new SearchablePixel[]{
            new SearchablePixel(-4794765, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5913750, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-4596873, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5189776, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8218541, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-6703774, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7362468, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-6308761, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-5453202, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9205686, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8679345, new SingleThresholdComparator(2), ColorModel.HSL)
    };

    private static final Area choppingArea = new RectangleArea(3699, 3830, 20, 10, 0);
    private static final Area bankingArea = new RectangleArea(3708, 3797, 42, 21, 0);

    public Chop(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos != null && !choppingArea.contains(myPos)) {
            task = "Walk to chopping area";
            if (useShortcut) {
                return walkWithShortcut();
            } else {
                return walkWithoutShortcut();
            }
        }

        usedBasketAlready = false;

        // === Pick correct pixel cluster ===
        SearchablePixel[] clusterToUse;
        if (logsId == ItemID.TEAK_LOGS) {
            clusterToUse = TEAK_PIXEL_CLUSTER;
        } else if (logsId == ItemID.MAHOGANY_LOGS) {
            clusterToUse = MAHOGANY_PIXEL_CLUSTER;
        } else {
            script.log(getClass(), "Invalid tree ID selected: " + logsId);
            return false;
        }

        // === New tree detection via RSObject + convex hull color check ===
        task = "Find tree";
        RSObject treePatch = findTree(clusterToUse);

        if (treePatch == null) {
            clusterFailCount++;
            script.log(getClass(), "No valid tree patch found. Fail count = " + clusterFailCount);

            if (clusterFailCount >= 3) {
                script.log(getClass(), "No patches found 3 times in a row, closing inventory and retrying.");

                if (script.getWidgetManager().getInventory().isVisible()) {
                    if (script.getWidgetManager().getInventory().close()) {
                        script.log(getClass(), "Inventory closed to refresh view.");
                    } else {
                        script.log(getClass(), "Failed to close inventory.");
                    }
                }
                clusterFailCount = 0;
            }

            script.pollFramesHuman(() -> false, script.random(400, 800));
            return false;
        }

        clusterFailCount = 0;

        // === Interact with tree ===
        task = "Chop tree";
        if (!treePatch.interact(getMenuHook())) {
            script.log(getClass(), "Failed to interact with tree.");
            return false;
        } else {
            targetTree = treePatch;
        }

        waitUntilFinishedChopping(clusterToUse);

        // === Small chance for additional delay ===
        if (script.random(1, 100) <= 15) {
            script.pollFramesHuman(() -> false, script.random(1, 100));
        }

        return true;
    }

    private void waitUntilFinishedChopping(SearchablePixel[] cluster) {

        int maxChopDuration = script.random(240_000, 270_000);

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(logsId));
        if (startSnapshot == null) {
            script.log(getClass(), "Aborting chop check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(logsId));

        long start = System.currentTimeMillis();

        // === INITIAL: wait to let user start chopping ===
        script.pollFramesHuman(() -> false, script.random(2750, 4000));

        // === Main monitoring loop ===
        script.pollFramesHuman(() -> {
            boolean gainedXP = false;

            // === Inventory check ===
            ItemGroupResult currentInv = script.getWidgetManager().getInventory().search(Set.of(logsId));
            if (currentInv == null) {
                script.log(getClass(), "Chop stopped: inventory became inaccessible.");
                return true;
            }
            if (currentInv.isFull()) {
                script.log(getClass(), "Chop stopped: inventory is full.");
                return true;
            }

            // === Dialogue check for level up ===
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log(getClass(), "Dialogue detected, leveled up?");
                script.pollFramesHuman(() -> false, script.random(1000, 3000));
                return true;
            }

            // === Inventory count tracking ===
            int currentCount = currentInv.getAmount(logsId);
            int lastCount = previousCount.get();
            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;
                previousCount.set(currentCount);
                logsChopped += gained;
                script.log(getClass(), "+" + gained + " logs chopped! (" + logsChopped + " total)");
                lastXpGain.reset();
            } else if (currentCount < lastCount) {
                script.log(getClass(), "Detected log count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
                previousCount.set(currentCount);
            }
            // === Tree presence check ===
            if (!isTreeStillPresent(targetTree, cluster)) {
                script.log(getClass(), "Chop stopped: tree despawned.");
                return true;
            }

            // === Duration / XP timeout check ===
            long elapsed = System.currentTimeMillis() - start;
            boolean noXpTooLong = lastXpGain.timeElapsed() > 30_000;

            if (elapsed > maxChopDuration) {
                script.log(getClass(), "Chop stopped: exceeded max chop duration.");
                return true;
            }

            if (noXpTooLong) {
                script.log(getClass(), "Chop stopped: no XP gain for " + lastXpGain.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxChopDuration);
    }

    private boolean walkWithShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (bankingArea.contains(myPos)) {
            task = "Use shortcut";

            // Search for Hole object at base tile 3712, 3828 with action "Climb-through"
            List<RSObject> holes = script.getObjectManager().getObjects(obj -> {
                if (obj.getName() == null || obj.getActions() == null) {
                    return false;
                }
                return obj.getName().equals("Hole")
                        && Arrays.asList(obj.getActions()).contains("Climb through")
                        && obj.getWorldPosition().getX() == 3714
                        && obj.getWorldPosition().getY() == 3816;
            });

            if (holes.isEmpty()) {
                script.log(getClass(), "No Hole object found at base tile (3714,3816).");
                return false;
            }

            RSObject hole = (RSObject) script.getUtils().getClosest(holes);
            if (hole == null) {
                script.log(getClass(), "Closest Hole object is null.");
                return false;
            }

            // Interact with Hole
            task = "Climbing through hole";
            if (!hole.interact("Climb through")) {
                script.log(getClass(), "Failed to climb through Hole.");
                return false;
            }

            return script.pollFramesHuman(() -> {
                WorldPosition currentPos = script.getWorldPosition();
                return currentPos != null && !bankingArea.contains(currentPos);
            }, script.random(7000, 12000));
        } else {
            return walkWithoutShortcut();
        }
    }

    private boolean walkWithoutShortcut() {
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        task = "Walk to chopping area";
        script.getWalker().walkTo(choppingArea.getRandomPosition());

        WorldPosition currentPos = script.getWorldPosition();

        script.pollFramesHuman(() -> currentPos != null && choppingArea.contains(currentPos), script.random(600, 1200));
        return true;
    }

    private RSObject findTree(SearchablePixel[] cluster) {
        List<RSObject> patches = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) return false;
            return obj.getName().equalsIgnoreCase("Tree patch")
                    && Arrays.asList(obj.getActions()).contains("Guide");
        });

        if (patches.isEmpty()) {
            script.log(getClass(), "No valid trees found nearby.");
            return null;
        }

        for (RSObject patch : patches) {
            Polygon hull = patch.getConvexHull();
            if (hull == null) continue;

            List<Point> matches = script.getPixelAnalyzer()
                    .findPixelsOnGameScreen(hull, cluster);

            if (matches != null && !matches.isEmpty()) {
                script.log(getClass(), "Tree at " + patch.getWorldPosition()
                        + " has " + matches.size() + " color matches.");
                return patch;
            }
        }

        return null;
    }

    private boolean isTreeStillPresent(RSObject targetTree, SearchablePixel[] cluster) {
        if (targetTree == null) return false;

        Polygon hull = targetTree.getConvexHull();
        if (hull == null) {
            script.log(getClass(), "Target tree has no convex hull anymore.");
            return false;
        }

        List<Point> matches = script.getPixelAnalyzer()
                .findPixelsOnGameScreen(hull, cluster);

        return matches != null && !matches.isEmpty();
    }

    private MenuHook getMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String text = entry.getRawText().toLowerCase();
                if (text.startsWith("chop down")) {
                    return entry;
                }
            }
            return null;
        };
    }
}