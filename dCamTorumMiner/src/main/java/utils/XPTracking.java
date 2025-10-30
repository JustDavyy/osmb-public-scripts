package utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import main.dCamTorumMiner;

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
        if (tracker == null) return 0.0;
        return tracker.getXpGained();
    }

    public int getMiningXpPerHour() {
        XPTracker tracker = getMiningTracker();
        if (tracker == null) return 0;
        return tracker.getXpPerHour();
    }

    public int getMiningLevel() {
        XPTracker tracker = getMiningTracker();
        if (tracker == null) return 0;
        return tracker.getLevel();
    }

    public String getMiningTimeToNextLevel() {
        XPTracker tracker = getMiningTracker();
        if (tracker == null) return "-";
        return tracker.timeToNextLevelString();
    }

    // Optional helper for your miner logic (used to reset timer on XP gain)
    public boolean hasGainedXpSinceLastCheck(double previousXp) {
        XPTracker tracker = getMiningTracker();
        if (tracker == null) return false;
        double currentXp = tracker.getXpGained();
        if (currentXp > previousXp) {
            dCamTorumMiner.lastXpGain.reset();
            return true;
        }
        return false;
    }
}