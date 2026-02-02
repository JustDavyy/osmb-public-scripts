package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.Set;

import static main.dFossilWCer.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        // Check required agility level
        task = "Get agility level";
        SkillsTabComponent.SkillLevel agilitySkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.AGILITY);
        if (agilitySkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }

        if (agilitySkillLevel.getLevel() < 70) {
            script.log("Setup", "Agility level is below 70 (" + agilitySkillLevel.getLevel() + "). Disabling usage of the shortcut.");
            useShortcut = false;
        }

        // Check required woodcutting level
        task = "Get woodcutting level";
        SkillsTabComponent.SkillLevel woodcuttingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.WOODCUTTING);
        if (woodcuttingSkillLevel == null) {
            script.log("Setup", "Failed to get skill levels.");
            return false;
        }
        startLevel = woodcuttingSkillLevel.getLevel();
        currentLevel = woodcuttingSkillLevel.getLevel();

        task = "Open inventory";
        script.log("Setup", "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using log basket
        task = "Check log basket usage";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
        if (inv == null) return false;

        if (inv.containsAny(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET))) {
            script.log("Setup", "Log basket detected in inventory. Marking usage as true.");
            useLogBasket = true;
        } else {
            script.log("Setup", "No log basket detected, not using basket logic...");
            useLogBasket = false;
        }

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
            task = "Set zoom level: " + zoomSet;

            script.log("Setup", "Zoom is out of range (" + currentZoom + "). Setting new level: " + zoomSet);

            if (!script.getWidgetManager().getSettings().setZoomLevel(zoomSet)) {
                script.log("Setup", "Failed to set zoom level!");
                return false;
            }

            script.log("Setup", "Zoom successfully set to: " + zoomSet);
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
