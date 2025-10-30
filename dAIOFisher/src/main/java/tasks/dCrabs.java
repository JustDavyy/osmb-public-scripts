package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class dCrabs extends Task {
    private static final SearchablePixel[] FISHING_SPOT_PIXELS = new SearchablePixel[]{
            new SearchablePixel(-11366999, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-7555094, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8605987, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-3283474, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-6702368, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12288621, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12617586, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12420721, new SingleThresholdComparator(2), ColorModel.HSL)
    };
    private FishingSpot lastFishingSpot = null;
    private int consecutiveNoSpotChecks = 0;
    private int relocationAttempts = 0;

    public dCrabs(Script script) {
        super(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {

        // Retrieve inventory
        task = "Get inventory snapshot";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.DARK_FISHING_BAIT, ItemID.LOBSTER_POT, ItemID.COINS_995, ItemID.RAW_DARK_CRAB, ItemID.DARK_CRAB, ItemID.BURNT_DARK_CRAB));
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        // Retrieve player position
        task = "Get player position";
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) {
            return false;
        }

        // Check if we have required items
        task = "Check required items";
        if (!inventorySnapshot.containsAll(Set.of(ItemID.DARK_FISHING_BAIT, ItemID.LOBSTER_POT, ItemID.COINS_995))) {
            script.log(getClass().getSimpleName(), "Not all required tools could be located in inventory, logging out and stopping script!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        } else if (inventorySnapshot.getAmount(ItemID.COINS_995) < 10000) {
            script.log(getClass().getSimpleName(), "We have less than 10k coins, stopping script!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        }

        // If outside, enter resource area
        if (!fishingLocation.getFishingArea().contains(myPos)) {
            script.log(getClass(), "Outside resource area, entering it!");
            task = "Enter resource area";

            // Interact with door object

            return script.getWalker().walkTo(fishingLocation.getFishingArea().getRandomPosition());
        }

        // If inventory full, go cook or note
        if (inventorySnapshot.isFull()) {
            if (cookMode) {

            }
        }

        // If inventory not full, go fish
        if (alreadyCountedFish) {
            alreadyCountedFish = false;
        }

        if (!isCurrentlyFishing()) {
            return initiateFishingAction();
        }

        // We're currently fishing; monitor exit conditions
        return script.pollFramesHuman(this::earlyExitCheck, script.random(6000, 7000));
    }

    private boolean isCurrentlyFishing() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return false;
        }

        task = "Check last fishing spot";
        if (lastFishingSpot == null) return false;

        boolean stillValid = isValidFishingSpot(lastFishingSpot.getPosition()) != null;
        task = "Check XP Gained";
        boolean xpRecently = System.currentTimeMillis() - lastXpGained <= fishingMethod.getFishingDelay();

        task = "Check if currently fishing";
        return stillValid && xpRecently;
    }

    private boolean earlyExitCheck() {
        task = "Check for level-up";
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Early exit, TAP_HERE_TO_CONTINUE dialogue detected (inventory full or leveled up)");
            return true;
        }

        task = "Check AFK timer";
        if (switchTabTimer.hasFinished()) {
            script.log("PREVENT-LOG", "Switching tabs...");
            script.getWidgetManager().getTabManager().openTab(Tab.Type.values()[script.random(Tab.Type.values().length)]);
            switchTabTimer.reset(script.random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
            script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);
        }

        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log(getClass().getSimpleName(), "Inventory not visible.");
            return false;
        }

        if (!fishingLocation.equals(FishingLocation.Karamja_West)) {
            if (inventorySnapshot.isFull()) {
                script.log(getClass().getSimpleName(), "Early exit, Inventory is full");
                return true;
            }
        }

        task = "Check last fishing spot";
        if (lastFishingSpot != null) {
            FishingSpot current = isValidFishingSpot(lastFishingSpot.getPosition());
            if (current == null) {
                script.log(getClass(), "Fishing spot no longer valid — early exit.");
                return true;
            }
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

        Area spotArea = fishingLocation.getFishingArea();
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

    private void walkToFishingSpot() {
        script.log(getClass(), "No fishing spots visible on screen, walking to one...");
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
}
