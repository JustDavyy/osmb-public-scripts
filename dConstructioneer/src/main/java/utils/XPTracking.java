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

    // --- Construction-specific methods ---
    public XPTracker getConstructionTracker() {
        return getTracker(SkillType.CONSTRUCTION);
    }

    public double getConstructionXpGained() {
        XPTracker tracker = getConstructionTracker();
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getConstructionXpPerHour() {
        XPTracker tracker = getConstructionTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getConstructionLevel() {
        XPTracker tracker = getConstructionTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getConstructionTimeToNextLevel() {
        XPTracker tracker = getConstructionTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }
}
