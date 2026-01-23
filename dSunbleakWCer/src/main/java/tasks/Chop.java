package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static main.dSunbleakWCer.*;

public class Chop extends Task {
    private static final long IRONWOOD_RESPAWN_MS = 120_000;

    private static final List<WorldPosition> PRIORITY_TREES = List.of(
            new WorldPosition(2203, 2316, 0),
            new WorldPosition(2207, 2313, 0),
            new WorldPosition(2210, 2317, 0),
            new WorldPosition(2211, 2320, 0)
    );

    public static Map<WorldPosition, Long> depletedTrees = new HashMap<>();

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

        usedBasketAlready = false;

        // === New tree detection via RSObject + convex hull color check ===
        task = "Find tree";
        RSObject tree = findIronwoodTree();

        if (tree == null) {
            script.log("CHOP", "No available Ironwood trees. Waiting...");
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(800, 1200));
            return false;
        }

        // === Interact with tree ===
        task = "Chop tree";
        if (!tryChopIronwood(tree)) {
            script.log("CHOP", "Could not chop Ironwood tree.");
            return false;
        }

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(32907));

        waitTillStopped();
        waitUntilFinishedChopping(startSnapshot);

        // === Small chance for additional delay ===
        if (RandomUtils.uniformRandom(1, 100) <= 15) {
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1, 100));
        }

        return true;
    }

    private void waitTillStopped() {
        task = "Wait till stopped";
        script.log("CHOP", "Waiting until player stops moving...");

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

    private void waitUntilFinishedChopping(ItemGroupResult startSnapshot) {

        int maxChopDuration = RandomUtils.uniformRandom(260_000, 285_000);

        if (startSnapshot == null) {
            script.log("CHOP", "Aborting chop check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(32907));

        long start = System.currentTimeMillis();

        // === INITIAL: wait to let user start chopping ===
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(2750, 4000));

        long[] animClearSince = { -1 };
        long animCheckMs = RandomUtils.uniformRandom(800, 1500);

        // === Main monitoring loop ===
        script.pollFramesHuman(() -> {
            boolean gainedXP = false;

            // === Inventory check ===
            ItemGroupResult currentInv = script.getWidgetManager().getInventory().search(Set.of(32907));
            if (currentInv == null) {
                script.log("CHOP", "Chop stopped: inventory became inaccessible.");
                return true;
            }
            if (currentInv.isFull()) {
                script.log("CHOP", "Chop stopped: inventory is full.");
                return true;
            }

            // === Dialogue check for level up ===
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.log("CHOP", "Dialogue detected, leveled up?");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            // === Inventory count tracking ===
            int currentCount = currentInv.getAmount(32907);
            int lastCount = previousCount.get();
            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;
                previousCount.set(currentCount);
                logsChopped += gained;
                script.log("CHOP", "+" + gained + " logs chopped! (" + logsChopped + " total)");
                lastXpGain.reset();
            } else if (currentCount < lastCount) {
                script.log("CHOP", "Detected log count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
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
                script.log("CHOP",
                        "Chop stopped: no animation for " +
                                (nowMs - animClearSince[0]) + "ms.");
                return true;
            }

            // === Duration / XP timeout check ===
            long elapsed = System.currentTimeMillis() - start;
            boolean noXpTooLong = lastXpGain.timeElapsed() > 75_000;

            if (elapsed > maxChopDuration) {
                script.log("CHOP", "Chop stopped: exceeded max chop duration.");
                return true;
            }

            if (noXpTooLong) {
                script.log("CHOP", "Chop stopped: no XP gain for " + lastXpGain.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxChopDuration);
    }

    private RSObject findIronwoodTree() {
        cleanupDepletedTrees();

        List<RSObject> trees = script.getObjectManager().getObjects(obj ->
                obj.getName() != null &&
                        obj.getName().equalsIgnoreCase("Ironwood tree")
        );

        if (trees.isEmpty()) {
            script.log("CHOP", "No Ironwood trees found.");
            return null;
        }

        // Priority trees first
        for (WorldPosition wp : PRIORITY_TREES) {
            RSObject t = getTreeAtPosition(trees, wp);
            if (t == null) continue;
            if (isDepleted(t)) continue;

            script.log("CHOP", "Candidate priority Ironwood tree @ " + wp);
            return t;
        }

        // Fallback: closest non-depleted
        WorldPosition me = script.getWorldPosition();

        return trees.stream()
                .filter(t -> !isDepleted(t))
                .min(Comparator.comparingDouble(
                        t -> t.getWorldPosition().distanceTo(me)))
                .orElse(null);
    }

    private void cleanupDepletedTrees() {
        long now = System.currentTimeMillis();
        depletedTrees.entrySet().removeIf(e -> now - e.getValue() >= IRONWOOD_RESPAWN_MS);
    }

    private RSObject getTreeAtPosition(List<RSObject> trees, WorldPosition wp) {
        return trees.stream()
                .filter(t -> wp.equals(t.getWorldPosition()))
                .findFirst()
                .orElse(null);
    }

    private boolean isDepleted(RSObject tree) {
        WorldPosition pos = tree.getWorldPosition();
        Long depletedAt = depletedTrees.get(pos);

        if (depletedAt == null) return false;

        long elapsed = System.currentTimeMillis() - depletedAt;
        if (elapsed >= IRONWOOD_RESPAWN_MS) {
            depletedTrees.remove(pos);
            return false;
        }
        return true;
    }

    private void markTreeDepleted(RSObject tree) {
        WorldPosition pos = tree.getWorldPosition();
        if (!depletedTrees.containsKey(pos)) {
            depletedTrees.put(pos, System.currentTimeMillis());
            script.log("CHOP", "Marked Ironwood tree depleted at " + pos);
        }
    }

    private static MenuHook getIronwoodMenuHook(
            AtomicReference<String> selected,
            AtomicReference<List<MenuEntry>> lastMenu,
            RSObject obj
    ) {
        return menuEntries -> {

            selected.set(null);
            lastMenu.set(menuEntries);

            if (menuEntries == null) return null;

            String target = obj.getName().toLowerCase().trim();
            String singular = target.endsWith("s")
                    ? target.substring(0, target.length() - 1)
                    : target;

            MenuEntry best = null;
            int bestScore = Integer.MIN_VALUE;

            for (MenuEntry entry : menuEntries) {

                String action = entry.getAction().toLowerCase();
                String entity = entry.getEntityName().toLowerCase();

                // ONLY allow chop
                if (!action.startsWith("chop down")) {
                    continue;
                }

                int score = 0;

                // Strongly prefer exact entity matches
                if (entity.equals(target)) score += 120;
                if (entity.equals(singular)) score += 110;
                if (entity.contains(target)) score += 90;

                if (score > bestScore) {
                    bestScore = score;
                    best = entry;
                }
            }

            if (best == null) {
                return null;
            }

            selected.set(best.getAction());
            return best;
        };
    }

    private boolean tryChopIronwood(RSObject tree) {

        if (!tree.isInteractableOnScreen()) {
            script.log("CHOP",
                    "Ironwood not fully on screen → walking closer to " +
                            tree.getWorldPosition());

            WalkConfig cfg = new WalkConfig.Builder()
                    .disableWalkScreen(true)
                    .breakCondition(() -> {
                        Polygon h = tree.getConvexHull();
                        return h != null &&
                                script.getWidgetManager()
                                        .insideGameScreenFactor(
                                                h,
                                                List.of(ChatboxComponent.class)
                                        ) >= 1.0;
                    })
                    .enableRun(true)
                    .build();

            script.getWalker().walkTo(tree.getWorldPosition(), cfg);
            return false;
        }

        Polygon poly = tree.getConvexHull();
        if (poly == null) {
            script.log("CHOP", "Ironwood convex hull is null, retrying...");
            return false;
        }

        if (!isPolygonTapSafe(poly)) {
            script.log("CHOP",
                    "Ironwood hull outside screen bounds → repositioning @ " +
                            tree.getWorldPosition());
            WalkConfig cfg = new WalkConfig.Builder()
                    .disableWalkScreen(true)
                    .enableRun(true)
                    .build();

            script.getWalker().walkTo(tree.getWorldPosition(), cfg);
            return false;
        }

        AtomicReference<String> selected = new AtomicReference<>(null);
        AtomicReference<List<MenuEntry>> menuCache = new AtomicReference<>(null);

        MenuHook hook = getIronwoodMenuHook(selected, menuCache, tree);

        boolean chopped = script.getFinger().tap(poly, hook);

        if (chopped) {
            script.log("CHOP",
                    "Selected chop action: " + selected.get() +
                            " @ " + tree.getWorldPosition());
            return true;
        }

        // ---- Tap failed → inspect menu ----
        List<MenuEntry> menu = menuCache.get();
        if (menu == null) return false;

        boolean hasChop = false;
        boolean hasExamine = false;

        for (MenuEntry e : menu) {
            String txt = e.getRawText().toLowerCase();
            if (txt.startsWith("chop down")) hasChop = true;
            if (txt.startsWith("examine")) hasExamine = true;
        }

        // === Examine-only → stump ===
        if (!hasChop && hasExamine) {
            script.log("CHOP",
                    "Ironwood stump detected → marking depleted @ " +
                            tree.getWorldPosition());
            markTreeDepleted(tree);
        }

        return false;
    }

    private boolean isPolygonTapSafe(Polygon poly) {
        if (poly == null || poly.numVertices() == 0) {
            return false;
        }

        int[] xs = poly.getXPoints();
        int[] ys = poly.getYPoints();

        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int y = ys[i];

            if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
                return false;
            }
        }
        return true;
    }
}