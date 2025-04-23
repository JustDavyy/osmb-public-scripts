package tasks;

// GENERAL JAVA IMPORTS

// OSMB SPECIFIC IMPORTS
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experiencetracker.Skill;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.UIResult;
import com.osmb.api.script.Script;

// OTHER CLASS IMPORTS
import utils.Task;
import static main.dPublicAlcher.*;


public class Setup extends Task {
    public Setup(Script script) {
        super(script); // pass the script into the parent Task class
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        script.log("DEBUG", "We are now inside the Setup task logic");

        UIResult<ItemSearchResult> alchitem = script.getItemManager().findItem(script.getWidgetManager().getInventory(), alchItemID);
        if (alchitem.isNotFound()) {
            script.log("FAIL", "Could not locate " + itemName + " to alch.");
            script.stop();
        } else {
            int stack = alchitem.get().getStackAmount();
            script.log("INFO", "Item " + itemName + " found, stack: " + stack);
            stackSize = stack;
            hasReqs = true;
            itemRect = alchitem.get().getTappableBounds();
        }

        script.log("Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        setupDone = true;
        return false;
    }
}
