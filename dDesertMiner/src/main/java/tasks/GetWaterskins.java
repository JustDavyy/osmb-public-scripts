package tasks;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import main.dDesertMiner;
import utils.Task;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static main.dDesertMiner.*;

public class GetWaterskins extends Task {

    private final Area quarryArea = new RectangleArea(3155, 2907, 5, 4, 0);
    private final Area outsidePassArea = new RectangleArea(3196, 2838, 5, 8, 0);
    private final Area insidePassArea = new RectangleArea(3190, 2840, 3, 5, 0);
    private final WorldPosition shopNPCPosition = new WorldPosition(3193, 2841, 0);
    private final WorldPosition passObjectPosition = new WorldPosition(3194, 2841, 0);

    public GetWaterskins(dDesertMiner script) {
        super(script);
    }

    @Override
    public boolean activate() {
        if (useHumidify || usingCirclet) return false;
        return ((dDesertMiner) script).shouldGetWaterskins();
    }

    @Override
    public boolean execute() {
        task = "Get waterskins";

        WorldPosition myPos = script.getWorldPosition();
        if (myPos == null) return false;

        // If we are not inside shantay OR outside shantay
        if (!outsidePassArea.contains(myPos) && !insidePassArea.contains(myPos)) {
            script.log("GetWaterskins", "Moving towards unkah shantay pass area.");
            return moveToArea(outsidePassArea);
        }

        // If we are outside the shantay pass area
        if (outsidePassArea.contains(myPos)) {
            // If we're done buying new ones already
            if (waterskinCharges > 10 || justBoughtSkins) {
                return moveToArea(quarryArea);
            }
            script.log("GetWaterskins", "Pass through unkah shantay pass object.");
            RSObject pass = getSpecificObjectAt("Shantay pass", passObjectPosition.getX(), passObjectPosition.getY(), passObjectPosition.getPlane());
            if (pass == null) return false;

            if (pass.interact("Go-through")) {
                waitTillStopped();
            }

            // Drop all old waterskins
            for (int attempt = 0; attempt < 3; attempt++) {
                script.getWidgetManager().getInventory().dropItems(ItemID.WATERSKIN0);
                ItemGroupResult afterDrop = script.getWidgetManager().getInventory().search(Set.of(ItemID.WATERSKIN0));

                if (afterDrop == null || afterDrop.isEmpty()) {
                    break; // All items successfully dropped
                }

                script.pollFramesHuman(() -> false, RandomUtils.uniformRandom(150, 400));
            }

            return false;
        }

        // If we are inside the shantay pass area
        if (insidePassArea.contains(myPos)) {
            // If we are done already
            if (waterskinCharges > 10 || justBoughtSkins) {
                // Close shop if open
                if (shopInterface.isVisible()) {
                    shopInterface.close();
                    return script.pollFramesHuman(() -> !shopInterface.isVisible(), RandomUtils.uniformRandom(3000, 6000));
                }

                // Go back outside if we're inside
                if (insidePassArea.contains(myPos)) {
                    script.log("GetWaterskins", "Pass through unkah shantay pass object.");
                    RSObject pass = getSpecificObjectAt("Shantay pass", passObjectPosition.getX(), passObjectPosition.getY(), passObjectPosition.getPlane());
                    if (pass == null) return false;

                    if (pass.interact("Go-through")) {
                        waitTillStopped();
                    }

                    return false;
                }
            }

            // Open shop if not open yet
            if (!shopInterface.isVisible()) {
                RSTile npcTile = script.getSceneManager().getTile(shopNPCPosition);
                if (npcTile == null) {
                    script.log("GetWaterskins", "NPC tile is null, cannot open shantay shop via NPC.");
                    return false;
                }

                if (!npcTile.isOnGameScreen()) {
                    script.log("GetWaterskins", "NPC tile not on screen, walking...");
                    script.getWalker().walkTo(shopNPCPosition);
                    return false;
                }

                script.log("GetWaterskins", "Interacting with NPC tile to open the shantay shop...");
                if (!script.getFinger().tap(npcTile.getTileCube(110).getResized(0.4), "Trade")) {
                    script.log("GetWaterskins", "Failed to long-press 'Trade' on NPC tile.");
                    return false;
                }

                boolean opened = script.pollFramesHuman(
                        () -> shopInterface != null && shopInterface.isVisible(),
                        RandomUtils.uniformRandom(4000, 7500)
                );

                if (!opened) {
                    script.log("GetWaterskins", "Shop did not open.");
                }

                return opened;
            }

            // If shop is open, first buy one shantay pass, followed by 5 waterskins
            script.log("GetWaterskins", "Shop open, buying Shantay pass...");

            Rectangle passArea = shopInterface.getShantayPassBuyArea();
            if (passArea == null) {
                script.log("GetWaterskins", "Failed to get Shantay Pass buy area.");
                return false;
            }

            if (!script.getFinger().tap(passArea, "Buy 1")) {
                script.log("GetWaterskins", "Failed to buy Shantay Pass.");
                return false;
            }

            script.pollFramesUntil(() -> false, RandomUtils.uniformRandom(350, 600));

            script.log("GetWaterskins", "Buying 5 Waterskins...");

            Rectangle waterskinArea = shopInterface.getWaterskinBuyArea();
            if (waterskinArea == null) {
                script.log("GetWaterskins", "Failed to get Waterskin buy area.");
                return false;
            }

            if (!script.getFinger().tap(waterskinArea, "Buy 5")) {
                script.log("GetWaterskins", "Failed to buy Waterskins.");
                return false;
            }

            justBoughtSkins = true;
            waterskinCharges = 20;

            shopInterface.close();

            return true;
        }

        return false;
    }

    private boolean moveToArea(Area destinationArea) {

        WorldPosition currentPos = script.getWorldPosition();
        if (currentPos == null) {
            script.log("GetWaterskins", "Player position is null, cannot move to area.");
            return false;
        }

        // Already inside the area
        if (destinationArea.contains(currentPos)) {
            waitTillStopped();
            return true;
        }

        WalkConfig cfg = new WalkConfig.Builder()
                .breakCondition(() -> {
                    WorldPosition pos = script.getWorldPosition();
                    return pos != null && destinationArea.contains(pos);
                })
                .disableWalkScreen(true)
                .enableRun(true)
                .build();

        boolean walking = script.getWalker().walkTo(
                destinationArea.getRandomPosition(),
                cfg
        );

        if (!walking) {
            script.log("GetWaterskins", "Failed to initiate walk to destination area.");
            return false;
        }

        // Ensure we fully stop after entering the area
        waitTillStopped();

        return true;
    }

    private void waitTillStopped() {
        task = "Wait till stopped";
        script.log("GetWaterskins", "Waiting until player stops moving...");

        AtomicReference<WorldPosition> lastPos =
                new AtomicReference<>(script.getWorldPosition());

        long[] stillStart = { System.currentTimeMillis() };
        long[] animClearSince = { -1 };

        int delay = RandomUtils.uniformRandom(750, 1050);

        java.util.function.BooleanSupplier stopCondition = () -> {

            WorldPosition now = script.getWorldPosition();
            WorldPosition prev = lastPos.get();

            if (now == null || prev == null) {
                stillStart[0] = System.currentTimeMillis();
                animClearSince[0] = -1;
                lastPos.set(now);
                return false;
            }

            boolean sameTile =
                    now.getX() == prev.getX() &&
                            now.getY() == prev.getY() &&
                            now.getPlane() == prev.getPlane();

            if (!sameTile) {
                stillStart[0] = System.currentTimeMillis();
                animClearSince[0] = -1;
                lastPos.set(now);
                return false;
            }

            long nowMs = System.currentTimeMillis();

            return nowMs - stillStart[0] >= delay;
        };

        script.pollFramesUntil(
                stopCondition,
                RandomUtils.uniformRandom(4000, 7500));
    }

    private RSObject getSpecificObjectAt(String name, int worldX, int worldY, int plane) {
        task = "Get RSObject";
        return script.getObjectManager().getRSObject(obj ->
                obj != null
                        && obj.getName() != null
                        && name.equalsIgnoreCase(obj.getName())
                        && obj.getWorldX() == worldX
                        && obj.getWorldY() == worldY
                        && obj.getPlane() == plane
        );
    }
}
