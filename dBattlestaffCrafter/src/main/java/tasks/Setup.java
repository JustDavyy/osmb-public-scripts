package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dBattlestaffCrafter.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log("DEBUG", "We are now inside the Setup task logic");

        // Check required crafting level
        task = "Get crafting level";
        SkillsTabComponent.SkillLevel craftingSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.CRAFTING);
        if (craftingSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = craftingSkillLevel.getLevel();
        currentLevel = craftingSkillLevel.getLevel();

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.AIR_ORB, ItemID.WATER_ORB, ItemID.EARTH_ORB, ItemID.FIRE_ORB, ItemID.BATTLESTAFF));

        task = "Check items";
        shouldBank = !inventorySnapshot.containsAny(ItemID.AIR_ORB, ItemID.WATER_ORB, ItemID.EARTH_ORB, ItemID.FIRE_ORB) && !inventorySnapshot.contains(ItemID.BATTLESTAFF);

        task = "Set flags";
        setupDone = true;
        return false;
    }
}
