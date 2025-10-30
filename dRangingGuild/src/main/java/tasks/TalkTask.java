package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import utils.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static main.dRangingGuild.*;

public class TalkTask extends Task {

    public static boolean alreadyStarted = false;
    private static final String TARGET_NAME = "Target";
    private static final String TARGET_ACTION = "Fire-at";
    private static final WorldPosition POS1 = new WorldPosition(2679, 3426, 0);
    private static final WorldPosition POS2 = new WorldPosition(2681, 3425, 0);
    private static final PolyArea NPC_WANDER_AREA = new PolyArea(List.of(new WorldPosition(2674, 3418, 0),new WorldPosition(2674, 3419, 0),new WorldPosition(2672, 3421, 0),new WorldPosition(2671, 3420, 0),new WorldPosition(2670, 3419, 0),new WorldPosition(2669, 3418, 0),new WorldPosition(2669, 3417, 0),new WorldPosition(2670, 3416, 0),new WorldPosition(2671, 3417, 0),new WorldPosition(2672, 3417, 0),new WorldPosition(2673, 3416, 0),new WorldPosition(2674, 3417, 0),new WorldPosition(2673, 3420, 0)));
    public static RSObject cachedTarget = null;

    public static final Predicate<RSObject> targetQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;

        if (!gameObject.getName().equalsIgnoreCase(TARGET_NAME)) return false;

        for (String action : gameObject.getActions()) {
            if (action != null && action.equalsIgnoreCase(TARGET_ACTION)) {
                return gameObject.isInteractable();
            }
        }

        return false;
    };

    public TalkTask(Script script) {
        super(script);
    }

    public boolean activate() {
        return !readyToShoot;
    }

    public boolean execute() {

        // Find or reuse cached target
        if (cachedTarget == null || !cachedTarget.isInteractableOnScreen()) {
            List<RSObject> allObjects = script.getObjectManager().getObjects(targetQuery);
            script.log(getClass().getSimpleName(), "🧪 Found " + allObjects.size() + " total objects.");

            for (RSObject obj : allObjects) {
                if (obj == null) continue;

                String name = obj.getName();
                String[] actions = obj.getActions();
                WorldPosition pos = obj.getWorldPosition();

                script.log(getClass().getSimpleName(), "🔍 Inspecting object: " +
                        "Name=" + name + ", Pos=" + pos + ", Actions=" + Arrays.toString(actions));

                if (name == null || actions == null) {
                    script.log(getClass().getSimpleName(), "⚠ Skipped object due to null name or actions.");
                    continue;
                }

                if (!name.equalsIgnoreCase(TARGET_NAME)) continue;
                if (!Arrays.asList(actions).contains(TARGET_ACTION)) {
                    script.log(getClass().getSimpleName(), "⛔ Skipped object at " + pos + " - no matching 'Fire-at' action.");
                    continue;
                }

                if (!pos.equals(POS1) && !pos.equals(POS2)) {
                    script.log(getClass().getSimpleName(), "⛔ Skipped object at " + pos + " - position not matched.");
                    continue;
                }

                script.log(getClass().getSimpleName(), "✅ Valid target found at " + pos);
                cachedTarget = obj;
                break;
            }

            if (cachedTarget == null) {
                script.log(getClass().getSimpleName(), "❌ No valid target found after filtering.");
                failSafeNeeded = true;
                return false;
            }
        }

        lastTaskRanAt = System.currentTimeMillis();

        // Check if inventory contains arrows
        task = "Check inventory";
        ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Set.of(ItemID.BRONZE_ARROW));

        if (inventorySnapshot == null) {
            // Inventory not visible
            return false;
        }

        if (inventorySnapshot.contains(ItemID.BRONZE_ARROW)) {
            script.log(getClass().getSimpleName(), "Bronze arrows found, equipping!");
            UIResult<Rectangle> tappableSlot = inventorySnapshot.getItem(ItemID.BRONZE_ARROW).getTappableBounds();
            boolean success = script.getFinger().tap(tappableSlot.get().getRandomPoint());
            script.pollFramesHuman(() -> false, script.random(100, 250));

            if (success) {
                readyToShoot = true;
                shotsLeft = 10;
                return true;
            }
        }

        if (script.getWidgetManager().getDialogue() != null) {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.CHAT_DIALOGUE) {
                UIResult<String> textResult = script.getWidgetManager().getDialogue().getText();
                if (textResult == null || textResult.isNotFound() || textResult.isNotVisible()) {
                    script.log(getClass(), "Dialogue text is missing or not visible");
                    return false;
                }

                String dialogueText = textResult.get().toLowerCase();
                script.log(getClass(), "Dialogue text: " + dialogueText);

                if (dialogueText.contains("like to take part")) {
                    script.log(getClass(), "Detected phrase: 'like to take part' — continuing chat...");
                    return script.getWidgetManager().getDialogue().continueChatDialogue();
                }

                if (dialogueText.contains("give it a go")) {
                    script.log(getClass(), "Detected phrase: 'give it a go' — continuing chat...");
                    return script.getWidgetManager().getDialogue().continueChatDialogue();
                }

                if (dialogueText.contains("be 200 coins then")) {
                    script.log(getClass(), "Detected phrase: 'be 200 coins then' — continuing chat...");
                    return script.getWidgetManager().getDialogue().continueChatDialogue();
                }

                if (dialogueText.contains("like to try again")) {
                    script.log(getClass(), "Detected phrase: 'like to try again' — continuing chat...");
                    return script.getWidgetManager().getDialogue().continueChatDialogue();
                }

                if (dialogueText.contains("use the targets for".toLowerCase())) {
                    // We need to talk to the NPC to start a new round
                    script.getWidgetManager().getDialogue().continueChatDialogue();
                    script.log(getClass().getSimpleName(), "We need to talk to the NPC to start a new round");
                    boolean success = findAndInteractWithNPC();

                    script.pollFramesHuman(() -> false, script.random(2000, 2500));
                    alreadyStarted = success;
                    return success;
                }

                script.log(getClass(), "No matching phrases found in CHAT_DIALOGUE.");
            }

            // Check for text option dialogue
            dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType == DialogueType.TEXT_OPTION) {
                List<String> options = script.getWidgetManager().getDialogue().getOptions();

                if (options != null && !options.isEmpty()) {
                    for (String option : options) {
                        if (option != null && option.toLowerCase(java.util.Locale.ROOT).contains("give it a go")) {
                            script.log(getClass().getSimpleName(), "Found option: 'give it a go'");
                            return script.getWidgetManager().getDialogue().selectOption(option);
                        }
                    }
                }
            }
        }

        // Check if the target interface is visible
        if (targetInterface.isVisible()) {
            script.log(getClass().getSimpleName(), "Already in a shooting round, marking readyToShoot as true, shots left set to 9.");
            // We are already shooting targets, mark ready to shoot as ready
            readyToShoot = true;
            shotsLeft = 10;
            alreadyStarted = true;
            return true;
        }

        // If all else fails, fire at target
        Polygon targetPoly = cachedTarget.getConvexHull().getResized(0.7);
        if (targetPoly == null) {
            script.log(getClass(), "❌ Failed to get convex hull for target.");
            return false;
        }

        boolean success = script.getFinger().tap(targetPoly);

        if (!success) {
            script.log(getClass(), "❌ Failed to tap target convex hull.");
            return false;
        }

        // Wait for chatbox or target view
        if (!script.pollFramesHuman(() -> targetInterface.isVisible() || script.getWidgetManager().getDialogue().getDialogueType() != null, script.random(4000, 5000))) {
            script.log(getClass().getSimpleName(), "❌ Target interface did not return — shot may have failed.");
        }

        lastTaskRanAt = System.currentTimeMillis();

        return false;
    }

    private boolean findAndInteractWithNPC() {
        for (int attempt = 0; attempt < 4; attempt++) {
            UIResultList<WorldPosition> npcPositions = script.getWidgetManager().getMinimap().getNPCPositions();

            if (npcPositions.isNotVisible() || npcPositions.isNotFound()) {
                script.log(getClass().getSimpleName(), "No NPCs found nearby...");
            } else {
                for (WorldPosition position : npcPositions) {
                    if (!NPC_WANDER_AREA.contains(position)) {
                        continue;
                    }

                    LocalPosition localPosition = position.toLocalPosition(script);
                    Polygon poly = script.getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), localPosition.getPlane(), 150);
                    if (poly == null) {
                        continue;
                    }

                    Polygon cubeResized = poly.getResized(1.3).convexHull();
                    if (cubeResized == null) {
                        continue;
                    }

                    if (script.getFinger().tap(cubeResized.getBounds(), "Talk-to")) {
                        script.log(getClass(), "Tapped NPC — waiting for dialogue...");
                        boolean appeared = script.pollFramesHuman(() -> {
                            DialogueType dt = script.getWidgetManager().getDialogue().getDialogueType();
                            script.log(getClass(), "Polling dialogue type during wait: " + dt);
                            return dt != null;
                        }, script.random(4000, 6000));

                        if (!appeared) {
                            script.log(getClass(), "Dialogue never appeared after tap.");
                            return false;
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
                script.log(getClass().getSimpleName(), "Failed to tap valid NPC, retrying...");
            }

            if (attempt < 3) {
                script.pollFramesHuman(() -> false, script.random(500, 1500));
            } else {
                script.log(getClass().getSimpleName(), "All attempts failed. Hopping worlds...");
                script.getProfileManager().forceHop();
            }
        }

        return false; // All attempts and one hop failed
    }
}
