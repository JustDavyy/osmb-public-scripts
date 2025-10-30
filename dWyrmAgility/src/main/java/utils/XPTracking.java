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

    public XPTracker getAgilityTracker() {
        Map<SkillType, XPTracker> trackers = core.getXPTrackers();
        if (trackers == null) return null;
        return trackers.get(SkillType.AGILITY);
    }

    public double getAgilityXpGained() {
        XPTracker tracker = getAgilityTracker();
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getAgilityXpPerHour() {
        XPTracker tracker = getAgilityTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getAgilityLevel() {
        XPTracker tracker = getAgilityTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getTimeToNextLevel() {
        XPTracker tracker = getAgilityTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }
}
