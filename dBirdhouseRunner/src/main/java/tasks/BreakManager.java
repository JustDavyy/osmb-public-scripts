package tasks;

import com.osmb.api.script.Script;
import com.osmb.api.utils.RandomUtils;
import utils.Task;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static main.dBirdhouseRunner.*;
import static main.dBirdhouseRunner.nextSeaweedRunTime;

public class BreakManager extends Task {

    public BreakManager(Script script) {
        super(script);
    }

    public boolean activate() {
        return true;
    }

    public boolean execute() {
        task = getClass().getSimpleName();

        task = "Evaluate next run";
        script.log(getClass(), "Evaluating which run is due first...");

        long now = System.currentTimeMillis();
        long birdhouseWait = Math.max(0, nextBirdhouseRunTime - now);
        long seaweedWait = enableSeaweedRun ? Math.max(0, nextSeaweedRunTime - now) : Long.MAX_VALUE;

        if (birdhouseWait < seaweedWait) {
            script.log(getClass(), "Birdhouse run is due first. Time remaining: " + formatTime(birdhouseWait));

            task = "Logout till next run";
            script.log(getClass().getSimpleName(), "Log out till next run is due");
            boolean success = script.getWidgetManager().getLogoutTab().logout();

            long waitTime = birdhouseWait + RandomUtils.uniformRandom(5000, 10000);
            waitTime = Math.max(0, waitTime); // Prevent negative sleep time

            // Format local time of next run
            String nextRunTime = formatLocalTime(System.currentTimeMillis() + waitTime);
            task = "Wait till next run at " + nextRunTime;
            sendPauseOrResumeWebhook(true, "Birdhouse", birdhouseWait);

            script.log(getClass().getSimpleName(), "Waiting ~" + formatTime(waitTime) + " till next run at " + nextRunTime);
            script.pollFramesHuman(() -> false, (int) waitTime, true);
            sendPauseOrResumeWebhook(false, "Birdhouse", 0);
        } else {
            script.log(getClass(), "Seaweed run is due first. Time remaining: " + formatTime(seaweedWait));

            task = "Logout till next run";
            script.log(getClass().getSimpleName(), "Log out till next run is due");
            boolean success = script.getWidgetManager().getLogoutTab().logout();

            if (!success) {
                script.log(getClass().getSimpleName(), "Logout failed, returning to retry.");
                return false;
            }

            long waitTime = seaweedWait + RandomUtils.uniformRandom(5000, 10000);
            waitTime = Math.max(0, waitTime);

            // Format local time of next run
            String nextRunTime = formatLocalTime(System.currentTimeMillis() + waitTime);
            task = "Wait till next run at " + nextRunTime;
            sendPauseOrResumeWebhook(true, "Seaweed", seaweedWait);

            script.log(getClass().getSimpleName(), "Waiting ~" + formatTime(waitTime) + " till next run at " + nextRunTime);
            script.pollFramesHuman(() -> false, (int) waitTime, true);
            sendPauseOrResumeWebhook(false, "Seaweed", 0);
        }

        return false;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }

    private String formatLocalTime(long timestampMillis) {
        LocalDateTime time = Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

}
