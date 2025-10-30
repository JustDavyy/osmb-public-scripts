package tasks;

import com.osmb.api.script.Script;
import utils.Task;

import static main.dCastlewarsAFKer.*;

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

        // Check location
        task = "Check location";
        int regionID = script.getWorldPosition().getRegionID();

        if (regionID != 9620 && regionID != 9520 && regionID != 9776) {
            script.log(getClass(), "Not in the any of the castle wars regions, stopping script!");
            script.stop();
        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
