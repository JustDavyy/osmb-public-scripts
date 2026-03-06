package utils;

import com.osmb.api.script.Script;
import main.dDesertMiner;

public abstract class Task {
    protected Script script;

    public Task(dDesertMiner script) {
        this.script = script;
    }

    public abstract boolean activate();
    public abstract boolean execute();
}