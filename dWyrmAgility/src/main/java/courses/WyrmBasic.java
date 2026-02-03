package courses;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.RandomUtils;
import main.Course;
import main.ObstacleHandleResponse;
import main.dWyrmAgility;

import static main.dWyrmAgility.currentLevel;
import static main.dWyrmAgility.task;

public class WyrmBasic implements Course {

    private final dWyrmAgility core;

    public WyrmBasic(dWyrmAgility core) {
        this.core = core;
    }

    private boolean readyForAdvanced = false;

    private static final RectangleArea START_AREA = new RectangleArea(1642, 2927, 11, 10, 0);


    private static final Area AREA_1 = new RectangleArea(1651, 2924, 9, 10, 1);
    private static final Area AREA_2 = new RectangleArea(1647, 2908, 2, 3, 1);
    private static final Area AREA_3 = new RectangleArea(1629, 2908, 8, 4, 1);
    private static final Area AREA_4 = new RectangleArea(1624, 2930, 6, 4, 1);
    private static final Area AREA_5 = new RectangleArea(1623, 2931, 2, 2, 2);

    private static final WorldPosition OBS1_END_POS = new WorldPosition(1653, 2931, 1);
    private static final WorldPosition OBS2_END_POS = new WorldPosition(1549, 2910, 1);
    private static final WorldPosition OBS3_END_POS = new WorldPosition(1635, 2910, 1);
    private static final WorldPosition OBS4_END_POS = new WorldPosition(1627, 2931, 1);
    private static final WorldPosition COURSE_END_POS = new WorldPosition(1645, 2933, 0);


    @Override
    public int poll(dWyrmAgility dWyrmAgility) {
        WorldPosition position = core.getWorldPosition();

        if (AREA_1.contains(position)) {
            task = "Obstacle 2";
            ObstacleHandleResponse response = main.dWyrmAgility.handleObstacle(core, "tightrope", "cross", AREA_2, 35000);
            if (response == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.log("Basic", "Seems we are encountering the RS Agility bug, moving randomly to get unstuck!");
                core.getWalker().walkTo(AREA_1.getRandomPosition());
            }
            return 0;
        } else if (AREA_2.contains(position)) {
            task = "Obstacle 3";
            core.noMovementTimeout = RandomUtils.weightedRandom(3000, 5000); // override just for this run
            ObstacleHandleResponse response = main.dWyrmAgility.handleObstacle(core, "tightrope", "cross", OBS3_END_POS, 35000);
            if (response == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.log("Basic", "Seems we are encountering the RS Agility bug, moving randomly to get unstuck!");
                core.getWalker().walkTo(AREA_2.getRandomPosition());
            }
            return 0;
        } else if (AREA_3.contains(position)) {
            task = "Obstacle 4";
            ObstacleHandleResponse response = main.dWyrmAgility.handleObstacle(core, "rope", "climb", OBS4_END_POS, 25000);
            if (response == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.log("Basic", "Seems we are encountering the RS Agility bug, moving randomly to get unstuck!");
                core.getWalker().walkTo(AREA_3.getRandomPosition());
            }
            return 0;
        } else if (AREA_4.contains(position)) {
            task = "Obstacle 5";
            ObstacleHandleResponse response = main.dWyrmAgility.handleObstacle(core, "ladder", "climb", AREA_5, 15000);
            if (response == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.log("Basic", "Seems we are encountering the RS Agility bug, moving randomly to get unstuck!");
                core.getWalker().walkTo(AREA_4.getRandomPosition());
            }
            return 0;
        } else if (AREA_5.contains(position)) {
            task = "Obstacle 6";
            ObstacleHandleResponse response = main.dWyrmAgility.handleObstacle(core, "zipline", "slide", COURSE_END_POS, 1, false, 15000);
            if (response == ObstacleHandleResponse.SUCCESS) {
                main.dWyrmAgility.lapCount++;
            } else if (response == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.log("Basic", "Seems we are encountering the RS Agility bug, moving randomly to get unstuck!");
                core.getWalker().walkTo(AREA_5.getRandomPosition());
            }
            return 0;
        } else {

            // Progressive switch point
            if (!readyForAdvanced && currentLevel >= 62) {
                readyForAdvanced = true;
                core.log("Basic", "Reached level 62 at lap start. Ready to switch to Advanced");
            }

            task = "Obstacle 1";
            ObstacleHandleResponse handleResponse = main.dWyrmAgility.handleObstacle(
                    core,
                    "ladder",
                    "climb",
                    OBS1_END_POS,
                    1,
                    true,
                    15000,
                    new WorldPosition(1652, 2931, 0)
            );
            if (handleResponse == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                core.getWalker().walkTo(START_AREA.getRandomPosition());
            }
            return 0;
        }
    }

    @Override
    public int[] regions() {
        return new int[]{6445};
    }

    @Override
    public String name() {
        return "WyrmBasic";
    }

    public boolean isReadyForAdvanced() {
        return readyForAdvanced;
    }

    @Override
    public String displayName() {
        return "WyrmBasic";
    }
}
