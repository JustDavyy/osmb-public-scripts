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
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import utils.Task;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static main.dAmethystMiner.*;

public class MineTask extends Task {
    public static final int BLACKLIST_TIMEOUT = 60000;
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

        if (!getMiningArea().contains(myPos)) {
            task = "Walk to mining area";
            script.log(getClass().getSimpleName(), "Walk to mining area");
            return script.getWalker().walkTo(getMiningWalkArea().getRandomPosition());
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

                if (diaryAreaMode) {
                    // === West checks ===
                    if (
                            ((pos.getX() == 3011 && pos.getY() == 9722) ||
                                    (pos.getX() == 3010 && pos.getY() == 9720) ||
                                    (pos.getX() == 3009 && pos.getY() == 9718) ||
                                    (pos.getX() == 3010 && pos.getY() == 9716) ||
                                    (pos.getX() == 3011 && pos.getY() == 9714) ||
                                    (pos.getX() == 3012 && pos.getY() >= 9710 && pos.getY() <= 9712) ||
                                    (pos.getX() == 3011 && pos.getY() == 9708) ||
                                    (pos.getX() == 3005 && (pos.getY() == 9711 || pos.getY() == 9710))
                            ) &&
                                    playerPos.equals(new WorldPosition(pos.getX() - 1, pos.getY(), plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (west check).");
                        beingMined = true;
                        break;
                    }

                    // === East checks ===
                    if (
                            ((pos.getX() == 3002 && (pos.getY() == 9723 || pos.getY() == 9724 || pos.getY() == 9713 || pos.getY() == 9714 || pos.getY() == 9708 || pos.getY() == 9709)) ||
                                    (pos.getX() == 3006 && (pos.getY() == 9720 || pos.getY() == 9719)) ||
                                    (pos.getX() == 3001 && pos.getY() == 9711) ||
                                    (pos.getX() == 3008 && (pos.getY() == 9710 || pos.getY() == 9711 || pos.getY() == 9712))
                            ) &&
                                    playerPos.equals(new WorldPosition(pos.getX() + 1, pos.getY(), plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (east check).");
                        beingMined = true;
                        break;
                    }

                    // === North checks ===
                    if (
                            ((pos.getX() == 3007 && pos.getY() == 9713) ||
                                    (pos.getX() >= 3005 && pos.getX() <= 3007 && pos.getY() == 9705)
                            ) &&
                                    playerPos.equals(new WorldPosition(pos.getX(), pos.getY() - 1, plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (north check).");
                        beingMined = true;
                        break;
                    }

                    // === South checks ===
                    if (
                            ((pos.getX() == 3007 && pos.getY() == 9709) ||
                                    (pos.getX() == 3006 && pos.getY() == 9709) ||
                                    (pos.getX() == 3010 && pos.getY() == 9729) ||
                                    (pos.getX() == 3009 && pos.getY() == 9727) ||
                                    (pos.getX() == 3006 && pos.getY() == 9727) ||
                                    (pos.getX() == 3004 && pos.getY() == 9726)
                            ) &&
                                    playerPos.equals(new WorldPosition(pos.getX(), pos.getY() + 1, plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (south check).");
                        beingMined = true;
                        break;
                    }
                } else {
                    // === West checks ===
                    if (
                            ((pos.getX() == 3020 && pos.getY() == 9703) || // special case
                                    (pos.getX() == 3028 || pos.getX() == 3029)) &&
                                    playerPos.equals(new WorldPosition(pos.getX() - 1, pos.getY(), plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (west check).");
                        beingMined = true;
                        break;
                    }

                    // === East checks ===
                    if (
                            ((pos.getX() == 3018 || pos.getX() == 3017 || pos.getX() == 3026)) &&
                                    playerPos.equals(new WorldPosition(pos.getX() + 1, pos.getY(), plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (east check).");
                        beingMined = true;
                        break;
                    }

                    // === South checks ===
                    if (
                            ((pos.getX() >= 3022 && pos.getX() <= 3025) && pos.getY() == 9701) &&
                                    playerPos.equals(new WorldPosition(pos.getX(), pos.getY() + 1, plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (south check).");
                        beingMined = true;
                        break;
                    }

                    // === North checks ===
                    if (
                            (((pos.getX() >= 3019 && pos.getX() <= 3021) && pos.getY() == 9699) ||
                                    ((pos.getX() >= 3023 && pos.getX() <= 3026) && pos.getY() == 9698)) &&
                                    playerPos.equals(new WorldPosition(pos.getX(), pos.getY() - 1, plane))
                    ) {
                        script.log(getClass(), "Skipping vein at " + pos + " due to player mining (north check).");
                        beingMined = true;
                        break;
                    }
                }
            }

            if (!beingMined) {
                availableVeins.add(vein);
            } else {
                skippedByPlayers.add(vein);
            }
        }

        if (availableVeins.isEmpty()) {
            script.log(getClass(), "No available veins free — force hop worlds.");
            script.getProfileManager().forceHop();
            return false;
        }

        task = "Get target vein";
        RSObject targetVein = availableVeins.get(0);

        task = "Draw active veins";
        drawActiveVeins(activeVeins, targetVein);

        Polygon poly = targetVein.getConvexHull();
        if (poly == null) return false;

        MenuHook hook = getVeinMenuHook(targetVein);
        if (!script.getFinger().tapGameScreen(poly, hook)) {
            objectPositionBlacklist.put(targetVein.getWorldPosition(), System.currentTimeMillis());
            return false;
        }

        task = "Wait before check";
        script.pollFramesHuman(() -> false, script.random(1500, 2500));
        task = "Wait until interrupt";
        waitUntilFinishedMining(targetVein);
        task = "Human delay task";
        script.pollFramesHuman(() -> false, script.random(150, 700));
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

            return o.getName() != null && o.getName().equalsIgnoreCase("Amethyst crystals")
                    && o.getActions() != null && Arrays.asList(o.getActions()).contains("Mine")
                    && o.canReach();
        });
    }

    private List<WorldPosition> getRespawnCirclePositions() {
        try {
            List<Rectangle> respawnCircles = script.getPixelAnalyzer().findRespawnCircles();
            return script.getUtils().getWorldPositionForRespawnCircles(respawnCircles, 20);
        } catch (RuntimeException e) {
            script.log("ERROR", "Could not get respawn circle positions: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<RSObject> getActiveVeinsOnScreen(List<RSObject> veins, WorldPosition myPosition) {
        List<RSObject> active = new ArrayList<>(veins);
        active.removeIf(o -> !o.isInteractableOnScreen());
        active.sort(Comparator.comparingDouble(o -> o.getWorldPosition().distanceTo(myPosition)));
        return active;
    }

    private MenuHook getVeinMenuHook(RSObject vein) {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().equalsIgnoreCase("mine amethyst crystals")) {
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
                WorldPosition pos = entry.getKey();

                RSObject match = veins.stream()
                        .filter(vein -> pos.equals(vein.getWorldPosition()) && vein.getFaces() != null)
                        .findFirst()
                        .orElse(null);

                if (match != null) {
                    for (Triangle t : match.getFaces()) {
                        canvas.drawPolygon(t.getXPoints(), t.getYPoints(), 3, Color.RED.getRGB());
                    }
                } else {
                    Polygon fallbackPoly = script.getSceneProjector().getTileCube(pos, 150);
                    if (fallbackPoly != null) {
                        canvas.fillPolygon(fallbackPoly, Color.RED.getRGB(), 0.7f);
                    }
                }
            }
        });
    }

    private void waitUntilFinishedMining(RSObject vein) {
        AtomicInteger localMinedCount = new AtomicInteger(0);
        int maxMiningDuration = (int) script.random(240_000, 270_000);

        ItemGroupResult startSnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST));
        if (startSnapshot == null) {
            script.log(getClass(), "Aborting mining check: could not read starting inventory.");
            return;
        }

        AtomicInteger previousCount = new AtomicInteger(startSnapshot.getAmount(ItemID.AMETHYST));

        WorldPosition playerTile = script.getWorldPosition();
        if (playerTile == null) {
            script.log(getClass(), "Aborting mining check: player tile is null.");
            return;
        }

        Polygon playerTileResized = script.getSceneProjector().getTileCube(playerTile, 120).getResized(0.7);
        WorldPosition veinPosition = vein.getWorldPosition();

        Timer animationTimer = new Timer();
        Timer debounceTimer = new Timer();
        long start = System.currentTimeMillis();

        final long gracePeriodMs = script.random(3500, 4500);
        final long maxNoAnimTime = script.random(7000, 9000);

        script.pollFramesHuman(() -> {
            ItemGroupResult currentInv = script.getWidgetManager().getInventory().search(Set.of(ItemID.AMETHYST));
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

            int currentCount = currentInv.getAmount(ItemID.AMETHYST);
            int lastCount = previousCount.get();

            // Ignore invalid reads that reset to 0 if we previously had more
            if (currentCount == 0 && lastCount > 0) {
                script.log(getClass(), "⚠️ Ignored invalid inventory read: attempted reset from " + lastCount + " to 0.");
                return false;
            }

            if (currentCount > lastCount) {
                int gained = currentCount - lastCount;

                if (gained > 5) {
                    script.log(getClass(), "Ignored suspicious amethyst jump: +" + gained +
                            " (from " + lastCount + " to " + currentCount + ")");
                } else {
                    previousCount.set(currentCount);
                    amethystMined += gained;
                    miningXpGained += gained * 240;
                    localMinedCount.addAndGet(gained);
                    animationTimer.reset();
                    debounceTimer.reset();
                    script.log(getClass(), "+" + gained + " amethyst mined! (" + amethystMined + " in total)");
                }
            } else if (currentCount < lastCount) {
                // Accept drops only if currentCount is not 0 (avoiding bad reset)
                if (currentCount > 0) {
                    script.log(getClass(), "Detected amethyst count drop (from " + lastCount + " to " + currentCount + "). Syncing.");
                    previousCount.set(currentCount);
                } else {
                    script.log(getClass(), "⚠️ Ignored suspicious count drop to 0 (last was " + lastCount + ").");
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            boolean graceOver = elapsed > gracePeriodMs;
            boolean animStale = animationTimer.timeElapsed() > maxNoAnimTime;

            if (elapsed > maxMiningDuration) {
                script.log(getClass(), "Mining stopped: exceeded max mining duration.");
                return true;
            }

            

            if (graceOver && animStale) {
                script.log(getClass(), "Mining stopped: no animation for " + animationTimer.timeElapsed() + "ms.");
                return true;
            }

            return false;
        }, maxMiningDuration);

        script.pollFramesHuman(() -> false, script.random(300, 800));
    }
}
