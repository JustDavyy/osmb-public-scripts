package tasks;

import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.script.Script;
import utils.Task;

import static main.dSpookyCollector.*;

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

        task = "Open inventory tab";
        script.log(getClass().getSimpleName(), "Opening inventory tab");
        script.getWidgetManager().getTabManager().openTab(Tab.Type.INVENTORY);

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
