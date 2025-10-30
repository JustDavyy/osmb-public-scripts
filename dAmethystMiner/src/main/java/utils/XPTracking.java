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

    // --- Mining ---

    public XPTracker getMiningTracker() {
        return getTracker(SkillType.MINING);
    }

    public double getMiningXpGained() {
        XPTracker tracker = getMiningTracker();
        return (tracker != null) ? tracker.getXpGained() : 0.0;
    }

    public int getMiningXpPerHour() {
        XPTracker tracker = getMiningTracker();
        return (tracker != null) ? tracker.getXpPerHour() : 0;
    }

    public int getMiningLevel() {
        XPTracker tracker = getMiningTracker();
        return (tracker != null) ? tracker.getLevel() : 0;
    }

    public String getMiningTimeToNextLevel() {
        XPTracker tracker = getMiningTracker();
        return (tracker != null) ? tracker.timeToNextLevelString() : "-";
    }

    // --- Crafting ---

    public XPTracker getCraftingTracker() {
        return getTracker(SkillType.CRAFTING);
    }

    public double getCraftingXpGained() {
        XPTracker tracker = getCraftingTracker();
        return (tracker != null) ? tracker.getXpGained() : 0.0;
    }

    public int getCraftingXpPerHour() {
        XPTracker tracker = getCraftingTracker();
        return (tracker != null) ? tracker.getXpPerHour() : 0;
    }

    public int getCraftingLevel() {
        XPTracker tracker = getCraftingTracker();
        return (tracker != null) ? tracker.getLevel() : 0;
    }

    public String getCraftingTimeToNextLevel() {
        XPTracker tracker = getCraftingTracker();
        return (tracker != null) ? tracker.timeToNextLevelString() : "-";
    }
}
