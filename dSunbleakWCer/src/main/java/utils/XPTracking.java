package utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.tabs.skill.SkillType;

import static main.dSunbleakWCer.lastXpGain;

import java.util.Map;

public class XPTracking {

    private final ScriptCore core;

    public XPTracking(ScriptCore core) {
        this.core = core;
    }

    // --- Internal helper to retrieve a specific tracker ---
    private XPTracker getTracker(SkillType skill) {
        Map<SkillType, XPTracker> trackers = core.getXPTrackers();
        if (trackers == null) return null;
        return trackers.get(skill);
    }

    // --- Woodcutting-specific methods ---
    public XPTracker getWoodcuttingTracker() {
        return getTracker(SkillType.WOODCUTTING);
    }

    public double getWoodcuttingXpGained() {
        XPTracker tracker = getWoodcuttingTracker();
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getWoodcuttingXpPerHour() {
        XPTracker tracker = getWoodcuttingTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getWoodcuttingLevel() {
        XPTracker tracker = getWoodcuttingTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getWoodcuttingTimeToNextLevel() {
        XPTracker tracker = getWoodcuttingTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }

    public void checkXP() {
        XPTracker tracker = getWoodcuttingTracker();
        if (tracker == null) return;

        double currentXp = tracker.getXpGained();
        if (currentXp > 0) {
            lastXpGain.reset();
        }
    }
}