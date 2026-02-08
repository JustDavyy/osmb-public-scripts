
package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

@ScriptDefinition(
name = "dERLooter",
threadUrl = "https://wiki.osmb.co.uk/article/derlooter",
        skillCategory = SkillCategory.OTHER,
        version = 1.8,
        author = "JustDavyy"
)
public class dERLooter extends Script {
    public static final String scriptVersion = "1.8";
    private final String scriptName = "ERLooter";
    public static String latestVersionString = "";
    private static boolean outdated = false;
    public static boolean setupDone = false;
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;

    // Hop tracking
    public static final int MAX_SAFE_HOPS = 350;
    public static final double HOP_REGEN_PER_HOUR = 93.0;

    public static int hopsUsed = 0;
    public static long hopTrackingStart = System.currentTimeMillis();

    // onPaint trackers
    public static int totalEclipseRed = 0;
    public static int previousEclipseRed = 0;
    public static int currentEclipseRed = 0;

    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();

    public static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    public static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    public static String user = "";
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://wiki.osmb.co.uk/assets/logo-Dq53Rvcx.gif";
    private volatile long nextWebhookEarliestMs = 0L;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();

    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    private List<Task> tasks;
    private ScriptUI ui;

    public dERLooter(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{6191};
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dERLooter v" + scriptVersion);

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

        tasks = Arrays.asList(
                new Setup(this),
                new ERLooter(this)
        );
    }

    @Override
    public int poll() {
        long nowMs = System.currentTimeMillis();

        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            long gpGained = totalEclipseRed * 700L;
            sendStats(gpGained, 0, elapsed);
            lastStatsSent = nowMs;
        }

        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
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

    @Override
    public void onPaint(Canvas c) {
        // --- Timing / runtime ---
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        int hopsAvailable = getAvailableHops();

        String hopStatus;
        int hopColor;

        // --- Wine / profit stats ---
        int eclipseLooted = totalEclipseRed;
        int eclipsePerHour = (int) Math.round(eclipseLooted / hours);

        long gpGained = eclipseLooted * 700L;
        long gpPerHour = (long) Math.round(gpGained / hours);

        DecimalFormat fullFormat = new DecimalFormat("#,###");
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        fullFormat.setDecimalFormatSymbols(sym);

        // === Panel + layout ===
        final int x = 5;
        final int baseY = 40;
        final int width = 260;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int logoBottomGap = 8;

        // colors
        final int labelGray   = new Color(180,180,180).getRGB();
        final int valueWhite  = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB();
        final int valueBlue = new Color(70, 130, 180).getRGB();

        // logo scaling
        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        if (hopsAvailable <= 5) {
            hopStatus = "COOLDOWN";
            hopColor = new Color(255, 80, 80).getRGB(); // red
        } else if (hopsAvailable <= 30) {
            hopStatus = "LOW";
            hopColor = new Color(255, 170, 0).getRGB(); // orange
        } else {
            hopStatus = "OK";
            hopColor = valueGreen;
        }

        int totalLines = 9;
        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap;
        y += 10;

        int innerHeight = Math.max(180, y - innerY);

        // Background + border
        c.fillRect(innerX - borderThickness, innerY - borderThickness,
                innerWidth + (borderThickness * 2),
                innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.decode("#01031C").getRGB(), 1);
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        int curY = innerY + topGap;

        if (scaledLogo != null) {
            int imgX = innerX + (innerWidth - scaledLogo.width) / 2;
            c.drawAtOn(scaledLogo, imgX, curY);
            curY += scaledLogo.height + logoBottomGap;
        }

        // 1) Runtime
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Runtime", runtime, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 2) Eclipse looted
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Eclipses looted", fullFormat.format(eclipseLooted), labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 3) Eclipse/hr
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Eclipses/hr", fullFormat.format(eclipsePerHour), labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 4) GP gained
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "GP gained", fullFormat.format(gpGained), labelGray, valueGreen,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 5) GP/hr
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "GP/hr", fullFormat.format(gpPerHour), labelGray, valueGreen,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 6) Hops used
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Hops used", String.valueOf(hopsUsed),
                labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 7) Hop safety
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Hop status",
                hopsAvailable + " left (" + hopStatus + ")",
                labelGray, hopColor,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 8) Task
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", String.valueOf(task), labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 9) Version
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // Optionally keep canvas for webhook screenshots
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

    private void ensureLogoLoaded() {
        if (logoImage != null) return;

        try (InputStream in = getClass().getResourceAsStream("/logo.png")) {
            if (in == null) {
                log(getClass(), "Logo '/logo.png' not found on classpath.");
                return;
            }

            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                log(getClass(), "Failed to decode logo.png");
                return;
            }

            BufferedImage argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = argb.createGraphics();
            g.setComposite(AlphaComposite.Src); // copy pixels as-is
            g.drawImage(src, 0, 0, null);
            g.dispose();

            int w = argb.getWidth();
            int h = argb.getHeight();
            int[] px = new int[w * h];
            argb.getRGB(0, 0, w, h, px, 0, w);

            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                int a = (p >>> 24) & 0xFF;
                if (a == 0) {
                    px[i] = 0x00000000; // fully transparent black
                }
            }

            boolean PREMULTIPLY = true;
            if (PREMULTIPLY) {
                for (int i = 0; i < px.length; i++) {
                    int p = px[i];
                    int a = (p >>> 24) & 0xFF;
                    if (a == 0) { px[i] = 0; continue; }
                    int r = (p >>> 16) & 0xFF;
                    int gch = (p >>> 8) & 0xFF;
                    int b = p & 0xFF;
                    // premultiply
                    r = (r * a + 127) / 255;
                    gch = (gch * a + 127) / 255;
                    b = (b * a + 127) / 255;
                    px[i] = (a << 24) | (r << 16) | (gch << 8) | b;
                }
            }

            logoImage = new Image(px, w, h);
            log(getClass(), "Logo loaded: " + w + "x" + h + " premultiplied=" + PREMULTIPLY);

        } catch (Exception e) {
            log(getClass(), "Error loading logo: " + e.getMessage());
        }
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

                    .append("\"footer\": { \"text\": \"").append(nextLocalStr).append("\" }")

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

    private boolean checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dERLooter/src/main/java/main/dERLooter.java");

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
                log("STATS", "Stats reported: gp=" + gpEarned + ", runtime=" + (runtimeMs / 1000) + "s");
            } else {
                log("STATS", "Failed to report stats, HTTP " + code);
            }
        } catch (Exception e) {
            log("STATS", "Error sending stats: " + e.getMessage());
        }
    }

    public static int getAvailableHops() {
        long elapsedMs = System.currentTimeMillis() - hopTrackingStart;
        double elapsedHours = elapsedMs / 3_600_000.0;

        int regenerated = (int) (elapsedHours * HOP_REGEN_PER_HOUR);
        int available = MAX_SAFE_HOPS - hopsUsed + regenerated;

        return Math.max(0, available);
    }
}
