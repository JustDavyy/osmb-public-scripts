package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.triangle.Triangle;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static main.dCamTorumMiner.*;

public class MineTask extends Task {
    public static final int BLACKLIST_TIMEOUT = 22000;
    private final Map<WorldPosition, Long> objectPositionBlacklist = new HashMap<>();
    private final Set<RSObject> skippedByPlayers = new HashSet<>();

    public MineTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return true;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();

        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Get world position";
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        if (!miningArea.contains(myPos)) {
            task = "Walk to mining area";
            script.log(getClass().getSimpleName(), "Walk to mining area");

            WalkConfig.Builder walkConfig = new WalkConfig.Builder();
            walkConfig.breakCondition(() -> {
                WorldPosition currentPos = script.getWorldPosition();
                return currentPos != null && miningArea.contains(currentPos);
            });

            return script.getWalker().walkTo(miningArea.getRandomPosition(), walkConfig.build());
        }

        if (useWDH) {
            if (checkAndDoWDHAction()) {
                script.log("WDH", "Player(s) detected in " + miningLocation + " mining area! Hopping world.");
                script.getProfileManager().forceHop();
                return false;
            }
        }

        task = "Clear skipped object list";
        skippedByPlayers.clear();

        task = "Get veins";
        List<RSObject> veins = getVeins();
        task = "Get active veins";
        List<RSObject> activeVeins = getActiveVeinsOnScreen(veins, myPos);

        UIResultList<WorldPosition> playerPositions = script.getWidgetManager().getMinimap().getPlayerPositions();
        List<RSObject> availableVeins = new ArrayList<>();

        task = "Process all veins";
        for (RSObject vein : activeVeins) {
            WorldPosition pos = vein.getWorldPosition();
            boolean beingMined = false;

            for (WorldPosition playerPos : playerPositions) {
                int plane = pos.getPlane();

                if ((pos.getY() == 9546 && playerPos.equals(new WorldPosition(pos.getX(), pos.getY() - 1, plane))) ||
                        (pos.getX() == 1521 && playerPos.equals(new WorldPosition(pos.getX() - 1, pos.getY(), plane))) ||
                        (pos.getX() == 1497 && playerPos.equals(new WorldPosition(pos.getX() + 1, pos.getY(), plane)))) {
                    script.log(getClass(), "Skipping vein at " + pos + " due to player already mining it.");
                    beingMined = true;
                    break;
                }
            }

            if (!beingMined) {
                availableVeins.add(vein);
            } else {
                skippedByPlayers.add(vein);
            }
        }

        task = "Get target vein";
        RSObject targetVein = null;
        if (!availableVeins.isEmpty()) {
            targetVein = availableVeins.get(0);
        } else if (!activeVeins.isEmpty()) {
            script.log(getClass(), "Only available veins are being mined. Proceeding anyway.");
            targetVein = activeVeins.get(0);
        } else {
            script.log(getClass(), "No veins found on screen. Hopping worlds.");
            script.getProfileManager().forceHop();
            return false;
        }


        task = "Draw active veins";
        drawActiveVeins(activeVeins, targetVein);

        Polygon poly = targetVein.getConvexHull();
        if (poly == null) return false;

        MenuHook hook = getVeinMenuHook();
        if (!script.getFinger().tapGameScreen(poly, hook)) {
            objectPositionBlacklist.put(targetVein.getWorldPosition(), System.currentTimeMillis());
            return false;
        }

        task = "Wait until interrupt";
        waitUntilFinishedMining(targetVein);
        task = "Add vein to blocklist";
        objectPositionBlacklist.put(targetVein.getWorldPosition(), System.currentTimeMillis());
        return true;
    }

    private List<RSObject> getVeins() {
        List<WorldPosition> respawnCircles = getRespawnCirclePositions();

        return script.getObjectManager().getObjects(o -> {
            WorldPosition position = o.getWorldPosition();

            if (position.getY() == 9537 || respawnCircles.contains(position)) {
                return false;
            }

            Long time = objectPositionBlacklist.get(position);
            if (time != null) {
                if ((System.currentTimeMillis() - time) < BLACKLIST_TIMEOUT) {
                    return false;
                } else {
                    objectPositionBlacklist.remove(position);
                }
            }

            return o.getName() != null && o.getName().equalsIgnoreCase("Calcified rocks")
                    && o.getActions() != null && Arrays.asList(o.getActions()).contains("Mine")
                    && o.canReach();
        });
    }

    private List<WorldPosition> getRespawnCirclePositions() {
        List<Rectangle> respawnCircles = script.getPixelAnalyzer().findRespawnCircles();
        return script.getUtils().getWorldPositionForRespawnCircles(respawnCircles, 20);
    }

    private List<RSObject> getActiveVeinsOnScreen(List<RSObject> veins, WorldPosition myPosition) {
        List<RSObject> active = new ArrayList<>(veins);
        active.removeIf(o -> !o.isInteractableOnScreen());
        active.sort(Comparator.comparingDouble(o -> o.getWorldPosition().distanceTo(myPosition)));
        return active;
    }

    private MenuHook getVeinMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().equalsIgnoreCase("mine calcified rocks")) {
                    return entry;
                }
            }
            return null;
        };
    }

    private void drawActiveVeins(List<RSObject> veins, RSObject target) {
        script.getScreen().queueCanvasDrawable("ActiveVeins", canvas -> {
            for (RSObject vein : veins) {
                if (vein.getFaces() == null) continue;

                Color color = Color.GREEN;
                if (vein.equals(target)) {
                    color = Color.CYAN;
                } else if (skippedByPlayers.contains(vein)) {
                    color = Color.ORANGE;
                }

                for (Triangle t : vein.getFaces()) {
                    canvas.drawPolygon(t.getXPoints(), t.getYPoints(), 3, color.getRGB());
                }
            }

            for (Map.Entry<WorldPosition, Long> entry : objectPositionBlacklist.entrySet()) {
                Polygon poly = script.getSceneProjector().getTileCube(entry.getKey(), 150);
                if (poly != null) {
                    canvas.fillPolygon(poly, Color.RED.getRGB(), 0.7f);
                }
            }
        });
    }

    private void waitUntilFinishedMining(RSObject vein) {
        AtomicInteger localMinedCount = new AtomicInteger(0);
        int maxMiningDuration = RandomUtils.uniformRandom(240_000, 270_000);

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BLESSED_BONE_SHARDS));
        if (startSnapshot == null) {
            script.log(getClass(), "Aborting mining check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(ItemID.BLESSED_BONE_SHARDS));

        WorldPosition playerTile = script.getWorldPosition();
        if (playerTile == null) {
            script.log(getClass(), "Aborting mining check: player tile is null.");
            return;
        }

        Polygon playerTileResized = script.getSceneProjector().getTileCube(playerTile, 120).getResized(0.7);
        WorldPosition veinPosition = vein.getWorldPosition();

        Timer animationTimer = new Timer();
        Timer debounceTimer = new Timer();
        Timer veinRefreshTimer = new Timer();

        long start = System.currentTimeMillis();

        final long gracePeriodMs = RandomUtils.uniformRandom(3500, 4500);
        final long maxNoAnimTime = RandomUtils.uniformRandom(6000, 8000);

        script.pollFramesHuman(() -> {
            // === Every ~5 seconds, refresh veins and redraw overlays ===
            if (veinRefreshTimer.timeElapsed() > 5000) {
                try {
                    WorldPosition myPos = script.getWorldPosition();
                    if (myPos != null) {
                        List<RSObject> veins = getVeins();
                        List<RSObject> activeVeins = getActiveVeinsOnScreen(veins, myPos);
                        drawActiveVeins(activeVeins, vein);
                        script.log(getClass(), "Refreshed and redrew veins (" + activeVeins.size() + " active).");
                    }
                } catch (Exception ex) {
                    script.log(getClass(), "Error during vein refresh: " + ex.getMessage());
                }
                veinRefreshTimer.reset();
            }

            ItemGroupResult currentInv = script.getWidgetManager().getInventory()
                    .search(Set.of(ItemID.BLESSED_BONE_SHARDS));

            if (currentInv == null) {
                script.log(getClass(), "Mining stopped: inventory became inaccessible.");
                return true;
            }

            if (currentInv.isFull()) {
                script.log(getClass(), "Mining stopped: inventory is full.");
                return true;
            }

            if (getRespawnCirclePositions().contains(veinPosition)) {
                script.log(getClass(), "Mining stopped: respawn circle detected at " + veinPosition);
                return true;
            }

            boolean isAnimating = script.getPixelAnalyzer().isAnimating(0.4, playerTileResized);
            if (isAnimating && debounceTimer.timeElapsed() > 500) {
                animationTimer.reset();
                debounceTimer.reset();
            }

            int currentCount = currentInv.getAmount(ItemID.BLESSED_BONE_SHARDS);
            int lastCount = previousCount.get();

            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;

                // Allow large increments that are exact multiples of 1000
                boolean isStackedIncrement = lastCount >= 100_000 && gained % 1000 == 0 && gained <= 10_000;

                if (gained > 200 && !isStackedIncrement) {
                    script.log(getClass(), "Ignored suspicious shard jump: +" + gained +
                            " (from " + lastCount + " to " + currentCount + ")");
                } else {
                    previousCount.set(currentCount);
                    blessedShardCount += gained;
                    localMinedCount.addAndGet(gained);
                    animationTimer.reset();
                    debounceTimer.reset();
                    lastXpGain.reset();
                    script.log(getClass(), "+" + gained + " blessed shard(s) mined! (" + blessedShardCount + " in total)");
                }
            } else if (currentCount < lastCount) {
                script.log(getClass(), "Detected shard count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
                previousCount.set(currentCount);
            }

            long elapsed = System.currentTimeMillis() - start;
            boolean graceOver = elapsed > gracePeriodMs;
            boolean animStale = animationTimer.timeElapsed() > maxNoAnimTime;
            boolean trustStale = lastXpGain.timeElapsed() > RandomUtils.uniformRandom(8000, 11000);

            if (elapsed > maxMiningDuration) {
                script.log(getClass(), "Mining stopped: exceeded max mining duration.");
                return true;
            }

            if (graceOver && animStale && trustStale) {
                script.log(getClass(), "Mining stopped: no animation for " + animationTimer.timeElapsed() +
                        "ms and no XP gain for " + lastXpGain.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxMiningDuration);

        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(300, 800));
    }

    private boolean checkAndDoWDHAction() {
        UIResultList<WorldPosition> playerPositions =
                script.getWidgetManager().getMinimap().getPlayerPositions();

        if (playerPositions.isNotVisible() || playerPositions.isNotFound()) {
            return false;
        }

        // Count players inside the wdh area
        long playersInside = playerPositions.asList().stream()
                .filter(wdhArea::contains)
                .count();

        if (playersInside >= theWDHThreshold) {
            script.log(getClass(), "WDH → " + playersInside +
                    " players detected (threshold=" + theWDHThreshold + "). Hopping world...");
            script.getProfileManager().forceHop();
            return true;
        }

        return false;
    }
}
