package tasks;

import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import com.osmb.api.utils.UIResultList;
import utils.Task;

import static main.dBoltEnchanter.*;

import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Enchant extends Task {
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    public Enchant(Script script) {
        super(script);
    }

    public boolean activate() {
        return setupDone;
    }

    public boolean execute() {
        // Monitor chat for out of bolts or out of runes
        monitorChatbox();

        task = "Cast spell";
        try {
            script.getWidgetManager().getSpellbook().selectSpell(
                    StandardSpellbook.CROSSBOW_BOLT_ENCHANTMENTS,
                    null
            );
        } catch (SpellNotFoundException e) {
            script.log(getClass().getSimpleName(), "Spell sprite not found for crossbow bolt enchant spell. Stopping script...");
            script.stop();
            return false;
        }

        BooleanSupplier condition = () -> {
            DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
            return type == DialogueType.ITEM_OPTION;
        };

        task = "Wait for enchant menu";
        boolean appeared = script.pollFramesHuman(condition, RandomUtils.uniformRandom(4000, 6000));

        if (!appeared) {
            script.log(getClass().getSimpleName(), "Enchant menu did not appear, retrying...");
            return false;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            task = "Start enchanting bolts";
            boolean selected = script.getWidgetManager().getDialogue().selectItem(enchantedBoltID);
            if (!selected) {
                task = "Retry bolt selection";
                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 300));
                selected = script.getWidgetManager().getDialogue().selectItem(enchantedBoltID);
            }

            if (selected) {
                task = "Wait to finish enchanting";

                BooleanSupplier eCondition = () -> {
                    DialogueType type = script.getWidgetManager().getDialogue().getDialogueType();
                    if (type == DialogueType.TAP_HERE_TO_CONTINUE) {
                        script.log(getClass().getSimpleName(), "Dialogue detected, leveled up?");
                        script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(1000, 3000));
                        return true;
                    }
                    return false;
                };

                script.pollFramesHuman(eCondition, RandomUtils.uniformRandom(16800, 19000));
            }
        }

        return false;
    }

    private void monitorChatbox() {
        // Make sure game filter tab is selected
        Chatbox chatbox = script.getWidgetManager().getChatbox();
        if (chatbox != null) {
            try {
                if (chatbox.getActiveFilterTab() != ChatboxFilterTab.GAME) {
                    if (!chatbox.openFilterTab(ChatboxFilterTab.GAME)) {
                        script.log("Failed to open chatbox tab (maybe not visible yet).");
                    }
                    return;
                }
            } catch (NullPointerException e) {
                script.log("Chatbox not ready for openFilterTab yet, skipping this tick.");
                return;
            }
        }

        UIResultList<String> chatResult = script.getWidgetManager().getChatbox().getText();
        if (!chatResult.isFound() || chatResult.isEmpty()) {
            return;
        }

        List<String> currentLines = chatResult.asList();
        if (currentLines.isEmpty()) return;

        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return;
            }

            int currSize = currentLines.size();
            int prevSize = PREVIOUS_CHATBOX_LINES.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;

                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(PREVIOUS_CHATBOX_LINES.get(j))) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        }

        List<String> newMessages = currentLines.subList(0, firstDifference);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);

        processNewChatboxMessages(newMessages);
    }

    private void processNewChatboxMessages(List<String> newLines) {
        if (newLines == null || newLines.isEmpty()) return;

        for (String msg : newLines) {
            if (msg == null || msg.isEmpty()) continue;
            msg = msg.toLowerCase();

            // 1) Out of runes
            if (msg.contains("do not have enough") && msg.contains("cast this spell")) {
                script.log(getClass(), "Chat: out of runes -> " + msg);
                script.log(getClass(), "Stopping script, we ran out of runes to enchant!");
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }

            // 2) Out of bolts
            if (msg.contains("no bolts to enchant")) {
                script.log(getClass(), "Chat: no bolts -> " + msg);
                script.log(getClass(), "Stopping script, we ran out of bolts to enchant!");
                script.getWidgetManager().getLogoutTab().logout();
                script.stop();
                return;
            }
        }
    }
}
