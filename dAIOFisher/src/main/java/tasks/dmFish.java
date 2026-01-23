package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.pathing.CollisionManager;
import data.FishingLocation;
import utils.Task;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static main.dAIOFisher.*;

public class dmFish extends Task {
    private static final RectangleArea EAST_FISHING_SPOT_AREA = new RectangleArea(2617, 3443, 3, 1, 0);
    private static final RectangleArea WEST_FISHING_SPOT_AREA = new RectangleArea(2609, 3443, 3, 1, 0);
    private static final int FISH_TILE_HEIGHT = 15;
    private WorldPosition lastFishingSpot = null;
    private boolean justDodgedFlyingFish = false;

    // Chat history stuff
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    public dmFish(Script script) {
        super(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {
        script.log(getClass(), "Getting fishing spots...");
        task = "Get world position";
        WorldPosition myPosition = script.getWorldPosition();
        if (myPosition == null) {
            script.log(getClass(), "Failed to get position.");
            return false;
        }

        task = "Get fishing spots";
        List<WorldPosition> activeFishingSpots = getFishingSpots();
        if (activeFishingSpots.isEmpty()) {
            script.log(getClass(), "No active fishing spots found.");
            if (WEST_FISHING_SPOT_AREA.distanceTo(myPosition) > 1 && EAST_FISHING_SPOT_AREA.distanceTo(myPosition) > 1) {
                script.log(getClass(), "Walking to fishing area...");
                walkToFishingArea(myPosition);
            }
            return false;
        }
        script.log(getClass(), "Found active fishing spots on screen: " + activeFishingSpots.size());
        WorldPosition closestFishingSpot;

        if (justDodgedFlyingFish && lastFishingSpot != null && activeFishingSpots.size() > 1) {
            task = "Avoid flying fish";
            // Exclude the last spot and choose the next closest
            List<WorldPosition> filteredSpots = new ArrayList<>(activeFishingSpots);
            filteredSpots.remove(lastFishingSpot);
            closestFishingSpot = myPosition.getClosest(filteredSpots);
            script.log(getClass(), "Avoiding previous spot (flying fish). Using alternate fishing spot: " + closestFishingSpot);
            justDodgedFlyingFish = false; // reset flag
        } else {
            task = "Determine closest spot";
            closestFishingSpot = myPosition.getClosest(activeFishingSpots);
        }

        task = "Get fishing poly";
        Polygon tilePoly = script.getSceneProjector().getTilePoly(closestFishingSpot);
        if (tilePoly == null) {
            script.log(getClass(), "No tile polygon found for closest fishing spot: " + closestFishingSpot);
            return false;
        }
        task = "Start fishing action";
        if (!script.getFinger().tap(tilePoly, "small net")) {
            script.log(getClass(), "Failed to tap on fishing spot: " + closestFishingSpot);
            return false;
        }
        lastFishingSpot = closestFishingSpot;
        if (waitUntilAdjacentToFishingSpot()) {
            waitUntilFinishedFishing();
        }
        return false;
    }

    private static WorldPosition getClosestFishingAreaPosition(WorldPosition myPosition) {
        List<WorldPosition> eastSidePositions = EAST_FISHING_SPOT_AREA.getSurroundingPositions(1);
        List<WorldPosition> westSidePositions = WEST_FISHING_SPOT_AREA.getSurroundingPositions(1);
        WorldPosition closestEastPosition = myPosition.getClosest(eastSidePositions);
        WorldPosition closestWestPosition = myPosition.getClosest(westSidePositions);
        // get closest out of the two sides
        return closestEastPosition.distanceTo(myPosition) < closestWestPosition.distanceTo(myPosition) ? closestEastPosition : closestWestPosition;
    }

    private boolean waitUntilAdjacentToFishingSpot() {
        task = "Wait to arrive at spot";
        script.log(getClass(), "Waiting until we arrive at the fishing spot...");
        return script.pollFramesHuman(() -> {
            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                script.log(getClass(), "Failed to get position");
                return false;
            }
            List<WorldPosition> fishingSpots = getFishingSpots();
            if (fishingSpots.isEmpty()) {
                script.log(getClass(), "No fishing spots found...");
                return false;
            }
            return getAdjacentFishingSpot(fishingSpots, myPosition) != null;
        }, RandomUtils.uniformRandom(2500, 6000));
    }

    private void waitUntilFinishedFishing() {
        task = "Wait until finished fishing";
        script.log(getClass(), "Waiting until finished fishing...");

        script.pollFramesHuman(() -> {

            if (isFlyingFish()) {
                script.log(getClass(), "Flying fish text detected, dodging...");
                justDodgedFlyingFish = true;
                return true;
            }

            WorldPosition myPosition = script.getWorldPosition();
            if (myPosition == null) {
                script.log(getClass(), "Failed to get position");
                return false;
            }
            List<WorldPosition> fishingSpots = getFishingSpots();
            if (fishingSpots.isEmpty()) {
                script.log(getClass(), "No fishing spots found...");
                return false;
            }
            boolean isAdjacent = getAdjacentFishingSpot(fishingSpots, myPosition) != null;
            if (!isAdjacent) {
                script.log(getClass(), "Not adjacent to fishing spot");
                return true;
            }

            // Update caught count of fish
            List<Integer> fishIds = fishingMethod.getCatchableFish();
            ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(fishingMethod.getCatchableFish()));
            if (inv != null) {
                for (int id : fishIds) {
                    if (inv.getAmount(id) > 0) {
                        fish1Caught = inv.getAmount(id) - startAmount;
                        break;
                    }
                }
            }

            return false;
        }, RandomUtils.uniformRandom(16000, 22000));
        // random delay before next fishing attempt
        int randomDelay = RandomUtils.gaussianRandom(300, 5000, 500, 1500);
        if (!skipMinnowDelay) {
            if (!justDodgedFlyingFish) {
                script.log(getClass(), "⏳ - Executing humanised delay before next fishing attempt: " + randomDelay + "ms");
                script.pollFramesUntil(() -> false, randomDelay);
            } else {
                int dodgeDelay = RandomUtils.gaussianRandom(5, 300, 140, 40);
                dodgeDelay = Math.max(5, Math.min(300, dodgeDelay));
                script.log(getClass(), "Dodging fish, using smaller humanised delay for speed! Delay: " + dodgeDelay + "ms");
                script.pollFramesUntil(() -> false, dodgeDelay);
            }
        }
    }

    private WorldPosition getAdjacentFishingSpot(List<WorldPosition> fishingSpots, WorldPosition myPosition) {
        if (fishingSpots.isEmpty()) {
            return null;
        }
        for (WorldPosition fishingSpotPosition : fishingSpots) {
            if (CollisionManager.isCardinallyAdjacent(myPosition, fishingSpotPosition)) {
                return fishingSpotPosition;
            }
        }
        return null;
    }

    private void walkToFishingArea(WorldPosition myPosition) {
        WorldPosition targetPosition = getClosestFishingAreaPosition(myPosition);
        script.getWalker().walkTo(targetPosition);
    }

    private List<WorldPosition> getFishingSpots() {
        List<WorldPosition> fishingSpots = new ArrayList<>();
        fishingSpots.addAll(EAST_FISHING_SPOT_AREA.getAllWorldPositions());
        fishingSpots.addAll(WEST_FISHING_SPOT_AREA.getAllWorldPositions());
        List<WorldPosition> activeFishingSpots = new ArrayList<>();
        for (WorldPosition fishingSpot : fishingSpots) {
            if (checkForTileItem(fishingSpot)) {
                activeFishingSpots.add(fishingSpot);
                script.getScreen().queueCanvasDrawable("foundSpot=" + fishingSpot, canvas -> {
                    // draw fishing spot
                    Polygon tilePoly = script.getSceneProjector().getTilePoly(fishingSpot);
                    canvas.fillPolygon(tilePoly, Color.GREEN.getRGB(), 0.3);
                    canvas.drawPolygon(tilePoly, Color.RED.getRGB(), 1);
                });
            }
        }
        return activeFishingSpots;
    }

    private boolean checkForTileItem(WorldPosition tilePosition) {
        Point point = script.getSceneProjector().getTilePoint(tilePosition, null/*null means center point*/, FISH_TILE_HEIGHT);
        if (point == null) {
            script.log(getClass(), "No tile point found for position: " + tilePosition);
            return false;
        }
        Point tileItemPoint = new Point(point.x - (minnowTileImageTop.width / 2), point.y - (minnowTileImageTop.height / 2) - 20);
        int radius = 6;
        return searchForItemInRadius(tilePosition, tileItemPoint, radius, minnowTileImageTop, minnowTileImageBottom);
    }

    private boolean searchForItemInRadius(WorldPosition worldPosition, Point position, int radius, SearchableImage... itemImages) {
        for (int x = position.x - radius; x <= position.x + radius; x++) {
            for (int y = position.y - radius; y <= position.y + radius; y++) {
                for (SearchableImage itemImage : itemImages) {
                    if (script.getImageAnalyzer().isSubImageAt(x, y, itemImage) != null) {
                        script.getScreen().queueCanvasDrawable("harpoonFishTile-" + worldPosition, canvas -> {
                            // draw search area
                            com.osmb.api.shape.Rectangle tileItemArea = new Rectangle(position.x - radius, position.y - radius, itemImage.height + (radius * 2), itemImage.height + (radius * 2));
                            canvas.fillRect(tileItemArea, Color.GREEN.getRGB(), 0.3);
                            canvas.drawRect(tileItemArea, Color.RED.getRGB(), 1);
                        });
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isFlyingFish() {
        task = "Monitor chatbox";

        // Make sure game filter tab is selected
        if (script.getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            script.getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
            return false;
        }

        UIResultList<String> chatResult = script.getWidgetManager().getChatbox().getText();
        if (!chatResult.isFound() || chatResult.isEmpty()) {
            return false;
        }

        List<String> currentLines = chatResult.asList();
        if (currentLines.isEmpty()) return false;

        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return false;
            }

            int currSize = currentLines.size();
            int prevSize = PREVIOUS_CHATBOX_LINES.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;

                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(PREVIOUS_CHATBOX_LINES.get(j))) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        }

        List<String> newMessages = currentLines.subList(0, firstDifference);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);

        return processNewChatboxMessages(newMessages);
    }

    private boolean processNewChatboxMessages(List<String> newLines) {
        for (String message : newLines) {
            String lower = message.toLowerCase();

            if (lower.contains("jumps up")) {
                task = "Dodge flying fish";
                return true;
            }
        }
        return false;
    }
}
