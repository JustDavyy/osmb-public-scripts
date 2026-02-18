package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import data.FishingLocation;
import data.FishingSpot;
import utils.Task;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;

import java.awt.Color;
import java.util.*;
import java.util.List;

import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class Fish extends Task {
    private static final SearchablePixel[] FISHING_SPOT_PIXELS = new SearchablePixel[]{
            // Normal spots
            new SearchablePixel(-14155777, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9726564, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9266012, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9068122, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9332063, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9924457, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-10252907, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9792353, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9529185, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9726823, new SingleThresholdComparator(2), ColorModel.RGB),
            new SearchablePixel(-9200215, new SingleThresholdComparator(5), ColorModel.RGB),

            // Sacred eels
            new SearchablePixel(-13146533, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13080483, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13343911, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13475753, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12883106, new SingleThresholdComparator(2), ColorModel.HSL),

            // Infernal eels
            new SearchablePixel(-7299223, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7964334, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-1850468, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-1587295, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7702703, new SingleThresholdComparator(2), ColorModel.HSL),

            // Yellow highlight after interacting
            new SearchablePixel(-2171876, new SingleThresholdComparator(2), ColorModel.HSL)
    };
    private FishingSpot lastFishingSpot = null;
    private int consecutiveNoSpotChecks = 0;
    private int relocationAttempts = 0;
    private final Map<WorldPosition, Long> fishingSpotBlacklist = new HashMap<>();

    public Fish(Script script) {
        super(script);
    }

    public boolean activate() {
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }

        if (fishingLocation.equals(FishingLocation.Karamja_West)) {
            return true;
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log("Fish", "Inventory not visible.");
            return false;
        }

        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) {
            return false;
        }

        // Always require being inside the fishing area
        if (!fishingLocation.getFishingArea().contains(myPos)) {
            return false;
        }

        // Require all tools
        if (!hasAllRequirements(fishingMethod.getRequiredTools())) {
            script.log("Fish", "Not all required tools could be located in inventory, stopping script!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        }

        return !inventorySnapshot.isFull();
    }

    public boolean execute() {
        task = "Fish";

        if (alreadyCountedFish) {
            alreadyCountedFish = false;
        }

        // Special case: for Karamja_West, count remaining stackable raw fish
        if (fishingLocation.equals(FishingLocation.Karamja_West)) {
            ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI));
            if (afterDrop != null) {
                fish3Caught = afterDrop.getAmount(ItemID.RAW_KARAMBWANJI) - startAmount;
            }
        }

        if (!isCurrentlyFishing()) {
            return initiateFishingAction();
        }

        // We're currently fishing; monitor exit conditions
        script.pollFramesHuman(this::earlyExitCheck, RandomUtils.uniformRandom(25000, 40000));

        return false;
    }

    private boolean isCurrentlyFishing() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log("Fish", "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return false;
        }

        task = "Check last fishing spot";
        if (lastFishingSpot == null) return false;

        boolean stillValid = isValidFishingSpot(lastFishingSpot.getPosition()) != null;
        task = "Check XP Gained";
        boolean xpRecently = System.currentTimeMillis() - lastXpGained <= fishingMethod.getFishingDelay();

        task = "Check if currently fishing";
        if (stillValid && xpRecently) task = "Wait till done fishing";
        return stillValid && xpRecently;
    }

    private boolean earlyExitCheck() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log("Fish", "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return true;
        }

        task = "Check AFK timer";
        if (switchTabTimer.hasFinished()) {
            script.log("PREVENT-LOG", "Switching tabs...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.values()[RandomUtils.uniformRandom(Tab.Type.values().length)]);
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        }

        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log("Fish", "Inventory not visible.");
            return false;
        }

        if (!fishingLocation.equals(FishingLocation.Karamja_West)) {
            if (inventorySnapshot.isFull()) {
                script.log("Fish", "Early exit, Inventory is full");
                return true;
            }
        }

        task = "Check last fishing spot";
        if (lastFishingSpot != null) {
            FishingSpot current = isValidFishingSpot(lastFishingSpot.getPosition());
            if (current == null) {
                script.log("Fish", "Fishing spot no longer valid. Early exit.");
                return true;
            }
        }

        task = "Check XP gained";
        long idleSinceXp = System.currentTimeMillis() - lastXpGained;
        if (idleSinceXp > fishingMethod.getFishingDelay()) {
            int delay = (int) (fishingMethod.getFishingDelay() / 1000);
            script.log("Fish", "No XP gained in last " + delay + "s. Early exit.");
            return true;
        }

        task = "Check if currently fishing";
        return false;
    }

    private boolean initiateFishingAction() {
        task = "Initiate fishing action";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            return false;
        }

        FishingSpot fishingSpot = getFishingSpot(myPosition);
        if (fishingSpot != null) {
            lastFishingSpot = fishingSpot;
            lastXpGained = System.currentTimeMillis();
            return attemptFishing(fishingSpot);
        }

        script.log("Fish", "No visible fishing spots, walking to another area...");
        if (fishingMethod.getName().equals("Small Fishing Net (SafeMode/10HP)")) {
            script.log("Fish", "SafeMode method in use, skipping relocation and hopping immediately.");
            script.getProfileManager().forceHop();
            relocationAttempts = 0;
            consecutiveNoSpotChecks = 0;
            return false;
        }

        relocationAttempts++;
        script.getWalker().walkTo(getDestination(), new WalkConfig.Builder().disableWalkScreen(true).breakDistance(5).build());

        if (relocationAttempts >= 3) {
            script.log("Fish", "Still no fishing spots after 3 relocations. Forcing world hop...");
            script.getProfileManager().forceHop();
            relocationAttempts = 0;
            consecutiveNoSpotChecks = 0;
        }

        fishingSpot = getFishingSpot(script.getWorldPosition());
        if (fishingSpot != null) {
            lastFishingSpot = fishingSpot;
            lastXpGained = System.currentTimeMillis();
            relocationAttempts = 0;
            return attemptFishing(fishingSpot);
        } else {
            script.log("Fish", "Still no fishing spots after relocating.");
        }

        return false;
    }

    private boolean interactWithFishingSpot(FishingSpot fishingSpot) {
        script.log("Fish", "Interacting with fishing spot at " + fishingSpot.getPosition());

        boolean clicked = script.getFinger().tap(fishingSpot.getFishingSpotPoly().getResized(0.85), menuHook);
        if (!clicked) {
            script.log("Fish", "Failed to click fishing spot.");

            // Add to blacklist for 10 seconds
            fishingSpotBlacklist.put(fishingSpot.getPosition(), System.currentTimeMillis() + 10_000);

            lastFishingSpot = null;
            return false;
        }

        lastXpGained = System.currentTimeMillis();
        return true;
    }

    private boolean attemptFishing(FishingSpot fishingSpot) {
        RSTile spotTile = script.getSceneManager().getTile(fishingSpot.getPosition());

        if (spotTile != null && spotTile.isOnGameScreen()) {
            script.log("Fish", "Fishing spot is on game screen, interacting directly.");
            boolean success = interactWithFishingSpot(fishingSpot);
            if (success) {
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(2500, 4500));
            }
            return success;
        } else {
            script.log("Fish", "Fishing spot is not on screen, walking to it...");
            WalkConfig.Builder walkConfig = new WalkConfig.Builder();
            walkConfig.breakCondition(() -> {
                RSTile t = script.getSceneManager().getTile(fishingSpot.getPosition());
                return t != null && t.isOnGameScreen();
            });
            script.getWalker().walkTo(fishingSpot.getPosition(), walkConfig.build());
            lastXpGained = System.currentTimeMillis() - 21000;
        }

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute. Resetting as we just performed an action.");
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
        }

        return false;
    }

    private FishingSpot getFishingSpot(WorldPosition worldPosition) {
        long start = System.currentTimeMillis();

        long currentTime = System.currentTimeMillis();
        fishingSpotBlacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);

        Area spotArea = fishingLocation.getFishingArea();
        Set<WorldPosition> fishingSpotPositions = fishingMethod.getFishingSpots();

        List<FishingSpot> activeFishingSpotsOnScreen = getFishingSpots(fishingSpotPositions).stream()
                .filter(spot -> !fishingSpotBlacklist.containsKey(spot.getPosition()))
                .toList();

        if (activeFishingSpotsOnScreen.isEmpty()) {
            consecutiveNoSpotChecks++;
            script.log("Fish", "No fishing spots found (" + consecutiveNoSpotChecks + " check(s) in a row)");

            if (fishingMethod.getName().equals("Small Fishing Net (SafeMode/10HP)")) {
                script.log("Fish", "SafeMode method detected, forcing hop immediately...");
                script.getProfileManager().forceHop();
                consecutiveNoSpotChecks = 0;
                relocationAttempts = 0;
                return null;
            }

            if (consecutiveNoSpotChecks >= 3) {
                script.log("Fish", "No fishing spots found for 3 consecutive checks. Forcing world hop...");
                script.getProfileManager().forceHop();
                consecutiveNoSpotChecks = 0;
                relocationAttempts = 0;
            }
            return null;
        }

        // Found spots; reset counter
        consecutiveNoSpotChecks = 0;

        long end = System.currentTimeMillis();
        script.log("Fish", "Fishing spots found in: " + (end - start) + "ms");

        FishingSpot closest = activeFishingSpotsOnScreen.get(0);
        for (FishingSpot spot : activeFishingSpotsOnScreen) {
            if (spot.getPosition().distanceTo(worldPosition) < closest.getPosition().distanceTo(worldPosition)) {
                closest = spot;
            }
        }

        FishingSpot finalClosest = closest;
        script.getScreen().queueCanvasDrawable("activeFishingSpots", canvas -> {
            for (FishingSpot spot : activeFishingSpotsOnScreen) {
                Polygon poly = script.getSceneProjector().getTilePoly(spot.getPosition(), true);
                if (poly != null) {
                    canvas.fillPolygon(poly, spot == finalClosest ? Color.GREEN.getRGB() : Color.RED.getRGB(), 0.3);
                }
            }
        });

        return closest;
    }

    private List<FishingSpot> getFishingSpots(Set<WorldPosition> fishingSpotPositions) {
        List<FishingSpot> validFishingSpots = new ArrayList<>();
        for (WorldPosition fishingSpotPosition : fishingSpotPositions) {
            FishingSpot fishingSpot = isValidFishingSpot(fishingSpotPosition);
            if (fishingSpot != null) {
                validFishingSpots.add(fishingSpot);
            }
        }
        return validFishingSpots;
    }

    private FishingSpot isValidFishingSpot(WorldPosition worldPosition) {
        Polygon fishingSpotPoly = getFishingSpotPoly(worldPosition);
        if (fishingSpotPoly == null) {
            return null;
        }
        fishingSpotPoly.getResized(0.8);
        boolean containsFishingPixel = script.getPixelAnalyzer().findPixel(fishingSpotPoly, FISHING_SPOT_PIXELS) != null;
        if (!containsFishingPixel) {
            return null;
        }
        return new FishingSpot(worldPosition, fishingSpotPoly);
    }

    private Polygon getFishingSpotPoly(WorldPosition worldPosition) {
        Polygon tilePoly = script.getSceneProjector().getTilePoly(worldPosition, true);
        if (tilePoly == null) {
            return null;
        }
        if (!script.getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            return null;
        }
        return tilePoly;
    }

    private void walkToFishingSpot() {
        script.log("Fish", "No fishing spots visible on screen, walking to one...");
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            // keep polling for fishing spots and break out as soon as one is visible
            FishingSpot fishingSpot1 = getFishingSpot(myPosition);
            return fishingSpot1 != null;
        });
        script.getWalker().walkTo(getDestination(), walkConfig.build());
    }

    private WorldPosition getDestination() {
        WorldPosition myPosition = script.getWorldPosition();

        if (myPosition == null) {
            script.log("Fish", "Failed to get position, as it is null");
            return new WorldPosition(1, 1, 0);
        }

        List<Area> fishingSpots = fishingLocation.getFishingSpotAreas();
        if (fishingSpots.isEmpty()) {
            script.log("Fish", "No fishing spot areas defined.");
            return myPosition;
        }

        // Determine if we're currently in any of the fishing areas
        Area currentArea = fishingSpots.stream()
                .filter(area -> area.contains(myPosition))
                .findFirst()
                .orElse(null);

        List<Area> candidates = fishingSpots.stream()
                .filter(area -> area != currentArea)
                .toList();

        if (candidates.isEmpty()) {
            // We're not inside any, or only one area is defined
            candidates = fishingSpots;
        }

        Area chosen = candidates.get(RandomUtils.uniformRandom(candidates.size()));
        return chosen.getRandomPosition();
    }

    public boolean hasAllRequirements(Collection<Integer> requiredIds) {
        boolean hasBarbRod   = requiredIds.contains(ItemID.BARBARIAN_ROD);
        boolean needsFeather = requiredIds.contains(ItemID.FEATHER);

        // Collect all relevant search IDs (but skip equippable tools if already equipped)
        Set<Integer> searchIds = new HashSet<>();
        for (int requiredId : requiredIds) {
            switch (requiredId) {
                case ItemID.FISHING_ROD -> {
                    if (!hasToolEquipped) {
                        searchIds.addAll(TOOL_EQUIVALENTS.get("fishingrod"));
                    }
                }
                case ItemID.FLY_FISHING_ROD -> {
                    if (!hasToolEquipped) {
                        searchIds.addAll(TOOL_EQUIVALENTS.get("flyfishingrod"));
                    }
                }
                case ItemID.HARPOON -> {
                    if (!hasToolEquipped && !useBarehand) {
                        searchIds.addAll(TOOL_EQUIVALENTS.get("harpoon"));
                    }
                }
                case ItemID.OILY_FISHING_ROD -> {
                    if (!hasToolEquipped) {
                        searchIds.addAll(TOOL_EQUIVALENTS.get("oilyfishingrod"));
                    }
                }
                case ItemID.BARBARIAN_ROD -> {
                    if (!hasToolEquipped) {
                        searchIds.addAll(TOOL_EQUIVALENTS.get("barbarianrod"));
                    }
                }
                case ItemID.FEATHER -> {
                    if (hasBarbRod && needsFeather) {
                        searchIds.addAll(BAIT_EQUIVALENTS.get("barbbait"));
                    } else {
                        searchIds.add(requiredId);
                    }
                }
                case ItemID.SANDWORMS -> searchIds.addAll(BAIT_EQUIVALENTS.get("sandworm"));
                default -> searchIds.add(requiredId);
            }
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(searchIds));
        if (inv == null) {
            script.log("Fish", "Inventory not visible.");
            return false;
        }

        // Validate each requirement
        for (int requiredId : requiredIds) {
            boolean satisfied = switch (requiredId) {
                case ItemID.FISHING_ROD -> hasToolEquipped ||
                        inv.containsAny(Set.copyOf(TOOL_EQUIVALENTS.get("fishingrod")));
                case ItemID.FLY_FISHING_ROD -> hasToolEquipped ||
                        inv.containsAny(Set.copyOf(TOOL_EQUIVALENTS.get("flyfishingrod")));
                case ItemID.HARPOON -> hasToolEquipped || useBarehand ||
                        inv.containsAny(Set.copyOf(TOOL_EQUIVALENTS.get("harpoon")));
                case ItemID.OILY_FISHING_ROD -> hasToolEquipped ||
                        inv.containsAny(Set.copyOf(TOOL_EQUIVALENTS.get("oilyfishingrod")));
                case ItemID.BARBARIAN_ROD -> hasToolEquipped ||
                        inv.containsAny(Set.copyOf(TOOL_EQUIVALENTS.get("barbarianrod")));
                case ItemID.FEATHER -> {
                    if (hasBarbRod && needsFeather) {
                        yield inv.containsAny(Set.copyOf(BAIT_EQUIVALENTS.get("barbbait")));
                    } else {
                        yield inv.contains(ItemID.FEATHER);
                    }
                }
                case ItemID.SANDWORMS -> inv.containsAny(Set.copyOf(BAIT_EQUIVALENTS.get("sandworm")));
                default -> inv.containsAny(Set.of(requiredId));
            };

            if (!satisfied) {
                return false;
            }
        }

        return true;
    }
}
