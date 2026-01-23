package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmb.api.trackers.experience.XPTracker;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.MineTask;
import tasks.Setup;
import tasks.CraftTask;
import utils.Task;
import utils.XPTracking;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
        name = "dAmethystMiner",
        description = "Mines and crafts/banks amethyst in the mining guild",
        skillCategory = SkillCategory.MINING,
        version = 2.7,
        author = "JustDavyy"
)
public class dAmethystMiner extends Script {
    public static final String scriptVersion = "2.7";
    private final String scriptName = "AmethystMiner";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;

    public static boolean bankMode = false;
    public static boolean craftMode = false;
    public static boolean normalAreaMode = false;
    public static boolean diaryAreaMode = false;
    public static boolean setupDone = false;
    public static int selectedAmethystItemId = -1;

    public static int amethystMined = 0;
    public static int amethystCrafted = 0;

    public static int miningXpGained = 0;
    public static int craftingXpGained = 0;

    // Area
    public static final Area normalMiningArea = new RectangleArea(3015, 9698, 16, 11, 0);
    public static final Area diaryMiningArea = new RectangleArea(2999, 9705, 13, 23, 0);
    public static final Area normalMiningWalkArea = new RectangleArea(3022, 9707, 3, 2, 0);
    public static final Area diaryMiningWalkArea = new RectangleArea(3004, 9707, 4, 1, 0);

    public static String task = "Initializing...";
    public static long startTime = System.currentTimeMillis();

    private List<Task> tasks;

    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    public static double levelProgressFraction = 0.0;
    public static int currentMiningLevel = 1;
    public static int startMiningLevel = 1;
    public static int currentCraftingLevel = 1;
    public static int startCraftingLevel = 1;

    // Webhook
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static String user = "";
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://www.osmb.co.uk/lovable-uploads/ad86059b-ce19-4540-8e53-9fd01c61c98b.png";
    private volatile long nextWebhookEarliestMs = 0L;
    private static long lastWebhookSent = 0;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();

    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    public dAmethystMiner(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12183, 12184, 11927};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dAmethystMiner v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Amethyst Options", false);

        String mode = ui.getMode();
        String area = ui.getSelectedAreaMode();
        bankMode = mode.equals("Bank");
        craftMode = mode.equals("Craft");
        normalAreaMode = area.equals("Normal");
        diaryAreaMode = area.equals("Diary");
        selectedAmethystItemId = ui.getSelectedAmethystProductId();

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            queueSendWebhook();
        }

        if (checkForUpdates()) {
            stop();
            return;
        }

        tasks = Arrays.asList(
                new Setup(this),
                new BankTask(this),
                new CraftTask(this),
                new MineTask(this)
        );
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats((amethystMined * 3702L), (long) (miningXpGained + craftingXpGained), elapsed);
            lastStatsSent = nowMs;
        }

        for (Task task : tasks) {
            if (task.activate()) {
                task.execute();
                return 0;
            }
        }
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        // ======= LIVE XP (via built-in trackers) & level sync =======
        // Mining
        String miningTTL = "-";
        double miningETL = 0;
        double miningXpLive = 0;
        double miningXpAbs = 0;
        double levelProgressFractionMining = 0.0;

        XPTracker mTrack = (xpTracking != null) ? xpTracking.getMiningTracker() : null;
        if (mTrack != null) {
            miningXpLive = mTrack.getXpGained();
            miningXpAbs = mTrack.getXp();

            final int MAX_LEVEL = 99;
            int guard = 0;
            while (currentMiningLevel < MAX_LEVEL
                    && miningXpAbs >= mTrack.getExperienceForLevel(currentMiningLevel + 1)
                    && guard++ < 10) {
                currentMiningLevel++;
            }

            miningTTL = mTrack.timeToNextLevelString();

            int curStart = mTrack.getExperienceForLevel(currentMiningLevel);
            int nextReq = mTrack.getExperienceForLevel(Math.min(MAX_LEVEL, currentMiningLevel + 1));
            int span = Math.max(1, nextReq - curStart);

            miningETL = Math.max(0, nextReq - miningXpAbs);
            levelProgressFractionMining = Math.max(0.0, Math.min(1.0,
                    (miningXpAbs - curStart) / (double) span));
        }

        // Crafting (only shown if craftMode)
        String craftingTTL = "-";
        double craftingETL = 0;
        double craftingXpLive = 0;
        double craftingXpAbs = 0;
        double levelProgressFractionCrafting = 0.0;

        XPTracker cTrack = (craftMode && xpTracking != null) ? xpTracking.getCraftingTracker() : null;
        if (cTrack != null) {
            craftingXpLive = cTrack.getXpGained();
            craftingXpAbs = cTrack.getXp();

            final int MAX_LEVEL = 99;
            int guard = 0;
            while (currentCraftingLevel < MAX_LEVEL
                    && craftingXpAbs >= cTrack.getExperienceForLevel(currentCraftingLevel + 1)
                    && guard++ < 10) {
                currentCraftingLevel++;
            }

            craftingTTL = cTrack.timeToNextLevelString();

            int curStart = cTrack.getExperienceForLevel(currentCraftingLevel);
            int nextReq = cTrack.getExperienceForLevel(Math.min(MAX_LEVEL, currentCraftingLevel + 1));
            int span = Math.max(1, nextReq - curStart);

            craftingETL = Math.max(0, nextReq - craftingXpAbs);
            levelProgressFractionCrafting = Math.max(0.0, Math.min(1.0,
                    (craftingXpAbs - curStart) / (double) span));
        }

        // ======= Totals & rates =======
        int amethystHr = (int) Math.round(amethystMined / hours);
        int miningXpHr = (int) Math.round((miningXpGained > 0 ? miningXpGained : miningXpLive) / hours);

        int itemsCrafted = calculateCraftedItems(amethystCrafted);
        int itemsCraftedHr = (int) Math.round(itemsCrafted / hours);
        int craftingXpHr = (int) Math.round((craftingXpGained > 0 ? craftingXpGained : craftingXpLive) / hours);

        // ======= Formatters =======
        java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols sy = new java.text.DecimalFormatSymbols();
        sy.setGroupingSeparator('.');
        fmt.setDecimalFormatSymbols(sy);

        // Level text
        if (startMiningLevel <= 0) startMiningLevel = currentMiningLevel;
        int miningLevelsGained = Math.max(0, currentMiningLevel - startMiningLevel);
        String miningLevelText = (miningLevelsGained > 0)
                ? (currentMiningLevel + " (+" + miningLevelsGained + ")")
                : String.valueOf(currentMiningLevel);

        if (startCraftingLevel <= 0) startCraftingLevel = currentCraftingLevel;
        int craftingLevelsGained = Math.max(0, currentCraftingLevel - startCraftingLevel);
        String craftingLevelText = (craftingLevelsGained > 0)
                ? (currentCraftingLevel + " (+" + craftingLevelsGained + ")")
                : String.valueOf(currentCraftingLevel);

        // Percent text
        String miningProgressText = percentText(levelProgressFractionMining);
        String craftingProgressText = percentText(levelProgressFractionCrafting);

        // ======= Panel + layout =======
        final int x = 5;
        final int baseY = 40;
        final int width = 225;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 6;
        final int logoBottomGap = 8;

        final int labelGray = new Color(180, 180, 180).getRGB();
        final int valueWhite = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB();
        final int valueBlue = new Color(70, 130, 180).getRGB();

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        int totalLines = 9;
        if (craftMode) totalLines += 8;
        totalLines += 2; // Task + Version

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap + smallGap + 10;
        int innerHeight = Math.max(240, y - innerY);

        // Panel
        c.fillRect(innerX - borderThickness, innerY - borderThickness,
                innerWidth + (borderThickness * 2),
                innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.decode("#01031C").getRGB(), 1);
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        int curY = innerY + topGap;

        // Logo
        if (scaledLogo != null) {
            int imgX = innerX + (innerWidth - scaledLogo.width) / 2;
            c.drawAtOn(scaledLogo, imgX, curY);
            curY += scaledLogo.height + logoBottomGap;
        }

        // ==== Lines ====
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Runtime", runtime, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Amethyst mined", fmt.format(amethystMined), labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Amethyst/hr", fmt.format(amethystHr), labelGray, valueBlue,
                FONT_VALUE_BOLD, FONT_LABEL);

        // --- Mining block ---
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Mining XP gained", fmt.format((miningXpGained > 0) ? miningXpGained : Math.round(miningXpLive)),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Mining XP/hr", fmt.format(miningXpHr), labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "ETL (Mining)", fmt.format(Math.round(miningETL)), labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "TTL (Mining)", miningTTL, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Level Progress (Mining)", miningProgressText, labelGray, valueGreen,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Current level (Mining)", miningLevelText, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // --- Crafting block (optional) ---
        if (craftMode) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Crafting XP gained", fmt.format((craftingXpGained > 0) ? craftingXpGained : Math.round(craftingXpLive)),
                    labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Crafting XP/hr", fmt.format(craftingXpHr), labelGray, valueWhite,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "ETL (Crafting)", fmt.format(Math.round(craftingETL)), labelGray, valueWhite,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "TTL (Crafting)", craftingTTL, labelGray, valueWhite,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Level Progress (Crafting)", craftingProgressText, labelGray, valueGreen,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Current level (Crafting)", craftingLevelText, labelGray, valueWhite,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Items crafted", fmt.format(itemsCrafted), labelGray, valueBlue,
                    FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Items/hr", fmt.format(itemsCraftedHr), labelGray, valueBlue,
                    FONT_VALUE_BOLD, FONT_LABEL);
        }

        // --- Footer ---
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", String.valueOf(task), labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion, labelGray, valueWhite,
                FONT_VALUE_BOLD, FONT_LABEL);

        // Store canvas for webhook usage
        try {
            lastCanvasFrame.set(c.toImageCopy());
        } catch (Exception ignored) {}
    }

    private static String percentText(double fraction) {
        double pct = Math.max(0, Math.min(100, fraction * 100.0));
        return (Math.abs(pct - Math.rint(pct)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", pct)
                : String.format(java.util.Locale.US, "%.1f%%", pct);
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
                    .append("https://discord.com/channels/736938454478356570/789791439487500299")
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAmethystMiner/src/main/java/main/dAmethystMiner.java");

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

    public static Area getMiningArea() {
        if (diaryAreaMode) {
            return diaryMiningArea;
        } else {
            return normalMiningArea;
        }
    }

    public static Area getMiningWalkArea() {
        if (diaryAreaMode) {
            return diaryMiningWalkArea;
        } else {
            return normalMiningWalkArea;
        }
    }

    private int calculateCraftedItems(int amethystUsed) {
        return switch (selectedAmethystItemId) {
            case ItemID.AMETHYST_ARROWTIPS, ItemID.AMETHYST_BOLT_TIPS -> amethystUsed * 15;
            case ItemID.AMETHYST_DART_TIP -> amethystUsed * 8;
            case ItemID.AMETHYST_JAVELIN_HEADS -> amethystUsed * 5;
            default -> 0;
        };
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
