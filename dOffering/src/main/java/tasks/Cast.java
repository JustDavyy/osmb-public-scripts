package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import utils.Task;

import static main.dOffering.*;

public class Cast extends Task {
    private long lastCastTime = 0;

    private int spellNotFoundFailCount = 0;

    public Cast(Script script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (!setupDone) return false;

        long remaining = 5400 - (System.currentTimeMillis() - lastCastTime);
        if (remaining > 0) {
            task = "Cooldown wait";
            script.pollFramesUntil(() -> (System.currentTimeMillis() - lastCastTime) >= 5400, (int) remaining + 100);
            return false;
        }

        return true;
    }


    @Override
    public boolean execute() {
        task = getClass().getSimpleName();

        script.log(getClass(), "Preparing to cast spell: humanized delay...");
        script.pollFramesHuman(() -> false, script.random(1, 100));

        boolean success;
        try {
            task = "Cast spell";
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    null
            );
        } catch (SpellNotFoundException e) {
            spellNotFoundFailCount++;
            script.log(getClass(), "Spell sprite not found for " + selectedSpell + ". Fail count: " + spellNotFoundFailCount);
            if (spellNotFoundFailCount >= 3) {
                script.log(getClass(), "Spell sprite not found 3 times in a row. Stopping script.");
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
            }
            return false;
        }

        if (!success) {
            script.log(getClass(), "Failed to cast " + selectedSpell + ". Will retry next loop.");
            return false;
        }

        // Reset fail counter on successful cast
        spellNotFoundFailCount = 0;

        lastCastTime = System.currentTimeMillis();
        castsDone++;
        castsThisInvent++;

        script.log(getClass(), "Cast successful. Total casts done: " + castsDone + " Casts done before banking: " + castsThisInvent + "/" + castsPerInvent);

        // If this was the final cast for this inventory, flag for banking next
        if (castsThisInvent >= castsPerInvent) {
            task = "Flagging ready to bank";
            script.log(getClass(), "Flagging bank, as we've done " + castsThisInvent + "/" + castsPerInvent + " casts.");
            castsThisInvent = 0;
            needToBank = true;
        }

        return false;
    }
}