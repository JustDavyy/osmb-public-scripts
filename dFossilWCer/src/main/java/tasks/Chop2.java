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
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static main.dFossilWCer.*;

public class Chop2 extends Task {
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

    private static final Area eastArea = new RectangleArea(3710, 3831, 3, 5, 0);

    private final Map<WorldPosition, Long> depletedTrees = new HashMap<>();

    private static final int TEAK_RESPAWN_MS = 9_000;
    private static final int MAHOGANY_RESPAWN_MS = 15_000;

    public Chop2(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return logsId == ItemID.TEAK_LOGS || logsId == ItemID.MAHOGANY_LOGS;
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

        // === Use spec if needed/enabled ===
        if (useSpecialAttack && needToSpec) {
            // Click spec orb
            script.getWidgetManager().getMinimapOrbs().setSpecialAttack(true, true);

            // Verify spec actually triggered (percentage dropped below 100) — wait up to 3–5s
            boolean activated = script.pollFramesUntil(() -> {
                int specPct = script.getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
                return (specPct != -1 && specPct < 100);
            }, RandomUtils.uniformRandom(3000, 5000));

            if (!activated) {
                script.log("Chop2", "Failed to activate special attack, retry!");
                script.getWidgetManager().getMinimapOrbs().setSpecialAttack(true, true);
            }

            // Re-verify after retry attempt (again 3–5s)
            activated = script.pollFramesUntil(() -> {
                int specPct = script.getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
                return (specPct != -1 && specPct < 100);
            }, RandomUtils.uniformRandom(3000, 5000));

            if (activated) {
                nextSpecAt = -1;
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
            script.log("Chop2", "Invalid tree ID selected: " + logsId);
            return false;
        }

        // === New tree detection via RSObject + convex hull color check ===
        task = "Find tree";
        RSObject treePatch = findTree(clusterToUse);

        if (treePatch == null) {
            // Check if all trees are depleted already
            if (handleAllTreesDepleted()) {
                return false;
            }

            // Reposition to east area if not already there
            if (walkToEastAreaIfNeeded()) {
                return false;
            }

            script.log("Chop2", "No valid tree patch found. Fail count = " + clusterFailCount);

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(400, 800));
            return false;
        }

        clusterFailCount = 0;

        // === Interact with tree ===
        task = "Chop tree";
        if (!treePatch.interact(getMenuHook(treePatch))) {
            script.log("Chop2", "Failed to interact with tree.");
            return false;
        } else {
            targetTree = treePatch;
        }

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(logsId));

        waitTillStopped();
        waitUntilFinishedChopping(clusterToUse, startSnapshot);

        // === Small chance for additional delay ===
        if (RandomUtils.uniformRandom(1, 100) <= 15) {
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1, 100));
        }

        return true;
    }

    private boolean handleAllTreesDepleted() {

        if (depletedTrees.size() < treeAmount) {
            return false;
        }

        int respawnMs = getRespawnTimeMs();
        long now = System.currentTimeMillis();

        WorldPosition soonestTree = null;
        long soonestReady = Long.MAX_VALUE;

        for (var e : depletedTrees.entrySet()) {
            long readyAt = e.getValue() + respawnMs;
            if (readyAt < soonestReady) {
                soonestReady = readyAt;
                soonestTree = e.getKey();
            }
        }

        long waitMs = soonestReady - now + RandomUtils.uniformRandom(0, 5_000);
        if (waitMs <= 0) {
            depletedTrees.clear();
            return false;
        }

        task = "Wait for respawn (" + (waitMs / 1000) + "s)";
        script.log("Chop2", "All trees depleted, waiting " + waitMs + "ms");

        if (treeAmount > 1 && soonestTree != null) {
            script.getWalker().walkTo(soonestTree);
        }

        script.pollFramesHuman(() -> false, (int) waitMs);

        depletedTrees.entrySet().removeIf(e ->
                now - e.getValue() > respawnMs
        );

        return true;
    }

    private void waitTillStopped() {
        task = "Wait till stopped";
        script.log("Chop2", "Waiting until player stops moving...");

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

    private void waitUntilFinishedChopping(SearchablePixel[] cluster, ItemGroupResult startSnapshot) {

        int maxChopDuration = RandomUtils.uniformRandom(240_000, 270_000);

        if (startSnapshot == null) {
            script.log("Chop2", "Aborting chop check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(logsId));

        long start = System.currentTimeMillis();

        // === INITIAL: wait to let user start chopping ===
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(2750, 4000));

        long[] animClearSince = { -1 };
        long animCheckMs = RandomUtils.uniformRandom(800, 1500);

        // === Main monitoring loop ===
        script.pollFramesHuman(() -> {
            boolean gainedXP = false;

            // === Inventory check ===
            ItemGroupResult currentInv = script.getWidgetManager().getInventory().search(Set.of(logsId));
            if (currentInv == null) {
                script.log("Chop2", "Chop stopped: inventory became inaccessible.");
                return true;
            }
            if (currentInv.isFull()) {
                script.log("Chop2", "Chop stopped: inventory is full.");
                return true;
            }

            // === Dialogue check for level up ===
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log("Chop2", "Dialogue detected, leveled up?");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            // === Special attack counter ===
            if (useSpecialAttack) {
                int specPct = script.getWidgetManager()
                        .getMinimapOrbs()
                        .getSpecialAttackPercentage();

                long nowMs = System.currentTimeMillis();

                // Spec just became ready → schedule usage
                if (specPct == 100 && nextSpecAt == -1) {
                    int delay = RandomUtils.uniformRandom(
                            SPEC_MIN_DELAY_MS,
                            SPEC_MAX_DELAY_MS
                    );

                    nextSpecAt = nowMs + delay;

                    script.log(
                            "Chop2",
                            "Spec is 100%. Scheduling special attack in "
                                    + delay + "ms"
                    );
                }

                // Timer reached → stop chopping so spec can be used
                if (nextSpecAt != -1 && nowMs >= nextSpecAt) {
                    script.log(
                            "Chop2",
                            "Special attack timer reached. Stopping chop to use spec."
                    );

                    needToSpec = true;
                    return true;
                }
            }

            // === Inventory count tracking ===
            int currentCount = currentInv.getAmount(logsId);
            int lastCount = previousCount.get();
            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;
                previousCount.set(currentCount);
                lastXpGain.reset();
            } else if (currentCount < lastCount) {
                script.log("Chop2", "Detected log count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
                previousCount.set(currentCount);
            }

            // ---- animation check ----
            long nowMs = System.currentTimeMillis();

            boolean animating =
                    script.getPixelAnalyzer().isPlayerAnimating(0.35);

            if (animating) {
                // Reset animation-clear timer while animating
                animClearSince[0] = -1;
                return false;
            }

            // Animation just stopped → start grace timer
            if (animClearSince[0] == -1) {
                animClearSince[0] = nowMs;
                return false;
            }

            // Animation has been clear long enough → stop chopping
            boolean animClearLongEnough =
                    nowMs - animClearSince[0] >= animCheckMs;

            if (animClearLongEnough) {
                script.log("Chop2",
                        "Chop stopped: no animation for " +
                                (nowMs - animClearSince[0]) + "ms.");
                markTreeDepleted(targetTree);
                return true;
            }

            // === Tree presence check ===
            if (!isTreeStillPresent(targetTree, cluster)) {
                script.log("Chop2", "Chop stopped: tree despawned.");
                markTreeDepleted(targetTree);
                return true;
            }

            // === Duration / XP timeout check ===
            long elapsed = System.currentTimeMillis() - start;
            boolean noXpTooLong = lastXpGain.timeElapsed() > 30_000;

            if (elapsed > maxChopDuration) {
                script.log("Chop2", "Chop stopped: exceeded max chop duration.");
                return true;
            }

            if (noXpTooLong) {
                script.log("Chop2", "Chop stopped: no XP gain for " + lastXpGain.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxChopDuration);
    }

    private void markTreeDepleted(RSObject tree) {
        if (tree == null) return;

        WorldPosition pos = tree.getWorldPosition();
        if (!depletedTrees.containsKey(pos)) {
            depletedTrees.put(pos, System.currentTimeMillis());
            script.log("Chop2", "Tree depleted at " + pos);
        }
    }

    private int getRespawnTimeMs() {
        if (logsId == ItemID.TEAK_LOGS) return TEAK_RESPAWN_MS;
        if (logsId == ItemID.MAHOGANY_LOGS) return MAHOGANY_RESPAWN_MS;
        return 0;
    }

    private boolean walkToEastAreaIfNeeded() {
        WorldPosition pos = script.getWorldPosition();
        if (pos != null && eastArea.contains(pos)) {
            return false;
        }

        task = "Walk to east tree area";
        script.log("Chop2", "No tree found, repositioning to east area.");

        script.getWalker().walkTo(eastArea.getRandomPosition());

        script.pollFramesHuman(() -> {
            WorldPosition p = script.getWorldPosition();
            return p != null && eastArea.contains(p);
        }, RandomUtils.uniformRandom(6000, 9000));

        waitTillStopped();
        return true;
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
                script.log("Chop2", "No Hole object found at base tile (3714,3816).");
                return false;
            }

            RSObject hole = (RSObject) script.getUtils().getClosest(holes);
            if (hole == null) {
                script.log("Chop2", "Closest Hole object is null.");
                return false;
            }

            // Interact with Hole
            task = "Climbing through hole";
            if (!hole.interact("Climb through")) {
                script.log("Chop2", "Failed to climb through Hole.");
                return false;
            }

            return script.pollFramesHuman(() -> {
                WorldPosition currentPos = script.getWorldPosition();
                return currentPos != null && !bankingArea.contains(currentPos);
            }, RandomUtils.uniformRandom(7000, 12000));
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

        script.pollFramesHuman(() -> currentPos != null && choppingArea.contains(currentPos), RandomUtils.uniformRandom(600, 1200));
        return true;
    }

    private RSObject findTree(SearchablePixel[] cluster) {
        List<RSObject> patches = script.getObjectManager().getObjects(obj -> {
            if (obj.getName() == null || obj.getActions() == null) return false;
            return obj.getName().equalsIgnoreCase("Tree patch")
                    && Arrays.asList(obj.getActions()).contains("Guide");
        });

        if (patches.isEmpty()) {
            script.log("Chop2", "No valid trees found nearby.");
            return null;
        }

        patches.sort(Comparator.comparingInt(
                (RSObject o) -> o.getWorldPosition().getX()
        ).reversed());

        for (RSObject patch : patches) {

            WorldPosition pos = patch.getWorldPosition();
            if (depletedTrees.containsKey(pos)) {
                continue;
            }

            if (!patch.isInteractableOnScreen()) {
                continue;
            }

            Polygon hull = patch.getConvexHull();
            if (hull == null) continue;

            hull = hull.getResized(1.9);

            if (hull == null) continue;

            List<Point> matches = script.getPixelAnalyzer()
                    .findPixelsOnGameScreen(hull, cluster);

            if (matches != null && !matches.isEmpty()) {
                script.log("Chop2",
                        "Tree selected at " + pos + " (matches=" + matches.size() + ")");
                return patch;
            }
        }

        return null;
    }

    private boolean isTreeStillPresent(RSObject targetTree, SearchablePixel[] cluster) {
        if (targetTree == null) return false;

        Polygon hull = targetTree.getConvexHull();
        if (hull == null) {
            script.log("Chop2", "Target tree has no convex hull anymore.");
            return false;
        }

        List<Point> matches = script.getPixelAnalyzer()
                .findPixelsOnGameScreen(hull, cluster);

        return matches != null && !matches.isEmpty();
    }

    private MenuHook getMenuHook(RSObject tree) {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String text = entry.getRawText().toLowerCase();
                if (text.startsWith("chop down")) {
                    return entry;
                }
                if (text.startsWith("clear")) {
                    markTreeDepleted(tree);
                    return null;
                }
            }
            return null;
        };
    }
}