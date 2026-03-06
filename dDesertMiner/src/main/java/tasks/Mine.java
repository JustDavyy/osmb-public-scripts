package tasks;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import data.SandstoneData;
import main.dDesertMiner;
import utils.Task;

import java.util.*;

import static data.SandstoneData.getAllowedRocks;
import static main.dDesertMiner.task;

public class Mine extends Task {

    public Mine(Script script) {
        super((dDesertMiner) script);
    }

    @Override
    public boolean activate() {
        return !((dDesertMiner) script).isInventoryFull();
    }

    @Override
    public boolean execute() {
        task = "Mine";
        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) {
            return false;
        }

        dDesertMiner.justBoughtSkins = false;

        WorldPosition anchor = ((dDesertMiner) script).getAnchorForLocation();
        if (((dDesertMiner) script).maybeHopForNearbyPlayers(anchor, myPos)) {
            return false;
        }

        double anchorDistance = anchor != null ? myPos.distanceTo(anchor) : Double.MAX_VALUE;
        if (anchor != null && anchorDistance > 3.0) {
            if (((dDesertMiner) script).requestAnchorWalk(anchor)) {
                return false;
            }
            return false;
        }

        List<WorldPosition> respawnCircles = ((dDesertMiner) script).getRespawnCirclePositions();
        List<RSObject> sandstoneRocks = script.getObjectManager().getObjects(object ->
                object != null &&
                        object.isInteractableOnScreen() &&
                        object.getWorldPosition() != null &&
                        ((dDesertMiner) script).allowRock(object.getWorldPosition(), respawnCircles, myPos) &&
                        isSandstoneRock(object) &&
                        ((dDesertMiner) script).hasMineAction(object)
        );

        if (sandstoneRocks == null || sandstoneRocks.isEmpty()) {
            ((dDesertMiner) script).requestAnchorWalk(anchor);
            return false;
        }

        List<WorldPosition> preferredOrder = getAllowedRocks(dDesertMiner.miningLocation);

        RSObject sandstoneRock = sandstoneRocks.stream()
                .sorted(Comparator.comparingInt(o -> preferredOrder.indexOf(o.getWorldPosition())))
                .findFirst()
                .orElse(null);
        if (sandstoneRock == null) {
            ((dDesertMiner) script).requestAnchorWalk(anchor);
            return false;
        }

        if (!((dDesertMiner) script).waitForPlayerIdle()) {
            return false;
        }

        if (!((dDesertMiner) script).tapRock(sandstoneRock)) {
            return false;
        }

        boolean mined = ((dDesertMiner) script).waitForMiningCompletion();
        if (mined && sandstoneRock.getWorldPosition() != null && ((dDesertMiner) script).isAllowedRockPosition(sandstoneRock.getWorldPosition())) {
            ((dDesertMiner) script).getWaitingRespawn().add(sandstoneRock.getWorldPosition());
        }

        return mined;
    }

    private boolean isSandstoneRock(RSObject object) {
        if (object == null || object.getName() == null) {
            return false;
        }
        return SandstoneData.TARGET_ROCK_NAME.equalsIgnoreCase(object.getName());
    }
}