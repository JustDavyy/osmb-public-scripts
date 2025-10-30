package tasks;

import com.osmb.api.item.ItemGroup;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.ui.overlay.HealthOverlay;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.ui.tabs.TabManager;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.walker.WalkConfig;
import main.WebhookSender;
import utils.Task;

import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static main.dGemstoneCrabber.*;

public class Fight extends Task {
    private HealthOverlay healthOverlay;
    private final WebhookSender webhook;

    // Locations
    private static final WorldPosition crabNorthSpot = new WorldPosition(1273, 3173, 0);
    private static final WorldPosition crabEastSpot = new WorldPosition(1353, 3112, 0);
    private static final WorldPosition crabSouthSpot = new WorldPosition(1240, 3043, 0);
    private static final WorldPosition crabNorthSafeSpot = new WorldPosition(1275, 3167, 0);
    private static final WorldPosition crabEastSafeSpot = new WorldPosition(1353, 3120, 0);
    private static final WorldPosition crabSouthSafeSpot = new WorldPosition(1247, 3040, 0);

    // Areas
    private static final Area crabNorthArea = new RectangleArea(1267, 3159, 15, 20, 0);
    private static final Area crabEastArea = new RectangleArea(1341, 3098, 24, 28, 0);
    private static final Area crabSouthArea = new RectangleArea(1229, 3032, 27, 19, 0);
    private static final Area bankArea = new RectangleArea(1233, 3113, 18, 15, 0);

    // Cave entrances
    private static final Area caveNorthArea = new RectangleArea(1275, 3167, 2, 2, 0);
    private static final WorldPosition caveNorthSpot = new WorldPosition(1278, 3167, 0);
    private static final Area caveEastArea = new RectangleArea(1351, 3121, 2, 1, 0);
    private static final WorldPosition caveEastSpot = new WorldPosition(1350, 3123, 0);
    private static final Area caveSouthArea = new RectangleArea(1245, 3037, 2, 2, 0);
    private static final WorldPosition caveSouthSpot = new WorldPosition(1245, 3035, 0);

    // Chat history stuff
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    // Reading XP
    private XPDropsComponent cachedXpComponent = null;
    private long cachedXpComponentAt = 0L;
    private static final long XP_COMPONENT_TTL_MS = 10 * 60_000; // optional TTL (10 min)

    public Fight(Script script, WebhookSender webhook) {
        super(script);
        this.webhook = webhook;
        this.healthOverlay = new HealthOverlay(script);
    }

    public boolean activate() {
        return setupDone;
    }

    public boolean execute() {

        task = "Get and cache position";
        currentPos = script.getWorldPosition();
        if (currentPos == null) return false;

        // Reset break/hop flags
        task = "Reset flags";
        if (!script.getProfileManager().isDueToHop()) canHopNow = false;
        if (!script.getProfileManager().isDueToBreak()) canBreakNow = false;

        if (foundCrab) {
            // This is where the fighting happens --> moved to its own method considering how large the logic became.
            alreadyFought = true;
            if (needToAttack) initiateAttack();
            return script.pollFramesHuman(this::fightMrCrabs, script.random(630000, 720000));
        } else {
            // Check if we need to bank
            if (needToBank) {
                task = "Prepare to bank (idle)";
                if (atNorthOrSouth()) {
                    script.log(getClass(), "Need to bank and idle at NORTH or SOUTH. Heading off to bank...");
                    canBankNow = true;
                    foundCrab = false;
                    return false;
                } else if (atEast()) {
                    script.log(getClass(), "Need to bank but idle at EAST. Going through cave to reach NORTH or SOUTH...");
                    if (waitCrabDieThenTraverseCave()) {
                        canBankNow = true;
                        foundCrab = false;
                        return false;
                    }
                    // If we failed, keep normal flow (try find/attack or hop)
                } else {
                    script.log(getClass(), "Need to bank at unknown spot; handing off to bank anyway.");
                    canBankNow = true;
                    foundCrab = false;
                    return false;
                }
            }
            // Check if we are at the bank, if so move to the north crab spawn
            task = "Check if at bank";
            if (atBank()) {
                script.log(getClass(), "We are at the bank area, walk back to north crab spawn!");

                WalkConfig cfg = new WalkConfig.Builder()
                        .enableRun(true)
                        .disableWalkScreen(true)
                        .breakCondition(() -> {
                            currentPos = script.getWorldPosition();
                            if (currentPos == null) return false;
                            return crabNorthArea.contains(currentPos);
                        })
                        .build();

                return script.getWalker().walkTo(crabNorthArea.getRandomPosition(), cfg);
            }

            // Check if we're in any of the crab locations, otherwise move to the nearest one.
            task = "Check if at any crab location";
            if (!atCrabLocation()) {
                script.log(getClass(), "Not at any of the crab locations, moving to the closest one!");

                Area closest = getClosestCrabArea();
                if (closest == null) return false;

                WalkConfig cfg = new WalkConfig.Builder()
                        .enableRun(true)
                        .disableWalkScreen(true)
                        .breakCondition(() -> {
                            currentPos = script.getWorldPosition();
                            if (currentPos == null) return false;
                            return closest.contains(currentPos);
                        })
                        .build();

                return script.getWalker().walkTo(closest.getRandomPosition(), cfg);
            }

            // Logic for if we found the crab (using minimap dots)
            task = "Check minimap for crab";
            if (crabActive3s()) {
                if (!initiateAttack()) return false;
                // Successfully attacked crab, we can now start looping by setting foundCrab to true.
                resetAntiAfkTimer();
                foundCrab = true;
                return false;
            }
            // Logic if the crab cannot be found in that location
            else {
                task = "Locate Gemstone crab";
                script.log(getClass(), "Already fought Gemstone crab, using cave to travel!");
                task = "Move to next area";
                Area currentArea = getClosestCrabArea();
                if (!handleObject("Cave", "Crawl-through", getClosestCaveSpot(), getClosestCaveArea())) {
                    script.log(getClass(), "Failed to go through cave.");
                    return false;
                } else {
                    task = "Wait till arrival at new area";
                    if (script.pollFramesHuman(() -> getClosestCrabArea() != currentArea || isDialogueOpen(), script.random(15000, 20000))) {

                        if (isDialogueOpen()) {
                            script.log(getClass(), "Dialogue detected, could not use the cave...");
                            script.getWidgetManager().getDialogue().continueChatDialogue();
                            script.pollFramesHuman(() -> false, script.random(1, 1000));
                            foundCrab = false;
                            alreadyFought = false;
                            return false;
                        }

                        script.log(getClass(), "Waiting for Gemstone crab to spawn...");
                        task = "Wait for Gemstone crab spawn.";
                        script.pollFramesHuman(this::crabActive3s, script.random(15000, 20000));
                        script.log(getClass(), "Add additional humanized delay");
                        task = "Additional human delay";
                        script.pollFramesHuman(() -> false, script.random(1, 500));
                        if (findCrabNPC(true)) {
                            boolean succeeded = initiateAttack();

                            if (!succeeded) {
                                script.log(getClass(), "Failed to attack crab, resetting flags.");
                                foundCrab = false;
                                needToAttack = true;
                                return false;
                            }
                            resetAntiAfkTimer();
                            foundCrab = true;
                            alreadyFought = true;
                            return false;
                        } else {
                            foundCrab = false;
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }

        }
    }

    private boolean fightMrCrabs() {
        // Monitor the need to bank
        if (needToBank) {
            task = "Prepare to bank";
            if (atNorthOrSouth()) {
                script.log(getClass(), "Need to bank and we are at NORTH or SOUTH. Disengaging and heading off to bank...");
                disengageAndArmBanking();
                return true;
            } else if (atEast()) {
                script.log(getClass(), "Need to bank but at EAST. AFK at cave until crab dies, then traverse to NORTH/SOUTH...");
                if (needToEat()) {
                    if (useFood) {
                        // Build ordered list of candidate IDs (lowest → highest)
                        List<Integer> eatOrder = getFoodVariantOrder(foodID);
                        java.util.Set<Integer> idSet = new java.util.HashSet<>(eatOrder);

                        ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
                        if (inv == null) return false;

                        // Count ALL variants before eating
                        int foodTotal = totalAmount(inv, eatOrder);

                        if (foodTotal == 0) {
                            script.log(getClass(), "Bank is flagged (EAST) but we're below our HP threshold and out of food, going to safety for now!");
                            walkToObject("Cave", "Crawl-through");
                            script.pollFramesHuman(() -> false, script.random(10000, 12500));
                        }
                    }
                }
                if (waitCrabDieThenTraverseCave()) {
                    disengageAndArmBanking();
                    return true;
                }
                return false;
            } else {
                script.log(getClass(), "Need to bank at unknown spot; attempting disengage then bank.");
                disengageAndArmBanking();
                return true;
            }
        }

        // This is the level up check
        DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
        if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
            script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
            if (webhookEnabled) {
                webhook.queueSendWebhook();
            }
            if (!initiateAttack()) {
                initiateAttack();
            }
        }

        // Monitor HP and eat if needed
        task = "Check HP";
        if (needToEat()) {
            script.log(getClass(), "Eating!");

            // Build ordered list of candidate IDs (lowest → highest)
            List<Integer> eatOrder = getFoodVariantOrder(foodID);
            java.util.Set<Integer> idSet = new java.util.HashSet<>(eatOrder);

            ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
            if (inv == null) return false;

            // Count ALL variants before eating
            int beforeTotal = totalAmount(inv, eatOrder);

            // Pick the lowest variant available
            boolean clicked = false;
            for (int id : eatOrder) {
                if (inv.contains(id)) {
                    var item = inv.getItem(id);
                    if (item != null && item.interact()) {
                        clicked = true;
                        break;
                    }
                }
            }
            if (!clicked) {
                script.log(getClass(), "No edible variant found for selected food; likely out of food.");
                needToBank = true;
                return false;
            }

            // Wait until any variant count decreases (i.e., we actually ate one)
            BooleanSupplier waitCon = () -> {
                ItemGroupResult inv2 = script.getWidgetManager().getInventory().search(idSet);
                if (inv2 == null) return false;
                return totalAmount(inv2, eatOrder) < beforeTotal;
            };
            script.pollFramesHuman(waitCon, script.random(4000, 8000));
        }

        // Monitor potion usage
        if (usePot) {
            task = "Check Potions";
            if (needToPot()) {
                script.log(getClass(), "Drinking potion!");

                List<Integer> drinkOrder = getPotionVariantOrder(potID);
                Set<Integer> idSet = new HashSet<>(drinkOrder);

                ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
                if (inv == null) return false;

                int beforeTotal = totalPotionAmount(inv, drinkOrder);

                boolean clicked = false;
                for (int id : drinkOrder) {
                    if (inv.contains(id)) {
                        var item = inv.getItem(id);
                        if (item != null && item.interact()) {
                            clicked = true;
                            break;
                        }
                    }
                }
                if (!clicked) {
                    script.log(getClass(), "No usable potion dose found for selected type.");
                    needToBank = true;
                    return false;
                }
                script.pollFramesUntil(() -> false, script.random(1200, 2000));
                if (!initiateAttack()) {
                    initiateAttack();
                }

                BooleanSupplier waitCon = () -> {
                    ItemGroupResult inv2 = script.getWidgetManager().getInventory().search(idSet);
                    if (inv2 == null) return false;
                    return totalPotionAmount(inv2, drinkOrder) < beforeTotal;
                };
                script.pollFramesHuman(waitCon, script.random(5000, 10000));
            }
        }

        // Monitor item boosts
        if (needToBoost()) {
            if (useDBAXE) {
                task = "Use dragon battleaxe spec";

                ItemGroup inv = script.getWidgetManager().getInventory();
                ItemGroupResult res = inv.search(Set.of(ItemID.DRAGON_BATTLEAXE));
                if (res == null) return false;

                ItemSearchResult dba = res.getItem(ItemID.DRAGON_BATTLEAXE);
                if (dba == null) return false;

                // Remember the exact slot the DBA is currently in (your previous weapon will land here after wielding DBA)
                int savedSlot = res.getSlotForItem(ItemID.DRAGON_BATTLEAXE);
                if (savedSlot < 0) return false;

                // Equip DBA
                if (!dba.interact()) return false;

                // Wait for the swap to complete (DBA should disappear from inventory)
                script.pollFramesUntil(() -> {
                    ItemGroupResult dbaxe = script.getWidgetManager().getInventory().search(Set.of(ItemID.DRAGON_BATTLEAXE));
                    if (dbaxe == null) return false;
                    return !dbaxe.contains(ItemID.DRAGON_BATTLEAXE);
                }, script.random(3000, 5000));

                // Small human-like delay before pressing spec
                script.pollFramesUntil(() -> false, script.random(200, 500));

                // Click spec orb
                script.getWidgetManager().getMinimapOrbs().setSpecialAttack(true);

                // Verify spec actually triggered (percentage dropped below 100) — wait up to 3–5s
                boolean activated = script.pollFramesUntil(() -> {
                    Integer specPct = script.getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
                    return (specPct != null && specPct < 100);
                }, script.random(3000, 5000));

                if (!activated) {
                    script.log(getClass(), "Failed to activate special attack, retry!");
                    script.getWidgetManager().getMinimapOrbs().setSpecialAttack(true);
                }

                // Re-verify after retry attempt (again 3–5s)
                activated = script.pollFramesUntil(() -> {
                    Integer specPct = script.getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
                    return (specPct != null && specPct < 100);
                }, script.random(3000, 5000));

                // --- RE-EQUIP PREVIOUS WEAPON USING THE SAME SLOT'S TAPPABLE BOUNDS ---
                UIResult<Rectangle> tapBoundsRes = inv.getBoundsForSlot(savedSlot);
                Rectangle tapBounds = (tapBoundsRes != null && tapBoundsRes.isFound()) ? tapBoundsRes.get() : null;

                // Fallback to missclick-safe bounds if needed
                if (tapBounds == null) {
                    UIResult<Rectangle> missRes = inv.getMissclickBoundsForSlot(savedSlot);
                    if (missRes == null || !missRes.isFound()) return false;
                    tapBounds = missRes.get();
                }

                // Tap to re-equip old weapon
                if (!script.getFinger().tap(tapBounds)) {
                    script.log(getClass(), "Failed to re-equip weapon, retry!!");
                    if (!script.getFinger().tap(tapBounds)) return false;
                }

                // Confirm DBA is back in inventory (meaning previous weapon re-equipped)
                boolean succ = script.pollFramesUntil(() -> {
                    ItemGroupResult dbaxe = script.getWidgetManager().getInventory().search(Set.of(ItemID.DRAGON_BATTLEAXE));
                    if (dbaxe == null) return false;
                    return dbaxe.contains(ItemID.DRAGON_BATTLEAXE);
                }, script.random(3000, 5000));

                if (!succ) {
                    script.log(getClass(), "Re-equip verification failed; retrying tap once...");
                    if (!script.getFinger().tap(tapBounds)) return false;
                    if (!script.pollFramesUntil(() -> {
                        ItemGroupResult dbaxe = script.getWidgetManager().getInventory().search(Set.of(ItemID.DRAGON_BATTLEAXE));
                        if (dbaxe == null) return false;
                        return dbaxe.contains(ItemID.DRAGON_BATTLEAXE);
                    }, script.random(3000, 5000))) return false;
                }

                if (!initiateAttack()) {
                    initiateAttack();
                }

                if (activated) resetBoostTimer();
                return false;

            } else if (useHearts) {
                if (heartID == ItemID.IMBUED_HEART || heartID == ItemID.SATURATED_HEART) {
                    task = "Invigorate " + (heartID == ItemID.IMBUED_HEART ? "imbued heart" : "saturated heart");
                    ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.IMBUED_HEART, ItemID.SATURATED_HEART));
                    if (inv == null) return false;

                    ItemSearchResult heart = inv.getItem(ItemID.IMBUED_HEART, ItemID.SATURATED_HEART);
                    if (heart == null) return false;

                    boolean success = heart.interact("Invigorate");
                    if (success) {
                        resetBoostTimer();
                        if (!initiateAttack()) {
                            initiateAttack();
                        }
                    }
                    return success;
                }
            }
        }

        // Check if we ran out of supplies
        if (useFood || usePot) {
            task = "Check supplies";
            if (needSupplies()) {
                script.log(getClass(), "We need supplies, flagging bank!");
                needToBank = true;
                return true;
            }
        }

        task = "Check Anti-AFK";
        // Upkeep AFK actions to prevent logging
        doAntiAfk();

        // Check XP gain (re-initiate attack)
        if (System.currentTimeMillis() - lastXpGainAt >= 60_000L) {
            // No XP for 60s → try to re-initiate attack
            task = "Re-initiate (no XP in 60s)";
            script.log(getClass(), "No XP in 60s — attempting to re-initiate attack...");
            initiateAttack();
        }

        // Read and process chat messages
        monitorChatbox();

        // Check if we need to stop
        task = "Check breaks/hops";
        boolean needToStop = script.getProfileManager().isDueToBreak() || script.getProfileManager().isDueToHop();

        // Verify the NPC is still there
        if (onlyHopAfterKill && needToStop) {
            task = "Check crab - Hop/break after kill";
        } else {
            task = "Check crab active";
        }
        boolean crabActive = findCrabNPC(false);

        // Send webhook if needed
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            webhook.queueSendWebhook();
            lastWebhookSent = System.currentTimeMillis();
        }

        if (!crabActive) {
            if (needToStop) {
                if (onlyHopAfterKill) {
                    task = "Finish kill before hop/break";
                    // Delay the hop/break until the crab is gone for a few seconds
                    if (!crabInactive3s()) {
                        script.pollFramesUntil(this::crabInactive3s, script.random(10000, 15000));
                        return false;
                    }
                }

                task = "Walk away from combat";
                script.log("BreakManager", "Walking away as we need to break/hop!");

                WorldPosition safeSpot = getCombatSafeSpot();
                if (safeSpot == null) return false;
                script.getWalker().walkTo(safeSpot);
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                canBreakNow = true;
                canHopNow   = true;
                foundCrab   = false;

                return true;
            }

            Area currentArea = getClosestCrabArea();
            script.log(getClass(), "Crab NPC no longer found on minimap, moving to next area!");
            task = "Move to next area";
            if (!handleObject("Cave", "Crawl-through", getClosestCaveSpot(), getClosestCaveArea())) {
                script.log(getClass(), "Failed to go through cave.");
                return false;
            } else {
                task = "Wait till arrival at new area";
                if (script.pollFramesHuman(() -> getClosestCrabArea() != currentArea || isDialogueOpen(), script.random(15000, 20000))) {

                    if (isDialogueOpen()) {
                        script.log(getClass(), "Dialogue detected, could not use the cave...");
                        script.getWidgetManager().getDialogue().continueChatDialogue();
                        script.pollFramesHuman(() -> false, script.random(1, 1000));
                        foundCrab = false;
                        alreadyFought = false;
                        return false;
                    }

                    script.log(getClass(), "Waiting for Gemstone crab to spawn...");
                    task = "Wait for Gemstone crab spawn.";
                    script.pollFramesHuman(this::crabActive3s, script.random(15000, 20000));
                    script.log(getClass(), "Add additional humanized delay");
                    task = "Additional human delay";
                    script.pollFramesHuman(() -> false, script.random(1, 4000));
                    if (findCrabNPC(true)) {
                        boolean succeeded = initiateAttack();

                        if (!succeeded) {
                            script.log(getClass(), "Failed to attack crab, resetting flags.");
                            foundCrab = false;
                            return false;
                        }
                        resetAntiAfkTimer();
                        foundCrab = true;
                        alreadyFought = true;
                        return false;
                    } else {
                        foundCrab = false;
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            if (needToStop) {
                if (onlyHopAfterKill) {
                    task = "Finish kill before hop/break";
                    // Delay the hop/break until the crab is gone for a few seconds
                    if (!crabInactive3s()) {
                        script.pollFramesUntil(this::crabInactive3s, script.random(10000, 15000));
                        return false;
                    }
                }

                task = "Walk away from combat";
                script.log("BreakManager", "Walking away as we need to break/hop!");

                WorldPosition safeSpot = getCombatSafeSpot();
                if (safeSpot == null) return false;
                script.getWalker().walkTo(safeSpot);
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                canBreakNow = true;
                canHopNow   = true;
                foundCrab   = false;

                return true;
            }
        }

        return false;
    }

    private void doAntiAfk() {
        // Only act when timer finishes; if the timer hasn't been started yet, treat it as finished.
        if (switchTabTimer == null || !switchTabTimer.hasFinished()) return;

        try {
            final var tabManager = script.getWidgetManager().getTabManager();

            int roll = script.random(100);
            boolean chooseAttack = (roll >= 85); // 85% tabs, 15% attack

            if (chooseAttack) {
                task = "AFK: Re-initiating attack";
                script.log(getClass(), "AFK: Re-initiating attack...");
                boolean ok = false;

                for (int attempt = 1; attempt <= 3 && !ok; attempt++) {
                    script.log(getClass(), "Attack attempt " + attempt + "/3...");
                    ok = initiateAttack();
                    if (!ok) {
                        // small jitter before retry
                        task = "Add human delay";
                        script.pollFramesHuman(() -> false, script.random(120, 420));
                    }
                }

                if (!ok) {
                    script.log(getClass(), "Attack retries failed; falling back to tab switch.");
                    doTabSwitch(tabManager);
                }
            } else {
                // 85%: tab switch anti-AFK
                doTabSwitch(tabManager);
            }
        } catch (Exception e) {
            script.log(getClass(), "anti-AFK error: " + e.getMessage());
        } finally {
            resetAntiAfkTimer();
        }
    }

    private void doTabSwitch(TabManager tabManager) {
        if (tabManager == null) return;
        task = "AFK: switch tabs";

        // Build a small pool of candidate tabs different from the current one
        var all = Tab.Type.values();
        var current = tabManager.getActiveTab();
        List<Tab.Type> pool = new ArrayList<>(all.length);
        for (var t : all) {
            if (t != null && t != current) pool.add(t);
        }
        if (pool.isEmpty()) pool = Arrays.asList(all); // extreme fallback

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
        task = "Reset AFK timer";
        long min = TimeUnit.MINUTES.toMillis(3);  // 180,000 ms
        long max = (long) (4.5 * 60_000);                              // 270,000 ms
        long delay = script.random(min, max);
        switchTabTimer.reset(delay);
    }

    private boolean initiateAttack() {
        task = "Get crab location";
        WorldPosition npcTilePosition = getCrabLocation();
        if (npcTilePosition == null || npcTilePosition.equals(new Position(0, 0, 0))) {
            script.log(getClass(), "Crab location unknown.");
            return false;
        }

        task = "Get scene";
        var sceneMgr = script.getSceneManager();
        if (sceneMgr == null) {
            script.log(getClass(), "Scene manager unavailable.");
            return false;
        }

        task = "Get gemcrab tile";
        RSTile npcTile = sceneMgr.getTile(npcTilePosition);
        if (npcTile == null) {
            script.log(getClass(), "NPC tile is null, cannot attack it.");
            return false;
        }

        task = "Check gemcrab tile";
        if (!npcTile.isOnGameScreen()) {
            script.log(getClass(), "NPC tile not on screen, walking...");

            WalkConfig cfg = new WalkConfig.Builder()
                    .enableRun(true)
                    .breakCondition(npcTile::isOnGameScreen)
                    .build();

            script.getWalker().walkTo(npcTilePosition, cfg);
            return false;
        }

        task = "Attack Gemstone crab";
        script.log(getClass(), "Interacting with NPC tile to start attacking...");

        // Base polygon of the tile
        com.osmb.api.shape.Polygon basePoly = npcTile.getTileCube(100);
        Rectangle screen  = script.getScreen().getBounds();
        Rectangle baseBB  = basePoly.getBounds(); // axis-aligned bbox of the polygon

        // Center (used only to derive distances cleanly via the bbox)
        int cx = baseBB.getX() + baseBB.getWidth()  / 2;
        int cy = baseBB.getY() + baseBB.getHeight() / 2;

        // Distances from center to the screen edges
        int leftSpace   = cx - screen.getX();
        int rightSpace  = (screen.getX() + screen.getWidth())  - cx;
        int topSpace    = cy - screen.getY();
        int bottomSpace = (screen.getY() + screen.getHeight()) - cy;

        // Max half-size allowed without leaving the screen (leave 1px margin)
        double maxHalfW = Math.max(0, Math.min(leftSpace, rightSpace)  - 1);
        double maxHalfH = Math.max(0, Math.min(topSpace,  bottomSpace) - 1);

        // Compute the maximum permissible scale for width/height
        // (baseWidth/2)*scale <= maxHalfW   and   (baseHeight/2)*scale <= maxHalfH
        double limitX = baseBB.getWidth()  > 0 ? (2.0 * maxHalfW / baseBB.getWidth())  : 1.0;
        double limitY = baseBB.getHeight() > 0 ? (2.0 * maxHalfH / baseBB.getHeight()) : 1.0;

        double desired   = 2.75;
        double safeScale = Math.max(1.0, Math.min(desired, Math.min(limitX, limitY)));

        // Resize the polygon safely
        Polygon scaledPoly = basePoly.getResized(safeScale);

        // Final guard: if the scaled polygon's bbox still overlaps the edge due to rounding,
        // tap the intersection rect; otherwise tap the polygon itself.
        Rectangle scaledBB = scaledPoly.getBounds();
        Rectangle inter    = intersectRect(scaledBB, screen);

        boolean withinScreen = inter.getWidth() > 0 && inter.getHeight() > 0
                && inter.getWidth() == scaledBB.getWidth()
                && inter.getHeight() == scaledBB.getHeight();

        boolean tapped;
        try {
            if (withinScreen) {
                // Tap the polygon when fully on-screen
                tapped = script.getFinger().tap(scaledPoly, "attack");
            } else {
                // Tap the clamped bbox as a safe fallback (never out-of-bounds)
                tapped = script.getFinger().tap(inter, "attack");
            }
        } catch (IndexOutOfBoundsException oob) {
            // Extra safety: in case the tap implementation still sampled OOB,
            // fall back to the clamped bbox.
            tapped = script.getFinger().tap(inter, "attack");
        }

        if (!tapped) {
            script.log(getClass(), "Failed to initiate attack action on Gemstone Crab.");
            return false;
        }

        // Human delay to wait for attacking to properly complete.
        script.pollFramesHuman(() -> false, script.random(2000, 5000));
        lastXpGainAt = System.currentTimeMillis();
        needToAttack = false;
        return true;
    }

    private boolean walkToObject(String objectName, String objectAction) {
        task = "Validate " + objectName + " request";
        // Basic validation
        if (objectName == null || objectName.isBlank() || objectAction == null || objectAction.isBlank()) {
            script.log(getClass(), "walkToObject: invalid name/action");
            return false;
        }

        // Predefined cave spots
        Set<WorldPosition> allowedCaveSpots = Set.of(caveNorthSpot, caveEastSpot, caveSouthSpot);

        task = "Build " + objectName + " query";
        // Build query
        Predicate<RSObject> objectQuery = gameObject -> {
            if (gameObject == null) return false;
            String name = gameObject.getName();
            String[] actions = gameObject.getActions();
            if (name == null || actions == null) return false;

            // Check name, action, reachability, and if position is one of the predefined spots
            boolean nameMatches = name.equalsIgnoreCase(objectName);
            boolean actionMatches = Arrays.stream(actions)
                    .filter(Objects::nonNull)
                    .anyMatch(a -> a.equalsIgnoreCase(objectAction));
            boolean atCaveSpot = allowedCaveSpots.contains(gameObject.getWorldPosition());

            return nameMatches && actionMatches && atCaveSpot && gameObject.canReach();
        };

        task = "Find " + objectName + " object";
        RSObject target = findClosest(objectQuery);
        if (target == null) {
            script.log(getClass(), "walkToObject: '" + objectName + "' not found at a valid cave spot.");
            return false;
        }

        task = "Walk to " + objectName + " object area";
        WalkConfig cfg = new WalkConfig.Builder()
                .breakCondition(target::isInteractableOnScreen)
                .enableRun(true)
                .build();

        return script.getWalker().walkTo(target.getWorldPosition(), cfg);
    }

    private boolean handleObject(String objectName, String objectAction, WorldPosition objectLocation, Area objectArea) {
        task = "Validate " + objectName + " request";
        // Basic validation
        if (objectName == null || objectName.isBlank() || objectAction == null || objectAction.isBlank()) {
            script.log(getClass(), "handleObject: invalid name/action");
            return false;
        }

        // Predefined cave spots
        Set<WorldPosition> allowedCaveSpots = Set.of(caveNorthSpot, caveEastSpot, caveSouthSpot);

        task = "Build " + objectName + " query";
        // Build query
        Predicate<RSObject> objectQuery = gameObject -> {
            if (gameObject == null) return false;
            String name = gameObject.getName();
            String[] actions = gameObject.getActions();
            if (name == null || actions == null) return false;

            boolean nameMatches = name.equalsIgnoreCase(objectName);
            boolean actionMatches = Arrays.stream(actions)
                    .filter(Objects::nonNull)
                    .anyMatch(a -> a.equalsIgnoreCase(objectAction));
            boolean atCaveSpot = allowedCaveSpots.contains(gameObject.getWorldPosition());

            return nameMatches && actionMatches && atCaveSpot && gameObject.canReach();
        };

        task = "Find " + objectName + " object";
        RSObject target = findClosest(objectQuery);
        if (target == null) {
            script.log(getClass(), "handleObject: '" + objectName + "' not found at a valid cave spot.");
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

        task = "Interact with " + objectName + " object (" + objectAction + ")";
        // Try interaction
        if (target.interact(objectAction)) {
            script.pollFramesHuman(() -> false, script.random(3500, 5000));
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

        task = "Interact with " + objectName + " object (" + objectAction + ")";
        resetAntiAfkTimer();
        boolean success = target.interact(objectAction);
        if (success) script.pollFramesHuman(() -> false, script.random(3500, 5000));
        return success;
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

    private static long dist2(WorldPosition a, WorldPosition b) {
        if (a == null || b == null) return Long.MAX_VALUE;
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        return dx * dx + dy * dy;
    }

    private Area getClosestCrabArea() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return crabNorthArea;
        currentPos = pos;

        long n = samePlaneManhattan(pos, crabNorthSpot);
        long e = samePlaneManhattan(pos, crabEastSpot);
        long s = samePlaneManhattan(pos, crabSouthSpot);

        if (n <= e && n <= s) {
            script.log(getClass(), "Closest crab area: NORTH (|dx|+|dy|=" + n + ")");
            return crabNorthArea;
        } else if (e <= s) {
            script.log(getClass(), "Closest crab area: EAST (|dx|+|dy|=" + e + ")");
            return crabEastArea;
        } else {
            script.log(getClass(), "Closest crab area: SOUTH (|dx|+|dy|=" + s + ")");
            return crabSouthArea;
        }
    }

    private static long samePlaneManhattan(WorldPosition a, WorldPosition b) {
        if (a.getPlane() != b.getPlane()) return Long.MAX_VALUE;
        long dx = Math.abs((long)a.getX() - (long)b.getX());
        long dy = Math.abs((long)a.getY() - (long)b.getY());
        return dx + dy;
    }

    private Area getClosestCaveArea() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) {
            return caveNorthArea;
        }
        currentPos = pos;

        int plane = pos.getPlane();

        if (caveNorthArea.getPlane() == plane && caveNorthArea.contains(pos)) {
            return caveNorthArea;
        }
        if (caveEastArea.getPlane() == plane && caveEastArea.contains(pos)) {
            return caveEastArea;
        }
        if (caveSouthArea.getPlane() == plane && caveSouthArea.contains(pos)) {
            return caveSouthArea;
        }

        // Check the closest crab area instead seeing as couldn't find anything
        Area closestArea = getClosestCrabArea();
        if (closestArea.equals(crabNorthArea)) {
            return caveNorthArea;
        }
        if (closestArea.equals(crabEastArea)) {
            return caveEastArea;
        }
        if (closestArea.equals(crabSouthArea)) {
            return caveSouthArea;
        }

        return caveNorthArea;
    }

    private WorldPosition getClosestCaveSpot() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return caveNorthSpot;
        currentPos = pos;

        int plane = pos.getPlane();

        if (caveNorthArea.getPlane() == plane && caveNorthArea.contains(pos)) {
            return caveNorthSpot;
        }
        if (caveEastArea.getPlane() == plane && caveEastArea.contains(pos)) {
            return caveEastSpot;
        }
        if (caveSouthArea.getPlane() == plane && caveSouthArea.contains(pos)) {
            return caveSouthSpot;
        }

        // Check the closest crab area instead seeing as couldn't find anything
        Area closestArea = getClosestCrabArea();
        if (closestArea.equals(crabNorthArea)) {
            return caveNorthSpot;
        }
        if (closestArea.equals(crabEastArea)) {
            return caveEastSpot;
        }
        if (closestArea.equals(crabSouthArea)) {
            return caveSouthSpot;
        }

        return caveNorthSpot;
    }

    private boolean atCrabLocation() {
        WorldPosition cPos = script.getWorldPosition();
        if (cPos == null) return false;
        currentPos = cPos;
        if (crabNorthArea.contains(currentPos)) return true;
        if (crabEastArea.contains(currentPos)) return true;
        return crabSouthArea.contains(currentPos);
    }

    private boolean atBank() {
        WorldPosition cPos = script.getWorldPosition();
        if (cPos == null) return false;
        currentPos = cPos;
        return bankArea.contains(currentPos);
    }

    private boolean findCrabNPC(boolean log) {
        // --- 1) OVERLAY FIRST ---
        if (healthOverlay != null && healthOverlay.isVisible()) {
            String npcName = (String) healthOverlay.getValue(HealthOverlay.NPC_NAME);

            if (npcName != null && npcName.toLowerCase().contains("crab")) {
                if (log) {
                    script.log(getClass(), "Overlay confirms Gemstone Crab (" + npcName + ").");
                }
                return true;
            }
            // if overlay is visible but not a crab, continue to minimap fallback
        }

        // --- 2) MINIMAP FALLBACK (radius-based, plane-aware) ---
        var minimap = script.getWidgetManager().getMinimap();
        if (minimap == null) return false;

        UIResultList<WorldPosition> npcs = minimap.getNPCPositions();
        if (npcs == null || !npcs.isFound() || npcs.isEmpty()) return false;

        WorldPosition me = script.getWorldPosition();
        if (me == null) return false;
        currentPos = me;

        final int RADIUS = 2;

        for (WorldPosition pos : npcs) {
            if (pos == null || pos.getPlane() != me.getPlane()) continue;

            boolean nearNorth = withinRadius(pos, crabNorthSpot, RADIUS);
            boolean nearEast  = withinRadius(pos, crabEastSpot,  RADIUS);
            boolean nearSouth = withinRadius(pos, crabSouthSpot, RADIUS);

            if (nearNorth || nearEast || nearSouth) {
                if (log) {
                    if (nearNorth)      script.log(getClass(), "Minimap: crab dot near North spot: " + pos);
                    else if (nearEast)  script.log(getClass(), "Minimap: crab dot near East spot: " + pos);
                    else                script.log(getClass(), "Minimap: crab dot near South spot: " + pos);
                }
                return true;
            }
        }

        return false;
    }

    private boolean crabActive3s() {
       return  script.pollFramesHuman(() -> findCrabNPC(false), script.random(3000, 4000));
    }

    private boolean crabInactive3s() {
        return script.pollFramesHuman(() -> !findCrabNPC(false), script.random(3000, 4000));
    }

    private static boolean withinRadius(WorldPosition a, WorldPosition b, int r) {
        if (a == null || b == null) return false;
        if (a.getPlane() != b.getPlane()) return false;
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return dx <= r && dy <= r;
    }

    private boolean isDialogueOpen() {
        return script.getWidgetManager().getDialogue() != null &&
                script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE;
    }

    private WorldPosition getCrabLocation() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return crabNorthSpot;
        currentPos = pos;
        if (crabNorthArea.contains(currentPos)) {
            return crabNorthSpot;
        }
        if (crabEastArea.contains(currentPos)) {
            return crabEastSpot;
        }
        if (crabSouthArea.contains(currentPos)) {
            return crabSouthSpot;
        }

        // Check the closest crab area instead seeing as couldn't find anything
        Area closestArea = getClosestCrabArea();
        if (closestArea.equals(crabNorthArea)) {
            return crabNorthSpot;
        }
        if (closestArea.equals(crabEastArea)) {
            return crabEastSpot;
        }
        if (closestArea.equals(crabSouthArea)) {
            return crabSouthSpot;
        }

        return crabNorthSpot;
    }

    private WorldPosition getCombatSafeSpot() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return crabNorthSafeSpot;
        currentPos = pos;
        if (crabNorthArea.contains(currentPos)) {
            return crabNorthSafeSpot;
        }
        if (crabEastArea.contains(currentPos)) {
            return crabEastSafeSpot;
        }
        if (crabSouthArea.contains(currentPos)) {
            return crabSouthSafeSpot;
        }

        // Check the closest crab area instead seeing as couldn't find anything
        Area closestArea = getClosestCrabArea();
        if (closestArea.equals(crabNorthArea)) {
            return crabNorthSafeSpot;
        }
        if (closestArea.equals(crabEastArea)) {
            return crabEastSafeSpot;
        }
        if (closestArea.equals(crabSouthArea)) {
            return crabSouthSafeSpot;
        }

        return crabNorthSafeSpot;
    }

    private boolean atNorthOrSouth() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return false;
        return crabNorthArea.contains(pos) || crabSouthArea.contains(pos);
    }

    private boolean atEast() {
        WorldPosition pos = script.getWorldPosition();
        if (pos == null) return false;
        return crabEastArea.contains(pos);
    }

    private static Rectangle intersectRect(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.getX(), b.getX());
        int y1 = Math.max(a.getY(), b.getY());
        int x2 = Math.min(a.getX() + a.getWidth(),  b.getX() + b.getWidth());
        int y2 = Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight());
        int w = Math.max(0, x2 - x1);
        int h = Math.max(0, y2 - y1);
        return new Rectangle(x1, y1, w, h);
    }

    private boolean waitCrabDieThenTraverseCave() {
        task = "Wait crab death (for banking)";
        // Wait up to ~12–15s for “no crab for 3s”
        boolean died = script.pollFramesHuman(() -> !crabActive3s(), script.random(12_000, 15_000));
        if (!died) {
            script.log(getClass(), "Crab still active; aborting cave traverse for banking.");
            return false;
        }

        task = "Traverse cave for banking";
        if (!handleObject("Cave", "Crawl-through", getClosestCaveSpot(), getClosestCaveArea())) {
            script.log(getClass(), "Failed to crawl through cave for banking.");
            return false;
        }

        // Small settle delay
        script.pollFramesHuman(() -> false, script.random(8000, 14000));
        return true;
    }

    private void disengageAndArmBanking() {
        task = "Disengage for banking";
        // Best-effort: step to cave to drop combat before banking
        script.getWalker().walkTo(getCombatSafeSpot());
        script.pollFramesHuman(() -> false, script.random(1_000, 1_500));

        canBankNow = true;     // allow bank task to proceed
        foundCrab = false;     // leave the fight loop
        script.log(getClass(), "Banking armed: canBankNow=" + canBankNow + ", foundCrab=" + foundCrab);
    }

    private boolean needToEat() {
        if (useFood) {
            Integer hpPerc = script.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
            if (hpPerc == null || !(hpPerc == -1)) {
                return false; // Can't see HP orb -> don't try to eat
            }

            return hpPerc < eatAtPerc;
        }
        return false;
    }

    private boolean needToBoost() {
        long now = System.currentTimeMillis();

        if (useDBAXE) {
            // Only boost if spec is full (100%)
            Integer specPct = script.getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();

            if (specPct == null || specPct < 100) {
                // Not ready → only push to 1 min if current timer is very soon (<30s), or not set/expired
                long remaining = (dbaNextBoostAt == 0L) ? 0L : (dbaNextBoostAt - now);
                if (dbaNextBoostAt == 0L || remaining < 30_000L) {
                    long nextTry = now + 60_000L;
                    if (dbaNextBoostAt < nextTry) dbaNextBoostAt = nextTry;
                }
                return false;
            }

            // First time → boost now; otherwise wait until timer elapses
            return (dbaNextBoostAt == 0L) || (now >= dbaNextBoostAt);

        } else if (useHearts) {
            // Only treat as boostable if we actually have a heart selected
            if (heartID == ItemID.IMBUED_HEART || heartID == ItemID.SATURATED_HEART) {
                return (heartNextBoostAt == 0L) || (now >= heartNextBoostAt);
            }
        }

        return false;
    }

    private void resetBoostTimer() {
        long now = System.currentTimeMillis();

        if (useDBAXE) {
            // 8.0 – 10.5 minutes
            dbaNextBoostAt = now + randomMillis(8.0, 10.5);
        } else if (useHearts) {
            if (heartID == ItemID.IMBUED_HEART) {
                // 7.2 – 8.5 minutes
                heartNextBoostAt = now + randomMillis(7.2, 8.5);
            } else if (heartID == ItemID.SATURATED_HEART) {
                // 5.2 – 6.5 minutes
                heartNextBoostAt = now + randomMillis(5.2, 6.5);
            } else {
                heartNextBoostAt = 0L;
            }
        }
    }

    private static long minutesToMillis(double minutes) {
        return (long) Math.round(minutes * 60_000.0);
    }
    private static long randomMillis(double minMinutes, double maxMinutes) {
        double r = Math.random(); // [0,1)
        double val = minMinutes + r * (maxMinutes - minMinutes);
        return minutesToMillis(val);
    }

    private boolean needToPot() {
        long now = System.currentTimeMillis();
        boolean isDivine = script.getItemManager().getItemName(potID).toLowerCase().contains("divine");

        // First run → schedule initial pot 1 minute from now
        if (nextPotAt == 0L) {
            nextPotAt = now + 60_000;
            return false;
        }

        // If we're at/after the time → trigger and reschedule
        if (now >= nextPotAt) {
            nextPotAt = isDivine
                    ? now + script.random((long)(5.2 * 60_000), 6 * 60_000) // 5.2-6 minutes
                    : now + script.random(6 * 60_000, 8 * 60_000); // 6 - 8 minutes
            return true;
        }

        return false;
    }

    private boolean needSupplies() {
        if (useFood) {
            // Build ordered list of candidate IDs (lowest → highest)
            List<Integer> eatOrder = getFoodVariantOrder(foodID);
            java.util.Set<Integer> idSet = new java.util.HashSet<>(eatOrder);

            ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
            if (inv == null) return false;

            // Count ALL variants before eating
            int foodTotal = totalAmount(inv, eatOrder);

            return foodTotal == 0;
        }

        if (usePot) {
            List<Integer> drinkOrder = getPotionVariantOrder(potID);
            Set<Integer> idSet = new HashSet<>(drinkOrder);

            ItemGroupResult inv = script.getWidgetManager().getInventory().search(idSet);
            if (inv == null) return false;

            int potDoses = totalPotionAmount(inv, drinkOrder);

            return potDoses == 0;
        }

        return false;
    }

    private void monitorChatbox() {
        // Make sure game filter tab is selected
        Chatbox chatbox = script.getWidgetManager().getChatbox();
        if (chatbox != null) {
            try {
                if (chatbox.getActiveFilterTab() != ChatboxFilterTab.GAME) {
                    if (!chatbox.openFilterTab(ChatboxFilterTab.GAME)) {
                        script.log("Failed to open chatbox tab (maybe not visible yet).");
                    }
                    return;
                }
            } catch (NullPointerException e) {
                script.log("Chatbox not ready for openFilterTab yet, skipping this tick.");
                return;
            }
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

        processNewChatboxMessages(newMessages);
    }

    private void processNewChatboxMessages(List<String> newLines) {
        if (newLines == null || newLines.isEmpty()) return;

        for (String msg : newLines) {
            if (msg == null || msg.isEmpty()) continue;
            msg = msg.toLowerCase();

            // 1) Out of runes
            if (msg.contains("do not have enough") && msg.contains("cast this spell")) {
                script.log(getClass(), "Chat: out of runes -> " + msg);
                script.log(getClass(), "Stopping script after going to safety");
                walkToObject("Cave", "Crawl-through");
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }

            // 2) Last one
            if (msg.contains("that was your last one")) {
                script.log(getClass(), "Chat: last one consumed -> " + msg);
                script.log(getClass(), "Stopping script after going to safety");
                walkToObject("Cave", "Crawl-through");
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }

            // 3) No ammo
            if (msg.contains("there is no ammo left in your quiver")) {
                script.log(getClass(), "Chat: out of ammo -> " + msg);
                script.log(getClass(), "Stopping script after going to safety");
                walkToObject("Cave", "Crawl-through");
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }

            // 3) Out of charges
            if (msg.contains("out of") && msg.contains("charge")) {
                script.log(getClass(), "Chat: out of charges -> " + msg);
                script.log(getClass(), "Stopping script after going to safety");
                walkToObject("Cave", "Crawl-through");
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }

            // 5) We dedded
            if (msg.contains("you are dead")) {
                script.log(getClass(), "Chat: we are dedded -> " + msg);
                script.log(getClass(), "Stopping scrip... fuck me man");
                script.pollFramesHuman(() -> false, script.random(10000, 12500));
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }
        }
    }
}