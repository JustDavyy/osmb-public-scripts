package tasks;

// GENERAL JAVA IMPORTS
import com.osmb.api.ui.spellbook.StandardSpellbook;
import main.dPublicAlcher;
import utils.Task;
import static main.dPublicAlcher.*;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.utils.UIResult;


public class AlchTask extends Task {
    private long lastAlchTime = 0;
    private long startTime = 0;
    private int alchCount = 0;

    public AlchTask(Script script) {
        super(script);
        this.startTime = System.currentTimeMillis();
    }

    public boolean activate() {
        return hasReqs && (System.currentTimeMillis() - lastAlchTime >= getCooldownForSpell());
    }

    public boolean execute() {
        // Only re-sync actual item count when we're close to running out
        if (stackSize <= 10) {
            UIResult<ItemSearchResult> result = script.getItemManager().findItem(script.getWidgetManager().getInventory(), alchItemID);

            if (result.isNotFound()) {
                script.log(dPublicAlcher.class, itemName + " could not be located in inventory. Stopping script...");
                script.stop();
                return false;
            }

            stackSize = result.get().getStackAmount();
            script.log("DEBUG", "Re-synced item stack size: " + stackSize);
        }

        boolean success = false;
        try {
            success = script.getWidgetManager().getSpellbook().selectSpell(
                    spellToCast,
                    Spellbook.ResultType.CHANGE_TAB
            );
        } catch (SpellNotFoundException e) {
            script.log(dPublicAlcher.class, "Spell sprite not found for " + spellToCast.getName() + ". Stopping script...");
            script.stop();
            return false;
        }

        if (success) {
            lastAlchTime = System.currentTimeMillis();
            alchCount++;
            stackSize--;

            script.getFinger().tap(itemRect.get().getBounds());
            script.log("INFO", "Cast " + spellToCast.getName() + " on " + itemName);

            // Alchs per hour
            long elapsed = System.currentTimeMillis() - startTime;
            int alchsPerHour = (int) ((alchCount * 3600000L) / elapsed);

            // XP tracking
            int xpPerCast = (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 65 : 31;
            int totalXp = alchCount * xpPerCast;
            int xpPerHour = (int) ((totalXp * 3600000L) / elapsed);

            // Time left (hh:mm:ss)
            long estimatedMillisLeft = (alchsPerHour > 0)
                    ? (stackSize * 3600000L) / alchsPerHour
                    : 0;

            long seconds = estimatedMillisLeft / 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;

            String timeLeftFormatted = String.format("%02d:%02d:%02d", hours, minutes, secs);

            script.log("STATS", String.format(
                    "Alchs done: %d | Alchs/hr: %,d | XP gained: %,d | XP/hr: %,d | Items left: %,d | Time left: %s",
                    alchCount, alchsPerHour, totalXp, xpPerHour, stackSize, timeLeftFormatted
            ));

            // Schedule next task attempt after cooldown
            script.submitTask(() -> (System.currentTimeMillis() - lastAlchTime) >= getCooldownForSpell(), (int) (getCooldownForSpell() + 1000));
        } else {
            script.log("WARN", "Failed to cast " + spellToCast.getName() + " on " + itemName);
        }

        return false;
    }

    private static long getCooldownForSpell() {
        return (spellToCast == StandardSpellbook.HIGH_LEVEL_ALCHEMY) ? 3000 : 1800;
    }
}