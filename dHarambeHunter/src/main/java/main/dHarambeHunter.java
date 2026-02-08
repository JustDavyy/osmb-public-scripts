package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
import tasks.Setup;
import tasks.Hunt;
import utils.Task;
import utils.XPTracking;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.Color;
import java.awt.Font;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
name = "dHarambeHunter",
threadUrl = "https://wiki.osmb.co.uk/article/dharambehunter",
        skillCategory = SkillCategory.HUNTER,
        version = 2.4,
        author = "JustDavyy"
)
public class dHarambeHunter extends Script {
    public static final String scriptVersion = "2.4";
    private final String scriptName = "HarambeHunter";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    public static boolean setupDone = false;
    public static boolean canHop = true;
    public static boolean canBreak = true;

    public static String task = "Initialize";
    private static final Font FONT_VALUE       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_LABEL_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://wiki.osmb.co.uk/assets/logo-Dq53Rvcx.gif";
    private volatile long nextWebhookEarliestMs = 0L;

    // =========================
    // Paint / Tracking Variables
    // =========================

    // Tracking XP
    public static double xpGained = 0;
    public static int currentLevel = 1;
    public static int startLevel = 1;
    public static double levelProgressFraction = 0.0;
    private final XPTracking xpTracking;

    // Timing
    private long startTime = System.currentTimeMillis();

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    private List<Task> tasks;
    private ScriptUI ui;

    public dHarambeHunter(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{11662};
    }

    @Override
    public boolean canBreak() {
        return canBreak;
    }

    @Override
    public boolean canHopWorlds() {return canHop;}

    @Override
    public void onStart() {
        log("INFO", "Starting dHarambeHunter v" + scriptVersion);

        if (checkForUpdates()) {
            stop();
            return;
        }

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Script Options", false);

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new Hunt(this));
    }

    @Override
    public void onRelog() {
        log(getClass(), "onRelog; clear previous world data!");
        Hunt.lastTrap = null;
        Hunt.lastTrapPos = null;
        Hunt.blacklistedTraps.clear();
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats(0L, (long) xpGained, elapsed);
            lastStatsSent = nowMs;
        }

        if (tasks != null) {
            for (Task taskObj : tasks) {
                if (taskObj.activate()) {
                    taskObj.execute();
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        double currentXp = 0.0;
        double xpGainedLive = 0.0;
        double etl = 0.0;
        String ttlText = "-";
        double levelProgressFraction = 0.0;

        if (xpTracking != null) {
            XPTracker tracker = xpTracking.getHunterTracker();
            if (tracker != null) {
                currentXp = tracker.getXp();
                xpGainedLive = tracker.getXpGained();
                ttlText = tracker.timeToNextLevelString();
                etl = tracker.getXpForNextLevel();

                int curLevelXpStart = tracker.getExperienceForLevel(currentLevel);
                int nextLevelXpTarget = tracker.getExperienceForLevel(Math.min(99, currentLevel + 1));
                int span = Math.max(1, nextLevelXpTarget - curLevelXpStart);
                levelProgressFraction = Math.max(0.0, Math.min(1.0,
                        (currentXp - curLevelXpStart) / (double) span));
            }
        }

        xpGained = xpGainedLive;
        double xpPerHour = xpGainedLive / hours;
        int catchesFromXp = (int) Math.ceil(xpGainedLive / 1000.0);
        int caughtPerHour = (int) Math.round(catchesFromXp / hours);

        if (startLevel <= 0) startLevel = currentLevel;
        int levelsGained = Math.max(0, currentLevel - startLevel);
        String currentLevelText = (levelsGained > 0)
                ? (currentLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentLevel);

        DecimalFormat intFmt = new DecimalFormat("#,###");
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        intFmt.setDecimalFormatSymbols(sym);

        String levelProgressText = formatPercent(levelProgressFraction * 100.0);
        if (currentLevel == 99) {
            ttlText = "MAXED";
            etl = 0;
            levelProgressText = "100%";
        }

        final int x = 5;
        final int baseY = 40;
        final int width = 225;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 6;
        final int logoBottomGap = 8;

        final int labelGray = new Color(180,180,180).getRGB();
        final int valueWhite = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB();
        final int valueBlue = new Color(70, 130, 180).getRGB();

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += 9 * lineGap + smallGap + 2 * lineGap + 10;
        int innerHeight = Math.max(275, y - innerY);

        c.fillRect(innerX - borderThickness, innerY - borderThickness,
                innerWidth + (borderThickness * 2),
                innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        int curY = innerY + topGap;

        if (scaledLogo != null) {
            int imgX = innerX + (innerWidth - scaledLogo.width) / 2;
            c.drawAtOn(scaledLogo, imgX, curY);
            curY += scaledLogo.height + logoBottomGap;
        }

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime,
                labelGray, valueWhite, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP Gained",
                intFmt.format(Math.round(xpGainedLive)), labelGray, valueWhite,
                FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP/Hour",
                intFmt.format(Math.round(xpPerHour)), labelGray, valueWhite,
                FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "ETL",
                intFmt.format(Math.round(etl)), labelGray, valueWhite,
                FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "TTL",
                ttlText, labelGray, valueWhite, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Level Progress",
                levelProgressText, labelGray, valueGreen, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Current Level",
                currentLevelText, labelGray, valueWhite, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Total Catches",
                intFmt.format(catchesFromXp), labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Caught/hr",
                intFmt.format(caughtPerHour), labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap + smallGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Task",
                String.valueOf(task), labelGray, valueWhite, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Version",
                scriptVersion, labelGray, valueWhite, FONT_LABEL_BOLD, FONT_VALUE);

        try {
            lastCanvasFrame.set(c.toImageCopy());
        } catch (Exception ignored) {}
    }

    private void drawStatLine(Canvas c, int innerX, int innerWidth, int paddingX, int y,
                              String label, String value, int labelColor, int valueColor,
                              Font labelFont, Font valueFont) {
        c.drawText(label, innerX + paddingX, y, labelColor, labelFont);
        int valW = c.getFontMetrics(valueFont).stringWidth(value);
        int valX = innerX + innerWidth - paddingX - valW;
        c.drawText(value, valX, y, valueColor, valueFont);
    }

    private String formatPercent(double pct) {
        double abs = Math.abs(pct);
        double rounded = Math.rint(abs);
        if (Math.abs(abs - rounded) < 1e-9) return String.format("%.0f%%", pct);
        return String.format("%.1f%%", pct);
    }

    private void sendWebhookInternal() {
        ByteArrayOutputStream baos = null;
        try {
            // Only proceed if we have a painted frame
            Image source = lastCanvasFrame.get();
            if (source == null) {
                log("WEBHOOK", "ℹ No painted frame available; skipping webhook.");
                return;
            }

            BufferedImage buffered = source.toBufferedImage();
            baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Runtime for description
            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);

            // Username (or anonymous)
            String displayUser = (webhookShowUser && user != null) ? user : "anonymous";

            // Next webhook local time (Europe/Amsterdam)
            long nextMillis = System.currentTimeMillis() + (webhookIntervalMinutes * 60_000L);
            ZonedDateTime nextLocal = ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(nextMillis),
                    ZoneId.systemDefault()
            );
            String nextLocalStr = nextLocal.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            String imageFilename = "canvas.png";
            StringBuilder json = new StringBuilder();
            json.append("{ \"embeds\": [ {")
                    .append("\"title\": \"Script run summary - ").append(displayUser).append("\",")

                    .append("\"color\": 5189303,")

                    .append("\"author\": {")
                    .append("\"name\": \"Davyy's ").append(scriptName).append("\",")
                    .append("\"icon_url\": \"").append(authorIconUrl).append("\"")
                    .append("},")

                    .append("\"description\": ")
                    .append("\"This is your progress report after running for **")
                    .append(runtime)
                    .append("**.\\n")
                    .append("Make sure to share your proggies in the OSMB proggies channel\\n")
                    .append("https://discord.com/channels/272130394655031308/1466620313742741649")
                    .append("\",")

                    .append("\"image\": { \"url\": \"attachment://").append(imageFilename).append("\" },")

                    .append("\"footer\": { \"text\": \"Next update/webhook at: ").append(nextLocalStr).append("\" }")

                    .append("} ] }");

            // Send multipart/form-data
            String boundary = "----WebBoundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                // payload_json
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                // image file
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + imageFilename + "\"\r\n").getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
            }

            int code = conn.getResponseCode();
            long now = System.currentTimeMillis();

            if (code == 200 || code == 204) {
                lastWebhookSent = now;
                log("WEBHOOK", "✅ Webhook sent.");
            } else if (code == 429) {
                long backoffMs = 30_000L;
                String ra = conn.getHeaderField("Retry-After");
                if (ra != null) {
                    try {
                        double sec = Double.parseDouble(ra.trim());
                        backoffMs = Math.max(1000L, (long)Math.ceil(sec * 1000.0));
                    } catch (NumberFormatException ignored) {}
                }
                nextWebhookEarliestMs = now + backoffMs + 250;
                log("WEBHOOK", "⚠ 429 rate-limited. Backing off ~" + backoffMs + "ms");
            } else {
                log("WEBHOOK", "⚠ Webhook failed. HTTP " + code);
            }

        } catch (Exception e) {
            log("WEBHOOK", "❌ Error: " + e.getMessage());
        } finally {
            try { if (baos != null) baos.close(); } catch (IOException ignored) {}
            webhookInFlight.set(false);
        }
    }

    public void queueSendWebhook() {
        if (!webhookEnabled) return;

        long now = System.currentTimeMillis();
        if (now < nextWebhookEarliestMs) return;
        if (now - lastWebhookSent < webhookIntervalMinutes * 60_000L) return;

        if (!webhookInFlight.compareAndSet(false, true)) return;

        sendWebhookAsync();
    }


    public void sendWebhookAsync() {
        Thread t = new Thread(this::sendWebhookInternal, "WebhookSender");
        t.setDaemon(true);
        t.start();
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd %02d:%02d:%02d", days, hours, minutes, secs);
        } else {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
    }

    private boolean checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dHarambeHunter/src/main/java/main/dHarambeHunter.java");

        if (latest == null) {
            log("VERSION", "Could not fetch latest version info.");
            return false;
        }

        // Compare versions
        if (compareVersions(scriptVersion, latest) < 0) {

            // Spam 10 log lines
            for (int i = 0; i < 10; i++) {
                log("VERSION", "New version v" + latest + " found! Please update the script before running it again.");
            }

            return true; // Outdated
        }

        // Up to date
        log("VERSION", "You are running the latest version (v" + scriptVersion + ").");
        return false;
    }

    public static int compareVersions(String v1, String v2) {
        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < a.length ? Integer.parseInt(a[i]) : 0;
            int n2 = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
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

    private void ensureLogoLoaded() {
        if (logoImage != null) return;

        try (InputStream in = getClass().getResourceAsStream("/logo.png")) {
            if (in == null) {
                log(getClass(), "Logo '/logo.png' not found on classpath.");
                return;
            }
            BufferedImage buf = ImageIO.read(in);
            if (buf == null) {
                log(getClass(), "Failed to decode logo.png");
                return;
            }
            // Convert BufferedImage -> API Image
            int w = buf.getWidth();
            int h = buf.getHeight();
            int[] argb = new int[w * h];
            buf.getRGB(0, 0, w, h, argb, 0, w);
            logoImage = new Image(argb, w, h);
        } catch (Exception e) {
            log(getClass(), "Error loading logo: " + e.getMessage());
        }
    }

    private void sendStats(long gpEarned, long xpGained, long runtimeMs) {
        try {
            String json = String.format(
                    "{\"script\":\"%s\",\"session\":\"%s\",\"gp\":%d,\"xp\":%d,\"runtime\":%d}",
                    scriptName,
                    sessionId,
                    gpEarned,
                    xpGained,
                    runtimeMs / 1000
            );

            URL url = new URL(obf.Secrets.STATS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Stats-Key", obf.Secrets.STATS_API);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                log("STATS", "✅ Stats reported: gp=" + gpEarned + ", runtime=" + (runtimeMs/1000) + "s");
            } else {
                log("STATS", "⚠ Failed to report stats, HTTP " + code);
            }
        } catch (Exception e) {
            log("STATS", "❌ Error sending stats: " + e.getMessage());
        }
    }
}
