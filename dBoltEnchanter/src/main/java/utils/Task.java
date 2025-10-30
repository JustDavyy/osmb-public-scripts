package utils;

import com.osmb.api.script.Script;

public abstract class Task {
    protected Script script;

    public Task(Script script) {
        this.script = script;
    }

    public abstract boolean activate();
    public abstract boolean execute();
}