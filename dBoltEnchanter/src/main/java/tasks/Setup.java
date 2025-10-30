package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import java.util.Set;

import static main.dBoltEnchanter.*;

public class Setup extends Task {
    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        script.log(getClass(), "We are now inside the Setup task logic");

        // Check required magic level
        task = "Get magic level";
        SkillsTabComponent.SkillLevel magicSkillLevel = script.getWidgetManager().getSkillTab().getSkillLevel(SkillType.MAGIC);
        if (magicSkillLevel == null) {
            script.log(getClass(), "Failed to get skill levels.");
            return false;
        }
        startLevel = magicSkillLevel.getLevel();
        currentLevel = magicSkillLevel.getLevel();

        if (currentLevel < requiredMagicLevel) {
            script.log("Current magic level: " + currentLevel + " is lower than required magic level: " + requiredMagicLevel + "... Stopping script!");
            script.stop();
        }

        task = "Open inventory";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        // Check inventory and initialize the start counts
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(boltID, enchantedBoltID));
        if (inventorySnapshot == null) {
            script.log(getClass(), "Inventory not visible.");
            return false;
        }

        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return false;
        }

        if (inventorySnapshot.contains(boltID)) {
            boltStartStackSize = inventorySnapshot.getAmount(boltID);
        } else {
            script.log(getClass(), "Couldn't find the selected bolts to enchant, stopping script!");
            script.stop();
        }

        if (inventorySnapshot.contains(enchantedBoltID)) {
            enchantedBoltStartStackSize = inventorySnapshot.getAmount(enchantedBoltID);
        }

        task = "Open magic tab";
        script.log(getClass().getSimpleName(), "Opening magic tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.SPELLBOOK);

        script.pollFramesHuman(() -> false, script.random(1200, 2000));

        setupDone = true;
        return false;
    }
}
