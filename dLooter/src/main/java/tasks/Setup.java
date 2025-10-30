package tasks;

import com.osmb.api.script.Script;
import utils.Task;

import static main.dLooter.*;

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

        if (location.equals("Guardians of the Rift")) {
            int regionID = script.getWorldPosition().getRegionID();

            if (regionID != 14483 && regionID != 14484) {
                script.log(getClass(), "Not in the GOTR region, stopping script!");
                script.stop();
            }
        }

        if (location.equals("Wintertodt")) {
            int regionID = script.getWorldPosition().getRegionID();

            if (regionID != 6461) {
                script.log(getClass(), "Not in the WT region, stopping script!");
                script.stop();
            }
        }

        if (location.equals("Tempoross")) {
            int regionID = script.getWorldPosition().getRegionID();

            if (regionID != 12588) {
                script.log(getClass(), "Not in the Tempoross region, stopping script!");
                script.stop();
            }


        }

        task = "Update flags";
        setupDone = true;
        return false;
    }
}
