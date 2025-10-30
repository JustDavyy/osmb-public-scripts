package utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.tabs.skill.SkillType;

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

    // --- Cooking-specific methods ---
    public XPTracker getCookingTracker() {
        return getTracker(SkillType.COOKING);
    }

    public double getCookingXpGained() {
        XPTracker tracker = getCookingTracker();
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getCookingXpPerHour() {
        XPTracker tracker = getCookingTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getCookingLevel() {
        XPTracker tracker = getCookingTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getCookingTimeToNextLevel() {
        XPTracker tracker = getCookingTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }
}