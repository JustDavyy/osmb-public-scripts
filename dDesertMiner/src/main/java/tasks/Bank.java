package tasks;

import utils.Task;
import main.dDesertMiner;

import static main.dDesertMiner.shopInterface;
import static main.dDesertMiner.task;

public class Bank extends Task{
    public Bank(dDesertMiner script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (shopInterface != null && shopInterface.isVisible()) {
            return false;
        }
        return ((dDesertMiner) script).isInventoryFull();
    }

    @Override
    public boolean execute() {
        task = "Deposit sand";
        return ((dDesertMiner) script).handleFullInventory();
    }
}
