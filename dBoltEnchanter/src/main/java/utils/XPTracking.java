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

    private XPTracker getTracker(SkillType skill) {
        Map<SkillType, XPTracker> trackers = core.getXPTrackers();
        if (trackers == null) return null;
        return trackers.get(skill);
    }

    // --- Magic ---

    public XPTracker getMagicTracker() {
        return getTracker(SkillType.MAGIC);
    }

    public double getMagicXpGained() {
        XPTracker tracker = getMagicTracker();
        return (tracker != null) ? tracker.getXpGained() : 0.0;
    }

    public int getMagicXpPerHour() {
        XPTracker tracker = getMagicTracker();
        return (tracker != null) ? tracker.getXpPerHour() : 0;
    }

    public int getMagicLevel() {
        XPTracker tracker = getMagicTracker();
        return (tracker != null) ? tracker.getLevel() : 0;
    }

    public String getMagicTimeToNextLevel() {
        XPTracker tracker = getMagicTracker();
        return (tracker != null) ? tracker.timeToNextLevelString() : "-";
    }
}
