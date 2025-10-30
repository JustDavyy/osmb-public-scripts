package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.walker.WalkConfig;
import utils.Task;

import static main.dRangingGuild.*;
import static tasks.Setup.minigameArea;

public class FailSafe extends Task {

    public FailSafe(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        return failSafeNeeded;
    }

    @Override
    public boolean execute() {
        script.log(getClass(), "Resetting states and caches and falling back to TalkTask. (due to break/hop)");

        // Check if interface is in our way
        if (targetInterface.isVisible()) {
            script.log(getClass(), "Target interface is visible, closing it!");

            boolean succeeded = targetInterface.close();

            if (succeeded) {
                lastTaskRanAt = System.currentTimeMillis();
            }

            return succeeded;
        }

        // Check our position and make sure we're still within the script area
        task = "Check character position";
        if (!minigameArea.contains(script.getWorldPosition())) {
            script.log(getClass().getSimpleName(), "Not at the right area to shoot, moving there!");
            return script.getWalker().walkTo(minigameArea.getRandomPosition());
        }

        task = "Reset states";
        // Reset states
        failSafeNeeded = false;
        readyToShoot = false;
        shotsLeft = 0;

        task = "Reset last task ran at";
        // Reset last task run time
        lastTaskRanAt = System.currentTimeMillis();

        return true;
    }
}
