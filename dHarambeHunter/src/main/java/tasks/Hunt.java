package tasks;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;

import static main.dHarambeHunter.*;

public class Hunt extends Task {

    public static WorldPosition lastTrapPos = null;
    public static RSObject lastTrap = null;
    private static final Map<WorldPosition, Long> lastBoneLoots = new HashMap<>();
    private static final long RESPAWN_DELAY = 60000; // ~60s respawn per spawn
    public static Set<WorldPosition> blacklistedTraps = new HashSet<>();
    public static SearchableImage monkeyTail = null;
    private int monkeyTailDropThreshold = script.random(2, 6);
    private int invalidTrapCount = 0;

    // List of bone spawn locations
    private static final List<WorldPosition> boneSpawns = List.of(
            new WorldPosition(2900, 9113, 0),
            new WorldPosition(2904, 9113, 0),
            new WorldPosition(2906, 9109, 0),
            new WorldPosition(2911, 9110, 0),
            new WorldPosition(2915, 9108, 0),
            new WorldPosition(2919, 9112, 0),
            new WorldPosition(2916, 9115, 0),
            new WorldPosition(2909, 9118, 0),
            new WorldPosition(2914, 9125, 0),
            new WorldPosition(2907, 9127, 0),
            new WorldPosition(2911, 9130, 0),
            new WorldPosition(2916, 9132, 0),
            new WorldPosition(2919, 9135, 0),
            new WorldPosition(2913, 9139, 0),
            new WorldPosition(2907, 9134, 0),
            new WorldPosition(2905, 9136, 0),
            new WorldPosition(2903, 9133, 0)
    );

    public Hunt(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return setupDone;
    }

    @Override
    public boolean execute() {
        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(ItemID.BANANA, ItemID.DAMAGED_MONKEY_TAIL, ItemID.BONES_TO_BANANAS, ItemID.BONES, ItemID.MONKEY_TAIL));
        if (inv == null) return false;

        // 1. Find or reuse trap object
        if (lastTrap == null) {
            if (lastTrapPos == null) {
                task = "Searching for Large boulder";

                Predicate<RSObject> trapQuery = obj -> {
                    if (obj == null) return false;
                    String name = obj.getName();
                    String[] actions = obj.getActions();
                    if (name == null || actions == null) return false;

                    boolean nameMatches = name.equalsIgnoreCase("Large boulder");
                    boolean actionMatches = java.util.Arrays.stream(actions)
                            .anyMatch(a -> a != null && a.equalsIgnoreCase("Set-trap"));

                    boolean notBlacklisted = !blacklistedTraps.contains(obj.getWorldPosition());
                    return nameMatches && actionMatches && obj.canReach() && notBlacklisted;
                };

                RSObject candidate = findClosest(trapQuery);
                if (candidate == null) {
                    return false;
                }

                // Don’t blacklist just because it has a circle, only skip it
                Map<RSObject, PixelAnalyzer.RespawnCircle> circles =
                        script.getPixelAnalyzer().getRespawnCircleObjects(
                                List.of(candidate),
                                PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER,
                                100,
                                15
                        );

                if (circles.containsKey(candidate)) {
                    script.log(getClass(), "Trap at " + candidate.getWorldPosition() + " already has a circle → skipping for now.");
                    return false;
                }

                lastTrap = candidate;
                lastTrapPos = candidate.getWorldPosition();
            } else {
                // Re-resolve RSObject at saved position
                List<RSObject> objsAtPos = script.getObjectManager().getObjects(
                        o -> o != null && o.getWorldPosition().equals(lastTrapPos)
                );
                if (!objsAtPos.isEmpty()) {
                    lastTrap = objsAtPos.get(0);
                } else {
                    script.log(getClass(), "Trap at " + lastTrapPos + " not found, resetting.");
                    lastTrapPos = null;
                    return false;
                }
            }
        }

        // 2. Check respawn circle state if reusing trap
        Map<RSObject, PixelAnalyzer.RespawnCircle> circles =
                script.getPixelAnalyzer().getRespawnCircleObjects(
                        List.of(lastTrap),
                        PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER,
                        100,
                        15
                );

        PixelAnalyzer.RespawnCircle circle = circles.get(lastTrap);

        if (circle != null) {
            PixelAnalyzer.RespawnCircle.Type type = circle.getType();
            script.log(getClass(), "Respawn circle type: " + type);

            // 2.1 If yellow → wait until green or fail
            if (type == PixelAnalyzer.RespawnCircle.Type.YELLOW) {
                task = "Waiting for trap process";

                final long[] circleMissingSince = {0};

                boolean turnedGreen = script.pollFramesHuman(() -> {
                    Map<RSObject, PixelAnalyzer.RespawnCircle> checkCircles =
                            script.getPixelAnalyzer().getRespawnCircleObjects(
                                    List.of(lastTrap),
                                    PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER,
                                    100,
                                    15
                            );

                    PixelAnalyzer.RespawnCircle c = checkCircles.get(lastTrap);

                    if (c != null) {
                        // Reset missing timer if circle is visible again
                        circleMissingSince[0] = 0;

                        // Success case → turned green
                        if (c.getType() == PixelAnalyzer.RespawnCircle.Type.GREEN) {
                            return true;
                        }
                    } else {
                        // Circle not visible
                        if (circleMissingSince[0] == 0) {
                            circleMissingSince[0] = System.currentTimeMillis();
                        } else {
                            long goneFor = System.currentTimeMillis() - circleMissingSince[0];
                            if (goneFor > 8000) {
                                script.log(getClass(), "Trap at " + lastTrap.getWorldPosition() + " failed (circle gone >8s).");
                                return true; // exit task early → will re-poll next loop
                            }
                        }
                    }

                    // Ensure trap stays on screen while waiting
                    if (lastTrap != null && !lastTrap.isInteractableOnScreen()) {
                        task = "Walking back to trap while waiting";
                        WalkConfig cfg = new WalkConfig.Builder()
                                .breakCondition(() -> lastTrap != null && lastTrap.isInteractableOnScreen())
                                .enableRun(true)
                                .build();
                        script.getWalker().walkTo(lastTrapPos, cfg);
                    }

                    // Update inventory again
                    ItemGroupResult inv2 = script.getWidgetManager().getInventory()
                            .search(Set.of(ItemID.BANANA, ItemID.DAMAGED_MONKEY_TAIL, ItemID.BONES_TO_BANANAS, ItemID.BONES, ItemID.MONKEY_TAIL));
                    if (inv2 == null) return false;

                    // Opportunistic banana gather while waiting
                    int bananaCount = inv2.getAmount(ItemID.BANANA);
                    int freeSlots = inv2.getFreeSlots();
                    if (bananaCount < 15 && freeSlots > 4) {
                        script.log(getClass(), "Low bananas while waiting, gathering bones...");
                        gatherBones();
                        return false;
                    }

                    // Opportunistic monkey tail drop while waiting
                    if (countMonkeyTails() >= monkeyTailDropThreshold) {
                        dropMonkeyTails();
                        // Reset threshold after dropping
                        monkeyTailDropThreshold = script.random(2, 6);
                        return false;
                    }

                    return false; // keep waiting
                }, script.random(90000, 120000));

                if (turnedGreen) {
                    canHop = false;
                    canBreak = false;
                    Map<RSObject, PixelAnalyzer.RespawnCircle> postCheck =
                            script.getPixelAnalyzer().getRespawnCircleObjects(
                                    List.of(lastTrap),
                                    PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER,
                                    100,
                                    15
                            );
                    PixelAnalyzer.RespawnCircle c = postCheck.get(lastTrap);
                    if (c == null) {
                        lastTrap = null; // force re-poll
                        return false;
                    }
                    return true; // trap turned green normally
                }

                return false;
            }

            // 2.2 If green → check trap for loot
            if (type == PixelAnalyzer.RespawnCircle.Type.GREEN) {
                task = "Checking trap for loot...";

                boolean gained = false;

                script.log(getClass(), "Insert additional human delay before checking.");
                script.pollFramesHuman(() -> false, script.random(1, 500));

                script.getWidgetManager().getInventory().unSelectItemIfSelected();
                if (lastTrap.interact("Check")) {
                    gained = script.pollFramesHuman(() -> {
                        // --- Ensure template is loaded ---
                        if (monkeyTail == null) {
                            try (InputStream res = getClass().getResourceAsStream("/damagedmonkeytail.png")) {
                                if (res != null) {
                                    monkeyTail = new SearchableImage(
                                            ImageIO.read(res),
                                            new SingleThresholdComparator(50),
                                            ColorModel.HSL
                                    );
                                }
                            } catch (IOException e) {
                                script.log(getClass(), "Error loading monkey tail image: " + e);
                            }
                        }
                        if (monkeyTail == null) return false;

                        // --- Take screen before check ---
                        Image beforeScreen = script.getScreen().getImage();
                        int beforeCount;
                        if (beforeScreen != null) {
                            List<ImageSearchResult> beforeMatches = script.getImageAnalyzer().findLocations(beforeScreen, monkeyTail);
                            beforeCount = (beforeMatches != null) ? beforeMatches.size() : 0;
                        } else {
                            beforeCount = 0;
                        }

                        // --- Wait and poll screen after check ---
                        return script.pollFramesUntil(() -> {
                            Image afterScreen = script.getScreen().getImage();
                            if (afterScreen == null) return false;

                            List<ImageSearchResult> afterMatches = script.getImageAnalyzer().findLocations(afterScreen, monkeyTail);
                            int afterCount = (afterMatches != null) ? afterMatches.size() : 0;

                            script.log(getClass(), "Tail check logic, before: " + beforeCount + " after: " + afterCount);

                            return afterCount > beforeCount;
                        }, script.random(6000, 9000));

                    }, script.random(6000, 9000));

                    if (gained) {
                        script.log(getClass(), "Successfully looted a (damaged) monkey tail (screen-based).");
                        canBreak = true;
                        canHop = true;
                        return true;
                    } else {
                        script.log(getClass(), "No loot detected after checking trap (screen-based).");
                        canBreak = true;
                        canHop = true;
                        return false;
                    }
                }
            }
        }

        // 3. No circle → set trap
        if (!inv.contains(ItemID.BANANA)) {
            task = "Need bananas";
            script.log(getClass(), "Can't set trap without bananas → gather/process first");

            // Handle inventory cleanup/processing
            if (inv.getFreeSlots() < 5) {
                // Get inventory or screen
                Image screen = script.getScreen().getImage();
                if (screen == null) return false;

                // Check for monkey tails
                if (monkeyTail == null) {
                    try (InputStream res = getClass().getResourceAsStream("/damagedmonkeytail.png")) {
                        if (res != null) {
                            monkeyTail = new SearchableImage(
                                    ImageIO.read(res),
                                    new SingleThresholdComparator(50),
                                    ColorModel.HSL
                            );
                        }
                    } catch (IOException e) {
                        script.log(getClass(), "Error loading monkey tail image: " + e);
                    }
                }
                if (monkeyTail == null) return false;

                List<ImageSearchResult> matches = script.getImageAnalyzer().findLocations(screen, monkeyTail);
                if (matches == null || matches.isEmpty()) {script.log(getClass(), "No monkey tails found.");}

                if (matches != null && !matches.isEmpty()) {
                    script.log(getClass(), "Monkey tails found: " + matches.size());

                    dropMonkeyTails();
                }

                if (inv.contains(ItemID.BONES)) {
                    script.log(getClass(), "Bone count: " + inv.getAmount(ItemID.BONES));
                    if (inv.getAmount(ItemID.BONES) > 10 && inv.contains(ItemID.BONES_TO_BANANAS)) {

                        // Check tap to drop
                        UIResult<Boolean> tapToDrop = script.getWidgetManager().getHotkeys().isTapToDropEnabled();
                        if (tapToDrop == null || tapToDrop.isNotFound() || tapToDrop.isNotVisible() || tapToDrop.get() == null) {
                            return false;
                        }

                        if (tapToDrop.get()) {
                            return script.getWidgetManager().getHotkeys().setTapToDropEnabled(false);
                        }

                        script.log(getClass(), "Got enough bones, using Bones to Bananas tab!");
                        script.getWidgetManager().getInventory().unSelectItemIfSelected();
                        inv.getItem(ItemID.BONES_TO_BANANAS).interact();
                        return script.pollFramesHuman(() -> inv.contains(ItemID.BANANA),
                                script.random(1500, 2500));
                    }
                }
            }

            // Otherwise gather fresh bones to make bananas
            gatherBones();
            return false;
        }

        if (lastTrap != null && !lastTrap.isInteractableOnScreen()) {
            task = "Walking toward Large boulder";
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(() -> {
                        if (lastTrap != null) return lastTrap.isInteractableOnScreen();
                        var tile = script.getSceneManager().getTile(lastTrapPos);
                        return tile != null && tile.isOnGameScreen();
                    })
                    .enableRun(true)
                    .build();
            if (!script.getWalker().walkTo(lastTrapPos, cfg)) {
                script.log(getClass(), "Walking failed.");
                return false;
            }
        }

        if (lastTrap == null || lastTrap.getName() == null || lastTrap.getName().isBlank()) {
            invalidTrapCount++;
            script.log(getClass(), "lastTrap invalid (null name). Count: " + invalidTrapCount);

            if (invalidTrapCount >= 5) {
                script.log(getClass(), "Resetting lastTrap after 5 consecutive invalid detections.");
                if (lastTrapPos != null) {
                    blacklistedTraps.add(lastTrapPos);
                    script.log(getClass(), "Blacklisted trap position: " + lastTrapPos);
                }
                lastTrap = null;
                lastTrapPos = null;
                invalidTrapCount = 0;
            }

            return false;
        } else {
            invalidTrapCount = 0;
        }

        script.getWidgetManager().getInventory().unSelectItemIfSelected();
        task = "Setting trap...";
        if (lastTrap.interact("Set-trap")) {
            boolean appeared = script.pollFramesHuman(() -> {
                Map<RSObject, PixelAnalyzer.RespawnCircle> checkCircles =
                        script.getPixelAnalyzer().getRespawnCircleObjects(
                                List.of(lastTrap),
                                PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER,
                                100,
                                15
                        );
                return checkCircles.containsKey(lastTrap);
            }, script.random(7500, 10500));

            if (appeared) {
                script.log(getClass(), "Respawn circle detected after setting trap.");
                return true;
            } else {
                script.log(getClass(), "Respawn circle never appeared after Set-trap.");
                return false;
            }
        }

        return false;
    }

    private RSObject findClosest(Predicate<RSObject> objectQuery) {
        List<RSObject> objs = script.getObjectManager().getObjects(objectQuery);
        if (objs == null || objs.isEmpty()) return null;
        return (RSObject) script.getUtils().getClosest(objs);
    }

    private boolean walkTowardTarget(RSObject target, WalkConfig cfg) {
        try {
            return script.getWalker().walkTo(target, cfg);
        } catch (Exception e) {
            script.log(getClass(), "walkTowardTarget error: " + e.getMessage());
            return false;
        }
    }

    private void gatherBones() {
        task = "Gather bones";
        script.log(getClass(), "Checking for nearby bones...");

        ItemGroupResult inv = script.getWidgetManager().getInventory()
                .search(Set.of(ItemID.BONES, ItemID.BANANA, ItemID.DAMAGED_MONKEY_TAIL));

        if (inv.getFreeSlots() < 2) {
            script.log(getClass(), "Inventory too full for bones.");
            return;
        }

        // Get all minimap items
        UIResultList<WorldPosition> itemPositions = script.getWidgetManager().getMinimap().getItemPositions();
        if (itemPositions.isNotVisible() || itemPositions.isNotFound()) {
            script.log(getClass(), "No ground items visible on minimap.");
            return;
        }

        // Convert to normal list
        List<WorldPosition> items = itemPositions.asList();
        if (items == null || items.isEmpty()) {
            script.log(getClass(), "No items in result list.");
            return;
        }

        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return;

        long now = System.currentTimeMillis();
        List<WorldPosition> candidates = items.stream()
                .filter(boneSpawns::contains)
                .filter(pos -> !lastBoneLoots.containsKey(pos) || now - lastBoneLoots.get(pos) > RESPAWN_DELAY)
                .sorted((a, b) -> {
                    int da = (int) myPos.distanceTo(a.getX(), a.getY());
                    int db = (int) myPos.distanceTo(b.getX(), b.getY());
                    return Integer.compare(da, db);
                })
                .toList(); // no need to sort later

        // Pick up 1–2 closest bones
        int toPick = Math.min(2, candidates.size());
        int picked = 0;

        for (WorldPosition target : candidates.subList(0, toPick)) {
            if (inv.getFreeSlots() < 2) break; // avoid full inv

            if (!script.getSceneManager().getTile(target).isOnGameScreen()) {
                task = "Walking to bones";
                WalkConfig cfg = new WalkConfig.Builder()
                        .breakCondition(() -> {
                            var tile = script.getSceneManager().getTile(lastTrapPos);
                            return tile != null && tile.isOnGameScreen();
                        })
                        .enableRun(true)
                        .build();
                script.getWalker().walkTo(target, cfg);
            }

            task = "Pick-up bone";
            script.log(getClass(), "Trying to take bone at " + target);

            int beforeBones = inv.getAmount(ItemID.BONES);
            var tile = script.getSceneManager().getTile(target);
            boolean clicked = false;
            if (tile != null) {
                var cube = tile.getTileCube(25);
                if (cube != null) {
                    var resized = cube.getResized(0.6);
                    if (resized != null) {
                        clicked = script.getFinger().tap(resized, getMenuHook());
                        if (!clicked) {
                            script.log(getClass(), "Failed to tap bone at " + target);
                        }
                    } else {
                        script.log(getClass(), "Resized cube was null for tile " + target);
                    }
                } else {
                    script.log(getClass(), "TileCube is null for target " + target);
                }
            } else {
                script.log(getClass(), "Tile is null for target " + target + ". Scene may not be loaded.");
            }

            if (clicked) {
                boolean taken = script.pollFramesHuman(() -> {
                    ItemGroupResult after = script.getWidgetManager().getInventory().search(Set.of(ItemID.BONES));
                    return after != null && after.getAmount(ItemID.BONES) > beforeBones;
                }, script.random(2500, 4000));

                if (taken) {
                    picked++;
                    lastBoneLoots.put(target, now); // mark spawn as looted
                    script.log(getClass(), "Picked up bone at " + target + " (" + picked + "/" + toPick + ")");
                }
            }
        }

        if (picked == 0) {
            script.log(getClass(), "No bones successfully picked up.");
        } else {
            script.log(getClass(), "Done bone run, picked " + picked + " → heading back to trap.");
        }
    }

    private void dropMonkeyTails() {
        Image screen = script.getScreen().getImage();
        if (screen == null) return;

        if (monkeyTail == null) {
            try (InputStream res = getClass().getResourceAsStream("/damagedmonkeytail.png")) {
                if (res != null) {
                    monkeyTail = new SearchableImage(
                            ImageIO.read(res),
                            new SingleThresholdComparator(50),
                            ColorModel.HSL
                    );
                }
            } catch (IOException e) {
                script.log(getClass(), "Error loading monkey tail image: " + e);
            }
        }
        if (monkeyTail == null) return;

        List<ImageSearchResult> matches = script.getImageAnalyzer().findLocations(screen, monkeyTail);
        if (matches == null || matches.isEmpty()) {
            script.log(getClass(), "No monkey tails found for dropping.");
            return;
        }

        // Enable tap-to-drop
        var hotkeys = script.getWidgetManager().getHotkeys();
        if (hotkeys != null) {
            var tapToDrop = hotkeys.isTapToDropEnabled();
            Boolean isEnabled = (tapToDrop != null) ? tapToDrop.get() : null;
            if (isEnabled == null || !isEnabled) {
                hotkeys.setTapToDropEnabled(true);
                script.log(getClass(), "Enabled Tap-to-Drop mode.");
            }
        } else {
            script.log(getClass(), "Hotkeys widget manager is null. Cannot check tap-to-drop state.");
            return;
        }

        for (ImageSearchResult match : matches) {
            script.getFinger().tap(match.getBounds());

            if (script.random(0, 99) < 10) { // 5% humanized wait
                script.pollFramesHuman(() -> false, script.random(1, 50));
            } else {
                script.pollFramesUntil(() -> false, script.random(100, 300));
            }
        }

        // Disable tap-to-drop
        hotkeys = script.getWidgetManager().getHotkeys();
        if (hotkeys != null) {
            var tapToDrop = hotkeys.isTapToDropEnabled();
            Boolean isEnabled = (tapToDrop != null) ? tapToDrop.get() : null;
            if (isEnabled != null && isEnabled) {
                hotkeys.setTapToDropEnabled(false);
                script.log(getClass(), "Disabled Tap-to-Drop mode.");
            }
        } else {
            script.log(getClass(), "Hotkeys widget manager is null. Cannot disable tap-to-drop.");
        }

        script.log(getClass(), "Dropped " + matches.size() + " monkey tails.");
    }

    private int countMonkeyTails() {
        Image screen = script.getScreen().getImage();
        if (screen == null) return 0;

        // Ensure template is loaded
        if (monkeyTail == null) {
            try (InputStream res = getClass().getResourceAsStream("/damagedmonkeytail.png")) {
                if (res != null) {
                    monkeyTail = new SearchableImage(
                            ImageIO.read(res),
                            new SingleThresholdComparator(50),
                            ColorModel.HSL
                    );
                }
            } catch (IOException e) {
                script.log(getClass(), "Error loading monkey tail image: " + e);
            }
        }
        if (monkeyTail == null) return 0;

        List<ImageSearchResult> matches = script.getImageAnalyzer().findLocations(screen, monkeyTail);
        return (matches != null) ? matches.size() : 0;
    }

    private MenuHook getMenuHook() {
        return menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String text = entry.getRawText().toLowerCase();
                if (text.startsWith("take") && text.endsWith("bones")) {
                    return entry;
                }
            }
            return null;
        };
    }
}