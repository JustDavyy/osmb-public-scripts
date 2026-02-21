package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Equipment;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.RandomUtils;
import data.FishingLocation;
import utils.Task;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class Setup extends Task {
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

    // Harpoon with spec item IDs
    private final int[] harpoonIds = {
            ItemID.DRAGON_HARPOON,
            ItemID.DRAGON_HARPOON_OR,
            ItemID.DRAGON_HARPOON_OR_30349,
            ItemID.CRYSTAL_HARPOON,
            ItemID.CRYSTAL_HARPOON_23864,
            ItemID.CRYSTAL_HARPOON_INACTIVE,
            ItemID.INFERNAL_HARPOON,
            ItemID.INFERNAL_HARPOON_OR,
            ItemID.INFERNAL_HARPOON_OR_30342,
            ItemID.INFERNAL_HARPOON_UNCHARGED,
            ItemID.INFERNAL_HARPOON_UNCHARGED_25367,
            ItemID.INFERNAL_HARPOON_UNCHARGED_30343
    };

    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = "Setup";
        script.log("Setup", "We are now inside the Setup task logic");

        // Check if we have all the necessary tools in our inventory
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        task = "Check equipped tools";
        if (hasAnyRequiredEquipped(fishingMethod.getRequiredTools())) {
            script.log("Setup", "Equipped fishing tool detected, marking as true!");
            hasToolEquipped = true;
        }

        task = "Check required tools";
        if (!hasAllRequirements(fishingMethod.getRequiredTools())) {
            script.log("Setup", "Not all required tools could be located in inventory, stopping script!");
            script.getWidgetManager().getLogoutTab().logout();
            script.stop();
            return false;
        }

        if (fishingLocation.equals(FishingLocation.Karamja_West)) {
            ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(Set.of(ItemID.RAW_KARAMBWANJI));

            if (invCheck == null) {return false;}

            task = "Check karambwanji start count";
            script.log("Setup", "Checking initial Karambwanji count.");
            startAmount = invCheck.getAmount(ItemID.RAW_KARAMBWANJI);
            script.log("Setup", "Karambwanji start count detected: " + startAmount);
        }
        if (fishingLocation.equals(FishingLocation.Minnows)) {
            ItemGroupResult invCheck = script.getWidgetManager().getInventory().search(Set.of(ItemID.MINNOW));

            if (invCheck == null) {return false;}

            task = "Check minnow start count";
            script.log("Setup", "Checking initial Minnow count.");
            startAmount = invCheck.getAmount(ItemID.MINNOW);
            script.log("Setup", "Minnow start count detected: " + startAmount);
        }

        // Check if we have a fishing barrel in our inventory
        ItemGroupResult inventorySnapshot2 = script.getWidgetManager().getInventory().search(Set.of(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL));

        if (inventorySnapshot2 == null) {return false;}

        // Check if we're using fishing barrel
        task = "Check fish barrel";
        if (inventorySnapshot2.containsAny(ItemID.FISH_BARREL, ItemID.OPEN_FISH_BARREL)) {
            script.log("Setup", "Fishing barrel detected in inventory, marking usage as TRUE");
            usingBarrel = true;
        } else {
            script.log("Setup", "No fishing barrel detected (closed or opened), usage kept marked as FALSE.");
        }

        // Reset timer
        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute. Resetting as we just performed an action.");
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
        }

        // Karambwan logic
        if (fishingLocation.equals(FishingLocation.Karambwans)) {
            // Check if we have Ardougne cloak equipped if we use that fairy ring option
            if (fairyOption.equals("Ardougne cloak")) {
                task = "Check ardy cloak";
                script.log("Setup", "Ardougne cloak option is selected, checking if it's equipped...");
                Equipment equipment = script.getWidgetManager().getEquipment();
                UIResult<Boolean> result = equipment.isEquipped(cloakIds);

                if (result.isFound()) {
                    script.log("Setup", "One of the ardougne cloaks is found to be equipped.");
                    // One of the cloaks is equipped, find which one
                    for (int cloakId : cloakIds) {
                        UIResult<Boolean> check = equipment.isEquipped(cloakId);
                        if (check.isFound()) {
                            equippedCloakId = cloakId;
                            script.log("Setup", "Ardougne cloak equipped: " + script.getItemManager().getItemName(equippedCloakId) + " (" + equippedCloakId + ").");
                            break;
                        }
                    }
                } else {
                    script.log("Setup", "Ardougne cloak is not equipped. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }

            // Check if we have Quest cape equipped if we use that fairy ring option
            if (fairyOption.equals("Quest cape")) {
                task = "Check quest cape";
                script.log("Setup", "Quest cape option is selected, checking if it's equipped...");
                Equipment equipment = script.getWidgetManager().getEquipment();
                UIResult<Boolean> result = equipment.isEquipped(qcapeIds);

                if (result.isFound()) {
                    script.log("Setup", "One of the quest capes is found to be equipped.");
                    // One of the capes is equipped, find which one
                    for (int cloakId : qcapeIds) {
                        UIResult<Boolean> check = equipment.isEquipped(cloakId);
                        if (check.isFound()) {
                            equippedCloakId = cloakId;
                            script.log("Setup", "Quest cape equipped: " + script.getItemManager().getItemName(equippedCloakId) + " (" + equippedCloakId + ").");
                            break;
                        }
                    }
                } else {
                    script.log("Setup", "Quest cape is not equipped. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }

            // Check if we have a crafting cape in our inventory if we use that bank option
            if (bankOption.equals("Crafting Guild")) {
                task = "Check crafting cape";
                script.log("Setup", "Crafting Guild bank option is selected, checking if we have the cape.");
                inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.CRAFTING_CAPE, ItemID.CRAFTING_CAPET));

                if (inventorySnapshot == null) {
                    // Inventory not visible
                    return false;
                }

                if (inventorySnapshot.contains(ItemID.CRAFTING_CAPE)) {
                    script.log("Setup", "Untrimmed crafting cape found in inventory, storing item ID...");
                    teleportCapeId = ItemID.CRAFTING_CAPE;
                    script.log("Setup", "Teleport item: " + script.getItemManager().getItemName(teleportCapeId) + "(" + teleportCapeId + ").");
                } else if (inventorySnapshot.contains(ItemID.CRAFTING_CAPET)) {
                    script.log("Setup", "Trimmed crafting cape found in inventory, storing item ID...");
                    teleportCapeId = ItemID.CRAFTING_CAPET;
                    script.log("Setup", "Teleport item: " + script.getItemManager().getItemName(teleportCapeId) + "(" + teleportCapeId + ").");
                } else {
                    script.log("Setup", "Crafting cape is not in inventory while option is selected. Please make sure it is and restart the script.");
                    script.stop();
                    return false;
                }
            }
        }

        // Check fishing level
        task = "Get fishing level";
        SkillsTabComponent.SkillLevel fishingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.FISHING);
        if (fishingSkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }
        startFishingLevel = fishingSkillLevel.getLevel();
        currentFishingLevel = fishingSkillLevel.getLevel();

        // Check cooking level
        task = "Get cooking level";
        SkillsTabComponent.SkillLevel cookingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.COOKING);
        if (cookingSkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }
        startCookingLevel = cookingSkillLevel.getLevel();
        currentCookingLevel = cookingSkillLevel.getLevel();

        // Check zoom level and set if needed
        task = "Get zoom level";

        // Open Display subtab
        if (!script.getWidgetManager().getSettings()
                .openSubTab(SettingsTabComponent.SettingsSubTabType.DISPLAY_TAB)) {
            script.log("Setup", "Failed to open settings display subtab... returning!");
            return false;
        }

        UIResult<Integer> zoomLevel = script.getWidgetManager().getSettings().getZoomLevel();

        if (zoomLevel.get() == null) {
            script.log("Setup", "Failed to get zoom level... returning!");
            return false;
        }

        int currentZoom = zoomLevel.get();
        script.log("Setup", "Current zoom level is: " + currentZoom);

        // Desired range: 1–15
        int minZoom = 1;
        int maxZoom = 15;

        // If already valid, do nothing
        if (currentZoom >= minZoom && currentZoom <= maxZoom) {
            script.log("Setup", "Zoom is within acceptable range (" + currentZoom + ")");
        } else {
            // Pick a new zoom level in desired range
            int zoomSet = RandomUtils.uniformRandom(minZoom, maxZoom);
            task = "Set zoom level " + zoomSet;

            script.log("Setup", "Zoom is out of range (" + currentZoom + "). Setting new level: " + zoomSet);

            if (!script.getWidgetManager().getSettings().setZoomLevel(zoomSet)) {
                script.log("Setup", "Failed to set zoom level!");
                return false;
            }

            script.log("Setup", "Zoom successfully set to: " + zoomSet);
        }

        task = "Open inventory";
        script.log("Setup", "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Finish set up";
        currentPos = script.getWorldPosition();
        setupDone = true;
        return false;
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
            script.log("Setup", "Inventory not visible.");
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

    public boolean hasAnyRequiredEquipped(Collection<Integer> requiredIds) {
        Equipment equipment = script.getWidgetManager().getEquipment();

        for (int requiredId : requiredIds) {
            boolean equipped = switch (requiredId) {
                case ItemID.HARPOON -> {
                    boolean found = false;
                    for (int id : TOOL_EQUIVALENTS.get("harpoon")) {
                        UIResult<Boolean> res = equipment.isEquipped(id);
                        if (res.isFound()) {
                            found = true;
                            break;
                        }
                    }
                    yield found;
                }
                case ItemID.FISHING_ROD -> equipment.isEquipped(ItemID.PEARL_FISHING_ROD).isFound();
                case ItemID.OILY_FISHING_ROD -> equipment.isEquipped(ItemID.OILY_PEARL_FISHING_ROD).isFound();
                case ItemID.FLY_FISHING_ROD -> equipment.isEquipped(ItemID.PEARL_FLY_FISHING_ROD).isFound();
                case ItemID.BARBARIAN_ROD -> equipment.isEquipped(ItemID.PEARL_BARBARIAN_ROD).isFound();
                default -> false;
            };

            if (equipped) {
                return true;
            }
        }

        return false;
    }
}
