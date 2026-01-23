package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.ImageAnalyzer;
import com.osmb.api.utils.RandomUtils;
import component.MushroomTransportInterface;
import utils.Task;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static main.dBirdhouseRunner.*;

public class BankTask extends Task {
    // Caches
    private List<Rectangle> cachedTransportBoxes = new ArrayList<>();
    private List<Rectangle> sortedTransportBoxes = new ArrayList<>();

    // Snapshots
    private ItemGroupResult inventorySnapshot;
    private ItemGroupResult bankSnapshot;

    // Booleans
    private boolean triedToWithdraw = false;

    // Iterators
    private Iterator<Runnable> withdrawStepIterator = null;

    // Bank stuff
    public static final String[] BANK_NAMES = {"Bank Chest-wreck"};
    public static final String[] BANK_ACTIONS = {"use"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    public BankTask(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return needToBank;
    }

    @Override
    public boolean execute() {
        task = getClass().getSimpleName();

        currentPos = script.getWorldPosition();

        if (currentPos == null) {
            script.log(getClass(), "⚠ World position is null, cannot verify current location. Returning!");
            return false;
        }

        // Logic if we're at verdant valley
        if (verdantValleyArea.contains(currentPos)) {
            task = "Travel to Mushroom Meadow";
            return handleVerdantValley();
        }

        // Logic if we're at the camp landing area
        if (campLandingArea.contains(currentPos)) {
            task = "Use boat to bank island";
            return boatToBankIsland();
        }

        // Logic if we're at mushroom meadow
        if (mushroomMeadowArea.contains(currentPos)) {
            task = "Move to boat in museum camp";
            return script.getWalker().walkTo(campLandingArea.getRandomPosition());
        }

        // Logic if we're in the farming area
        if (underwaterArea.contains(currentPos)) {
            task = "Go back upstairs";
            return chainToMainland();
        }

        // Logic if we're at the bank island
        if (bankIslandArea.contains(currentPos)) {
            task = "Banking";
            script.log(getClass(), "At bank island. Evaluating which run is due first...");

            long now = System.currentTimeMillis();
            long birdhouseWait = Math.max(0, nextBirdhouseRunTime - now);
            long seaweedWait = enableSeaweedRun ? Math.max(0, nextSeaweedRunTime - now) : Long.MAX_VALUE;

            if (birdhouseWait < seaweedWait) {
                script.log(getClass(), "Birdhouse run is due first. Time remaining: " + formatTime(birdhouseWait));
                if (bankForBirdhouse()) {
                    return waitForNextRun();
                } else {
                    return false;
                }
            } else {
                script.log(getClass(), "Seaweed run is due first. Time remaining: " + formatTime(seaweedWait));
                if (bankForSeaweedRun()) {
                    return waitForNextRun();
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean waitForNextRun() {
        return false;
    }

    private boolean bankForBirdhouse() {
        // Open bank first
        if (!openBank()) {
            return false;
        }

        // Bank item by item and keep tools (if we're not in banking process already)
        if (!triedToWithdraw) {
            if (lastRunType.equals("Birdhouse")) {
                task = "Deposit all but needed items";
                script.getWidgetManager().getBank().depositAll(Set.of(ItemID.HAMMER, ItemID.CHISEL, selectedSeedId, usedLogsId));
            } else { // Use deposit all here as it's quicker
                task = "Deposit all";
                script.getWidgetManager().getBank().depositAll(Collections.emptySet());
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 1500));
        }

        task = "Take inventory snapshot";
        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.HAMMER, ItemID.CHISEL, selectedSeedId, usedLogsId));
        task = "Take bank snapshot";
        bankSnapshot = script.getWidgetManager().getBank().search(Set.of(ItemID.HAMMER, ItemID.CHISEL, selectedSeedId, usedLogsId));

        // Withdraw logic
        task = "Withdraw needed items";

        if (withdrawStepIterator == null) {
            List<Runnable> steps = new ArrayList<>(List.of(
                    () -> withdrawIfMissing(ItemID.HAMMER, 1, "hammer"),
                    () -> withdrawIfMissing(ItemID.CHISEL, 1, "chisel"),
                    () -> withdrawIfMissing(selectedSeedId, 40, "seeds"),
                    () -> withdrawIfMissing(usedLogsId, 4, "logs")
            ));
            Collections.shuffle(steps);
            withdrawStepIterator = steps.iterator();
        }

        if (withdrawStepIterator.hasNext()) {
            Runnable step = withdrawStepIterator.next();
            step.run();
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(100, 300));
            return true;
        }

        withdrawStepIterator = null;
        triedToWithdraw = false;

        task = "Close bank";
        script.getWidgetManager().getBank().close();

        // Check if we have all items needed and mark banking as done
        inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.HAMMER, ItemID.CHISEL, selectedSeedId, usedLogsId));

        if (inventorySnapshot.contains(ItemID.HAMMER) ||
            inventorySnapshot.contains(ItemID.CHISEL) ||
            inventorySnapshot.getAmount(selectedSeedId) >= 40 ||
            inventorySnapshot.getAmount(usedLogsId) >= 4) {

            script.log(getClass().getSimpleName(), "We have all required items, changing needToBank flag to false.");
            needToBank = false;
        }

        return false;
    }

    private boolean bankForSeaweedRun() {
        // Open bank first
        if (!openBank()) {
            return false;
        }


        return false;
    }

    private boolean openBank() {
        if (script.getWidgetManager().getBank().isVisible()) {
            return true;
        }
        task = "Open bank";
        script.log(getClass().getSimpleName(), "Searching for a bank...");

        List<RSObject> banksFound = script.getObjectManager().getObjects(bankQuery);
        if (banksFound.isEmpty()) {
            script.log(getClass().getSimpleName(), "No bank objects found.");
            return false;
        }

        RSObject bank = (RSObject) script.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            script.log(getClass().getSimpleName(), "Failed to interact with bank object.");
            return false;
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        task = "Wait for open bank";
        return script.pollFramesUntil(() -> {
            WorldPosition current = script.getWorldPosition();
            if (current == null) return false;

            if (!Objects.equals(current, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(current);
            }

            return script.getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private boolean chainToMainland() {
        task = "Search for anchor rope";
        script.log(getClass(), "Searching for the Anchor rope to interact with...");

        // Find all anchor rope objects with the "Climb" action and that are reachable
        List<RSObject> anchors = script.getObjectManager().getObjects(obj ->
                "Anchor rope".equalsIgnoreCase(obj.getName()) &&
                        Arrays.asList(obj.getActions()).contains("Climb") &&
                        obj.canReach());

        if (anchors.isEmpty()) {
            script.log(getClass(), "❌ No anchor rope object found.");
            return false;
        }

        task = "Climb anchor rope";
        RSObject rowboat = (RSObject) script.getUtils().getClosest(anchors);
        if (!rowboat.interact("Climb")) {
            script.log(getClass(), "❌ Failed to interact with the anchor rope.");
            return false;
        }

        script.log(getClass(), "✅ Interacted with anchor rope. Waiting for to arrive on mainland...");

        task = "Wait to arrive at bank island";
        boolean success = script.pollFramesUntil(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && bankIslandArea.contains(current);
        }, RandomUtils.uniformRandom(14000, 17500));

        script.log(getClass(), success ? "✅ Successfully arrived at the bank island." : "⚠ Timed out waiting to arrive.");
        return success;
    }

    private boolean boatToBankIsland() {
        script.log(getClass(), "Searching for the rowboat to interact with...");

        task = "Search for rowboat";
        // Find all rowboat objects with the "Travel" action and that are reachable
        List<RSObject> rowboats = script.getObjectManager().getObjects(obj ->
                "Rowboat".equalsIgnoreCase(obj.getName()) &&
                        obj.getActions() != null &&
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

        task = "Selecting 'Row out to sea, north of the island'";
        script.getWidgetManager().getDialogue().selectOption("Row out to sea, north of the island");

        script.log(getClass(), "✅ Dialogue option selected. Waiting to arrive...");

        task = "Wait to arrive at bank island";
        boolean success = script.pollFramesUntil(() -> {
            WorldPosition current = script.getWorldPosition();
            return current != null && bankIslandArea.contains(current);
        }, RandomUtils.uniformRandom(14000, 17500));

        script.log(getClass(), success ? "✅ Successfully arrived at the bank island." : "⚠ Timed out waiting to arrive.");
        return success;
    }

    private boolean handleVerdantValley() {
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

    private boolean openMushroomTransport() {
        task = "Open mushroom transport";
        script.log(getClass(), "🔍 Searching for the Magic Mushtree to interact with...");

        List<RSObject> mushtrees = script.getObjectManager().getObjects(obj ->
                "Magic Mushtree".equalsIgnoreCase(obj.getName()) &&
                        Arrays.asList(obj.getActions()).contains("Use"));

        if (mushtrees.isEmpty()) {
            script.log(getClass(), "❌ No Magic Mushtree object found.");
            return false;
        }

        RSObject mushtree = (RSObject) script.getUtils().getClosest(mushtrees);

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

        if (!mushtree.interact("Use")) {
            script.log(getClass(), "❌ Failed to interact with the Magic Mushtree.");
            return false;
        }

        script.log(getClass(), "✅ Interacted with Magic Mushtree. Waiting for transport interface...");

        boolean interfaceAppeared = script.pollFramesUntil(() -> mushroomInterface.isVisible(), RandomUtils.uniformRandom(10000, 15000));

        if (!interfaceAppeared) {
            script.log(getClass(), "❌ Mushroom transport interface did not appear.");
            return false;
        }

        script.log(getClass(), "✅ Mushroom transport interface is now visible.");
        return true;
    }

    private List<Rectangle> findMushroomTransportBoxes() {
        List<Rectangle> validBoxes = new ArrayList<>();
        final int expectedWidth = 200;
        final int expectedHeight = 52;
        final int maxSizeOffset = 5;

        ImageAnalyzer analyzer = script.getImageAnalyzer();

        // Use sprite IDs for corners to detect container boxes
        List<Rectangle> containers = analyzer.findContainers(
                script.getWidgetManager().getCenterComponentBounds(), 913, 914, 915, 916);

        for (Rectangle box : containers) {
            int widthDiff = Math.abs(box.width - expectedWidth);
            int heightDiff = Math.abs(box.height - expectedHeight);

            if (widthDiff <= maxSizeOffset && heightDiff <= maxSizeOffset) {
                validBoxes.add(box);
            }
        }

        if (validBoxes.isEmpty()) {
            script.log(getClass(), "⚠ No valid mushroom transport boxes found.");
        } else {
            script.log(getClass(), "✅ Found " + validBoxes.size() + " mushroom transport boxes.");

            List<Rectangle> sorted = validBoxes.stream()
                    .sorted(Comparator.comparingInt(r -> r.y))
                    .toList();

            String[] labels = {"House on the Hill", "Verdant Valley", "Sticky Swamp", "Mushroom Meadow"};

            for (int i = 0; i < sorted.size() && i < labels.length; i++) {
                Rectangle box = sorted.get(i);
                script.getScreen().getDrawableCanvas().drawRect(box, 0x00FF00);
                script.getScreen().getDrawableCanvas().drawText(
                        labels[i],
                        box.x + 4,
                        box.y + 18,
                        0x00FF00,
                        new Font("Arial", Font.BOLD, 12)
                );
                script.log(getClass(), "📦 Box " + (i + 1) + ": " + labels[i] + " at " + box);
            }
        }

        return validBoxes;
    }

    private boolean isMushroomTransportInterfaceVisible() {
        cachedTransportBoxes = findMushroomTransportBoxes();
        sortedTransportBoxes = cachedTransportBoxes.stream()
                .sorted(Comparator.comparingInt(r -> r.y)) // top to bottom
                .collect(Collectors.toList());
        return sortedTransportBoxes.size() == 4;
    }

    private Rectangle getMushroomTransportBox(int index) {
        if (sortedTransportBoxes == null || sortedTransportBoxes.size() < index || index < 1) return null;
        return sortedTransportBoxes.get(index - 1); // 1-based index
    }

    private boolean tapShapeWithRetry(Shape interactShape) {
        for (int i = 0; i < 10; i++) {
            if (!script.getFinger().tap(interactShape)) {
                script.log(getClass(), "Failed to tap teleport box");
            } else {
                return true;
            }
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
        }
        script.log(getClass().getSimpleName(), "⚠ Failed to tap teleport box");
        return false;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private boolean withdrawIfMissing(int itemId, int requiredAmount, String itemName) {
        int invAmount = inventorySnapshot.getAmount(itemId);
        if (invAmount >= requiredAmount)
            return false;

        if (triedToWithdraw && !bankSnapshot.contains(itemId)) {
            script.log(getClass().getSimpleName(), "Couldn't withdraw " + itemName + ", and " + itemName + " not found in the bank. Stopping script!");
            script.stop();
            return false;
        }

        task = "Withdraw " + itemName;
        triedToWithdraw = true;
        return script.getWidgetManager().getBank().withdraw(itemId, requiredAmount - invAmount);
    }
}
