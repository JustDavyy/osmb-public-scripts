package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.RandomUtils;
import data.FishingLocation;
import utils.Task;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static main.dAIOFisher.*;

public class dkTravel extends Task {
    private final Area zanarisBankArea = new RectangleArea(2381, 4454, 7, 7, 0);
    private final Area craftingGuildBankArea = new RectangleArea(2928, 3275, 16, 17, 0);
    private final Area zanarisFairyRingArea = new RectangleArea(2408, 4431, 8, 6, 0);
    private final Area zanarisArea = new RectangleArea(2375, 4419, 64, 48, 0);
    private final Area legendsArea = new RectangleArea(2709, 3334, 40, 34, 0);
    private final Area legendsFairyArea = new RectangleArea(2736, 3347, 7, 7, 0);
    private final Area monasteryArea = new RectangleArea(2584, 3219, 87, 24, 0);
    private final Area monasteryFairyArea = new RectangleArea(2653, 3226, 10, 9, 0);
    private final Area fishingFailsafeArea = new RectangleArea(2881, 3055, 77, 73, 0);
    private final Area fishingFailsafeWalkArea = new RectangleArea(2896, 3113, 8, 4, 0);

    // Ardougne cloak item IDs
    private final int[] cloakIds = {
            ItemID.ARDOUGNE_CLOAK_4,
            ItemID.ARDOUGNE_CLOAK_3,
            ItemID.ARDOUGNE_CLOAK_2,
            ItemID.ARDOUGNE_CLOAK_1
    };

    // Quest cape item IDs
    private final int[] qcapeIds = {
            ItemID.QUEST_POINT_CAPE_T,
            ItemID.QUEST_POINT_CAPE

    };

    public static final String[] FAIRY_NAMES = {"Fairy ring"};
    public static final String[] FAIRY_ACTIONS = {"zanaris", "configure"};
    public static final Predicate<RSObject> fairyRingQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(FAIRY_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(FAIRY_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };

    private ItemGroupResult inventorySnapshot;

    public dkTravel(Script script) {
        super(script);
    }

    public boolean activate() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
        if (inventorySnapshot == null) {
            script.log("kTravel", "Inventory not visible.");
            return false;
        }

        return (inventorySnapshot.isFull() && !withinBankArea()) || doneBanking && !withinFishArea() || !withinFishArea() && !withinBankArea();
    }

    public boolean execute() {
        task = "kTravel";

        if (script.getWidgetManager().getBank().isVisible()) {
            script.getWidgetManager().getBank().close();
            return false;
        }

        currentPos = script.getWorldPosition();
        // Handle if we're within the fishing area still
        if (fishingArea.contains(currentPos)) {
            // Additional check to make sure we don't early travel
            inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
            if (!inventorySnapshot.isFull()) {
                script.log("kTravel", "Travel task ran too early, inventory is not full yet. Returning!");
                return false;
            }
            if (bankOption.equals("Zanaris")) {
                task = "Travel to zanaris";
                script.log("kTravel", "Traveling to zanaris with fairy ring");
                return handleFishingFairyRing();
            }
            if (bankOption.equals("Crafting Guild")) {
                task = "Travel to craft guild";
                script.log("kTravel", "Traveling to Crafting Guild with " + script.getItemManager().getItemName(teleportCapeId));
                return handleFishingCraftingCape();
            } else {
                script.log("kTravel", "Invalid bank: " + bankOption);
            }
        }

        // Handle if we're within a bank and have teleport cloak options
        if (withinBankArea()) {
            if (fairyOption.equals("Quest cape") || fairyOption.equals("Ardougne cloak")) {
                task = "Use cape teleport";
                Equipment equipment = script.getWidgetManager().getEquipment();

                final String menuOption     = fairyOption.equals("Ardougne cloak") ? "Kandarin Monastery" : "Teleport";
                final Area destinationArea  = fairyOption.equals("Ardougne cloak") ? monasteryArea : legendsArea;

                int[] baseIds = fairyOption.equals("Ardougne cloak") ? cloakIds : qcapeIds;

                int[] candidates;
                if (equippedCloakId > 0) {
                    java.util.LinkedHashSet<Integer> set = new java.util.LinkedHashSet<>();
                    set.add(equippedCloakId);
                    for (int id : baseIds) set.add(id);
                    candidates = set.stream().mapToInt(Integer::intValue).toArray();
                } else {
                    candidates = baseIds;
                }

                boolean teleported = false;
                int usedId = -1;

                for (int id : candidates) {
                    if (equipment.interact(id, menuOption)) {
                        usedId = id;
                        script.log("kTravel",
                                "Teleporting using " + script.getItemManager().getItemName(id) + " (" + id + ")");
                        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(3500, 4500));

                        if (arrivedAtArea(destinationArea)) {
                            script.log("kTravel", "Teleport was successful");
                            doneBanking = false;
                            script.log("kTravel", "Marked banking flag to false.");
                            teleported = true;
                        } else {
                            script.log("kTravel", "Teleport interaction done, but destination not confirmed yet.");
                        }
                        break;
                    } else {
                        script.log("kTravel", "Cape interaction failed for ID " + id + " — trying next candidate...");
                    }
                }

                if (!teleported && usedId == -1) {
                    script.log("kTravel", "Interaction failed in cape slot for all candidate IDs.");
                }

                return false;
            }
        }

        // Handle if we're at the zanaris bank (we can only get here with the doneBanking flag)
        if (zanarisBankArea.contains(currentPos)) {
            task = "Travel to zanaris fairy";
            script.log("kTravel", "Walking to zanaris fairy ring area from bank area");

            WalkConfig cfg = new WalkConfig.Builder()
                    .enableRun(true)
                    .breakCondition(() -> {
                        RSObject ring = getSpecificObjectAt("Fairy ring", 2412, 4434, 0);
                        return ring != null && ring.isInteractableOnScreen();
                    })
                    .build();

            if (script.getWalker().walkTo(zanarisFairyRingArea.getRandomPosition(), cfg)) {
                doneBanking = false;
            }

            return false;
        }
        // Handle if we're at the zanaris fairy ring area
        RSObject zanarisRing = getSpecificObjectAt("Fairy ring", 2412, 4434, 0);
        if (!inventorySnapshot.isFull()
                && (zanarisFairyRingArea.contains(currentPos)
                || (zanarisRing != null && zanarisRing.isInteractableOnScreen()))) {

            task = "Fairy to fishing area";
            script.log("kTravel", "Traveling to Fishing area from Zanaris Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're within the zanaris area
        if (zanarisArea.contains(currentPos) ) {
            if (inventorySnapshot.isFull()) {
                task = "Travel to bank";
                script.log("kTravel", "Walking to zanaris bank area");

                WalkConfig cfg = new WalkConfig.Builder()
                        .enableRun(true)
                        .breakCondition(() -> {
                            RSObject bank = getClosestBank();
                            return bank != null && bank.isInteractableOnScreen();
                        })
                        .build();

                return script.getWalker().walkTo(zanarisBankArea.getRandomPosition(), cfg);
            } else {
                task = "Travel to fairy ring";
                script.log("kTravel", "Walking to zanaris fairy ring area");

                WalkConfig cfg = new WalkConfig.Builder()
                        .enableRun(true)
                        .breakCondition(() -> {
                            RSObject ring = getSpecificObjectAt("Fairy ring", 2412, 4434, 0);
                            return ring != null && ring.isInteractableOnScreen();
                        })
                        .build();

                return script.getWalker().walkTo(zanarisFairyRingArea.getRandomPosition(), cfg);
            }
        }

        // Legends Guild fairy ring (base tile 2740, 3351, 0)
        RSObject legendsRing = getSpecificObjectAt("Fairy ring", 2740, 3351, 0);

        // Handle if we're at the Legends guild fairy ring area OR if the ring is interactable on screen
        if (legendsFairyArea.contains(currentPos)
                || (legendsRing != null && legendsRing.isInteractableOnScreen())) {
            task = "Travel to fishing area";
            script.log("kTravel", "Traveling to Fishing area from Legends Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're at the Legends guild area (walk until the ring is on screen)
        if (legendsArea.contains(currentPos)) {
            task = "Travel to fairy ring";
            script.log("kTravel", "Traveling to Legends Fairy Ring");
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(() -> legendsRing != null && legendsRing.isInteractableOnScreen())
                    .enableRun(true)
                    .build();
            return script.getWalker().walkTo(legendsFairyArea.getRandomPosition(), cfg);
        }


        // Monastery/Ardougne fairy ring (base tile 2658, 3230, 0)
        RSObject monasteryRing = getSpecificObjectAt("Fairy ring", 2658, 3230, 0);

        // Handle if we're at the Monastery fairy ring area OR if the ring is interactable on screen
        if (monasteryFairyArea.contains(currentPos)
                || (monasteryRing != null && monasteryRing.isInteractableOnScreen())) {
            task = "Travel to fishing area";
            script.log("kTravel", "Traveling to Fishing area from Monastery Fairy Ring");
            return handleOtherFairyRing();
        }

        // Handle if we're at the Monastery area (walk until the ring is on screen)
        if (monasteryArea.contains(currentPos)) {
            task = "Travel to fairy ring";
            script.log("kTravel", "Traveling to Monastery Fairy Ring");
            WalkConfig cfg = new WalkConfig.Builder()
                    .breakCondition(() -> monasteryRing != null && monasteryRing.isInteractableOnScreen())
                    .enableRun(true)
                    .build();
            return script.getWalker().walkTo(monasteryFairyArea.getRandomPosition(), cfg);
        }

        // Fail safe if we're south of the fishing area
        if (fishingFailsafeArea.contains(currentPos)) {
            task = "Fail safe, we're south of fishing area";
            script.log("kTravel", "Fail safe triggered, we're south of the fishing area!");
            script.log("kTravel", "Pathing back to fishing area!");
            return script.getWalker().walkTo(fishingFailsafeWalkArea.getRandomPosition());
        }

        return false;
    }

    private boolean arrivedAtArea(Area destination) {
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return destination.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
        }, RandomUtils.uniformRandom(14000, 16000));
        return destination.contains(currentPos);
    }

    private boolean handleFishingFairyRing() {
        // Get the fairy ring object
        List<RSObject> ringsFound = script.getObjectManager().getObjects(fairyRingQuery);
        if (ringsFound.isEmpty()) {
            script.log("kTravel", "No fairy ring objects found.");
            return false;
        }

        RSObject ring = (RSObject) script.getUtils().getClosest(ringsFound);
        if (!ring.interact("zanaris")) {
            script.log("kTravel", "Failed to interact with fairy ring object.");
            return false;
        }

        doneBanking = false;
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(4000, 5000));

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return zanarisFairyRingArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
        }, RandomUtils.uniformRandom(14000, 16000));
        return zanarisFairyRingArea.contains(currentPos);
    }

    private boolean handleFishingCraftingCape() {
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (inventorySnapshot.contains(teleportCapeId)) {
            if (!inventorySnapshot.getItem(teleportCapeId).interact("Teleport")) {
                script.log("kTravel", "Failed to teleport using the crafting cape in our inventory.");
                return false;
            }

            doneBanking = false;
            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(3000, 4000));

            // Interaction seems successful, wait till we arrive at the guild
            AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);
            script.pollFramesHuman(() -> {
                currentPos = script.getWorldPosition();
                if (currentPos == null) return false;

                if (!Objects.equals(currentPos, previousPosition.get())) {
                    positionChangeTimer.get().reset();
                    previousPosition.set(currentPos);
                }

                return craftingGuildBankArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 10000;
            }, RandomUtils.uniformRandom(14000, 16000));
        } else {
            script.log("kTravel", "It seems the crafting cape is not in our inventory? Re-polling script.");
            return false;
        }

        // Return if we are within the guild or not at last
        return craftingGuildBankArea.contains(currentPos);
    }

    private boolean handleOtherFairyRing() {
        // Get the fairy ring object
        List<RSObject> ringsFound = script.getObjectManager().getObjects(fairyRingQuery);
        if (ringsFound.isEmpty()) {
            script.log("kTravel", "No fairy ring objects found.");
            return false;
        }

        RSObject ring = (RSObject) script.getUtils().getClosest(ringsFound);
        if (!ring.interact("last-destination (dkp)")) {
            script.log("kTravel", "Failed to interact with fairy ring object.");
            return false;
        }

        doneBanking = false;
        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(4000, 5000));

        // Reset afk timer
        switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);

        script.pollFramesHuman(() -> {
            currentPos = script.getWorldPosition();
            if (currentPos == null) return false;

            if (!Objects.equals(currentPos, previousPosition.get())) {
                positionChangeTimer.get().reset();
                previousPosition.set(currentPos);
            }

            return fishingArea.contains(currentPos) || positionChangeTimer.get().timeElapsed() > 15000;
        }, RandomUtils.uniformRandom(20000, 25000));
        dkFish.lastAnimationDetected = System.currentTimeMillis() - 10_000L;
        return fishingArea.contains(currentPos);
    }

    private boolean withinBankArea() {
        // Get our position
        currentPos = script.getWorldPosition();

        // Check Zanaris bank area
        if (zanarisBankArea.contains(currentPos)) {
            return true;
        }

        // Check Crafting Guild bank area
        if (craftingGuildBankArea.contains(currentPos)) {
            return true;
        }

        // Full inventory + bank object on screen
        if (inventorySnapshot.isFull()) {
            RSObject bankObj = getClosestBank();
            return (bankObj != null && bankObj.isInteractableOnScreen());
        }

        // Not in any known bank area
        return false;
    }

    private boolean withinFishArea() {
        // Get our position
        currentPos = script.getWorldPosition();

        return fishingArea.contains(currentPos);
    }

    private RSObject getClosestBank() {
        List<RSObject> objs = script.getObjectManager().getObjects(obj ->
                obj != null &&
                        obj.getName() != null &&
                        (obj.getName().equalsIgnoreCase("Bank chest"))
                        && obj.canReach());
        if (objs == null || objs.isEmpty()) return null;
        return (RSObject) script.getUtils().getClosest(objs);
    }

    private RSObject getSpecificObjectAt(String name, int worldX, int worldY, int plane) {
        return script.getObjectManager().getRSObject(obj ->
                obj != null
                        && obj.getName() != null
                        && name.equalsIgnoreCase(obj.getName())
                        && obj.getWorldX() == worldX
                        && obj.getWorldY() == worldY
                        && obj.getPlane() == plane
        );
    }
}
