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

    // --- Internal helper ---
    private XPTracker getTracker(SkillType skill) {
        Map<SkillType, XPTracker> trackers = core.getXPTrackers();
        if (trackers == null) return null;
        return trackers.get(skill);
    }

    // --- Smithing-specific accessors ---
    public XPTracker getSmithingTracker() {
        return getTracker(SkillType.SMITHING);
    }

    public double getSmithingXpGained() {
        XPTracker tracker = getSmithingTracker();
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getSmithingXpPerHour() {
        XPTracker tracker = getSmithingTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getSmithingLevel() {
        XPTracker tracker = getSmithingTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getSmithingTimeToNextLevel() {
        XPTracker tracker = getSmithingTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }
}