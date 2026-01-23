package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.Set;

import static main.dSunbleakWCer.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        // Check required woodcutting level
        task = "Get woodcutting level";
        SkillsTabComponent.SkillLevel woodcuttingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.WOODCUTTING);
        if (woodcuttingSkillLevel == null) {
            script.log("SETUP", "Failed to get skill levels.");
            return false;
        }
        startLevel = woodcuttingSkillLevel.getLevel();
        currentLevel = woodcuttingSkillLevel.getLevel();

        if (woodcuttingSkillLevel.getLevel() < 80) {
            script.log("SETUP", "Woodcutting level is below 80, which is required for Ironwood trees. Stopping script!");
            script.stop();
            return false;
        }

        task = "Open inventory";
        script.log("SETUP", "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check if using log basket
        task = "Check log basket usage";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET));
        if (inv == null) return false;

        if (inv.containsAny(Set.of(ItemID.LOG_BASKET, ItemID.OPEN_LOG_BASKET))) {
            script.log("SETUP", "Log basket detected in inventory. Marking usage as true.");
            useLogBasket = true;
        } else {
            script.log("SETUP", "No log basket detected, not using basket logic...");
            useLogBasket = false;
        }

        // Check zoom level and set if needed
        task = "Get zoom level";

        // Open Display subtab
        if (!script.getWidgetManager().getSettings()
                .openSubTab(SettingsTabComponent.SettingsSubTabType.DISPLAY_TAB)) {
            script.log("SETUP", "Failed to open settings display subtab... returning!");
            return false;
        }

        UIResult<Integer> zoomLevel = script.getWidgetManager().getSettings().getZoomLevel();

        if (zoomLevel.get() == null) {
            script.log("SETUP", "Failed to get zoom level... returning!");
            return false;
        }

        int currentZoom = zoomLevel.get();
        script.log("SETUP", "Current zoom level is: " + currentZoom);

        // Desired range: 1–15
        int minZoom = 1;
        int maxZoom = 15;

        // If already valid, do nothing
        if (currentZoom >= minZoom && currentZoom <= maxZoom) {
            script.log("SETUP", "Zoom is within acceptable range (" + currentZoom + ")");
        } else {
            // Pick a new zoom level in desired range
            int zoomSet = RandomUtils.uniformRandom(minZoom, maxZoom);
            task = "Set zoom level: " + zoomSet;

            script.log("SETUP", "Zoom is out of range (" + currentZoom + "). Setting new level: " + zoomSet);

            if (!script.getWidgetManager().getSettings().setZoomLevel(zoomSet)) {
                script.log("SETUP", "Failed to set zoom level!");
                return false;
            }

            script.log("SETUP", "Zoom successfully set to: " + zoomSet);
        }

        // Check screen resolution
        Rectangle screenRect = script.getScreen().getBounds();
        screenWidth = screenRect.width;
        screenHeight = screenRect.height;

        script.log("SETUP", "Detected screen rectangle is: " + screenRect);
        script.log("SETUP", "Detected screen width is: " + screenWidth);
        script.log("SETUP", "Detected screen height is: " + screenHeight);

        if (screenWidth == 0 || screenHeight == 0) {
            script.log("SETUP", "Detected screen width or height is invalid, stopping script!");
            script.stop();
            return false;
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
