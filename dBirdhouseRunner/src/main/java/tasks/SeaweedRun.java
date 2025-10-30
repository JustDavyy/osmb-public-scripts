package tasks;

import com.osmb.api.script.Script;
import utils.Task;
import static main.dBirdhouseRunner.*;

public class SeaweedRun extends Task {

    public SeaweedRun(Script script) {
        super(script);
    }

    public boolean activate() {
        return false;
    }

    public boolean execute() {
        task = getClass().getSimpleName();
        lastRunType = "Seaweed";

        return false;
    }
}
