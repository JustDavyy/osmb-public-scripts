package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.util.Collections;
import java.util.Set;

import static main.dFossilWCer.*;

public class Drop extends Task {

    public Drop(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!dropMode) return false;
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Collections.emptySet());
        return inv != null && inv.isFull();
    }

    @Override
    public boolean execute() {
        task = "Drop";
        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.of(logsId, ItemID.CLUE_NEST_BEGINNER, ItemID.CLUE_NEST_EASY, ItemID.CLUE_NEST_MEDIUM, ItemID.CLUE_NEST_HARD, ItemID.CLUE_NEST_ELITE));
        if (inv == null) return false;

        for (int attempt = 0; attempt < 3; attempt++) {
            script.getWidgetManager().getInventory().dropItems(Set.of(logsId, ItemID.CLUE_NEST_BEGINNER, ItemID.CLUE_NEST_EASY, ItemID.CLUE_NEST_MEDIUM, ItemID.CLUE_NEST_HARD, ItemID.CLUE_NEST_ELITE));

            ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(Set.of(logsId));

            if (afterDrop == null || afterDrop.isEmpty()) {
                break; // All items successfully dropped
            }

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 400));
        }

        return false;
    }
}