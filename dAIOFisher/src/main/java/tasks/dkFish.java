package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import data.FishingLocation;
import data.FishingSpot;
import utils.Task;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class dkFish extends Task {
    private static final SearchablePixel[] FISHING_SPOT_PIXELS = new SearchablePixel[]{
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
    };

    private FishingSpot lastFishingSpot = null;
    private int consecutiveNoSpotChecks = 0;
    private int relocationAttempts = 0;

    public static long lastAnimationDetected = System.currentTimeMillis();
    private long currentIdleThreshold = getRandomIdleThreshold();
    private final PixelAnalyzer pixelAnalyzer = script.getPixelAnalyzer();

    public dkFish(Script script) {
        super(script);
    }

    public boolean activate() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getRequiredTools()));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
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
        if (!inventorySnapshot.containsAll(Set.copyOf(fishingMethod.getRequiredTools()))) {
            script.log(getClass().getSimpleName(), "Not all required tools could be located in inventory, stopping script!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        }

        return !inventorySnapshot.isFull();
    }

    public boolean execute() {

        if (alreadyCountedFish) {
            alreadyCountedFish = false;
        }

        task = "Check animation";
        boolean isAnimating = pixelAnalyzer.isPlayerAnimating(0.4);

        if (isAnimating) {
            lastAnimationDetected = System.currentTimeMillis();
            script.pollFramesHuman(this::earlyExitCheck, script.random(30000, 55000));
            return false;
        }

        long idleTime = System.currentTimeMillis() - lastAnimationDetected;

        if (idleTime >= currentIdleThreshold) {
            script.log(getClass().getSimpleName(), "No animation detected for " + idleTime + "ms. Re-initiating fishing.");
            boolean initiated = initiateFishingAction();
            if (initiated) {
                lastAnimationDetected = System.currentTimeMillis(); // reset timer
                currentIdleThreshold = getRandomIdleThreshold();    // randomize next threshold
            }
            return initiated;
        }

        return false;
    }

    private boolean earlyExitCheck() {
        task = "Monitor wait condition";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return true;
        }

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (inventorySnapshot.isFull()) {
            script.log(getClass().getSimpleName(), "Early exit, Inventory is full");
            return true;
        }

        boolean isAnimating = pixelAnalyzer.isPlayerAnimating(0.4);

        if (isAnimating) {
            lastAnimationDetected = System.currentTimeMillis(); // refresh animation timer
            return false;
        }

        long idleTime = System.currentTimeMillis() - lastAnimationDetected;
        if (idleTime >= currentIdleThreshold) {
            script.log(getClass().getSimpleName(), "Early exit due to inactivity for " + idleTime + "ms.");
            return true;
        }

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

        script.log(getClass(), "No visible fishing spots, walking to another area...");
        if (fishingMethod.getName().equals("Small Fishing Net (SafeMode/10HP)")) {
            script.log(getClass(), "SafeMode method in use, skipping relocation and hopping immediately.");
            script.getProfileManager().forceHop();
            relocationAttempts = 0;
            consecutiveNoSpotChecks = 0;
            return false;
        }

        relocationAttempts++;
        script.getWalker().walkTo(getDestination(), new WalkConfig.Builder().disableWalkScreen(true).breakDistance(5).build());

        if (relocationAttempts >= 3) {
            script.log(getClass(), "Still no fishing spots after 3 relocations. Forcing world hop...");
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
            script.log(getClass(), "Still no fishing spots after relocating.");
        }

        return false;
    }

    private boolean interactWithFishingSpot(FishingSpot fishingSpot) {
        script.log(getClass(), "Interacting with fishing spot at " + fishingSpot.getPosition());

        boolean clicked = script.getFinger().tap(fishingSpot.getFishingSpotPoly().getResized(0.85), menuHook);
        if (!clicked) {
            script.log(getClass(), "Failed to click fishing spot.");
            lastFishingSpot = null;
            return false;
        }

        lastXpGained = System.currentTimeMillis();
        return true;
    }

    private boolean attemptFishing(FishingSpot fishingSpot) {
        RSTile spotTile = script.getSceneManager().getTile(fishingSpot.getPosition());

        if (spotTile != null && spotTile.isOnGameScreen()) {
            script.log(getClass(), "Fishing spot is on game screen, interacting directly.");
            return interactWithFishingSpot(fishingSpot);
        } else {
            script.log(getClass(), "Fishing spot is not on screen, walking to it...");
            WalkConfig.Builder walkConfig = new WalkConfig.Builder();
            walkConfig.breakCondition(() -> {
                RSTile t = script.getSceneManager().getTile(fishingSpot.getPosition());
                return t != null && t.isOnGameScreen();
            });
            script.getWalker().walkTo(fishingSpot.getPosition(), walkConfig.build());
            lastXpGained = System.currentTimeMillis() - 21000;
        }

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute – resetting as we just performed an action.");
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
        }

        return false;
    }

    private FishingSpot getFishingSpot(WorldPosition worldPosition) {
        long start = System.currentTimeMillis();

        Set<WorldPosition> fishingSpotPositions = fishingMethod.getFishingSpots();

        List<FishingSpot> activeFishingSpotsOnScreen = getFishingSpots(fishingSpotPositions);
        if (activeFishingSpotsOnScreen.isEmpty()) {
            consecutiveNoSpotChecks++;
            script.log(getClass(), "No fishing spots found (" + consecutiveNoSpotChecks + " check(s) in a row)");

            if (fishingMethod.getName().equals("Small Fishing Net (SafeMode/10HP)")) {
                script.log(getClass(), "SafeMode method detected, forcing hop immediately...");
                script.getProfileManager().forceHop();
                consecutiveNoSpotChecks = 0;
                relocationAttempts = 0;
                return null;
            }

            if (consecutiveNoSpotChecks >= 3) {
                script.log(getClass(), "No fishing spots found for 3 consecutive checks. Forcing world hop...");
                script.getProfileManager().forceHop();
                consecutiveNoSpotChecks = 0;
                relocationAttempts = 0;
            }
            return null;
        }

        // Found spots; reset counter
        consecutiveNoSpotChecks = 0;

        long end = System.currentTimeMillis();
        script.log(getClass(), "Fishing spots found in: " + (end - start) + "ms");

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

    private WorldPosition getDestination() {
        WorldPosition myPosition = script.getWorldPosition();

        if (myPosition == null) {
            script.log(getClass(), "Failed to get position, as it is null");
            return new WorldPosition(1, 1, 0);
        }

        List<Area> fishingSpots = fishingLocation.getFishingSpotAreas();
        if (fishingSpots.isEmpty()) {
            script.log(getClass(), "No fishing spot areas defined.");
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

        Area chosen = candidates.get(script.random(candidates.size()));
        return chosen.getRandomPosition();
    }

    private long getRandomIdleThreshold() {
        return ThreadLocalRandom.current().nextLong(8_000, 13_000); // 8 to 13 sec
    }
}
