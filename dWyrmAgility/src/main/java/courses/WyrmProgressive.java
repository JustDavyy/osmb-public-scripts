package courses;

import main.Course;
import main.dWyrmAgility;

import static main.dWyrmAgility.currentLevel;

public class WyrmProgressive implements Course {

    private final dWyrmAgility core;

    private final WyrmBasic basic;
    private final WyrmAdvanced advanced;

    private boolean useAdvanced = false;
    private boolean initialized = false;

    private static final int ADVANCED_LEVEL = 62;

    public WyrmProgressive(dWyrmAgility core) {
        this.core = core;
        this.basic = new WyrmBasic(core);
        this.advanced = new WyrmAdvanced(core);
    }

    @Override
    public int poll(dWyrmAgility script) {

        // One-time startup decision
        if (!initialized) {
            useAdvanced = currentLevel >= ADVANCED_LEVEL;
            initialized = true;

            script.log(
                    "Progressive",
                    "Progressive start → " + (useAdvanced ? "Advanced" : "Basic")
            );
        }

        if (!useAdvanced && basic.isReadyForAdvanced()) {
            useAdvanced = true;

            script.log(
                    "Progressive",
                    "Switching to Advanced course now that we've reached level 62 agility."
            );
        }

        return useAdvanced
                ? advanced.poll(script)
                : basic.poll(script);
    }

    @Override
    public int[] regions() {
        return new int[]{6445};
    }

    @Override
    public String name() {
        return "Progressive";
    }

    @Override
    public String displayName() {
        return useAdvanced
                ? "Progressive (Advanced)"
                : "Progressive (Basic)";
    }
}
