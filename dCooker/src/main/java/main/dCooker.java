package main;

import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import data.CookingItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import tasks.BankTask;
import tasks.ProcessTask;
import tasks.Setup;
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
import java.util.function.Predicate;

@ScriptDefinition(
        name = "dCooker",
        description = "Cooks a wide variety of fish and other items at cookable objects.",
        skillCategory = SkillCategory.COOKING,
        version = 3.1,
        author = "JustDavyy"
)
public class dCooker extends Script {
    public static String scriptVersion = "3.1";
    private final String scriptName = "Cooker";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use", "bank banker"};
    public static final String[] COOKING_ACTIONS = {"cook"};
    public static final Predicate<RSObject> bankQuery = gameObject -> {
        if (gameObject.getName() == null || gameObject.getActions() == null) return false;
        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) return false;
        return Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))
                && gameObject.canReach();
    };
    public static long startTime = System.currentTimeMillis();
    public static String task = "N/A";
    public static int cookCount = 0;
    public static int burnCount = 0;
    public static int totalCookCount = 0;
    private int xpGained = 0;
    public static boolean setupDone = false;
    public static int cookingItemID;
    public static int cookedItemID;
    public static String bankMethod;
    private List<Task> tasks;

    // Webhook stuff
    private static final Stopwatch webhookTimer = new Stopwatch();
    private static String webhookUrl = "";
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static int webhookIntervalMinutes = 5;
    private static String user = "";
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://www.osmb.co.uk/lovable-uploads/ad86059b-ce19-4540-8e53-9fd01c61c98b.png";
    private volatile long nextWebhookEarliestMs = 0L;
    private static long lastWebhookSent = 0;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();

    public static boolean isMultipleMode = false;
    public static ObservableList<Integer> selectedItemIDs = FXCollections.observableArrayList();
    public static int selectedItemIndex = 0;

    public static double levelProgressFraction = 0.0;
    public static int currentLevel = 1;
    public static int startLevel = 1;

    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    public dCooker(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    // Override regions to prioritise to prevent or limit global searches
    @Override
    public int[] regionsToPrioritise() {
        return new int[]{
                12109, // Rogues den
                11061, // Catherby
                11317, // Catherby2
                6712,  // Hosidius Kitchen
                13105, // Al-Kharid
                9772,  // Myth's Guild
                12597, // Cook's Guild
                12588, // Ruins of Unkah
                13613, // Nardah
                14907, // Museum Camp
                9275,  // Neitiznot
                12895, // Priff NW
                13151, // Priff NE
                9541,  // Zanaris
                12084, // Falador East
        };
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        // ==== Totals & rates ====
        int totalCooked  = cookCount + burnCount;
        int totalPerHour = (int) Math.round(totalCooked / hours);
        int xpPerHourLive;
        int xpGainedLiveInt;

        // Success rate
        String rate = (totalCooked > 0)
                ? String.format("%d%%", (int) Math.floor((cookCount * 100.0) / totalCooked))
                : "N/A";

        // ==== Cooking XP live via tracker ====
        String ttlText = "-";
        double etl = 0.0;
        double xpGainedLive = 0.0;
        double currentXp = 0.0;
        double levelProgressFractionCook = 0.0;

        XPTracker tracker = (xpTracking != null) ? xpTracking.getCookingTracker() : null;
        if (tracker != null) {
            xpGainedLive = tracker.getXpGained();
            currentXp = tracker.getXp();

            // --- Level sync (only increases)
            final int MAX_LEVEL = 99;
            int guard = 0;
            while (currentLevel < MAX_LEVEL
                    && currentXp >= tracker.getExperienceForLevel(currentLevel + 1)
                    && guard++ < 10) {
                currentLevel++;
            }

            ttlText = tracker.timeToNextLevelString();

            int curLevelXpStart   = tracker.getExperienceForLevel(currentLevel);
            int nextLevelXpTarget = tracker.getExperienceForLevel(Math.min(MAX_LEVEL, currentLevel + 1));
            int span              = Math.max(1, nextLevelXpTarget - curLevelXpStart);

            etl = Math.max(0, nextLevelXpTarget - currentXp);

            levelProgressFractionCook = Math.max(0.0, Math.min(1.0,
                    (currentXp - curLevelXpStart) / (double) span));
        }

        xpPerHourLive = (int) Math.round(xpGainedLive / hours);
        xpGainedLiveInt = (int) Math.round(xpGainedLive);
        xpGained = xpGainedLiveInt;

        // --- Level text (+N)
        if (startLevel <= 0) startLevel = currentLevel;
        int levelsGained = Math.max(0, currentLevel - startLevel);
        String currentLevelText = (levelsGained > 0)
                ? (currentLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentLevel);

        // --- Level progress percentage
        double pct = Math.max(0, Math.min(100, levelProgressFractionCook * 100.0));
        String levelProgressText = (Math.abs(pct - Math.rint(pct)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", pct)
                : String.format(java.util.Locale.US, "%.1f%%", pct);

        // === Formatting with dots ===
        java.text.DecimalFormat intFmt = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        intFmt.setDecimalFormatSymbols(sym);

        // === Panel + layout (standardized) ===
        final int x = 5;
        final int baseY = 40;
        final int width = 225;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 6;
        final int logoBottomGap = 8;

        final int labelGray  = new Color(180,180,180).getRGB();
        final int valueWhite = Color.WHITE.getRGB();
        final int valueGreen = new Color(80, 220, 120).getRGB(); // success/progress
        final int valueBlue  = new Color(70, 130, 180).getRGB(); // general rate
        final int valueRed   = new Color(255, 0, 0).getRGB();    // burns

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;

        int baseLines = 13;
        int extra = isMultipleMode ? 2 : 0;
        int totalLines = baseLines + extra;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap + smallGap + 10;

        int innerHeight = Math.max(240, y - innerY);

        // --- Panel
        c.fillRect(innerX - borderThickness, innerY - borderThickness,
                innerWidth + (borderThickness * 2),
                innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.decode("#01031C").getRGB(), 1);
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        int curY = innerY + topGap;

        // --- Logo (optional)
        if (scaledLogo != null) {
            int imgX = innerX + (innerWidth - scaledLogo.width) / 2;
            c.drawAtOn(scaledLogo, imgX, curY);
            curY += scaledLogo.height + logoBottomGap;
        }

        // === Draw stat lines ===
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime,
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Cooked", intFmt.format(cookCount),
                labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Burned", intFmt.format(burnCount),
                labelGray, valueRed, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Cook rate", rate,
                labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Total/hr", intFmt.format(totalPerHour),
                labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP gained", intFmt.format(xpGainedLiveInt),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP/hr", intFmt.format(xpPerHourLive),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "ETL", intFmt.format(Math.round(etl)),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "TTL", ttlText,
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Level progress", levelProgressText,
                labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Current level", currentLevelText,
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        // Optional multiple-mode section
        if (isMultipleMode) {
            int remaining = Math.max(0, selectedItemIDs.size() - selectedItemIndex - 1);
            String nextFishName = "N/A";
            if (selectedItemIndex + 1 < selectedItemIDs.size()) {
                Integer nextId = selectedItemIDs.get(selectedItemIndex + 1);
                try {
                    nextFishName = getItemManager().getItemName(nextId) + " (" + nextId + ")";
                } catch (Exception ignored) {
                    nextFishName = String.valueOf(nextId);
                }
            }

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY, "Next fish", nextFishName,
                    labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY, "Types left",
                    String.valueOf(remaining), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        }

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Task", String.valueOf(task),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Version", scriptVersion,
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        // Snapshot for webhook
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
    public void onStart() {
        log(getClass().getSimpleName(), "Starting dCooker v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Cooking Options", false);

        // Load webhook options
        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }

        isMultipleMode = ui.isMultipleSelectionMode();
        selectedItemIDs = ui.getMultipleSelectedItemIds(); // ObservableList<Integer>
        selectedItemIndex = 0;

        if (isMultipleMode && !selectedItemIDs.isEmpty()) {
            cookingItemID = selectedItemIDs.get(0);
            assert CookingItem.fromRawItemId(cookingItemID) != null;
            cookedItemID = CookingItem.fromRawItemId(cookingItemID).getCookedItemId();
        } else {
            cookingItemID = ui.getSelectedItemId();
            cookedItemID = ui.getSelectedCookedItemId();
        }

        bankMethod = ui.getSelectedBankMethod();

        log(getClass().getSimpleName(), "We're cooking: " + getItemManager().getItemName(cookingItemID));
        log(getClass().getSimpleName(), "Banking method: " + bankMethod);

        String version = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCooker/src/main/java/main/dCooker.java");

        if (version == null) {
            log("SCRIPTVERSION", "⚠ Could not fetch the latest version from GitHub. Proceeding with local version: v" + scriptVersion);
        } else {
            int comparison = compareVersions(scriptVersion, version);
            if (comparison == 0) {
                log("SCRIPTVERSION", "✅ You are running the latest script version: v" + scriptVersion);
            } else if (comparison < 0) {
                log("VERSIONERROR", "❌ You are NOT running the latest version of this script!\nYour version: v" + scriptVersion + "\nLatest version: v" + version);

                try {
                    String userHome = System.getProperty("user.home");
                    File scriptsDir = new File(userHome + File.separator + ".osmb" + File.separator + "Scripts");

                    // Delete old versions
                    File[] oldFiles = scriptsDir.listFiles((dir, name) ->
                            name.equals("dCooker.jar") || name.matches("dCooker-\\d+(\\.\\d+)+\\.jar"));
                    if (oldFiles != null) {
                        for (File f : oldFiles) f.delete();
                    }

                    String downloadUrl = "https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dCooker/jar/dCooker.jar";
                    File newFile = new File(scriptsDir, "dCooker-" + version + ".jar");

                    try (InputStream in = new URL(downloadUrl).openStream();
                         FileOutputStream out = new FileOutputStream(newFile)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                    }

                    log("SCRIPTDOWNLOAD", "✅ Downloaded new version to: " + newFile.getAbsolutePath());
                    stop();
                    return;

                } catch (Exception e) {
                    log("SCRIPTDOWNLOAD", "❌ Failed to update script: " + e.getMessage());
                }

            } else {
                log("SCRIPTVERSION", "✅ You are running a newer version than GitHub: v" + scriptVersion);
            }
        }

        tasks = Arrays.asList(
                new Setup(this),
                new ProcessTask(this),
                new BankTask(this)
        );
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats(0, xpGained, elapsed);
            lastStatsSent = nowMs;
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

    // Script version checks
    public String getLatestVersion(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("version")) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String version = parts[1].replace(",", "").trim();
                            return version;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log("VERSIONCHECK", "Exception occurred while fetching version from GitHub.");
        }

        return null;
    }

    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        return 0; // Equal
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
