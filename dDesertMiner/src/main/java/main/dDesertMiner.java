package main;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.spellbook.LunarSpellbook;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.ui.tabs.Spellbook;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.world.SkillTotal;
import com.osmb.api.world.World;
import component.ShantayShop;
import data.SandstoneData;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;
import utils.WaterskinTracker;
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
import java.util.function.BooleanSupplier;

@ScriptDefinition(
name = "dDesertMiner",
threadUrl = "https://wiki.osmb.co.uk/article/ddesertminer",
        skillCategory = SkillCategory.MINING,
        version = 1.0,
        author = "JustDavyy"
)
public class dDesertMiner extends Script {
    public static final String scriptVersion = "1.0";
    private final String scriptName = "DesertMiner";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    public static boolean setupDone = false;

    public static String task = "Initialize";
    private static final Font FONT_VALUE       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_LABEL_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    // Old stuff from base
    private WorldPosition lastMovePosition = null;
    private long lastMoveChangeMs = 0;
    private WorldPosition lastAnchorWalkTarget = null;
    private long lastAnchorWalkMs = 0;
    private boolean hopOnNearbyPlayersEnabled = false;
    private int hopRadiusTiles = 0;
    private long lastHopAttemptMs = 0;
    private final Set<WorldPosition> waitingRespawn = new HashSet<>();
    private ItemGroupResult inventorySnapshot = null;
    public static Integer waterskinCharges = 0;
    private final WaterskinTracker waterskinTracker;
    private int sandstoneMined = 0;
    private long lastHumidifyCastMs = 0;
    public static SandstoneData.MiningLocation miningLocation = SandstoneData.MiningLocation.NORTH;
    public static boolean useHumidify = false;
    public static boolean usingCirclet = false;
    private final Area outsidePassArea = new RectangleArea(3196, 2838, 5, 8, 0);
    private final Area insidePassArea = new RectangleArea(3190, 2840, 3, 5, 0);
    public static boolean justBoughtSkins = false;

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

    public static ShantayShop shopInterface;

    // Timing
    private long startTime = System.currentTimeMillis();

    // Logo image
    private Image logoImage = null;

    private List<Task> tasks;
    private ScriptUI ui;

    public dDesertMiner(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
        waterskinTracker = new WaterskinTracker(this, SandstoneData.WATERSKIN_IDS);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12589, 12588, 12844, 12845};
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dDesertMiner v" + scriptVersion);

        if (checkForUpdates()) {
            stop();
            return;
        }

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Script Options", false);

        miningLocation = ui.getMiningLocation();
        hopOnNearbyPlayersEnabled = ui.isHopEnabled();
        hopRadiusTiles = ui.getHopRadius();
        useHumidify = ui.isHumidifyEnabled();
        usingCirclet = ui.isCircletEnabled();

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }

        shopInterface = new ShantayShop(this);

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));
        tasks.add(new Humidify(this));
        tasks.add(new GetWaterskins(this));
        tasks.add(new Bank(this));
        tasks.add(new Mine(this));
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

        waterskinCharges = waterskinTracker.getCharges();

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
            XPTracker tracker = xpTracking.getMiningTracker();
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

        int sandstonePerHour = (int) Math.round(sandstoneMined / hours);

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
        Image scaledLogo = (logoImage != null) ? logoImage : null;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += 10 * lineGap + smallGap + 2 * lineGap + 10;
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
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Sandstone Mined",
                intFmt.format(sandstoneMined), labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Sandstone/hr",
                intFmt.format(sandstonePerHour), labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);

        if (usingCirclet) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY, "Waterskin sips",
                    "Unlimited (circlet)", labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);
        } else {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY, "Waterskin sips",
                    String.valueOf(waterskinCharges), labelGray, valueBlue, FONT_LABEL_BOLD, FONT_VALUE);
        }

        curY += lineGap;
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dDesertMiner/src/main/java/main/dDesertMiner.java");

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

    public boolean isInventoryFull() {
        ItemGroupResult inv = inventorySnapshot;
        if (inv == null) {
            inv = getWidgetManager().getInventory().search(Collections.emptySet());
        }
        return inv != null && inv.isFull();
    }

    public boolean handleFullInventory() {
        RSObject grinder = findGrinder();

        if (grinder != null && grinder.isInteractableOnScreen()) {
            if (!grinder.interact("Deposit")) {
                return false;
            }
        } else {
            if (isPlayerMoving()) {
                return false;
            }
            WalkConfig config = new WalkConfig.Builder()
                    .breakCondition(() -> {
                        RSObject g = findGrinder();
                        return g != null && g.isInteractableOnScreen();
                    })
                    .build();
            getWalker().walkTo(SandstoneData.GRINDER_POS, config);

            grinder = findGrinder();
            if (grinder == null || !grinder.isInteractableOnScreen() || !grinder.interact("Deposit")) {
                return false;
            }
        }

        if (RandomUtils.uniformRandom(0, 100) < 35) {
            pollFramesHuman(() -> {
                ItemGroupResult inv = getWidgetManager().getInventory().search(Collections.emptySet());
                return inv == null || !inv.isFull();
            }, 4_000);
        } else {
            pollFramesUntil(() -> {
                ItemGroupResult inv = getWidgetManager().getInventory().search(Collections.emptySet());
                return inv == null || !inv.isFull();
            }, 4_000);
            pollFramesUntil(() -> false, RandomUtils.uniformRandom(75, 250));
        }

        return false;
    }

    private RSObject findGrinder() {
        return getObjectManager().getRSObject(object ->
                object != null &&
                        object.getWorldPosition() != null &&
                        object.getName() != null &&
                        "Grinder".equalsIgnoreCase(object.getName()) &&
                        hasDepositAction(object)
        );
    }

    private boolean hasDepositAction(RSObject object) {
        String[] actions = object.getActions();
        if (actions == null) {
            return false;
        }
        for (String action : actions) {
            if ("Deposit".equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMineAction(RSObject object) {
        String[] actions = object.getActions();
        if (actions == null) {
            return false;
        }
        for (String action : actions) {
            if ("Mine".equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }

    public WorldPosition getAnchorForLocation() {
        return SandstoneData.getAnchor(miningLocation);
    }

    public boolean isAllowedRockPosition(WorldPosition position) {
        if (position == null) {
            return false;
        }
        return SandstoneData.getAllowedRocks(miningLocation).contains(position);
    }

    public boolean allowRock(WorldPosition position, List<WorldPosition> respawnCircles, WorldPosition playerPos) {
        if (!isAllowedRockPosition(position)) {
            return false;
        }
        if (waitingRespawn.contains(position)) {
            if (respawnCircles.contains(position)) {
                return false;
            }
            waitingRespawn.remove(position);
        }
        return true;
    }

    public boolean requestAnchorWalk(WorldPosition anchor) {
        if (anchor == null) {
            return false;
        }
        WorldPosition pos = getWorldPosition();
        if (pos != null && pos.distanceTo(anchor) <= 1.0) {
            return false;
        }
        if (isPlayerMoving()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (anchor.equals(lastAnchorWalkTarget) && now - lastAnchorWalkMs < 1200) {
            return false;
        }
        walkToAnchor(anchor);
        lastAnchorWalkTarget = anchor;
        lastAnchorWalkMs = now;
        return true;
    }

    private void walkToAnchor(WorldPosition anchor) {
        WalkConfig config = new WalkConfig.Builder()
                .minimapTapDelay(1200, 2000)
                .tileRandomisationRadius(0)
                .breakDistance(3)
                .setWalkMethods(false, true)
                .breakCondition(() -> {
                    WorldPosition pos = getWorldPosition();
                    return pos != null && pos.distanceTo(anchor) <= 0.0;
                })
                .build();
        getWalker().walkTo(anchor, config);
    }

    public List<WorldPosition> getRespawnCirclePositions() {
        List<com.osmb.api.shape.Rectangle> respawnCircles = getPixelAnalyzer().findRespawnCircles();
        List<WorldPosition> positions = getUtils().getWorldPositionForRespawnCircles(respawnCircles, 20);
        return positions != null ? positions : Collections.emptyList();
    }

    public boolean maybeHopForNearbyPlayers(WorldPosition anchor, WorldPosition myPos) {
        if (!hopOnNearbyPlayersEnabled || hopRadiusTiles <= 0 || anchor == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastHopAttemptMs < 10_000) {
            return false;
        }
        var minimap = getWidgetManager().getMinimap();
        if (minimap == null) {
            return false;
        }
        var players = minimap.getPlayerPositions();
        if (players == null || !players.isFound()) {
            return false;
        }
        for (WorldPosition pos : players.asList()) {
            if (pos == null) {
                continue;
            }
            if (myPos != null && pos.equals(myPos)) {
                continue;
            }
            if (pos.getPlane() != anchor.getPlane()) {
                continue;
            }
            int dx = Math.abs(pos.getX() - anchor.getX());
            int dy = Math.abs(pos.getY() - anchor.getY());
            int chebyshevDistance = Math.max(dx, dy);
            if (chebyshevDistance <= hopRadiusTiles) {
                lastHopAttemptMs = now;
                if (attemptWorldHop()) {
                    waitingRespawn.clear();
                }
                return true;
            }
        }
        return false;
    }

    private boolean attemptWorldHop() {
        try {
            Integer currentWorld = getCurrentWorld();
            getProfileManager().forceHop(worlds -> {
                if (worlds == null || worlds.isEmpty()) {
                    return null;
                }
                List<World> filtered = new ArrayList<>();
                for (World world : worlds) {
                    if (world != null &&
                            (currentWorld == null || world.getId() != currentWorld) &&
                            world.isMembers() &&
                            !world.isIgnore() &&
                            (world.getSkillTotal() == null || world.getSkillTotal() == SkillTotal.NONE)) {
                        filtered.add(world);
                    }
                }
                if (filtered.isEmpty()) {
                    for (World world : worlds) {
                        if (world != null &&
                                (currentWorld == null || world.getId() != currentWorld) &&
                                world.isMembers()) {
                            return world;
                        }
                    }
                    return worlds.get(0);
                }
                int idx = Math.max(0, Math.min(filtered.size() - 1, random(0, filtered.size())));
                return filtered.get(idx);
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPlayerMoving() {
        boolean animating = getPixelAnalyzer().isPlayerAnimating(0.4);
        WorldPosition pos = getWorldPosition();
        if (pos != null && !pos.equals(lastMovePosition)) {
            lastMovePosition = pos;
            lastMoveChangeMs = System.currentTimeMillis();
            return true;
        }
        long sinceMove = System.currentTimeMillis() - lastMoveChangeMs;
        return animating || sinceMove < 800;
    }

    public boolean waitForPlayerIdle() {
        com.osmb.api.utils.timing.Timer stationaryTimer = new com.osmb.api.utils.timing.Timer();
        WorldPosition[] lastPosition = { getWorldPosition() };

        BooleanSupplier condition = () -> {
            WorldPosition current = getWorldPosition();
            if (current == null) {
                return false;
            }

            if (lastPosition[0] == null || !current.equals(lastPosition[0])) {
                lastPosition[0] = current;
                stationaryTimer.reset();
            }

            boolean stationary = stationaryTimer.timeElapsed() > 600;
            boolean animating = getPixelAnalyzer().isPlayerAnimating(0.4);
            return stationary && !animating;
        };

        if (RandomUtils.uniformRandom(0, 100) < 20) {
            return pollFramesHuman(condition, 2_000);
        } else {
            boolean result = pollFramesUntil(condition, 2_000);
            pollFramesUntil(() -> false, RandomUtils.uniformRandom(75, 250));
            return result;
        }
    }

    public boolean tapRock(RSObject rock) {
        if (rock == null) {
            return false;
        }
        Polygon hull = getSceneProjector().getConvexHull(rock);
        if (hull == null || hull.numVertices() == 0) {
            return false;
        }
        Polygon shrunk = hull.getResized(0.5);
        Polygon targetHull = shrunk != null ? shrunk : hull;
        BooleanSupplier condition = () -> {
            MenuEntry response = getFinger().tapGetResponse(false, targetHull);
            if (response == null) {
                return false;
            }

            String action = response.getAction();
            String name = response.getEntityName();

            return action != null && name != null &&
                    "mine".equalsIgnoreCase(action) &&
                    SandstoneData.TARGET_ROCK_NAME.equalsIgnoreCase(name);
        };

        if (RandomUtils.uniformRandom(0, 100) < 15) {
            return pollFramesHuman(condition, 1_000);
        } else {
            boolean result = pollFramesUntil(condition, 1_000);
            pollFramesUntil(() -> false, RandomUtils.uniformRandom(75, 250));
            return result;
        }
    }

    public boolean waitForMiningCompletion() {
        final double startingMiningXp = getMiningXp();
        com.osmb.api.utils.timing.Timer noAnimationTimer = new com.osmb.api.utils.timing.Timer();
        com.osmb.api.utils.timing.Timer graceTimer = new com.osmb.api.utils.timing.Timer();

        BooleanSupplier condition = () -> {
            boolean animating = getPixelAnalyzer().isPlayerAnimating(0.4);
            if (animating) {
                noAnimationTimer.reset();
            }

            double currentXp = getMiningXp();
            boolean gainedXp = currentXp > startingMiningXp;
            if (gainedXp) {
                sandstoneMined++;
            }

            ItemGroupResult inventory = getWidgetManager().getInventory().search(Collections.emptySet());
            boolean inventoryFull = inventory != null && inventory.isFull();

            boolean animationStale = !animating && noAnimationTimer.timeElapsed() > 2_000 && graceTimer.timeElapsed() > 1_000;
            return gainedXp || inventoryFull || animationStale;
        };

        if (RandomUtils.uniformRandom(0, 100) < 12) {
            return pollFramesHuman(condition, 6_000);
        } else {
            boolean result = pollFramesUntil(condition, 6_000);
            pollFramesUntil(() -> false, RandomUtils.uniformRandom(75, 250));
            return result;
        }
    }

    private boolean canCastHumidify() {
        try {
            var spellbook = getWidgetManager().getSpellbook();
            if (spellbook == null) {
                return false;
            }

            ItemGroupResult inv = inventorySnapshot != null ? inventorySnapshot : getWidgetManager().getInventory().search(Set.of(
                    ItemID.ASTRAL_RUNE,
                    ItemID.WATER_RUNE,
                    ItemID.FIRE_RUNE
            ));
            if (inv == null) {
                return false;
            }

            int astral = inv.getAmount(ItemID.ASTRAL_RUNE);
            int water = inv.getAmount(ItemID.WATER_RUNE);
            int fire = inv.getAmount(ItemID.FIRE_RUNE);
            return astral >= 1 && water >= 3 && fire >= 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean castHumidify() {
        try {
            Spellbook spellbook = getWidgetManager().getSpellbook();
            if (spellbook == null) {
                return false;
            }
            spellbook.open();
            return spellbook.selectSpell(LunarSpellbook.HUMIDIFY, null);
        } catch (SpellNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean shouldCastHumidify() {
        if (!canCastHumidify()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastHumidifyCastMs < 3_000) {
            return false;
        }
        if (waterskinCharges == null) {
            return false;
        }
        return waterskinCharges == 0;
    }

    public boolean shouldGetWaterskins() {
        WorldPosition myPos = getWorldPosition();
        return waterskinCharges <= 2 || myPos != null && insidePassArea.contains(myPos) || myPos != null && outsidePassArea.contains(myPos);
    }

    public void markHumidifyCast() {
        lastHumidifyCastMs = System.currentTimeMillis();
    }

    public Set<WorldPosition> getWaitingRespawn() {
        return waitingRespawn;
    }

    private double getMiningXp() {
        Map<?, ?> trackers = getXPTrackers();
        if (trackers == null) {
            return 0;
        }
        Object tracker = trackers.get(SkillType.MINING);
        if (tracker instanceof XPTracker xpTracker) {
            return xpTracker.getXp();
        }
        return 0;
    }
}
