package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import data.FishingLocation;
import utils.Task;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static main.dAIOFisher.*;

public class Drop extends Task {

    public Drop(Script script) {
        super(script);
    }

    public boolean activate() {
        if (!dropMode) return false;
        if (script.getWidgetManager().getDepositBox().isVisible()) {
            return false;
        }

        List<Integer> relevantFishIds = new ArrayList<>(
                cookMode ? fishingMethod.getCookedFish() : fishingMethod.getCatchableFish()
        );

        if (cookMode) {
            relevantFishIds.addAll(fishingMethod.getBurntFish());
        }

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(relevantFishIds));
        return inv != null && inv.isFull();
    }

    public boolean execute() {
        task = "Drop";

        List<Integer> relevantFishIds = new ArrayList<>(
                cookMode ? fishingMethod.getCookedFish() : fishingMethod.getCatchableFish()
        );

        if (cookMode) {
            relevantFishIds.addAll(fishingMethod.getBurntFish());
        }

        if (relevantFishIds.isEmpty()) return false;

        ItemGroupResult inv = script.getWidgetManager().getInventory().search(Set.copyOf(relevantFishIds));
        if (inv == null) return false;

        List<Integer> cooked = cookMode ? fishingMethod.getCookedFish() : fishingMethod.getCatchableFish();
        List<Integer> burnt = cookMode ? fishingMethod.getBurntFish() : Collections.emptyList();

        for (int i = 0; i < cooked.size(); i++) {
            int id = cooked.get(i);
            int count = inv.getAmount(id);

            switch (i) {
                case 0 -> fish1Caught += count;
                case 1 -> fish2Caught += count;
                case 2 -> fish3Caught += count;
                case 3 -> fish4Caught += count;
                case 4 -> fish5Caught += count;
                case 5 -> fish6Caught += count;
                case 6 -> fish7Caught += count;
                case 7 -> fish8Caught += count;
            }
        }

        for (Integer burntId : burnt) {
            fish1Caught += inv.getAmount(burntId);
        }

        Set<Integer> dropIds = new HashSet<>();
        for (int id : relevantFishIds) {
            if (!fishingMethod.isStackableFish(id)) {
                dropIds.add(id);
            }
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            script.getWidgetManager().getInventory().dropItems(dropIds);
            ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(dropIds);

            if (afterDrop == null || afterDrop.isEmpty()) {
                break; // All items successfully dropped
            }

            script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 400));
        }

        if (switchTabTimer.timeLeft() < TimeUnit.MINUTES.toMillis(1)) {
            script.log("PREVENT-LOG", "Timer was under 1 minute. Resetting as we just performed an action.");
            switchTabTimer.reset(RandomUtils.uniformRandom(180000, 300000));
        }

        return false;
    }
}
