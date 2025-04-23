package main;

// GENERAL JAVA IMPORTS
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

import tasks.AlchTask;
import tasks.Setup;
import utils.Task;

// OSMB SPECIFIC IMPORTS
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;


// Script manifest (displays in script overview)
@ScriptDefinition(name = "dPublic Alcher", description = "Alchs items (both high & low) until out of items or runes.", skillCategory = SkillCategory.MAGIC, version = 1.0, author = "JustDavyy")

public class dPublicAlcher extends Script {
    public static boolean setupDone = false;
    public static StandardSpellbook spellToCast;
    public static int alchItemID;
    public static String itemName;
    public static int stackSize;
    public static boolean hasReqs;
    public static UIResult<Rectangle> itemRect;

    private List<Task> tasks;

    public dPublicAlcher(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart(){
        log("INFO", "Starting dPublic Alcher v1.0");

        // Build and show our UI
        ScriptUI ui = new ScriptUI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Alcher Options", false);

        spellToCast = ui.getSelectedSpell();
        alchItemID = ui.getSelectedItemId();
        itemName = getItemManager().getItemName(alchItemID);

        log("DEBUG", "We are alching " + itemName + " with itemID: " + alchItemID + " using: " + spellToCast);

        // Build our list of tasks, tasks will be trying to execute from top to bottom
        tasks = Arrays.asList(
                new Setup(this),
                new AlchTask(this)
        );
    }

    @Override
    public int poll() {
        if (tasks != null) {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
        }
        return 0;
    }
}
