package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.Script;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import component.MushroomTransportInterface;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;

@ScriptDefinition(
name = "dBirdhouseRunner",
threadUrl = "",
        version = 1.1,
        author = "JustDavyy",
        skillCategory = SkillCategory.HUNTER
)
public class dBirdhouseRunner extends Script {
    public static final String scriptVersion = "1.1";
    public static WorldPosition currentPos;

    // Settings from UI
    public static int selectedBirdhouseId;
    public static int selectedSeedId;
    public static boolean enableSeaweedRun;
    public static int compostId;
    public static int usedLogsId;

    // Webhook settings
    private static final Stopwatch webhookTimer = new Stopwatch();
    public static boolean webhookEnabled;
    public static String webhookUrl;
    public static int webhookInterval;
    public static boolean webhookIncludeUser;
    public static boolean webhookIncludeStats;
    private static String user = "";

    // Runtime state
    public static boolean needToBank = false;
    public static boolean setupDone = false;
    public static long startTime;

    // Status/task tracking
    public static String task = "Initializing";
    public static String lastRunType = "None yet";

    // Birdhouse tracking
    public static int birdhousesPlaced = 0;
    public static int birdhouseRuns = 0;
    public static int seedNests = 0;
    public static int ringNests = 0;
    public static int emptyNests = 0;
    public static int clueNests = 0;
    public static int eggNests = 0;
    public static int totalNests = 0;
    public static long nextBirdhouseRunTime = 0L;

    // Seaweed tracking
    public static int seaweedCount = 0;
    public static int seaweedRuns = 0;
    public static long nextSeaweedRunTime = 0L;

    // XP tracking
    public static int craftingXpGained = 0;
    public static int hunterXpGained = 0;

    // Areas
    public static final Area bankIslandArea = new RectangleArea(3768, 3896, 4, 4, 0);
    public static final Area mushroomMeadowArea = new RectangleArea(3645, 3798, 79, 109, 0);
    public static final Area verdantValleyArea = new RectangleArea(3748, 3750, 26, 15, 0);
    public static final Area campLandingArea = new RectangleArea(3721, 3807, 8, 5, 0);
    public static final Area underwaterArea = new RectangleArea(3716, 10253, 30, 35, 1);
    public static final Area underwaterFarmingArea = new RectangleArea(3728, 10264, 8, 12, 1);
    public static final Area underwaterLandingArea = new RectangleArea(3730, 10280, 4, 4, 1);

    private static final Font FONT = Font.getFont("Arial");
    public static MushroomTransportInterface mushroomInterface;

    private List<Task> tasks;

    public dBirdhouseRunner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Launching dBirdhouseRunner v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dBirdhouseRunner Config", false);

        // BIRDHOUSE
        selectedBirdhouseId = ui.getSelectedBirdhouseTier();
        selectedSeedId = ui.getSelectedSeedId();
        usedLogsId = ui.getRequiredLogId();

        // SEAWEED
        enableSeaweedRun = ui.isSeaweedRunEnabled();
        compostId = ui.getCompostId();

        // WEBHOOKS
        webhookEnabled = ui.isWebhookEnabled();
        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            webhookUrl = ui.getWebhookUrl();
            webhookInterval = ui.getWebhookInterval();
            webhookIncludeUser = ui.isUsernameIncluded();
            webhookIncludeStats = ui.isStatsIncluded();
            webhookTimer.reset(webhookInterval * 60_000L);
            log("WEBHOOK", "Webhook enabled, sending every " + webhookInterval + " minutes.");
        }

        checkForUpdates();
        startTime = System.currentTimeMillis();

        // Initialize transport component
        mushroomInterface = new MushroomTransportInterface(this);

        // Get current position
        currentPos = getWorldPosition();

        // Tasks
        tasks = Arrays.asList(
                new Setup(this),
                new BirdHouseRun(this),
                new SeaweedRun(this),
                new BankTask(this),
                new BreakManager(this)
        );
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                14908, // Bank chest island
                14652, // Mushroom meadow north
                14651, // Mushroom meadow south
                14906, // Verdant valley
                14907, // Museum camp
                15008, // Underwater area
                14909, // North of bank island
                15165, // Northeast of bank island
                15164, // East of bank island
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onPaint(Canvas c) {
        DecimalFormat f = new DecimalFormat("#,###");
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        String runtime = formatTime(elapsed);

        long timeToNextBirdhouse = nextBirdhouseRunTime - now;
        long timeToNextSeaweed = nextSeaweedRunTime - now;

        boolean showSeaweed = enableSeaweedRun;
        boolean birdhouseReady = nextBirdhouseRunTime > 0;
        boolean seaweedReady = nextSeaweedRunTime > 0;

        long nextRun = Long.MAX_VALUE;
        if (birdhouseReady) nextRun = Math.min(nextRun, timeToNextBirdhouse);
        if (showSeaweed && seaweedReady) nextRun = Math.min(nextRun, timeToNextSeaweed);

        String nextRunFormatted = nextRun < Long.MAX_VALUE ? formatTime(nextRun) : "N/A";

        int y = 40;
        c.fillRect(5, y, 250, showSeaweed ? 390 : 330, Color.BLACK.getRGB(), 0.75f);
        c.drawRect(5, y, 250, showSeaweed ? 390 : 330, Color.BLACK.getRGB());

        // === Birdhouse section ===
        c.drawText("Birdhouses placed: " + f.format(birdhousesPlaced), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Runs completed: " + f.format(birdhouseRuns), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Seed nests: " + f.format(seedNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Ring nests: " + f.format(ringNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Empty nests: " + f.format(emptyNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Egg nests: " + f.format(eggNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Clue nests: " + f.format(clueNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Total nests: " + f.format(totalNests), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Next birdhouse run in: " + (birdhouseReady ? formatTime(timeToNextBirdhouse) : "N/A"), 10, y += 20, Color.WHITE.getRGB(), FONT);

        // === Seaweed section (if enabled) ===
        if (showSeaweed) {
            c.drawText("Seaweed farmed: " + f.format(seaweedCount), 10, y += 30, Color.WHITE.getRGB(), FONT);
            c.drawText("Seaweed runs: " + f.format(seaweedRuns), 10, y += 20, Color.WHITE.getRGB(), FONT);
            c.drawText("Next seaweed run in: " + (seaweedReady ? formatTime(timeToNextSeaweed) : "N/A"), 10, y += 20, Color.WHITE.getRGB(), FONT);
        }

        // === General stats ===
        c.drawText("Task: " + task, 10, y += 30, Color.WHITE.getRGB(), FONT);
        c.drawText("Runtime: " + runtime, 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Next run in: " + nextRunFormatted, 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Crafting XP: " + f.format(craftingXpGained), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Hunter XP: " + f.format(hunterXpGained), 10, y += 20, Color.WHITE.getRGB(), FONT);
        c.drawText("Version: " + scriptVersion, 10, y += 20, Color.WHITE.getRGB(), FONT);
    }

    private static String formatTime(long millis) {
        long seconds = Math.max(millis / 1000, 0);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public int poll() {
        if (webhookEnabled && webhookTimer.hasFinished()) {
            sendWebhook();
            webhookTimer.reset(webhookInterval * 60_000L);
        }

        if (tasks != null) {
            for (Task task : tasks) {
                if (task.activate()) {
                    task.execute();
                    return 0;
                }
            }
        }
        return 0;
    }

    private void checkForUpdates() {
        try {
            String urlRaw = "https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dBirdhouseRunner/src/main/java/main/dBirdhouseRunner.java";
            String latest = getLatestVersion(urlRaw);
            if (latest == null) {
                log("UPDATE", "⚠ Could not fetch latest version info.");
                return;
            }

            if (compareVersions(scriptVersion, latest) < 0) {
                log("UPDATE", "⏬ New version v" + latest + " found! Updating...");
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");
                if (!dir.exists()) dir.mkdirs();

                for (File f : dir.listFiles((d, n) -> n.startsWith("dBirdhouseRunner"))) {
                    if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());
                }

                File out = new File(dir, "dBirdhouseRunner-" + latest + ".jar");
                URL jarUrl = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dBirdhouseRunner/jar/dBirdhouseRunner.jar");
                try (InputStream in = jarUrl.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }
                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();
            } else {
                log("SCRIPTVERSION", "✅ You are running a newer version (v" + scriptVersion + ") than the published one (v" + latest + ").");
                log("SCRIPTVERSION", "🙏 Thank you for testing a development build — your time and feedback are appreciated!");
            }
        } catch (Exception e) {
            log("UPDATE", "❌ Error updating: " + e.getMessage());
        }
    }

    private String getLatestVersion(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            if (c.getResponseCode() != 200) return null;

            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    if (l.trim().startsWith("version")) {
                        return l.split("=")[1].replace(",", "").trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    private void sendWebhook() {
        ByteArrayOutputStream baos = null;

        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) return;

            com.osmb.api.visual.image.Image screenImage = getScreen().getImage();
            BufferedImage bufferedImage = screenImage.toBufferedImage();

            baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            String boundary = "----WebhookBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            long now = System.currentTimeMillis();
            long elapsed = now - startTime;
            String formattedRuntime = formatTime(elapsed);

            long timeToNextBirdhouse = nextBirdhouseRunTime - now;
            long timeToNextSeaweed = nextSeaweedRunTime - now;

            boolean showSeaweed = enableSeaweedRun;
            boolean birdhouseReady = nextBirdhouseRunTime > 0;
            boolean seaweedReady = nextSeaweedRunTime > 0;

            long nextRun = Long.MAX_VALUE;
            if (birdhouseReady) nextRun = Math.min(nextRun, timeToNextBirdhouse);
            if (showSeaweed && seaweedReady) nextRun = Math.min(nextRun, timeToNextSeaweed);
            String nextRunFormatted = nextRun < Long.MAX_VALUE ? formatTime(nextRun) : "N/A";

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setGroupingSeparator('.');
            DecimalFormat f = new DecimalFormat("#,###");
            f.setDecimalFormatSymbols(symbols);

            StringBuilder payload = new StringBuilder();
            payload.append("{\"embeds\": [{");
            payload.append("\"title\": \"\\uD83D\\uDCCA dBirdhouseRunner Stats - ")
                    .append(webhookIncludeUser && user != null ? escapeJson(user) : "anonymous").append("\",");
            payload.append("\"color\": 15844367,");

            if (webhookIncludeStats) {
                payload.append("\"fields\": [")
                        .append("{\"name\": \"Birdhouses Placed\", \"value\": \"").append(f.format(birdhousesPlaced)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Birdhouse Runs\", \"value\": \"").append(f.format(birdhouseRuns)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Seed Nests\", \"value\": \"").append(f.format(seedNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Ring Nests\", \"value\": \"").append(f.format(ringNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Empty Nests\", \"value\": \"").append(f.format(emptyNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Egg Nests\", \"value\": \"").append(f.format(eggNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Clue Nests\", \"value\": \"").append(f.format(clueNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Total Nests\", \"value\": \"").append(f.format(totalNests)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Next Birdhouse Run\", \"value\": \"").append(birdhouseReady ? formatTime(timeToNextBirdhouse) : "N/A").append("\", \"inline\": true}");

                if (enableSeaweedRun) {
                    payload.append(",")
                            .append("{\"name\": \"Seaweed Farmed\", \"value\": \"").append(f.format(seaweedCount)).append("\", \"inline\": true},")
                            .append("{\"name\": \"Seaweed Runs\", \"value\": \"").append(f.format(seaweedRuns)).append("\", \"inline\": true},")
                            .append("{\"name\": \"Next Seaweed Run\", \"value\": \"").append(seaweedReady ? formatTime(timeToNextSeaweed) : "N/A").append("\", \"inline\": true}");
                }

                payload.append(",")
                        .append("{\"name\": \"Current Task\", \"value\": \"").append(escapeJson(task)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Runtime\", \"value\": \"").append(formattedRuntime).append("\", \"inline\": true},")
                        .append("{\"name\": \"Next Run In\", \"value\": \"").append(nextRunFormatted).append("\", \"inline\": true},")
                        .append("{\"name\": \"Crafting XP\", \"value\": \"").append(f.format(craftingXpGained)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Hunter XP\", \"value\": \"").append(f.format(hunterXpGained)).append("\", \"inline\": true},")
                        .append("{\"name\": \"Version\", \"value\": \"").append(scriptVersion).append("\", \"inline\": true}")
                        .append("],");
            } else {
                payload.append("\"description\": \"Currently running task: ").append(escapeJson(task)).append("\",");
            }

            payload.append("\"image\": {\"url\": \"attachment://screen.png\"}}]}");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());
                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
            }

            int response = conn.getResponseCode();
            log("WEBHOOK", response == 204 || response == 200 ? "✅ Sent webhook" : "⚠ Failed with HTTP " + response);

        } catch (Exception e) {
            log("WEBHOOK", "❌ Failed to send webhook: " + e.getMessage());
        } finally {
            try { if (baos != null) baos.close(); } catch (IOException ignored) {}
        }
    }

    public static void sendPauseOrResumeWebhook(boolean paused, String runType, long waitMillis) {
        try {
            if (webhookUrl == null || webhookUrl.isEmpty()) return;

            long now = System.currentTimeMillis();
            String formattedWait = formatTime(waitMillis);
            String resumeTime = formatLocalTime(now + waitMillis);

            String title = paused ? "\u23F8 Script Paused" : "\u25B6 Script Resumed";
            String description;

            if (paused) {
                description = "Paused script until next `" + runType + "` run.\n\n"
                        + "**Time Until Run:** ``" + formattedWait + "``\n"
                        + "**Resume Time (Local):** ``" + resumeTime + "``";
            } else {
                description = "Resuming script for `" + runType + "` run at ``" + resumeTime + "``.";
            }

            String payload = "{" +
                    "\"embeds\": [" +
                    "{" +
                    "\"title\": \"" + title + "\"," +
                    "\"description\": \"" + escapeJson(description) + "\"," +
                    "\"color\": " + (paused ? "15158332" : "3066993") +
                    "}" +
                    "]" +
                    "}";

            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            int response = conn.getResponseCode();
        } catch (Exception e) {
            // Do nothing here
        }
    }

    private static String formatLocalTime(long millis) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(millis));
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
