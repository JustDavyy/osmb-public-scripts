package tasks;

import main.dTeleporter;
import utils.Task;
import static main.dTeleporter.*;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;

public class TeleportTask extends Task {
    private long lastTeleportTime = 0;

    public TeleportTask(Script script) {
        super(script);
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastTeleportTime >= getCooldownForSpell());
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        boolean success;
        try {
            task = "Cast spell";
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    null
            );
        } catch (SpellNotFoundException e) {
            script.log(dTeleporter.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            long now = System.currentTimeMillis();
            long timeSinceLastTeleport = now - lastTeleportTime;
            script.log("DEBUG", "Time since last teleport: " + timeSinceLastTeleport + "ms");

            task = "Update stats";
            lastTeleportTime = System.currentTimeMillis();
            teleportCount++;

            // Schedule next task attempt after cooldown
            task = "Schedule next cast";
            script.pollFramesUntil(() -> (System.currentTimeMillis() - lastTeleportTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName());
        }

        return false;
    }

    private long getCooldownForSpell() {
        // Randomized delay
        int roll = script.random(100);

        if (roll < 50) {
            return script.random(1800, 1901);
        } else if (roll < 90) {
            return script.random(1850, 2001);
        } else {
            return script.random(1900, 2301);
        }
    }
}