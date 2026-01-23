package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import component.MushroomTransportInterface;
import utils.Task;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import static main.dBirdhouseRunner.*;
import static main.dBirdhouseRunner.currentPos;

import java.util.*;
import java.util.List;
import java.util.function.BooleanSupplier;

public class BirdHouseRun extends Task {

    // World positions
    private final WorldPosition verdantValleyNE = new WorldPosition(3768, 3761, 0);
    private final WorldPosition verdantValleySW = new WorldPosition(3763, 3755, 0);
    private final WorldPosition mushroomMeadowN = new WorldPosition(3677, 3882, 0);
    private final WorldPosition mushroomMeadowS = new WorldPosition(3679, 3815, 0);

    // Areas
    private final Area verdantValleyNEWalkArea = new RectangleArea(3766, 3759, 4, 1, 0);
    private final Area verdantValleySWWalkArea = new RectangleArea(3761, 3753, 2, 3, 0);
    private final Area mushroomMeadowNWalkArea = new RectangleArea(3675, 3878, 4, 3, 0);
    private final Area mushroomMeadowSWalkArea = new RectangleArea(3680, 3817, 2, 3, 0);
    private final Area mushroomMeadowMushroomWalkArea = new RectangleArea(3677, 3869, 2, 2, 0);
    private final Area mushroomMeadowMushroomArea = new RectangleArea(3671, 3866, 12, 9, 0);

    // Longs
    private long lastRun = 0;
    private long nextDelay = generateNextBHRun();

    // Booleans
    private boolean justArrivedFromBank = false;
    private boolean startLocationDecided = false;
    boolean startAtVerdantThisRun = false;
    private final List<String> currentVerdantOrder = new ArrayList<>();
    private final List<String> currentMushroomOrder = new ArrayList<>();
    private boolean birdhousesNotReady = false;
    private boolean resetWasFullyDone = false;

    // Caches
    private List<Rectangle> cachedTransportBoxes = new ArrayList<>();
    private List<Rectangle> sortedTransportBoxes = new ArrayList<>();

    // Strings
    private String birdhousesNotReadyString;

    private static final Random random = new Random();

    // Trackers
    private final Set<String> completedHouses = new HashSet<>();

    public BirdHouseRun(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return !needToBank && System.currentTimeMillis() - lastRun >= nextDelay;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();
        currentPos = script.getWorldPosition();

        lastRunType = "Birdhouse";

        if (currentPos == null) {
            script.log(getClass(), "⚠ World position is null, cannot verify current location. Returning!");
            return false;
        }

        if (birdhousesNotReady) {
            birdhousesNotReady = false;

            if (birdhousesNotReadyString != null && birdhousesNotReadyString.contains("Seed level")) {
                try {
                    int currentLevel = Integer.parseInt(birdhousesNotReadyString.replaceAll(".*Seed level is: (\\d+)/10.*", "$1"));

                    // this many seeds are left
                    int baseMinutes = currentLevel * 5; // remaining time
                    int bufferMinutes = RandomUtils.uniformRandom(7, 12);
                    int totalMinutes = baseMinutes + bufferMinutes;
                    int extraSeconds = RandomUtils.uniformRandom(0, 59);

                    lastRun = System.currentTimeMillis();
                    nextDelay = (totalMinutes * 60_000L) + (extraSeconds * 1_000L);
                    nextBirdhouseRunTime = lastRun + nextDelay;

                    script.log(getClass(), "Parsed seed level " + currentLevel + "/10. Setting next run in ~" +
                            String.format("00:%02d:%02d", totalMinutes, extraSeconds));
                } catch (Exception e) {
                    script.log(getClass(), "⚠ Failed to parse seed level: \"" + birdhousesNotReadyString + "\", using default logic.");
                }
            }

            if (nextDelay <= 0) {
                lastRun = System.currentTimeMillis();
                nextDelay = generateNextBHRun();
                nextBirdhouseRunTime = lastRun + nextDelay;
                script.log(getClass(), "Using default logic. Next run in " + (nextDelay / 60000) + " minutes.");
            }

            return false;
        }

        // Logic if we're still at the bank
        if (bankIslandArea.contains(currentPos)) {
            task = "Leave bank island";
            script.log(getClass().getSimpleName(), "It is time for a birdhouse run, going to main island from bank island.");
            return leaveBankIsland();
        }

        // Logic to go to verdant for randomization if needed
        if (justArrivedFromBank) {
            // Randomize whether to start at Verdant or not
            if (!startLocationDecided) {
                startAtVerdantThisRun = RandomUtils.uniformRandom(0, 100) < 35;
                startLocationDecided = true;
            }

            script.log(getClass(), "🎲 Random start: " + (startAtVerdantThisRun ? "Verdant Valley" : "Mushroom Meadow"));

            // If we're currently in Mushroom Meadow but need to start at Verdant, teleport immediately
            if (startAtVerdantThisRun && mushroomMeadowArea.contains(currentPos)) {
                task = "Move to Verdant Valley (random start)";
                if (!mushroomMeadowMushroomArea.contains(currentPos)) {
                    script.getWalker().walkTo(mushroomMeadowMushroomWalkArea.getRandomPosition());
                }
                if (mushroomMeadowMushroomArea.contains(script.getWorldPosition())) {
                    if (openMushroomTransport()) {
                        if (mushroomInterface.selectOption(MushroomTransportInterface.ButtonType.VERDANT_VALLEY)) {
                            justArrivedFromBank = false;
                        }
                        return true;
                    } else {
                        justArrivedFromBank = false;
                        return false;
                    }
                } else {
                    script.log(getClass().getSimpleName(), "Failed to move to Magic Mushtree.");
                }
            } else {
                justArrivedFromBank = false; // Reset for next cycle
            }
            return false;
        }

        // Logic if we're at verdant valley
        if (verdantValleyArea.contains(currentPos)) {

            // Verdant Valley is already done, but Mushroom Meadow isn't -> walk there
            if (verdantValleyDone() && !mushroomMeadowDone()) {
                task = "Verdant done, go to Mushroom Meadow";
                // Handle mushroom first (within range at verdant)
                if (openMushroomTransport()) {
                    task = "Teleport to Mushroom Meadow";
                    script.log(getClass(), "Teleport to Mushroom Meadow");
                    if (mushroomInterface.selectOption(MushroomTransportInterface.ButtonType.MUSHROOM_MEADOW)) {
                        boolean success = script.pollFramesHuman(() -> {
                            WorldPosition current = script.getWorldPosition();
                            return current != null && mushroomMeadowArea.contains(current);
                        }, RandomUtils.uniformRandom(14000, 17500));

                        script.log(getClass(), success ? "✅ Successfully arrived at Mushroom Meadow." : "⚠ Timed out waiting to arrive.");
                        return success;
                    } else {
                        return false;
                    }
                } else {
                    script.log(getClass(), "❌ Failed to open Mushroom Teleport system, returning!");
                    return false;
                }
            }

            if (verdantValleyDone()) {
                task = "Verdant Valley complete, ready to move";
                return true;
            }

            if (currentVerdantOrder.isEmpty()) {
                if (!completedHouses.contains("VV_NE")) currentVerdantOrder.add("VV_NE");
                if (!completedHouses.contains("VV_SW")) currentVerdantOrder.add("VV_SW");
                Collections.shuffle(currentVerdantOrder);
            }

            for (String house : new ArrayList<>(currentVerdantOrder)) {
                if (house.equals("VV_SW")) {
                    script.log(getClass().getSimpleName(), "Process VV_SW house");
                    task = "Process VV_SW house";
                    if (emptyBirdhouse(verdantValleySW, verdantValleySWWalkArea) && !birdhousesNotReady &&
                            buildBirdHouse(verdantValleySW, verdantValleySWWalkArea)) {
                        completedHouses.add("VV_SW");
                        currentVerdantOrder.remove("VV_SW");
                    }
                    if (birdhousesNotReady) {
                        return false;
                    }
                } else if (house.equals("VV_NE")) {
                    script.log(getClass().getSimpleName(), "Process VV_NE house");
                    task = "Process VV_NE house";
                    if (emptyBirdhouse(verdantValleyNE, verdantValleyNEWalkArea) && !birdhousesNotReady &&
                            buildBirdHouse(verdantValleyNE, verdantValleyNEWalkArea)) {
                        completedHouses.add("VV_NE");
                        currentVerdantOrder.remove("VV_NE");
                    }
                    if (birdhousesNotReady) {
                        return false;
                    }
                }
            }
        }

        // Logic if we're at mushroom meadow
        if (mushroomMeadowArea.contains(currentPos)) {

            // Mushroom Meadow is done, but Verdant isn't -> walk there
            if (mushroomMeadowDone() && !verdantValleyDone()) {
                task = "Meadow done, go to Verdant Valley";
                // Move to mushroom first if not within the area already
                if (!mushroomMeadowMushroomArea.contains(currentPos)) {
                    return script.getWalker().walkTo(mushroomMeadowMushroomWalkArea.getRandomPosition());
                }
                // Now interact with the mushroom
                if (openMushroomTransport()) {
                    task = "Teleport to Verdant Valley";
                    script.log(getClass(), "Teleport to Verdant Valley");
                    if (mushroomInterface.selectOption(MushroomTransportInterface.ButtonType.VERDANT_VALLEY)) {
                        boolean success = script.pollFramesHuman(() -> {
                            WorldPosition current = script.getWorldPosition();
                            return current != null && verdantValleyArea.contains(current);
                        }, RandomUtils.uniformRandom(14000, 17500));

                        script.log(getClass(), success ? "✅ Successfully arrived at Verdant Valley." : "⚠ Timed out waiting to arrive.");
                        return success;
                    }
                    else {
                        return false;
                    }
                } else {
                    script.log(getClass(), "❌ Failed to open Mushroom Teleport system, returning!");
                    return false;
                }
            }

            if (mushroomMeadowDone()) {
                task = "Mushroom Meadow complete, ready to move";
                return true;
            }

            if (currentMushroomOrder.isEmpty()) {
                if (!completedHouses.contains("MM_N")) currentMushroomOrder.add("MM_N");
                if (!completedHouses.contains("MM_S")) currentMushroomOrder.add("MM_S");
                Collections.shuffle(currentMushroomOrder);
            }

            for (String house : new ArrayList<>(currentMushroomOrder)) {
                if (house.equals("MM_N")) {
                    script.log(getClass().getSimpleName(), "Process MM_N house");
                    task = "Process MM_N house";
                    if (emptyBirdhouse(mushroomMeadowN, mushroomMeadowNWalkArea) && !birdhousesNotReady &&
                            buildBirdHouse(mushroomMeadowN, mushroomMeadowNWalkArea)) {
                        completedHouses.add("MM_N");
                        currentMushroomOrder.remove("MM_N");
                    }
                    if (birdhousesNotReady) {
                        return false;
                    }
                } else if (house.equals("MM_S")) {
                    task = "Process MM_S house";
                    script.log(getClass().getSimpleName(), "Process MM_S house");
                    if (emptyBirdhouse(mushroomMeadowS, mushroomMeadowSWalkArea) && !birdhousesNotReady &&
                            buildBirdHouse(mushroomMeadowS, mushroomMeadowSWalkArea)) {
                        completedHouses.add("MM_S");
                        currentMushroomOrder.remove("MM_S");
                    }
                    if (birdhousesNotReady) {
                        return false;
                    }
                }
            }
        }

        // Generate next run time randomly if we're done
        if (completedHouses.containsAll(Arrays.asList("VV_NE", "VV_SW", "MM_N", "MM_S"))) {
            task = "BH run completed, reset flags, clear caches";
            script.log(getClass().getSimpleName(), "Current run done, generating next run time.");
            birdhouseRuns++;
            lastRun = System.currentTimeMillis();
            nextDelay = generateNextBHRun();
            nextBirdhouseRunTime = lastRun + nextDelay;
            completedHouses.clear();
            cachedTransportBoxes.clear();
            sortedTransportBoxes.clear();
            startLocationDecided = false;
            script.log(getClass().getSimpleName(), "Next birdhouse run in " + (nextDelay / 60000) + " minutes.");
            needToBank = true;
        }

        return true;
    }

    private long generateNextBHRun() {
        int roll = RandomUtils.uniformRandom(0, 100);
        if (roll < 85) {
            // 85% chance between 55 and 70 minutes
            return (55 + random.nextInt(16)) * 60_000L;
        } else {
            // 15% chance between 70 and 85 minutes
            return (70 + random.nextInt(16)) * 60_000L;
        }
    }

    private boolean leaveBankIsland() {
        task = "Leaving bank island";
        script.log(getClass(), "Searching for the rowboat to leave the island...");

        task = "Get rowboat object";
        // Find all rowboat objects with the "Travel" action and that are reachable
        List<RSObject> rowboats = script.getObjectManager().getObjects(obj ->
                "Rowboat".equalsIgnoreCase(obj.getName()) &&
                        Arrays.asList(obj.getActions()).contains("Travel") &&
                        obj.canReach());

        if (rowboats.isEmpty()) {
            script.log(getClass(), "❌ No rowboat object found.");
            return false;
        }

        task = "Interact with rowboat";
        RSObject rowboat = (RSObject) script.getUtils().getClosest(rowboats);
        if (!rowboat.interact("Travel")) {
            script.log(getClass(), "❌ Failed to interact with the rowboat.");
            return false;
        }

        script.log(getClass(), "✅ Interacted with rowboat. Waiting for dialogue...");

        task = "Wait for dialogue";
        // Wait until dialogue of type TEXT_OPTION appears
        boolean dialogueAppeared = script.pollFramesUntil(() -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.TEXT_OPTION;
        }, RandomUtils.uniformRandom(6000, 10000));

        if (!dialogueAppeared) {
            script.log(getClass(), "❌ Dialogue option did not appear.");
            return false;
        }

        task = "Selecting 'Row to the camp'";
        script.getWidgetManager().getDialogue().selectOption("Row to the camp");

        script.log(getClass(), "✅ Dialogue option selected. Waiting to arrive...");

        task = "Wait to arrive in camp";
        boolean success = script.pollFramesUntil(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && campLandingArea.contains(current);
        }, RandomUtils.uniformRandom(14000, 17500));

        justArrivedFromBank = success;

        script.log(getClass(), success ? "✅ Successfully arrived in camp." : "⚠ Timed out waiting to arrive.");
        return success;
    }

    private boolean openMushroomTransport() {
        task = "Open mushroom transport";
        script.log(getClass(), "🔍 Searching for the Magic Mushtree to interact with...");

        task = "Search for Magic Mushtree";
        List<RSObject> mushtrees = script.getObjectManager().getObjects(obj ->
                "Magic Mushtree".equalsIgnoreCase(obj.getName()) &&
                        Arrays.asList(obj.getActions()).contains("Use"));

        if (mushtrees.isEmpty()) {
            script.log(getClass(), "❌ No Magic Mushtree object found.");
            return false;
        }

        RSObject mushtree = (RSObject) script.getUtils().getClosest(mushtrees);

        task = "Walk to Magic Mushtree";
        // Walk to it if we can't reach it
        if (!mushtree.canReach()) {
            script.log(getClass(), "⚠ Magic Mushtree not reachable, walking to it...");
            boolean walked = script.getWalker().walkTo(mushtree.getWorldPosition());
            if (!walked) {
                script.log(getClass(), "❌ Failed to walk to Magic Mushtree.");
                return false;
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(500, 2000));
        }

        task = "Interact with Magic Mushtree";
        if (!mushtree.interact("Use")) {
            script.log(getClass(), "❌ Failed to interact with the Magic Mushtree.");
            return false;
        }

        script.log(getClass(), "✅ Interacted with Magic Mushtree. Waiting for transport interface...");

        task = "Wait for interface";
        boolean interfaceAppeared = script.pollFramesUntil(() -> mushroomInterface.isVisible(), RandomUtils.uniformRandom(12500, 17500));

        if (!interfaceAppeared) {
            script.log(getClass(), "❌ Mushroom transport interface did not appear.");
            return false;
        }

        script.log(getClass(), "✅ Mushroom transport interface is now visible.");
        return true;
    }

    private boolean emptyBirdhouse(WorldPosition position, Area walkToArea) {
        task = "Check current position";
        WorldPosition currentPos = script.getWorldPosition();

        if (currentPos == null) {
            script.log(getClass(), "⚠ World position is null. Returning!");
            return false;
        }

        if (currentPos != null && currentPos.getRegionID() != position.getRegionID()) {
            task = "Travel to other region";
            script.log(getClass(), "⚠ Different region (" + currentPos.getRegionID() + " -> " + position.getRegionID() + "), walking to target region/area first...");
            boolean walked = script.getWalker().walkTo(walkToArea.getRandomPosition());
            if (!walked) {
                script.log(getClass(), "❌ Failed to walk to destination region.");
                return false;
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 2000));
        }

        task = "Locating birdhouse tile";
        RSTile interactTile = script.getSceneManager().getTile(position);
        if (interactTile == null) {
            script.log(getClass(), "Tile to interact with is null, cannot do birdhouse actions. Trying to walk there!");
            script.getWalker().walkTo(walkToArea.getRandomPosition());
            return false;
        }

        task = "Checking if tile is visible";
        if (!interactTile.isOnGameScreen()) {
            script.log(getClass(), "Tile to interact with is not on screen, walking...");
            script.getWalker().walkTo(walkToArea.getRandomPosition());
            return false;
        }

        task = "Taking pre-reset snapshot";
        ItemGroupResult before = script.getWidgetManager().getInventory().search(Set.of(
                ItemID.BIRD_NEST, ItemID.BIRD_NEST_5071, ItemID.BIRD_NEST_5072,
                ItemID.BIRD_NEST_5073, ItemID.BIRD_NEST_5074, ItemID.BIRD_NEST_5075,
                ItemID.CLUE_NEST_BEGINNER, ItemID.CLUE_NEST_EASY, ItemID.CLUE_NEST_MEDIUM,
                ItemID.CLUE_NEST_HARD, ItemID.CLUE_NEST_ELITE, usedLogsId
        ));

        task = "Attempting to empty birdhouse";
        script.log(getClass(), "Interacting with birdhouse tile for emptying...");
        if (!script.getFinger().tap(interactTile.getTileCube(20, 0).getResized(0.6), "Empty")) {
            script.log(getClass(), "Failed to long-press 'Empty' on birdhouse tile. Checking inventory");
            ItemGroupResult inbetween = script.getWidgetManager().getInventory().search(Set.of(
                    ItemID.CLOCKWORK
            ));
            if (inbetween.contains(ItemID.CLOCKWORK)) {
                script.log(getClass().getSimpleName(), "Inventory contains clockwork, empty/reset already happened? Continue!");
            } else {
                return false;
            }
        }

        task = "Waiting for empty interaction to complete";
        Timer amountChangeTimer = new Timer();
        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();

            if (type == DialogueType.TEXT_OPTION) {
                script.log(getClass(), "Birdhouse is not ready yet — detected TEXT_OPTION dialogue.");
                birdhousesNotReady = true;
                script.log(getClass().getSimpleName(), "Getting seed level count of birdhouse.");
                boolean success = script.getFinger().tap(interactTile.getTileCube(20, 0).getResized(0.6), "Seeds");
                if (success) {
                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(500, 1000));
                    birdhousesNotReadyString = script.getWidgetManager().getDialogue().getText().toString();
                } else {
                    script.log(getClass().getSimpleName(), "Failed to long press 'Seeds', fallback to default logic.");
                }
                return true;
            }

            if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1000, 3000));
                return true;
            }

            int timeout = RandomUtils.uniformRandom(12500, 15000);
            if (amountChangeTimer.timeElapsed() > timeout) {
                return true;
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CLOCKWORK));
            return inventorySnapshot != null && inventorySnapshot.contains(ItemID.CLOCKWORK);
        };

        script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1500, 3000));
        script.log(getClass(), "Using human task to wait until birdhouse empty is complete.");
        script.pollFramesHuman(condition, RandomUtils.uniformRandom(20000, 25000));

        task = "Taking post-reset snapshot";
        ItemGroupResult after = script.getWidgetManager().getInventory().search(Set.of(
                ItemID.BIRD_NEST, ItemID.BIRD_NEST_5071, ItemID.BIRD_NEST_5072,
                ItemID.BIRD_NEST_5073, ItemID.BIRD_NEST_5074, ItemID.BIRD_NEST_5075,
                ItemID.CLUE_NEST_BEGINNER, ItemID.CLUE_NEST_EASY, ItemID.CLUE_NEST_MEDIUM,
                ItemID.CLUE_NEST_HARD, ItemID.CLUE_NEST_ELITE
        ));

        task = "Updating nest statistics after reset";
        seedNests += Math.max(0, after.getAmount(ItemID.BIRD_NEST_5073) - before.getAmount(ItemID.BIRD_NEST_5073));
        ringNests += Math.max(0, after.getAmount(ItemID.BIRD_NEST_5074) - before.getAmount(ItemID.BIRD_NEST_5074));
        emptyNests += Math.max(0, after.getAmount(ItemID.BIRD_NEST_5075) - before.getAmount(ItemID.BIRD_NEST_5075));
        eggNests += Math.max(0,
                after.getAmount(ItemID.BIRD_NEST)
                        + after.getAmount(ItemID.BIRD_NEST_5071)
                        + after.getAmount(ItemID.BIRD_NEST_5072)
                        - (before.getAmount(ItemID.BIRD_NEST)
                        + before.getAmount(ItemID.BIRD_NEST_5071)
                        + before.getAmount(ItemID.BIRD_NEST_5072))
        );
        clueNests += Math.max(0,
                after.getAmount(ItemID.CLUE_NEST_BEGINNER)
                        + after.getAmount(ItemID.CLUE_NEST_EASY)
                        + after.getAmount(ItemID.CLUE_NEST_MEDIUM)
                        + after.getAmount(ItemID.CLUE_NEST_HARD)
                        + after.getAmount(ItemID.CLUE_NEST_ELITE)
                        - (before.getAmount(ItemID.CLUE_NEST_BEGINNER)
                        + before.getAmount(ItemID.CLUE_NEST_EASY)
                        + before.getAmount(ItemID.CLUE_NEST_MEDIUM)
                        + before.getAmount(ItemID.CLUE_NEST_HARD)
                        + before.getAmount(ItemID.CLUE_NEST_ELITE))
        );

        totalNests = seedNests + ringNests + emptyNests + eggNests + clueNests;

        task = "Birdhouse empty completed";
        return true;
    }

    private boolean buildBirdHouse(WorldPosition position, Area walkToArea) {
        RSTile interactTile = script.getSceneManager().getTile(position);
        if (interactTile == null) {
            script.log(getClass(), "Tile to interact with is null, cannot do birdhouse actions.");
            return false;
        }

        if (!interactTile.isOnGameScreen()) {
            script.log(getClass(), "Tile to interact with is not on screen, walking...");
            script.getWalker().walkTo(walkToArea.getRandomPosition());
            return false;
        }

        task = "Checking inventory for materials";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(
                selectedSeedId, ItemID.CLOCKWORK, usedLogsId, ItemID.CHISEL
        ));

        if (inventorySnapshot == null) return false;

        if (!inventorySnapshot.contains(selectedSeedId) ||
                !inventorySnapshot.contains(ItemID.CLOCKWORK) ||
                !inventorySnapshot.contains(ItemID.CHISEL)) {
            script.log(getClass().getSimpleName(), "Missing required items, stopping script...");
            script.stop();
            return false;
        }

        task = "Building birdhouse";
        List<Integer> materials = Arrays.asList(ItemID.CLOCKWORK, usedLogsId, ItemID.CHISEL);
        Collections.shuffle(materials);

        if (!resetWasFullyDone) {
            task = "Using " + script.getItemManager().getItemName(materials.get(0));
            if (!interactWithRetry(inventorySnapshot, materials.get(0))) return false;
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));

            task = "Using " + script.getItemManager().getItemName(materials.get(1));
            if (!interactWithRetry(inventorySnapshot, materials.get(1))) return false;

            task = "Handling make-dialogue or waiting for birdhouse to be created";
            Timer amountChangeTimer = new Timer();
            BooleanSupplier condition = () -> {
                DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();

                if (type == DialogueType.ITEM_OPTION) {
                    task = "Selecting birdhouse in dialogue";
                    boolean selected = script.getWidgetManager().getDialogue().selectItem(
                            selectedBirdhouseId,
                            ItemID.BIRD_HOUSE, ItemID.OAK_BIRD_HOUSE, ItemID.WILLOW_BIRD_HOUSE,
                            ItemID.TEAK_BIRD_HOUSE, ItemID.MAPLE_BIRD_HOUSE, ItemID.MAHOGANY_BIRD_HOUSE,
                            ItemID.YEW_BIRD_HOUSE, ItemID.MAGIC_BIRD_HOUSE, ItemID.REDWOOD_BIRD_HOUSE
                    );

                    if (!selected) {
                        script.sleep(RandomUtils.uniformRandom(150, 300)); // Retry after delay
                        selected = script.getWidgetManager().getDialogue().selectItem(
                                selectedBirdhouseId,
                                ItemID.BIRD_HOUSE, ItemID.OAK_BIRD_HOUSE, ItemID.WILLOW_BIRD_HOUSE,
                                ItemID.TEAK_BIRD_HOUSE, ItemID.MAPLE_BIRD_HOUSE, ItemID.MAHOGANY_BIRD_HOUSE,
                                ItemID.YEW_BIRD_HOUSE, ItemID.MAGIC_BIRD_HOUSE, ItemID.REDWOOD_BIRD_HOUSE
                        );
                    }

                    return selected; // proceed if selected successfully
                }

                if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                    script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1000, 3000));
                    return true;
                }

                int timeout = 15000;
                if (amountChangeTimer.timeElapsed() > timeout) return true;

                return Optional.ofNullable(script.getWidgetManager().getInventory()
                                .search(Set.of(selectedBirdhouseId)))
                        .map(inv -> inv.contains(selectedBirdhouseId)).orElse(false);
            };
            script.pollFramesHuman(condition, RandomUtils.uniformRandom(15000, 20000));

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(100, 200));
            task = "Placing birdhouse";
            if (!tapTileWithRetry(interactTile)) return false;
        }

        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(100, 200));
        // Feed the birdhouse seeds
        if (!feedWithRetry(selectedSeedId, interactTile)) return false;

        resetWasFullyDone = true;
        birdhousesPlaced++;
        updateXpForBirdhouseTier(selectedBirdhouseId);

        task = "Birdhouse built & fed successfully";
        return true;
    }

    private boolean interactWithRetry(ItemGroupResult inventory, int itemId) {
        for (int i = 0; i < 5; i++) {
            ItemSearchResult item = inventory.getItem(itemId);
            if (item != null && item.interact()) {
                return true;
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
        }
        script.log(getClass().getSimpleName(), "⚠ Failed to interact with item ID: " + itemId);
        return false;
    }

    private boolean tapTileWithRetry(RSTile interactTile) {
        for (int i = 0; i < 5; i++) {
            script.log(getClass(), "Interacting with birdhouse");
            if (!script.getFinger().tap(interactTile.getTileCube(20, 0).getResized(0.6))) {
                script.log(getClass(), "Failed to tap on Birdhouse.");
            } else {
                return true;
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
        }
        script.log(getClass().getSimpleName(), "⚠ Failed to interact with birdhouse");
        return false;
    }

    private boolean feedWithRetry(int itemId, RSTile interactTile) {
        for (int i = 0; i < 5; i++) {
            script.log(getClass(), "Attempt " + (i + 1) + ": Trying to feed birdhouse");

            // Snapshot before using seed
            ItemGroupResult before = script.getWidgetManager().getInventory().search(Set.of(itemId, ItemID.CLOCKWORK, usedLogsId));
            int countBefore = before.getAmount(itemId);

            task = "Selecting seed to feed";
            // Try to interact with the seed
            ItemSearchResult seedItem = before.getItem(itemId);
            if (seedItem == null || !seedItem.interact()) {
                script.log(getClass().getSimpleName(), "⚠ Failed to interact with seed (itemId=" + itemId + ")");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                continue;
            }

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(100, 200));

            task = "Feeding birdhouse";
            // Try to tap the birdhouse
            if (!script.getFinger().tap(interactTile.getTileCube(20, 0).getResized(0.6))) {
                script.log(getClass(), "⚠ Failed to tap on birdhouse.");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                continue;
            }

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(300, 500));

            // Snapshot after interaction
            ItemGroupResult after = script.getWidgetManager().getInventory().search(Set.of(itemId));
            int countAfter = after.getAmount(itemId);

            if (countAfter < countBefore) {
                script.log(getClass(), "✅ Successfully fed the birdhouse (seeds used).");
                return true;
            } else {
                script.log(getClass(), "⚠ Seeds not consumed, retrying...");
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                if (before.contains(ItemID.CLOCKWORK) && before.contains(usedLogsId)) {
                    if (!interactWithRetry(before, usedLogsId)) return false;
                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                    if (!interactWithRetry(before, ItemID.CLOCKWORK)) return false;

                    task = "Handling make-dialogue or waiting for birdhouse to be created";
                    Timer amountChangeTimer = new Timer();
                    BooleanSupplier condition = () -> {
                        DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();

                        if (type == DialogueType.ITEM_OPTION) {
                            task = "Selecting birdhouse in dialogue";
                            boolean selected = script.getWidgetManager().getDialogue().selectItem(
                                    selectedBirdhouseId,
                                    ItemID.BIRD_HOUSE, ItemID.OAK_BIRD_HOUSE, ItemID.WILLOW_BIRD_HOUSE,
                                    ItemID.TEAK_BIRD_HOUSE, ItemID.MAPLE_BIRD_HOUSE, ItemID.MAHOGANY_BIRD_HOUSE,
                                    ItemID.YEW_BIRD_HOUSE, ItemID.MAGIC_BIRD_HOUSE, ItemID.REDWOOD_BIRD_HOUSE
                            );

                            if (!selected) {
                                script.sleep(RandomUtils.uniformRandom(150, 300)); // Retry after delay
                                selected = script.getWidgetManager().getDialogue().selectItem(
                                        selectedBirdhouseId,
                                        ItemID.BIRD_HOUSE, ItemID.OAK_BIRD_HOUSE, ItemID.WILLOW_BIRD_HOUSE,
                                        ItemID.TEAK_BIRD_HOUSE, ItemID.MAPLE_BIRD_HOUSE, ItemID.MAHOGANY_BIRD_HOUSE,
                                        ItemID.YEW_BIRD_HOUSE, ItemID.MAGIC_BIRD_HOUSE, ItemID.REDWOOD_BIRD_HOUSE
                                );
                            }

                            return selected; // proceed if selected successfully
                        }

                        if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                            script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1000, 3000));
                            return true;
                        }

                        int timeout = 15000;
                        if (amountChangeTimer.timeElapsed() > timeout) return true;

                        return Optional.ofNullable(script.getWidgetManager().getInventory()
                                        .search(Set.of(selectedBirdhouseId)))
                                .map(inv -> inv.contains(selectedBirdhouseId)).orElse(false);
                    };
                    script.pollFramesHuman(condition, RandomUtils.uniformRandom(15000, 20000));

                    script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(100, 200));
                }
            }
        }

        script.log(getClass().getSimpleName(), "❌ Failed to feed the birdhouse after 5 attempts.");
        return false;
    }

    private boolean verdantValleyDone() {
        return completedHouses.contains("VV_NE") && completedHouses.contains("VV_SW");
    }

    private boolean mushroomMeadowDone() {
        return completedHouses.contains("MM_N") && completedHouses.contains("MM_S");
    }

    private void updateXpForBirdhouseTier(int birdhouseId) {
        switch (birdhouseId) {
            case ItemID.BIRD_HOUSE -> {
                craftingXpGained += 15;
                hunterXpGained += 280;
            }
            case ItemID.OAK_BIRD_HOUSE -> {
                craftingXpGained += 20;
                hunterXpGained += 420;
            }
            case ItemID.WILLOW_BIRD_HOUSE -> {
                craftingXpGained += 25;
                hunterXpGained += 560;
            }
            case ItemID.TEAK_BIRD_HOUSE -> {
                craftingXpGained += 30;
                hunterXpGained += 700;
            }
            case ItemID.MAPLE_BIRD_HOUSE -> {
                craftingXpGained += 35;
                hunterXpGained += 820;
            }
            case ItemID.MAHOGANY_BIRD_HOUSE -> {
                craftingXpGained += 40;
                hunterXpGained += 960;
            }
            case ItemID.YEW_BIRD_HOUSE -> {
                craftingXpGained += 45;
                hunterXpGained += 1020;
            }
            case ItemID.MAGIC_BIRD_HOUSE -> {
                craftingXpGained += 50;
                hunterXpGained += 1140;
            }
            case ItemID.REDWOOD_BIRD_HOUSE -> {
                craftingXpGained += 55;
                hunterXpGained += 1200;
            }
        }
    }
}
