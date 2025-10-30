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

    // --- Fishing ---

    public XPTracker getFishingTracker() {
        return getTracker(SkillType.FISHING);
    }

    public double getFishingXpGained() {
        XPTracker tracker = getFishingTracker();
        return (tracker != null) ? tracker.getXpGained() : 0.0;
    }

    public int getFishingXpPerHour() {
        XPTracker tracker = getFishingTracker();
        return (tracker != null) ? tracker.getXpPerHour() : 0;
    }

    public int getFishingLevel() {
        XPTracker tracker = getFishingTracker();
        return (tracker != null) ? tracker.getLevel() : 0;
    }

    public String getFishingTimeToNextLevel() {
        XPTracker tracker = getFishingTracker();
        return (tracker != null) ? tracker.timeToNextLevelString() : "-";
    }

    // --- Cooking ---

    public XPTracker getCookingTracker() {
        return getTracker(SkillType.COOKING);
    }

    public double getCookingXpGained() {
        XPTracker tracker = getCookingTracker();
        return (tracker != null) ? tracker.getXpGained() : 0.0;
    }

    public int getCookingXpPerHour() {
        XPTracker tracker = getCookingTracker();
        return (tracker != null) ? tracker.getXpPerHour() : 0;
    }

    public int getCookingLevel() {
        XPTracker tracker = getCookingTracker();
        return (tracker != null) ? tracker.getLevel() : 0;
    }

    public String getCookingTimeToNextLevel() {
        XPTracker tracker = getCookingTracker();
        return (tracker != null) ? tracker.timeToNextLevelString() : "-";
    }
}