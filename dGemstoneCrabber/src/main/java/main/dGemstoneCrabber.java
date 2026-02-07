package main;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
import tasks.Bank;
import tasks.Setup;
import tasks.Fight;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(
        name = "dGemstoneCrabber",
        description = "Trains combat by hunting the gem stone crab",
        skillCategory = SkillCategory.COMBAT,
        version = 3.1,
        author = "JustDavyy"
)
public class dGemstoneCrabber extends Script implements WebhookSender {
    public static final String scriptVersion = "3.1";
    private final String scriptName = "GemstoneCrabber";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    private static double xpGained = 0.0;
    public static boolean setupDone = false;
    public static boolean canHopNow = false;
    public static boolean canBreakNow = false;
    public static boolean canBankNow = false;
    public static boolean needToBank = false;
    public static boolean foundCrab = false;
    public static boolean alreadyFought = false;
    public static boolean needToAttack = false;
    public static boolean onlyHopAfterKill = false;
    public static boolean shouldEat = false;
    public static boolean shouldPot = false;
    public static boolean useFood = false;
    public static boolean usePot = false;
    public static boolean useDBAXE = false;
    public static boolean useHearts = false;
    public static int heartID;
    public static int foodID = 1;
    public static int potID = 2;
    public static int foodAmount;
    public static int potAmount;
    public static int eatAtPerc = 60;

    public static long nextPotAt = 0L;
    public static long dbaNextBoostAt   = 0L;
    public static long heartNextBoostAt = 0L;

    // Position
    public static WorldPosition currentPos;

    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    public static boolean webhookEnabled = false;
    public static boolean webhookShowUser = false;
    public static String webhookUrl = "";
    public static int webhookIntervalMinutes = 5;
    public static long lastWebhookSent = 0;
    public static String user = "";
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://www.osmb.co.uk/lovable-uploads/ad86059b-ce19-4540-8e53-9fd01c61c98b.png";
    private volatile long nextWebhookEarliestMs = 0L;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();

    // XP Reading stuff
    public static long lastXpGainAt  = 0L;
    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    public static final Stopwatch switchTabTimer = new Stopwatch();

    private List<Task> tasks;
    private ScriptUI ui;

    public dGemstoneCrabber(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                4913, 4912, 4911,
                5169, 5168, 5167,
                5425, 5424, 5423
        };
    }

    @Override
    public boolean promptBankTabDialogue() {
        return usePot || useFood;
    }

    @Override
    public boolean canBreak() {
        return canBreakNow;
    }

    @Override
    public boolean canHopWorlds() {
        return canHopNow;
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dGemstoneCrabber v" + scriptVersion);

        if (checkForUpdates()) {
            stop();
            return;
        }

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Gemstone crabber Options", false);

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }

        usePot = ui.isUsePotions();
        useFood = ui.isUseFood();
        onlyHopAfterKill = ui.isOnlyHopBreakAfterKill();
        if (useFood) {
            foodID = ui.getSelectedFoodItemId();
            foodAmount = ui.getFoodQuantity();
            eatAtPerc = ui.getFoodEatPercent();
        }
        if (usePot) {
            potID = ui.getSelectedPotionItemId();
            potAmount = ui.getPotionQuantity();
        }
        useDBAXE = ui.isUseDragonBattleaxeSpec();
        useHearts = ui.isUseHeart();

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new Bank(this));
        tasks.add(new Fight(this, this));
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

        // === Combined XP tracking ===
        double xpGainedLive = 0.0;
        if (xpTracking != null) {
            XPTracker totalTracker = xpTracking.getXpTracker();
            if (totalTracker != null) {
                xpGainedLive = totalTracker.getXpGained();
            }
        }

        // === Display numbers ===
        int xpPerHourLive = (int) Math.round(xpGainedLive / hours);
        java.text.DecimalFormat totalFmt = new java.text.DecimalFormat("#,###");
        String totalXpText = totalFmt.format(Math.round(xpGainedLive));
        String xpPerHourText = formatRateKMB(xpPerHourLive) + "/hr";
        xpGained = Math.round(xpGainedLive);

        String breakText = "" + getProfileManager().isDueToBreak();
        String hopText = "" + getProfileManager().isDueToHop();
        String eatText = "" + shouldEat;
        String potText = formatBoost(nextPotAt);
        String axeText = formatBoost(dbaNextBoostAt);
        String heartText = formatBoost(heartNextBoostAt);
        String taskText = String.valueOf(task);
        String lastXpText = formatLastXpGain();

        // === Layout ===
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
        final int valueBlue = new Color(70, 130, 180).getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB();

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int totalLines = 9 + (usePot ? 1 : 0) + (useDBAXE ? 1 : 0) + (useHearts ? 1 : 0);
        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap + smallGap + 10;
        int innerHeight = Math.max(220, y - innerY);

        // === Frame ===
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

        // === Stats ===
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Runtime", runtime, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "XP gained", totalXpText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "XP rate", xpPerHourText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Time to break", breakText, labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Time to hop", hopText, labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Should eat", eatText, labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);

        if (usePot) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Next pot", potText, labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        }

        if (useDBAXE) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Next spec", axeText, labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        }

        if (useHearts) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Next heart", heartText, labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        }

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", taskText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Last XP gain", lastXpText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

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

    @Override
    public void onNewFrame() {
        xpTracking.checkXP();
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

    private String formatRateKMB(double value) {
        double abs = Math.abs(value);
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");
        if (abs >= 1_000_000_000d) {
            return df.format(value / 1_000_000_000d) + " b";
        } else if (abs >= 1_000_000d) {
            return df.format(value / 1_000_000d) + " m";
        } else if (abs >= 1_000d) {
            return df.format(value / 1_000d) + " k";
        } else {
            return df.format(value);
        }
    }

    private boolean checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dGemstoneCrabber/src/main/java/main/dGemstoneCrabber.java");

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

    public static List<Integer> getPotionVariantOrder(int potionId) {
        switch (potionId) {
            // Strength potion
            case ItemID.STRENGTH_POTION4:
            case ItemID.STRENGTH_POTION3:
            case ItemID.STRENGTH_POTION2:
            case ItemID.STRENGTH_POTION1:
                return List.of(ItemID.STRENGTH_POTION1, ItemID.STRENGTH_POTION2, ItemID.STRENGTH_POTION3, ItemID.STRENGTH_POTION4);

            // Combat potion
            case ItemID.COMBAT_POTION4:
            case ItemID.COMBAT_POTION3:
            case ItemID.COMBAT_POTION2:
            case ItemID.COMBAT_POTION1:
                return List.of(ItemID.COMBAT_POTION1, ItemID.COMBAT_POTION2, ItemID.COMBAT_POTION3, ItemID.COMBAT_POTION4);

            // Super strength
            case ItemID.SUPER_STRENGTH4:
            case ItemID.SUPER_STRENGTH3:
            case ItemID.SUPER_STRENGTH2:
            case ItemID.SUPER_STRENGTH1:
                return List.of(ItemID.SUPER_STRENGTH1, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH4);

            // Divine super strength
            case ItemID.DIVINE_SUPER_STRENGTH_POTION4:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION3:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION2:
            case ItemID.DIVINE_SUPER_STRENGTH_POTION1:
                return List.of(ItemID.DIVINE_SUPER_STRENGTH_POTION1, ItemID.DIVINE_SUPER_STRENGTH_POTION2, ItemID.DIVINE_SUPER_STRENGTH_POTION3, ItemID.DIVINE_SUPER_STRENGTH_POTION4);

            // Super attack
            case ItemID.SUPER_ATTACK1:
            case ItemID.SUPER_ATTACK2:
            case ItemID.SUPER_ATTACK3:
            case ItemID.SUPER_ATTACK4:
                return List.of(ItemID.SUPER_ATTACK1, ItemID.SUPER_ATTACK2, ItemID.SUPER_ATTACK3, ItemID.SUPER_ATTACK4);

            // Divine super attack
            case ItemID.DIVINE_SUPER_ATTACK_POTION1:
            case ItemID.DIVINE_SUPER_ATTACK_POTION2:
            case ItemID.DIVINE_SUPER_ATTACK_POTION3:
            case ItemID.DIVINE_SUPER_ATTACK_POTION4:
                return List.of(ItemID.DIVINE_SUPER_ATTACK_POTION1, ItemID.DIVINE_SUPER_ATTACK_POTION2, ItemID.DIVINE_SUPER_ATTACK_POTION3, ItemID.DIVINE_SUPER_ATTACK_POTION4);

            // Ranging
            case ItemID.RANGING_POTION4:
            case ItemID.RANGING_POTION3:
            case ItemID.RANGING_POTION2:
            case ItemID.RANGING_POTION1:
                return List.of(ItemID.RANGING_POTION1, ItemID.RANGING_POTION2, ItemID.RANGING_POTION3, ItemID.RANGING_POTION4);

            // Divine ranging
            case ItemID.DIVINE_RANGING_POTION4:
            case ItemID.DIVINE_RANGING_POTION3:
            case ItemID.DIVINE_RANGING_POTION2:
            case ItemID.DIVINE_RANGING_POTION1:
                return List.of(ItemID.DIVINE_RANGING_POTION1, ItemID.DIVINE_RANGING_POTION2, ItemID.DIVINE_RANGING_POTION3, ItemID.DIVINE_RANGING_POTION4);

            // Zamorak brew
            case ItemID.ZAMORAK_BREW4:
            case ItemID.ZAMORAK_BREW3:
            case ItemID.ZAMORAK_BREW2:
            case ItemID.ZAMORAK_BREW1:
                return List.of(ItemID.ZAMORAK_BREW1, ItemID.ZAMORAK_BREW2, ItemID.ZAMORAK_BREW3, ItemID.ZAMORAK_BREW4);

            // Bastion
            case ItemID.BASTION_POTION4:
            case ItemID.BASTION_POTION3:
            case ItemID.BASTION_POTION2:
            case ItemID.BASTION_POTION1:
                return List.of(ItemID.BASTION_POTION1, ItemID.BASTION_POTION2, ItemID.BASTION_POTION3, ItemID.BASTION_POTION4);

            // Divine bastion
            case ItemID.DIVINE_BASTION_POTION4:
            case ItemID.DIVINE_BASTION_POTION3:
            case ItemID.DIVINE_BASTION_POTION2:
            case ItemID.DIVINE_BASTION_POTION1:
                return List.of(ItemID.DIVINE_BASTION_POTION1, ItemID.DIVINE_BASTION_POTION2, ItemID.DIVINE_BASTION_POTION3, ItemID.DIVINE_BASTION_POTION4);

            // Super combat
            case ItemID.SUPER_COMBAT_POTION4:
            case ItemID.SUPER_COMBAT_POTION3:
            case ItemID.SUPER_COMBAT_POTION2:
            case ItemID.SUPER_COMBAT_POTION1:
                return List.of(ItemID.SUPER_COMBAT_POTION1, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION4);

            // Divine super combat
            case ItemID.DIVINE_SUPER_COMBAT_POTION4:
            case ItemID.DIVINE_SUPER_COMBAT_POTION3:
            case ItemID.DIVINE_SUPER_COMBAT_POTION2:
            case ItemID.DIVINE_SUPER_COMBAT_POTION1:
                return List.of(ItemID.DIVINE_SUPER_COMBAT_POTION1, ItemID.DIVINE_SUPER_COMBAT_POTION2, ItemID.DIVINE_SUPER_COMBAT_POTION3, ItemID.DIVINE_SUPER_COMBAT_POTION4);

            // Forgotten Brew
            case ItemID.FORGOTTEN_BREW4:
            case ItemID.FORGOTTEN_BREW3:
            case ItemID.FORGOTTEN_BREW2:
            case ItemID.FORGOTTEN_BREW1:
                return List.of(ItemID.FORGOTTEN_BREW1, ItemID.FORGOTTEN_BREW2, ItemID.FORGOTTEN_BREW3, ItemID.FORGOTTEN_BREW4);

            default:
                // If it’s a single ID or unknown, just return the given one
                return List.of(potionId);
        }
    }

    public static int totalPotionAmount(ItemGroupResult inv, List<Integer> ids) {
        int sum = 0;
        for (int id : ids) sum += inv.getAmount(id);
        return sum;
    }

    public static List<Integer> getFoodVariantOrder(int foodId) {
        if (foodId == ItemID.CAKE) {
            return java.util.List.of(1895, 1893, 1891);
        }
        if (foodId == ItemID.PLAIN_PIZZA) {
            return java.util.List.of(2291, 2289);
        }

        // Default: single-ID food
        return java.util.List.of(foodId);
    }

    public static int totalAmount(ItemGroupResult inv, List<Integer> ids) {
        int sum = 0;
        for (int id : ids) sum += inv.getAmount(id);
        return sum;
    }

    private String formatBoost(long nextBoostAt) {
        if (nextBoostAt == 0L) {
            return "now";
        }
        long now = System.currentTimeMillis();
        long remaining = nextBoostAt - now;
        if (remaining <= 0) {
            return "now";
        }

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long sec = seconds % 60;

        return String.format("%02d:%02d", minutes, sec);
    }

    private String formatLastXpGain() {
        if (lastXpGainAt <= 0) {
            return "never";
        }

        long now = System.currentTimeMillis();
        long diffMs = now - lastXpGainAt;
        long diffSec = diffMs / 1000L;

        // Format local time hh:mm:ss
        java.time.LocalTime local =
                java.time.Instant.ofEpochMilli(lastXpGainAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime();

        String timeStr = local.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        return String.format("%s (%ds)", timeStr, diffSec);
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
