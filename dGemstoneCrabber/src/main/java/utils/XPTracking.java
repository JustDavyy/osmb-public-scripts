package utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import main.dGemstoneCrabber;

import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

import static main.dGemstoneCrabber.lastXpGainAt;

public class XPTracking {

    private static final int SPRITE_TOTAL = 222;

    private final ScriptCore core;
    private final XPDropsComponent xpDropsComponent;
    private final SearchableImage totalXpSprite;

    // Built-in trackers (per skill) + total
    private final Map<SkillType, XPTracker> skillTrackers = new EnumMap<>(SkillType.class);
    private final XPTracker combinedTracker;

    // OCR-based total tracker
    private XPTracker totalSpriteTracker;

    private static final SkillType[] TRACKED_SKILLS = {
            SkillType.ATTACK,
            SkillType.STRENGTH,
            SkillType.DEFENCE,
            SkillType.RANGE,
            SkillType.MAGIC,
            SkillType.HITPOINTS
    };

    public XPTracking(ScriptCore core) {
        this.core = core;
        this.xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        this.combinedTracker = new XPTracker(core, 0);

        // total-XP sprite for fallback OCR
        SearchableImage full = new SearchableImage(SPRITE_TOTAL, core, new SingleThresholdComparator(3), ColorModel.RGB);
        this.totalXpSprite = full.subImage(full.width / 2, 0, full.width / 2, full.height);
    }

    public XPTracker getXpTracker() {
        return combinedTracker;
    }

    public void checkXP() {
        boolean xpGainedThisTick = false;

        // --- Built-in skill tracking ---
        Map<SkillType, XPTracker> liveTrackers = core.getXPTrackers();
        if (liveTrackers != null && !liveTrackers.isEmpty()) {
            for (SkillType skill : TRACKED_SKILLS) {
                XPTracker live = liveTrackers.get(skill);
                if (live == null) continue;

                double currentXp = live.getXp();
                XPTracker cached = skillTrackers.get(skill);
                if (cached == null) {
                    cached = new XPTracker(core, (int) Math.round(currentXp));
                    skillTrackers.put(skill, cached);
                    continue;
                }

                double prevXp = cached.getXp();
                double delta = currentXp - prevXp;
                if (delta > 0 && delta <= 10_000) {
                    cached.incrementXp(delta);
                    combinedTracker.incrementXp(delta);
                    xpGainedThisTick = true;
                }
            }
        }

        // --- OCR “total-XP” fallback ---
        if (checkXPCounterActive()) {
            Integer totalXpValue = getTotalXpCounter();
            if (totalXpValue != null) {
                if (totalSpriteTracker == null) {
                    totalSpriteTracker = new XPTracker(core, totalXpValue);
                } else {
                    double prev = totalSpriteTracker.getXp();
                    double delta = totalXpValue - prev;
                    if (delta > 0 && delta <= 100_000) {
                        totalSpriteTracker.incrementXp(delta);
                        combinedTracker.incrementXp(delta);
                        xpGainedThisTick = true;
                    }
                }
            }
        }

        // --- unified last-gain timestamp ---
        if (xpGainedThisTick) {
            lastXpGainAt = System.currentTimeMillis();
        }
    }

    private Integer getTotalXpCounter() {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) return null;

        boolean matches = core.getImageAnalyzer().findLocation(bounds, totalXpSprite) != null;
        if (!matches) return null;

        String xpText = core.getOCR()
                .getText(com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT, bounds, -1)
                .replaceAll("[^0-9]", "");
        if (xpText.isEmpty()) return null;

        try {
            return Integer.parseInt(xpText);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean checkXPCounterActive() {
        if (xpDropsComponent == null) return false;
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) return true;

        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            core.getFinger().tap(bounds);
            boolean succeed = core.pollFramesHuman(() -> {
                ComponentSearchResult<Integer> r = xpDropsComponent.getResult();
                return r != null && r.getComponentImage().getGameFrameStatusType() == 1;
            }, RandomUtils.uniformRandom(1500, 3000));
            bounds = xpDropsComponent.getBounds();
            return succeed && bounds != null;
        }
        return true;
    }

    private Rectangle getXPDropsBounds() {
        XPDropsComponent comp = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        if (comp == null) return null;
        Rectangle b = comp.getBounds();
        if (b == null) return null;
        ComponentSearchResult<Integer> result = comp.getResult();
        if (result == null || result.getComponentImage().getGameFrameStatusType() != 1) {
            return null;
        }
        return new Rectangle(b.x - 140, b.y - 1, 119, 38);
    }

    public double getTotalXpGained() {
        return combinedTracker.getXpGained();
    }
}