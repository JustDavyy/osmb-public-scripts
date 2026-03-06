package tasks;

import utils.Task;
import main.dDesertMiner;

import static main.dDesertMiner.shopInterface;
import static main.dDesertMiner.task;

public class Humidify extends Task {

    public Humidify(dDesertMiner script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!dDesertMiner.useHumidify || dDesertMiner.usingCirclet) return false;
        if (shopInterface != null && shopInterface.isVisible()) {
            return false;
        }
        return ((dDesertMiner) script).shouldCastHumidify();
    }

    @Override
    public boolean execute() {
        task = "Cast humidify";
        if (((dDesertMiner) script).castHumidify()) {
            ((dDesertMiner) script).markHumidifyCast();
            return false;
        }
        return false;
    }
}
