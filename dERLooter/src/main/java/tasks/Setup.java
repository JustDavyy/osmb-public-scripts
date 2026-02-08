package tasks;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;
import static main.dERLooter.*;


public class Setup extends Task {
    private final Area scriptArea = new RectangleArea(1537, 3028, 34, 21, 0);
    private final Area scriptArea2 = new RectangleArea(1537, 3028, 34, 21, 1);
    private final Area scriptArea3 = new RectangleArea(1537, 3028, 34, 21, 2);

    public Setup(Script script) {
        super(script);
    }

    public boolean activate() {
        return !setupDone;
    }

    public boolean execute() {
        task = "Setup";
        script.log(getClass(), "We are now inside the Setup task logic");

        WorldPosition myPosition = script.getWorldPosition();
        if (!(scriptArea.contains(myPosition)
                || scriptArea2.contains(myPosition)
                || scriptArea3.contains(myPosition))) {
            script.log(getClass(), "We're not in any valid script area (hunter guild between bank and loot area), stopping script!");
            script.stop();
            return false;
        }

        task = "Open inventory tab";
        script.log("Setup", "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
