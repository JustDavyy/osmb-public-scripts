
package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ZoomType;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.visual.image.Image;
import data.MineArea;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;
import utils.XPTracking;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
    name = "dMLMv2",
    threadUrl = "https://wiki.osmb.co.uk/article/dmlmv2",
    skillCategory = SkillCategory.MINING,
    version = 1.1,
    author = "JustDavyy"
)
public class dMLMv2 extends Script {
    public static final String scriptVersion = "1.1";
    private final String scriptName = "MLMv2";
    public static String latestVersionString = "";
    private static boolean outdated = false;
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    public static boolean setupDone = false;

    // Config settings

    // MLM
    public static MineArea selectedMineArea;
    public static boolean repairWaterWheel;
    public static boolean dropHammer;
    public static int paydirtMined = 0;
    public static boolean useUpperHopper = false;

    // onPaint trackers
    public static String paintTask = "Initialize";
    public static int xpGained = 0;
    public static int gpGained = 0;
    public static long startTime = System.currentTimeMillis();
    public static int nuggetsGained = 0;
    public static int coalGained = 0;
    public static int goldGained = 0;
    public static int mithrilGained = 0;
    public static int adamantGained = 0;
    public static int runeGained = 0;

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

    public static double levelProgressFraction = 0.0;
    public static int currentLevel = 1;
    public static int startLevel = 1;

    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    private List<Task> tasks;

    public dMLMv2(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public void onRelog() {
        log(getClass(), "onRelog; clear flags!");
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{14936};
    }

    @Override
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dMLMv2 v" + scriptVersion);

        if (checkForUpdates()) {
            log("MLMv2", "This script version is outdated, please download the latest version from GitHub.");
            stop();
            return;
        }

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dMLMv2 Options", false);

        // Retrieve UI settings
        selectedMineArea = ui.getSelectedArea();
        useUpperHopper = ui.isUseUpperHopperEnabled();

        log("MLM", "Selected mine area: " + selectedMineArea);
        log("MLM", "Repair water wheel: " + repairWaterWheel);
        log("MLM", "Drop hammer: " + dropHammer);

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
                new MLM(this)
        );
    }

    @Override
    public int poll() {
        long nowMs = System.currentTimeMillis();

        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats((gpGained), xpGained, elapsed);
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

        // --- Live XP via built-in tracker ---
        String ttlText = "-";
        double etl = 0;                  // remaining XP to next level
        double xpGainedLive = 0;         // live gained XP since start
        double currentXp = 0;            // live absolute XP

        // --- Get built-in XPTracker for Mining ---
        XPTracker tracker = null;
        Map<SkillType, XPTracker> trackers = getXPTrackers(); // Use built-in tracker map
        if (trackers != null) {
            tracker = trackers.get(SkillType.MINING);
        }

        if (tracker != null) {
            xpGainedLive = tracker.getXpGained();
            currentXp = tracker.getXp();

            // level sync (only ever increases)
            final int MAX_LEVEL = 99;
            int guard = 0;
            while (currentLevel < MAX_LEVEL
                    && currentXp >= tracker.getExperienceForLevel(currentLevel + 1)
                    && guard++ < 10) {
                currentLevel++;
            }

            ttlText = tracker.timeToNextLevelString();

            if (currentLevel == 99) {
                ttlText = "MAXED";
            }

            int curLevelXpStart = tracker.getExperienceForLevel(currentLevel);
            int nextLevelXpTarget = tracker.getExperienceForLevel(Math.min(MAX_LEVEL, currentLevel + 1));
            int span = Math.max(1, nextLevelXpTarget - curLevelXpStart);

            etl = Math.max(0, nextLevelXpTarget - currentXp);

            levelProgressFraction = Math.max(0.0, Math.min(1.0,
                    (currentXp - curLevelXpStart) / (double) span));
        }

        int xpPerHour = (int) Math.round(xpGainedLive / hours);
        xpGained = (int) Math.round(xpGainedLive);

        // (+N) display
        if (startLevel <= 0) startLevel = currentLevel;
        int levelsGained = Math.max(0, currentLevel - startLevel);
        String currentLevelText = (levelsGained > 0)
                ? (currentLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentLevel);

        // Percent text (dot decimal)
        double pct = Math.max(0, Math.min(100, levelProgressFraction * 100.0));
        String levelProgressText = (Math.abs(pct - Math.rint(pct)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", pct)
                : String.format(java.util.Locale.US, "%.1f%%", pct);

        DecimalFormat fullFormat = new DecimalFormat("#,###");
        DecimalFormatSymbols sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        fullFormat.setDecimalFormatSymbols(sym);

        // === Panel + layout (new style) ===
        final int x = 5;
        final int baseY = 40;
        final int width = 260;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 8;
        final int logoBottomGap = 8;

        // colors
        final int labelGray = new Color(180, 180, 180).getRGB();
        final int valueWhite = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB();
        final int valueBlue = new Color(70, 130, 180).getRGB();
        final int valueRed = new Color(255, 0, 0).getRGB();

        // logo scaling
        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = logoImage;

        int totalLines = 19;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap;
        y += smallGap;
        y += 10;

        int innerHeight = Math.max(275, y - innerY);

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

        // 2) Current level (+N)
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Current level", currentLevelText, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 3) Level progress (green)
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Level progress", levelProgressText, labelGray, valueGreen,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 4) ETL
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "ETL", fullFormat.format(Math.round(etl)), labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 5) TTL
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "TTL", ttlText, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 6) XP gained (+ /hr combined)
        curY += lineGap;
        String xpCombined = fullFormat.format(xpGained)
                + " (" + fullFormat.format(xpPerHour) + "/hr)";
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "XP gained", xpCombined,
                labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 7) GP gained (+ /hr combined)
        int gpPerHour = (int) Math.round(gpGained / hours);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "GP gained",
                fullFormat.format(gpGained) + " (" + fullFormat.format(gpPerHour) + "/hr)",
                labelGray, valueGreen,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 8) Paydirt mined
        int paydirtPerHour = (int) Math.round(paydirtMined / hours);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Paydirt mined",
                paydirtMined + " (" + paydirtPerHour + "/hr)",
                labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawDivider(c, innerX, curY, innerWidth);

        curY += 10;
        drawOreGrid(c, innerX + 37, curY, this, hours);

        curY += (38 * 2);
        curY += 35;
        drawDivider(c, innerX, curY, innerWidth);

        // 9) Mode (selected mine area)
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Area", selectedMineArea.toString(),
                labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 10) Task
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", String.valueOf(paintTask),
                labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // 11) Version
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion,
                labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // Store canvas for webhook usage (if you use it here too)
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

    private void drawDivider(Canvas c, int x, int y, int width) {
        c.drawLine(x + 10, y, x + width - 10, y, new Color(255, 255, 255, 80).getRGB());
    }

    private void drawOreGrid(
            Canvas c,
            int startX,
            int startY,
            ScriptCore core,
            double hours
    ) {
        int iconSize = 28;
        int cellWidth = 70;
        int rowHeight = iconSize + 14 + 10;

        int[] oresTop = {
                ItemID.GOLDEN_NUGGET,
                ItemID.COAL,
                ItemID.GOLD_ORE
        };

        int[] oresBottom = {
                ItemID.MITHRIL_ORE,
                ItemID.ADAMANTITE_ORE,
                ItemID.RUNITE_ORE
        };

        drawOreRow(c, oresTop, startX, startY, hours, iconSize, cellWidth);
        drawOreRow(c, oresBottom, startX, startY + rowHeight, hours, iconSize, cellWidth);
    }

    private void drawOreRow(
            Canvas c,
            int[] ores,
            int x,
            int y,
            double hours,
            int iconSize,
            int cellWidth
    ) {
        int gap = 5;

        for (int i = 0; i < ores.length; i++) {
            int id = ores[i];

            int gained = getOreGained(id);
            int perHour = (int) Math.round(gained / hours);

            Image img = getItemManager().getItemImage(
                    id,
                    Math.max(1, gained),
                    ZoomType.SIZE_1,
                    0x00000000
            );

            // add gap * per index
            int drawX = x + (i * cellWidth) + (i * gap);

            // Draw icon
            if (img != null) {
                c.drawAtOn(img, drawX, y);
            }

            // Text below icon
            String text = gained + " (" + perHour + "/hr)";
            int textY = y + iconSize + 14;

            // Center text under icon
            int textWidth = c.getFontMetrics(FONT_LABEL).stringWidth(text);
            int textX = drawX + (iconSize / 2) - (textWidth / 2);

            c.drawText(
                    text,
                    textX,
                    textY,
                    Color.WHITE.getRGB(),
                    FONT_LABEL
            );
        }
    }

    private int getOreGained(int itemId) {
        return switch (itemId) {
            case ItemID.GOLDEN_NUGGET -> nuggetsGained;
            case ItemID.COAL -> coalGained;
            case ItemID.GOLD_ORE -> goldGained;
            case ItemID.MITHRIL_ORE -> mithrilGained;
            case ItemID.ADAMANTITE_ORE -> adamantGained;
            case ItemID.RUNITE_ORE -> runeGained;
            default -> 0;
        };
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

            String footerText = "Next update/webhook at: " + nextLocalStr;

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

                    .append("\"footer\": { \"text\": \"").append(escapeJson(footerText)).append("\" }")

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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dMLMv2/src/main/java/main/dMLMv2.java");

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
        } catch (Exception ignored) {
        }
        return null;
    }

    // Tiny JSON escaper (for webhook footer text)
    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
                log("STATS", "✅ Stats reported: xp=" + xpGained + ", gp=" + gpGained + ", runtime=" + (runtimeMs/1000) + "s");
            } else {
                log("STATS", "⚠ Failed to report stats, HTTP " + code);
            }
        } catch (Exception e) {
            log("STATS", "❌ Error sending stats: " + e.getMessage());
        }
    }
}
