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

    public XPTracker getMiningTracker() {
        // Get all active XP trackers
        Map<SkillType, XPTracker> trackers = core.getXPTrackers();

        // Return the mining tracker if it exists
        return trackers.get(SkillType.MINING);
    }
}