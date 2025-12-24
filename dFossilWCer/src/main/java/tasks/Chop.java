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
import java.util.*;
import java.util.List;
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

    private static final SearchablePixel[] CAMPHOR_PIXEL_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-12701654, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-11321288, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-11781325, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10532032, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10729410, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10926277, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-12241361, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-11518411, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-11123655, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-11978448, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10334912, new SingleThresholdComparator(2), ColorModel.RGB),
    };

    private static final SearchablePixel[] CAMPHOR_DEPLETED_PIXEL_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-9150132, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9018290, new SingleThresholdComparator(2), ColorModel.RGB),
    };

    private static final SearchablePixel[] IRONWOOD_PIXEL_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-14868969, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-6645622, new SingleThresholdComparator(5), ColorModel.RGB),
    };

    private static final SearchablePixel[] ROSEWOOD_PIXEL_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-12306890, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-11057852, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-11846853, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-13687258, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-11452352, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-12701136, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-13227221, new SingleThresholdComparator(5), ColorModel.RGB),
            new SearchablePixel(-14278131, new SingleThresholdComparator(5), ColorModel.RGB),
    };

    private static final SearchablePixel[] ROSEWOOD_DEPLETED_PIXEL_CLUSTER = new SearchablePixel[] {
            new SearchablePixel(-8696765, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10733261, new SingleThresholdComparator(2), ColorModel.RGB),
    };

    private static final Area choppingArea = new RectangleArea(3699, 3830, 20, 10, 0);
    private static final Area bankingArea = new RectangleArea(3708, 3797, 42, 21, 0);

    private final Map<WorldPosition, Long> depletedTrees = new HashMap<>();
    private static final int TEAK_RESPAWN_MS      = 9_000;
    private static final int MAHOGANY_RESPAWN_MS  = 8_400;
    private static final int CAMPHOR_RESPAWN_MS  = 60_000;
    private static final int IRONWOOD_RESPAWN_MS = 120_000;
    private static final int ROSEWOOD_RESPAWN_MS = 123_000;

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

        SearchablePixel[] depletedCluster = null;

        if (logsId == 32904) depletedCluster = CAMPHOR_DEPLETED_PIXEL_CLUSTER;
        if (logsId == 32910) depletedCluster = ROSEWOOD_DEPLETED_PIXEL_CLUSTER;

        usedBasketAlready = false;

        // === Pick correct pixel cluster ===
        SearchablePixel[] clusterToUse;
        if (logsId == ItemID.TEAK_LOGS) {
            clusterToUse = TEAK_PIXEL_CLUSTER;
        } else if (logsId == ItemID.MAHOGANY_LOGS) {
            clusterToUse = MAHOGANY_PIXEL_CLUSTER;
        } else if (logsId == 32904) {
            clusterToUse = CAMPHOR_PIXEL_CLUSTER;
        } else if (logsId == 32907) {
            clusterToUse = IRONWOOD_PIXEL_CLUSTER;
        } else if (logsId == 32910) {
            clusterToUse = ROSEWOOD_PIXEL_CLUSTER;
        } else {
            script.log(getClass(), "Invalid tree ID selected: " + logsId);
            return false;
        }

        // === New tree detection via RSObject + convex hull color check ===
        task = "Find tree";
        RSObject treePatch = findTree(clusterToUse, depletedCluster);

        if (treePatch == null) {
            if (handleAllTreesDepleted()) {
                return false; // let task repoll after wait
            }
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
            lastXpGain.reset();
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

            // === Depletion detection (overlay-based trees) ===
            if ((logsId == 32904 || logsId == 32910)
                    && isTreeDepleted(
                    targetTree,
                    logsId == 32904
                            ? CAMPHOR_DEPLETED_PIXEL_CLUSTER
                            : ROSEWOOD_DEPLETED_PIXEL_CLUSTER
            )) {

                markTreeDepleted(targetTree, "depleted cluster detected");
                return true;
            }

            // === Despawn-based trees (teak / mahogany / ironwood) ===
            if (!isTreeStillPresent(targetTree, cluster)) {
                markTreeDepleted(targetTree, "cluster gone");
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

    private RSObject findTree(
            SearchablePixel[] aliveCluster,
            SearchablePixel[] depletedCluster
    ){
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

            WorldPosition pos = patch.getWorldPosition();

            // Skip already-known depleted trees
            if (depletedTrees.containsKey(pos)) continue;

            // === Alive check ===
            List<Point> aliveMatches = script.getPixelAnalyzer()
                    .findPixelsOnGameScreen(hull, aliveCluster);

            if (aliveMatches != null && !aliveMatches.isEmpty()) {
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

    private boolean isTreeDepleted(RSObject tree, SearchablePixel[] depletedCluster) {
        Polygon hull = tree.getConvexHull();
        if (hull == null) return false;

        List<Point> matches = script.getPixelAnalyzer()
                .findPixelsOnGameScreen(hull, depletedCluster);

        return matches != null && !matches.isEmpty();
    }

    private void markTreeDepleted(RSObject tree, String reason) {
        WorldPosition pos = tree.getWorldPosition();

        if (!depletedTrees.containsKey(pos)) {
            depletedTrees.put(pos, System.currentTimeMillis());
            script.log(getClass(),
                    "Tree depleted (" + reason + ") at " + pos
            );
        }
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

    private int getRespawnTimeMs() {
        if (logsId == ItemID.TEAK_LOGS) return TEAK_RESPAWN_MS;
        if (logsId == ItemID.MAHOGANY_LOGS) return MAHOGANY_RESPAWN_MS;
        if (logsId == 32904) return CAMPHOR_RESPAWN_MS;
        if (logsId == 32907) return IRONWOOD_RESPAWN_MS;
        if (logsId == 32910) return ROSEWOOD_RESPAWN_MS;
        return 0;
    }

    private boolean handleAllTreesDepleted() {
        if (depletedTrees.size() < treeAmount) {
            return false;
        }

        int respawnMs = getRespawnTimeMs();

        long now = System.currentTimeMillis();

        WorldPosition soonestTree = null;
        long soonestReadyTime = Long.MAX_VALUE;

        for (var entry : depletedTrees.entrySet()) {
            long readyAt = entry.getValue() + respawnMs;
            if (readyAt < soonestReadyTime) {
                soonestReadyTime = readyAt;
                soonestTree = entry.getKey();
            }
        }

        long baseWaitMs = soonestReadyTime - now;
        long randomExtraMs = script.random(0, 5_000);
        long waitMs = baseWaitMs + randomExtraMs;
        if (waitMs <= 0) {
            depletedTrees.clear(); // safety
            return false;
        }

        task = "Waiting for tree respawn (" + (waitMs / 1000) + "s)";
        script.log(getClass(),
                "All trees depleted (" + depletedTrees.size() + "/" + treeAmount +
                        "). Waiting " + waitMs + "ms"
        );

        // === Move closer ONLY if multiple trees exist ===
        if (treeAmount > 1 && soonestTree != null) {
            script.getWalker().walkTo(soonestTree);
        }

        // === Human wait ===
        script.pollFramesHuman(() -> false, (int) waitMs);

        // Clear expired entries
        depletedTrees.entrySet().removeIf(e ->
                now - e.getValue() > respawnMs
        );

        return true;
    }
}