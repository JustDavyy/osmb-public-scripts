package tasks;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.tabs.TabManager;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static main.dCastlewarsAFKer.*;

public class CwarsSlave extends Task {

    // Red side
    private static final Area redSideUpstairsArea = new RectangleArea(2368, 3127, 10, 8, 2);
    private static final Area redSideUpstairsAFKArea = new RectangleArea(2369, 3135, 4, 0, 2);
    private static final Area redSideInCastleLobbyArea = new RectangleArea(2368, 3127, 8, 8, 1);
    private static final Area redSideLadderArea = new RectangleArea(2369, 3132, 3, 3, 1);
    private static final WorldPosition redLobbyLadder = new WorldPosition(2370, 3133, 1);
    // Red lobby stairs to AFK = Ladder, Climb-up, Loc: 2370, 3133, 1

    // Blue side
    private static final Area blueSideUpstairsArea = new RectangleArea(2423, 3072, 8, 8, 2);
    private static final Area blueSideUpstairsAFKArea = new RectangleArea(2426, 3072, 3, 0, 2);
    private static final Area blueSideInCastleLobbyArea = new RectangleArea(2423, 3072, 8, 8, 1);
    private static final Area blueSideLadderArea = new RectangleArea(2428, 3073, 3, 3, 1);
    private static final WorldPosition blueLobbyLadder = new WorldPosition(2429, 3074, 1);
    // blue lobby stairs to AFK = Ladder, Climb-up, Loc: 2429, 3074, 1

    // Lobby
    private static final Area redLobbyArea = new RectangleArea(2406, 9513, 27, 21, 0);
    private static final Area redLobbyWalkArea = new RectangleArea(2416, 9520, 9, 6, 0);
    private static final Area blueLobbyArea = new RectangleArea(2362, 9480, 35, 23, 0);
    private static final Area blueLobbyWalkArea = new RectangleArea(2377, 9485, 9, 7, 0);

    // Castle lobby
    private static final Area guthixPortalWalkArea = new RectangleArea(2438, 3088, 3, 3, 0);

    // Position
    private static AtomicReference<WorldPosition> currentPos;

    // Trackers
    private int upstairsWalkFailures = 0;
    private boolean upstairsWalkBlocked = false;

    // Chat history stuff
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();
    private int lastTicketsTotalSeen  = -1;
    private int lastPlauditsTotalSeen = -1;
    private int pendingTicketsAddedFromAwarded  = 0;
    private int pendingPlauditsAddedFromAwarded = 0;


    public CwarsSlave(Script script) {
        super(script);
    }

    public boolean activate() {
        return setupDone;
    }

    public boolean execute() {

        // Monitor chat always
        monitorChatbox();

        currentPos = new AtomicReference<>(script.getWorldPosition());
        if (currentPos.get() == null) return false;

        updateLocation();

        // 1. If within castle wars outside the lobby
        if (castleWarsArea.contains(currentPos.get())) {
            monitorChatbox();
            canBreakNow = true;
            canHopNow = true;
            upstairsWalkBlocked = false;
            upstairsWalkFailures = 0;
            updateLocation();
            if (script.getProfileManager().isDueToBreak()) {
                monitorChatbox();
                return false;
            }
            // Join guthix portal
            if (!handleObject("Guthix Portal", "Enter", null, guthixPortalWalkArea)) {
                return false;
            }

            // Wait till we are no longer in the castle wars castle lobby
            BooleanSupplier condition = () -> {
                currentPos.set(script.getWorldPosition());
                if (currentPos.get() == null) return false;

                return !castleWarsArea.contains(currentPos.get());
            };

            task = "Wait till new area arrival";
            return script.pollFramesHuman(condition, script.random(6000, 10000));
        }

        // 2. If inside a waiting lobby
        if (redLobbyArea.contains(currentPos.get()) || blueLobbyArea.contains(currentPos.get())) {
            canBreakNow = true;
            canHopNow = true;
            upstairsWalkBlocked = false;
            upstairsWalkFailures = 0;
            task = "Check dialogue";
            if (isTextOptionDialogueOpen()) {
                script.log(getClass(), "Early join dialogue detected, joining!");
                return script.getWidgetManager().getDialogue().selectOption("Yes please!");
            }

            updateLocation();

            task = "Check AFK timer & Handle";
            doAntiAfk(true, getLobbyWalkArea());

            // Wait until: left lobby area OR anti-AFK timer finished OR text-option dialogue opens
            BooleanSupplier condition = () -> {
                currentPos.set(script.getWorldPosition());
                var pos = currentPos.get();

                boolean inLobbyArea = (pos != null) && getWaitLobbyArea().contains(pos);
                boolean timerDone   = (switchTabTimer != null) && switchTabTimer.hasFinished();
                boolean dialogueOpen = isTextOptionDialogueOpen();

                // proceed when left the lobby, or it's time for AFK action, or a dialogue popped
                return (!inLobbyArea) || timerDone || dialogueOpen;
            };

            task = "Wait till next action";
            return script.pollFramesHuman(condition, script.random(120_000, 270_000));
        }

        // 3. If inside the castle lobby area
        if (redSideInCastleLobbyArea.contains(currentPos.get()) || blueSideInCastleLobbyArea.contains(currentPos.get())) {
            canBreakNow = false;
            canHopNow = false;
            updateLocation();
            // Climb up ladder
            if (!handleObject("Ladder", "climb-up", getLadderPosition(), getLadderArea())) {
                return false;
            }

            Area insideArea = getSideLobbyArea();

            // Wait till we are no longer in the castle side lobby area
            BooleanSupplier condition = () -> {
                currentPos.set(script.getWorldPosition());
                if (currentPos.get() == null) return false;

                return !insideArea.contains(currentPos.get());
            };

            task = "Wait till new area arrival";
            return script.pollFramesHuman(condition, script.random(6000, 10000));
        }

        // 4. If inside the Upstairs AFK area
        if (redSideUpstairsAFKArea.contains(currentPos.get()) || blueSideUpstairsAFKArea.contains(currentPos.get())) {
            canBreakNow = false;
            canHopNow = false;
            upstairsWalkBlocked = false;
            upstairsWalkFailures = 0;
            updateLocation();

            task = "Check AFK timer & Handle";
            doAntiAfk(false, null); // tabs only here

            // Wait until: left AFK area OR anti-AFK timer finished
            BooleanSupplier condition = () -> {
                currentPos.set(script.getWorldPosition());
                var pos = currentPos.get();

                boolean inAfkArea = (pos != null) && getUpstairsAFKArea().contains(pos);
                boolean timerDone = (switchTabTimer != null) && switchTabTimer.hasFinished();

                // proceed when either we left the AFK area OR it's time to do a new AFK action
                return (!inAfkArea) || timerDone;
            };

            task = "Wait till next action";
            return script.pollFramesHuman(condition, script.random(120_000, 270_000));
        }

        // 5. If inside the Upstairs area
        if (redSideUpstairsArea.contains(currentPos.get()) || blueSideUpstairsArea.contains(currentPos.get())) {
            canBreakNow = false;
            canHopNow = false;
            updateLocation();

            // If we previously failed too many times, just idle here instead of trying again
            if (upstairsWalkBlocked) {
                task = "AFKing upstairs (walk possibly blocked)";
                doAntiAfk(false, null);

                // Wait until: left AFK area OR anti-AFK timer finished
                BooleanSupplier condition = () -> {
                    currentPos.set(script.getWorldPosition());
                    var pos = currentPos.get();

                    boolean inAfkArea = (pos != null) && getUpstairsAFKArea().contains(pos);
                    boolean timerDone = (switchTabTimer != null) && switchTabTimer.hasFinished();

                    // proceed when either we left the AFK area OR it's time to do a new AFK action
                    return (!inAfkArea) || timerDone;
                };

                return script.pollFramesHuman(condition, script.random(120_000, 270_000));
            }

            WalkConfig precise = new WalkConfig.Builder()
                    .enableRun(true)
                    .breakDistance(0)
                    .tileRandomisationRadius(0)
                    .timeout(20_000)
                    .allowInterrupt(true)
                    .breakCondition(() -> getUpstairsAFKArea().contains(script.getWorldPosition()))
                    .build();

            task = "Walk to upstairs AFK area";
            boolean walked = script.getWalker().walkTo(getUpstairsAFKArea().getRandomPosition(), precise);

            if (!walked) {
                upstairsWalkFailures++;
                if (upstairsWalkFailures >= 5) {
                    upstairsWalkBlocked = true;
                    script.log(getClass(), "Too many walk failures upstairs (likely barricades). Will AFK here instead.");
                }
            } else {
                // reset on success
                upstairsWalkFailures = 0;
            }

            return walked;
        }

        task = "Idle - nothing to do";
        return false;
    }

    private boolean handleObject(String objectName, String objectAction, WorldPosition objectLocation, Area objectArea) {
        monitorChatbox();
        task = "Validate " + objectName + " request";
        // Basic validation
        if (objectName == null || objectName.isBlank() || objectAction == null || objectAction.isBlank()) {
            script.log(getClass(), "handleObject: invalid name/action");
            return false;
        }

        task = "Build " + objectName + " query";
        // Build query
        Predicate<RSObject> objectQuery = gameObject -> {
            if (gameObject == null) return false;
            String name = gameObject.getName();
            String[] actions = gameObject.getActions();
            if (name == null || actions == null) return false;
            if (!name.equalsIgnoreCase(objectName)) return false;
            boolean hasAction = Arrays.stream(actions)
                    .filter(Objects::nonNull)
                    .anyMatch(a -> a.equalsIgnoreCase(objectAction));
            return hasAction && gameObject.canReach();
        };

        task = "Find " + objectName + " object";
        // Finder
        RSObject target = findClosest(objectQuery);
        if (target == null) {
            script.log(getClass(), "handleObject: '" + objectName + "' not found nearby.");
            // If caller gave us a place to move toward, do so before bailing
            walkTowardFallback(null, objectLocation, objectArea);
            return false;
        }

        // If not interactable on screen yet, approach using best info we have
        if (!target.isInteractableOnScreen()) {
            task = "Walk to " + objectName + " object area";
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(target::isInteractableOnScreen)
                    .enableRun(true)
                    .build();

            boolean walkedOk = walkTowardTarget(target, cfg, objectLocation, objectArea);
            if (!walkedOk) {
                script.log(getClass(), "handleObject: walking failed (pre-interact).");
                return false;
            }
        }

        monitorChatbox();

        task = "Interact with " + objectName + " object (" + objectAction + ")";
        // Try interaction
        if (target.interact(objectAction)) {
            resetAntiAfkTimer();
            return true;
        }

        task = "Retry Interact with " + objectName + " object (" + objectAction + ")";
        // Retry once after a short jitter (re-locate target to avoid staleness)
        script.pollFramesHuman(() -> false, script.random(250, 550));
        target = findClosest(objectQuery);
        if (target == null) {
            script.log(getClass(), "handleObject: target disappeared before retry.");
            return false;
        }

        task = "Walk to " + objectName + " object area";
        if (!target.isInteractableOnScreen()) {
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(target::isInteractableOnScreen)
                    .enableRun(true)
                    .build();
            walkTowardTarget(target, cfg, objectLocation, objectArea);
        }

        monitorChatbox();

        task = "Interact with " + objectName + " object (" + objectAction + ")";
        resetAntiAfkTimer();
        return target.interact(objectAction);
    }

    private RSObject findClosest(Predicate<RSObject> objectQuery) {
        List<RSObject> objs = script.getObjectManager().getObjects(objectQuery);
        if (objs == null || objs.isEmpty()) return null;
        return (RSObject) script.getUtils().getClosest(objs);
    }

    private boolean walkTowardTarget(RSObject target, WalkConfig cfg, WorldPosition objectLocation, Area objectArea) {
        try {
            if (objectArea != null && objectArea.getRandomPosition() != null) {
                return script.getWalker().walkTo(objectArea.getRandomPosition(), cfg);
            } else if (objectLocation != null) {
                return script.getWalker().walkTo(objectLocation, cfg);
            } else {
                return script.getWalker().walkTo(target, cfg);
            }
        } catch (Exception e) {
            script.log(getClass(), "walkTowardTarget error: " + e.getMessage());
            return false;
        }
    }

    private void walkTowardFallback(RSObject targetOrNull, WorldPosition objectLocation, Area objectArea) {
        try {
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(targetOrNull != null ? targetOrNull::isInteractableOnScreen : null)
                    .enableRun(true)
                    .build();
            if (objectArea != null && objectArea.getRandomPosition() != null) {
                script.getWalker().walkTo(objectArea.getRandomPosition(), cfg);
            } else if (objectLocation != null) {
                script.getWalker().walkTo(objectLocation, cfg);
            }
        } catch (Exception e) {
            script.log(getClass(), "walkTowardFallback error: " + e.getMessage());
        }
    }

    private boolean isTextOptionDialogueOpen() {
        return script.getWidgetManager().getDialogue() != null &&
                script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TEXT_OPTION;
    }

    private WorldPosition getLadderPosition() {
        if (currentPos == null) return new WorldPosition(0, 0, 0);
        if (redSideUpstairsArea.contains(currentPos.get())) {
            return redLobbyLadder;
        } else if (blueSideUpstairsArea.contains(currentPos.get())) {
            return blueLobbyLadder;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue upstairs area... can't return correct ladder!");
            return new WorldPosition(0, 0, 0);
        }
    }

    private Area getLadderArea() {
        if (currentPos == null) return new RectangleArea(0, 0, 0, 0, 0);
        if (redSideUpstairsArea.contains(currentPos.get())) {
            return redSideLadderArea;
        } else if (blueSideUpstairsArea.contains(currentPos.get())) {
            return blueSideLadderArea;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue upstairs area... can't return correct ladder area!");
            return new RectangleArea(0, 0, 0, 0, 0);
        }
    }

    private Area getSideLobbyArea() {
        if (currentPos == null) return new RectangleArea(0, 0, 0, 0, 0);
        if (redSideInCastleLobbyArea.contains(currentPos.get())) {
            return redSideInCastleLobbyArea;
        } else if (blueSideInCastleLobbyArea.contains(currentPos.get())) {
            return blueSideInCastleLobbyArea;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue area... can't return correct area!");
            return new RectangleArea(0, 0, 0, 0, 0);
        }
    }

    private Area getUpstairsAFKArea() {
        if (currentPos == null) return new RectangleArea(0, 0, 0, 0, 0);
        if (redSideUpstairsArea.contains(currentPos.get())) {
            return redSideUpstairsAFKArea;
        } else if (blueSideUpstairsArea.contains(currentPos.get())) {
            return blueSideUpstairsAFKArea;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue area... can't return correct AFK area!");
            return new RectangleArea(0, 0, 0, 0, 0);
        }
    }

    private Area getLobbyWalkArea() {
        if (currentPos == null) return new RectangleArea(0, 0, 0, 0, 0);
        if (redLobbyArea.contains(currentPos.get())) {
            return redLobbyWalkArea;
        } else if (blueLobbyArea.contains(currentPos.get())) {
            return blueLobbyWalkArea;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue area... can't return correct lobby walk area!");
            return new RectangleArea(0, 0, 0, 0, 0);
        }
    }

    private Area getWaitLobbyArea() {
        if (currentPos == null) return new RectangleArea(0, 0, 0, 0, 0);
        if (redLobbyArea.contains(currentPos.get())) {
            return redLobbyArea;
        } else if (blueLobbyArea.contains(currentPos.get())) {
            return blueLobbyArea;
        } else {
            script.log(getClass(), "Could not locate us in either the red or blue area... can't return correct lobby area!");
            return new RectangleArea(0, 0, 0, 0, 0);
        }
    }

    private void updateLocation() {
        task = "Update location";
        if (currentPos == null) {
            location = "Unknown";
            return;
        }

        // Most specific → broadest
        if (redSideUpstairsAFKArea.contains(currentPos.get())) { location = "Red Upstairs AFK"; return; }
        if (blueSideUpstairsAFKArea.contains(currentPos.get())) { location = "Blue Upstairs AFK"; return; }

        if (redSideUpstairsArea.contains(currentPos.get())) { location = "Red Upstairs"; return; }
        if (blueSideUpstairsArea.contains(currentPos.get())) { location = "Blue Upstairs"; return; }

        if (redSideInCastleLobbyArea.contains(currentPos.get())) { location = "Red Castle Lobby"; return; }
        if (blueSideInCastleLobbyArea.contains(currentPos.get())) { location = "Blue Castle Lobby"; return; }

        if (redLobbyArea.contains(currentPos.get())) { location = "Red Waiting Lobby"; return; }
        if (blueLobbyArea.contains(currentPos.get())) { location = "Blue Waiting Lobby"; return; }

        if (castleWarsArea.contains(currentPos.get())) { location = "Castle Wars (Outside)"; return; }

        // Fallback
        location = "Unknown";
    }

    private void doAntiAfk(boolean allowWalk, Area walkArea) {
        // Only act when timer finishes; if the timer hasn't been started yet, treat it as finished.
        if (switchTabTimer == null || !switchTabTimer.hasFinished()) return;

        try {
            final var tabManager = script.getWidgetManager().getTabManager();
            final var walker     = script.getWalker();

            // Decide action: 70% tabs, 30% walk (only if allowed)
            final int roll = script.random(100);
            final boolean doWalk = allowWalk && roll >= 70;

            if (doWalk && walkArea != null) {
                var dest = walkArea.getRandomPosition();
                if (dest != null) {
                    script.log(getClass(), "Walking a few tiles...");
                    walker.walkTo(dest);

                    script.pollFramesHuman(() -> false, script.random(100, 5000));
                } else {
                    // Fallback to tabs if we couldn't get a destination
                    doTabSwitch(tabManager);
                }
            } else {
                // --- 70% tabs OR walking disabled/null area ---
                doTabSwitch(tabManager);
            }
        } catch (Exception e) {
            script.log(getClass(), "anti-AFK error: " + e.getMessage());
        } finally {
            long min = TimeUnit.MINUTES.toMillis(2);          // 120,000 ms
            long max = (long) (4.2 * 60_000);                 // 252,000 ms
            long delay = script.random(min, max);
            switchTabTimer.reset(delay);
        }
    }

    private void doTabSwitch(TabManager tabManager) {
        if (tabManager == null) return;

        // Build a small pool of candidate tabs different from the current one
        var all = com.osmb.api.ui.tabs.Tab.Type.values();
        var current = tabManager.getActiveTab();
        java.util.List<com.osmb.api.ui.tabs.Tab.Type> pool = new java.util.ArrayList<>(all.length);
        for (var t : all) {
            if (t != null && t != current) pool.add(t);
        }
        if (pool.isEmpty()) pool = java.util.Arrays.asList(all); // extreme fallback

        // Pick a random tab from the pool
        var pick = pool.get(script.random(pool.size()));

        // Open it
        script.log(getClass(), "Switching tab to " + pick + "...");
        tabManager.openTab(pick);

        script.pollFramesHuman(() -> false, script.random(100, 5000));

        // Close the container linked to the active tab
        tabManager.closeContainer();
    }

    private void resetAntiAfkTimer() {
        long min = TimeUnit.MINUTES.toMillis(2);          // 120,000 ms
        long max = (long) (4.2 * 60_000);                 // 252,000 ms
        long delay = script.random(min, max);
        switchTabTimer.reset(delay);
    }

    private void monitorChatbox() {
        task = "Read chatbox";
        // Make sure game filter tab is selected
        if (script.getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            script.getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
            return;
        }

        UIResultList<String> chatResult = script.getWidgetManager().getChatbox().getText();
        if (!chatResult.isFound() || chatResult.isEmpty()) {
            return;
        }

        List<String> currentLines = chatResult.asList();
        if (currentLines.isEmpty()) return;

        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return;
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

        task = "Process new chatbox messages";
        processNewChatboxMessages(newMessages);
    }

    private void processNewChatboxMessages(List<String> newLines) {
        if (newLines == null || newLines.isEmpty()) return;

        // Match numbers with optional comma or dot separators: 1,000 | 1.000 | 12,345 | 12.345 | 12345
        final String NUM = "(\\d{1,3}(?:[.,]\\d{3})*|\\d+)";

        // P2P patterns (plaudits + tickets)
        final Pattern AWARDED_P2P = Pattern.compile(
                "been awarded\\s+" + NUM + "\\s+plaudits\\s+and\\s+" + NUM + "\\s+tickets\\b",
                Pattern.CASE_INSENSITIVE
        );
        final Pattern NOW_HAVE_P2P = Pattern.compile(
                "you now have\\s+" + NUM + "\\s+plaudits\\s+and\\s+" + NUM + "\\s+tickets\\b",
                Pattern.CASE_INSENSITIVE
        );

        // F2P patterns (tickets only)
        final Pattern AWARDED_F2P = Pattern.compile(
                "been awarded\\s+" + NUM + "\\s+tickets\\b",
                Pattern.CASE_INSENSITIVE
        );
        final Pattern NOW_HAVE_F2P = Pattern.compile(
                "you now have\\s+" + NUM + "\\s+tickets\\b",
                Pattern.CASE_INSENSITIVE
        );


        for (String message : newLines) {
            if (message == null || message.isEmpty()) continue;

            // --- P2P: "You've been awarded X plaudits and Y tickets ..."
            java.util.regex.Matcher mAwardedP2P = AWARDED_P2P.matcher(message);
            if (mAwardedP2P.find()) {
                int plauditsThisGame = safeParseInt(mAwardedP2P.group(1));
                int ticketsThisGame  = safeParseInt(mAwardedP2P.group(2));

                script.log(getClass(), "Awarded this game: +" + plauditsThisGame + " plaudits, +" + ticketsThisGame + " tickets.");

                // Count immediately
                plauditsGained += plauditsThisGame;
                ticketsGained  += ticketsThisGame;

                // Track what was already counted this cycle
                pendingPlauditsAddedFromAwarded += plauditsThisGame;
                pendingTicketsAddedFromAwarded  += ticketsThisGame;
                continue;
            }

            // --- P2P: "You now have X plaudits and Y tickets."
            java.util.regex.Matcher mNowHaveP2P = NOW_HAVE_P2P.matcher(message);
            if (mNowHaveP2P.find()) {
                int newPlaudits = safeParseInt(mNowHaveP2P.group(1));
                int newTickets  = safeParseInt(mNowHaveP2P.group(2));

                if (lastPlauditsTotalSeen >= 0 && lastTicketsTotalSeen >= 0) {
                    int plauditsDelta = newPlaudits - lastPlauditsTotalSeen;
                    int ticketsDelta  = newTickets  - lastTicketsTotalSeen;

                    int extraPlaudits = plauditsDelta - pendingPlauditsAddedFromAwarded;
                    int extraTickets  = ticketsDelta  - pendingTicketsAddedFromAwarded;

                    if (extraPlaudits > 0) plauditsGained += extraPlaudits;
                    if (extraTickets  > 0) ticketsGained  += extraTickets;
                }

                plaudits = newPlaudits;
                tickets  = newTickets;
                script.log(getClass(), "Totals updated: " + plaudits + " plaudits, " + tickets + " tickets.");

                lastPlauditsTotalSeen = newPlaudits;
                lastTicketsTotalSeen  = newTickets;
                pendingPlauditsAddedFromAwarded = 0;
                pendingTicketsAddedFromAwarded  = 0;
                continue;
            }

            // --- F2P: "You've been awarded X tickets ..."
            java.util.regex.Matcher mAwardedF2P = AWARDED_F2P.matcher(message);
            if (mAwardedF2P.find()) {
                int ticketsThisGame = safeParseInt(mAwardedF2P.group(1));

                script.log(getClass(), "Awarded this game: +" + ticketsThisGame + " tickets.");

                ticketsGained += ticketsThisGame;
                pendingTicketsAddedFromAwarded += ticketsThisGame;
                // no plaudits in F2P
                continue;
            }

            // --- F2P: "You now have X tickets."
            java.util.regex.Matcher mNowHaveF2P = NOW_HAVE_F2P.matcher(message);
            if (mNowHaveF2P.find()) {
                int newTickets = safeParseInt(mNowHaveF2P.group(1));

                if (lastTicketsTotalSeen >= 0) {
                    int ticketsDelta = newTickets - lastTicketsTotalSeen;
                    int extraTickets = ticketsDelta - pendingTicketsAddedFromAwarded;
                    if (extraTickets > 0) ticketsGained += extraTickets;
                }

                tickets = newTickets;
                script.log(getClass(), "Totals updated: " + tickets + " tickets.");

                lastTicketsTotalSeen = newTickets;
                // Reset only the tickets pending; plaudits pending is irrelevant here but keep it intact for P2P cycles.
                pendingTicketsAddedFromAwarded = 0;
            }
        }
    }

    private static int safeParseInt(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll("[^\\d]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}