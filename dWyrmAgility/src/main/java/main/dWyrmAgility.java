package main;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.Chatbox;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import javafx.scene.Scene;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@ScriptDefinition(
name = "dWyrmAgility",
threadUrl = "https://wiki.osmb.co.uk/article/dwyrmagility",
        author = "JustDavyy",
        version = 2.8,
        skillCategory = SkillCategory.AGILITY
)
public class dWyrmAgility extends Script {
    public static final String scriptVersion = "2.8";
    private final String scriptName = "WyrmAgility";
    private static final String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    private Course selectedCourse;
    private int nextRunActivate;
    public int noMovementTimeout = RandomUtils.weightedRandom(6000, 9000);
    public static double xpGained = 0;
    public static int lapCount = 0;
    public static int termitesGained = 0;
    public static int shardsGained = 0;
    private final long startTime = System.currentTimeMillis();

    // Webhook config
    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";
    private final AtomicBoolean webhookInFlight = new AtomicBoolean(false);
    final String authorIconUrl = "https://wiki.osmb.co.uk/assets/logo-Dq53Rvcx.gif";
    private volatile long nextWebhookEarliestMs = 0L;
    private final AtomicReference<Image> lastCanvasFrame = new AtomicReference<>();

    public static int currentLevel = 1;
    public static int startLevel = 1;
    public static boolean levelChecked = false;

    public static String task = "Initialize";
    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

    private static final java.util.regex.Pattern TERMITES_PAT =
            java.util.regex.Pattern.compile("\\bmanaged to scoop up\\s+(\\d{1,2})\\s+termites\\b",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern SHARDS_PAT =
            java.util.regex.Pattern.compile("\\balso find\\s+(\\d{1,2})\\s+piles? of bone shards(?:\\s+they\\s+chipped\\s+off)?\\b",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private final XPTracking xpTracking;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    private int failThreshold = RandomUtils.uniformRandom(4, 6);
    private int failCount = 0;

    public dWyrmAgility(Object object) {
        super(object);
        this.xpTracking = new XPTracking(this);
    }

    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, 1, timeout);
    }

    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, true, timeout);
    }

    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, canReach, timeout, null);
    }

    public static ObstacleHandleResponse handleObstacle(dWyrmAgility core, String obstacleName, String menuOption, Object end, int interactDistance, boolean canReach, int timeout, WorldPosition objectBaseTile) {
        Optional<RSObject> result = core.getObjectManager().getObject(gameObject -> {

            if (gameObject.getName() == null || gameObject.getActions() == null) return false;

            if (!gameObject.getName().equalsIgnoreCase(obstacleName)) {
                return false;
            }

            if (objectBaseTile != null) {
                if (!objectBaseTile.equals(gameObject.getWorldPosition())) {
                    return false;
                }
            }
            if (!canReach) {
                return true;
            }

            return gameObject.canReach(interactDistance);
        });
        if (result.isEmpty()) {
            core.log("Main", "ERROR: Obstacle (" + obstacleName + ") does not exist with criteria.");
            return ObstacleHandleResponse.OBJECT_NOT_IN_SCENE;
        }
        RSObject object = result.get();

        boolean interacted = false;
        boolean preferShortTap = RandomUtils.uniformRandom(1, 100) <= 85;

        core.log("Main", preferShortTap
                ? "Interaction mode: SHORT TAP (preferred)"
                : "Interaction mode: LONG TAP");

        if (preferShortTap) {
            Polygon tapPoly = object.getConvexHull() != null
                    ? object.getConvexHull().getResized(0.6)
                    : null;

            if (tapPoly != null) {
                double visibleFactor = core.getWidgetManager().insideGameScreenFactor(
                        tapPoly,
                        List.of(ChatboxComponent.class)
                );

                if (visibleFactor >= 1.0) {
                    MenuEntry response = core.getFinger().tapGetResponse(true, tapPoly);

                    if (response != null && response.getAction() != null
                            && response.getAction().equalsIgnoreCase(menuOption)) {

                        core.log("Main", String.format(
                                "TapGetResponse → entity='%s' action='%s' type=%s",
                                response.getEntityName(),
                                response.getAction(),
                                response.getEntityType()
                        ));
                        interacted = true;
                    } else {
                        core.log("Main", "TapGetResponse mismatch or null → falling back to interact()");
                        interacted = object.interact(menuOption);
                    }
                } else {
                    core.log("Main",
                            String.format("Tap poly only %.2f visible → using interact()", visibleFactor));
                    interacted = object.interact(menuOption);
                }
            } else {
                interacted = object.interact(menuOption);
            }
        } else {
            interacted = object.interact(menuOption);
        }

        if (interacted) {
            core.log("Main", "Interacted successfully, sleeping until conditions are met...");

            Timer noMovementTimer = new Timer();
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>();

            BooleanSupplier condition = () -> {
                WorldPosition currentPos = core.getWorldPosition();
                if (currentPos == null) {
                    return false;
                }

                // check for being stood still
                if (previousPosition.get() != null) {
                    if (currentPos.equals(previousPosition.get())) {
                        if (noMovementTimer.timeElapsed() > core.noMovementTimeout) {
                            core.noMovementTimeout = RandomUtils.weightedRandom(2500, 4000);
                            core.printFail();
                            core.failCount++;
                            return true;
                        }
                    } else {
                        noMovementTimer.reset();
                    }
                } else {
                    noMovementTimer.reset();
                }
                previousPosition.set(currentPos);

                RSTile tile = core.getSceneManager().getTile(currentPos);
                if (tile != null) {
                    Polygon poly = tile.getTileCube(120);
                    if (core.getPixelAnalyzer().isAnimating(0.1, poly)) {
                        return false;
                    }
                }

                if (end instanceof Area area) {
                    if (area.contains(currentPos)) {
                        core.failThreshold = RandomUtils.uniformRandom(2, 3);
                        return true;
                    }
                } else if (end instanceof Position pos) {
                    if (currentPos.equals(pos)) {
                        core.failThreshold = RandomUtils.uniformRandom(2, 3);
                        return true;
                    }
                }

                return false;
            };

            boolean useHumanPoll = RandomUtils.uniformRandom(1, 100) <= 30;
            boolean delayAfter = RandomUtils.uniformRandom(0, 10) > 8;

            core.log("Main", useHumanPoll
                    ? "Polling mode: HUMAN (30%)"
                    : "Polling mode: FAST (70%)");

            boolean completed = useHumanPoll
                    ? core.pollFramesHuman(condition, timeout)
                    : core.pollFramesUntil(condition, timeout, true, delayAfter);

            if (completed) {
                return ObstacleHandleResponse.SUCCESS;
            } else {
                core.failCount++;
                core.printFail();
                return ObstacleHandleResponse.TIMEOUT;
            }
        } else {
            core.log("Main", "ERROR: Failed interacting with obstacle (" + obstacleName + ").");
            core.failCount++;
            return ObstacleHandleResponse.FAILED_INTERACTION;
        }
    }

    private void printFail() {
        log("Main", "Failed to handle obstacle. Fail count: " + failCount + "/" + failThreshold);
    }

    @Override
    public void onStart() {

        if (checkForUpdates()) {
            stop();
            return;
        }

        UI ui = new UI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dWyrmAgility Settings", false);

        this.selectedCourse = ui.selectedCourse();
        this.nextRunActivate = RandomUtils.uniformRandom(30, 70);

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }
    }

    @Override
    public void onRelog() {
        failCount = 0;
    }

    @Override
    public int poll() {
        if (failCount > failThreshold) {
            log("ERROR", "Failed object multiple times. Relogging.");
            getWidgetManager().getLogoutTab().logout();
            return 0;
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats((termitesGained * 835L), (long) xpGained, elapsed);
            lastStatsSent = nowMs;
        }

        monitorChatbox();

        if (!levelChecked) {
            // Check agility level
            task = "Get agility level";
            SkillsTabComponent.SkillLevel agilitySkillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.AGILITY);
            if (agilitySkillLevel == null) {
                log("Main", "Failed to get skill levels.");
                return 0;
            }
            startLevel = agilitySkillLevel.getLevel();
            currentLevel = agilitySkillLevel.getLevel();
            levelChecked = true;
        }

        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            queueSendWebhook();
        }

        Boolean runEnabled = getWidgetManager().getMinimapOrbs().isRunEnabled();
        if (!runEnabled) {
            int runEnergy = getWidgetManager().getMinimapOrbs().getRunEnergy();
            if (runEnergy > nextRunActivate) {
                log("RUN", "Enabling run");
                getWidgetManager().getMinimapOrbs().setRun(true);
                nextRunActivate = RandomUtils.uniformRandom(30, 70);
            }
        }

        WorldPosition pos = getWorldPosition();
        if (pos == null) return 0;
        return selectedCourse.poll(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        if (selectedCourse == null) {
            return new int[0];
        }
        return selectedCourse.regions();
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        // ==== Laps/hr ====
        int lapsPerHour = (int) Math.round(lapCount / hours);

        // ==== Termites / Shards ====
        int termitesPerHour = (int) Math.round(termitesGained / hours);
        int shardsPerHour   = (int) Math.round(shardsGained / hours);

        // ==== Live XP via built-in Agility tracker ====
        String ttlText = "-";
        double etl = 0.0;
        double xpGainedLive = 0.0;
        double currentXp = 0.0;
        double levelProgressFraction = 0.0;

        if (xpTracking != null) {
            XPTracker tracker = xpTracking.getAgilityTracker();
            if (tracker != null) {
                xpGainedLive = tracker.getXpGained();
                currentXp = tracker.getXp();
                ttlText = tracker.timeToNextLevelString();
                etl = tracker.getXpForNextLevel();

                final int MAX_LEVEL = 99;
                int guard = 0;
                while (currentLevel < MAX_LEVEL
                        && currentXp >= tracker.getExperienceForLevel(currentLevel + 1)
                        && guard++ < 10) {
                    currentLevel++;
                }

                int curLevelXpStart = tracker.getExperienceForLevel(currentLevel);
                int nextLevelXpTarget = tracker.getExperienceForLevel(Math.min(MAX_LEVEL, currentLevel + 1));
                int span = Math.max(1, nextLevelXpTarget - curLevelXpStart);

                levelProgressFraction = Math.max(0.0, Math.min(1.0,
                        (currentXp - curLevelXpStart) / (double) span));
            }
        }

        int xpPerHour = (int) Math.round(xpGainedLive / hours);
        xpGained = (int) Math.round(xpGainedLive);

        // Current level text with (+N)
        if (startLevel <= 0) startLevel = currentLevel;
        int levelsGained = Math.max(0, currentLevel - startLevel);
        String currentLevelText = (levelsGained > 0)
                ? (currentLevel + " (+" + levelsGained + ")")
                : String.valueOf(currentLevel);

        // Percent text
        double pct = Math.max(0, Math.min(100, levelProgressFraction * 100.0));
        String levelProgressText = (Math.abs(pct - Math.rint(pct)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", pct)
                : String.format(java.util.Locale.US, "%.1f%%", pct);

        // === Formatting with dots ===
        java.text.DecimalFormat intFmt = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        intFmt.setDecimalFormatSymbols(sym);

        // === Panel + layout ===
        final int x = 5;
        final int baseY = 40;
        final int width = 225;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int logoBottomGap = 8;

        final int labelGray   = new Color(180,180,180).getRGB();
        final int valueWhite  = Color.WHITE.getRGB();
        final int valueGreen  = new Color(80, 220, 120).getRGB();
        final int valueBlue   = new Color(70, 130, 180).getRGB();

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        int innerX = x;
        int innerY = baseY;
        int innerWidth = width;
        int totalLines = 16;

        int y = innerY + topGap;
        if (scaledLogo != null) y += scaledLogo.height + logoBottomGap;
        y += totalLines * lineGap + 10;
        int innerHeight = Math.max(230, y - innerY);

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

        // === Stats ===
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Runtime", runtime, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP gained", intFmt.format(xpGained), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "XP/hr", intFmt.format(xpPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Termites gained", intFmt.format(termitesGained), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Termites/hr", intFmt.format(termitesPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Shards gained", intFmt.format(shardsGained), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Shards/hr", intFmt.format(shardsPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Laps done", intFmt.format(lapCount), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Laps/hr", intFmt.format(lapsPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "ETL", intFmt.format(Math.round(etl)), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "TTL", ttlText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Level progress", levelProgressText, labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Current level", currentLevelText, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Task", String.valueOf(task), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Course", selectedCourse.displayName(), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY, "Version", scriptVersion, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        // Store canvas for webhook usage
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
                log("Main", "Logo '/logo.png' not found on classpath.");
                return;
            }

            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                log("Main", "Failed to decode logo.png");
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
            log("Main", "Logo loaded: " + w + "x" + h + " premultiplied=" + PREMULTIPLY);

        } catch (Exception e) {
            log("Main", "Error loading logo: " + e.getMessage());
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
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dWyrmAgility/src/main/java/main/dWyrmAgility.java");

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

    private void monitorChatbox() {
        // Make sure game filter tab is selected
        Chatbox chatbox = getWidgetManager().getChatbox();
        if (chatbox != null) {
            try {
                if (chatbox.getActiveFilterTab() != ChatboxFilterTab.GAME) {
                    if (!chatbox.openFilterTab(ChatboxFilterTab.GAME)) {
                        log("Main", "Failed to open chatbox tab (maybe not visible yet).");
                    }
                    return;
                }
            } catch (NullPointerException e) {
                log("Main", "Chatbox not ready for openFilterTab yet, skipping this tick.");
                return;
            }
        }

        UIResultList<String> chatResult = getWidgetManager().getChatbox().getText();
        if (!chatResult.isFound() || chatResult.isEmpty()) {
            return;
        }

        java.util.List<String> currentLines = chatResult.asList();
        if (currentLines.isEmpty()) return;

        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return;
            }

            int currSize = currentLines.size();
            int prevSize = PREVIOUS_CHATBOX_LINES.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;

                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(PREVIOUS_CHATBOX_LINES.get(j))) {
                        match = false;
                        break;
                    }
                }

                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        }

        java.util.List<String> newMessages = currentLines.subList(0, firstDifference);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);

        processNewChatboxMessages(newMessages);
    }

    private void processNewChatboxMessages(List<String> newLines) {
        if (newLines == null || newLines.isEmpty()) return;

        for (String raw : newLines) {
            if (raw == null || raw.isEmpty()) continue;

            // 1) Termites: "You managed to scoop up xx termites!"
            java.util.regex.Matcher mt = TERMITES_PAT.matcher(raw);
            if (mt.find()) {
                int n = safeParseInt(mt.group(1));
                if (n >= 1 && n <= 99) {
                    termitesGained += n;
                }
                continue;
            }

            // 2) Bone shards: "You also find xx piles of bone shards they chipped off"
            java.util.regex.Matcher ms = SHARDS_PAT.matcher(raw);
            if (ms.find()) {
                int n = safeParseInt(ms.group(1));
                if (n >= 1 && n <= 99) {
                    shardsGained += n;
                }
            }
        }
    }

    private static int safeParseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
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
