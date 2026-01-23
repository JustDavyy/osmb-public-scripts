package main;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.trackers.experience.XPTracker;
import data.FishingLocation;
import data.FishingMethod;
import data.HandlingMode;
import javafx.scene.Scene;
import tasks.*;
import utils.Task;

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
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.script.Script;
import utils.XPTracking;

import javax.imageio.ImageIO;

@ScriptDefinition(
        name = "dAIOFisher",
        description = "AIO Fisher that fishes, banks and/or drops to get those gains!",
        skillCategory = SkillCategory.FISHING,
        version = 4.2,
        author = "JustDavyy"
)
public class dAIOFisher extends Script {
    public static String scriptVersion = "4.2";
    private final String scriptName = "AIOFisher";
    private static String sessionId = UUID.randomUUID().toString();
    private static long lastStatsSent = 0;
    private static final long STATS_INTERVAL_MS = 600_000L;
    public static boolean setupDone = false;
    public static boolean usingBarrel = false;
    public static boolean skipMinnowDelay = false;
    public static boolean useBarehand = false;
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    public static String task = "N/A";
    public static final Stopwatch switchTabTimer = new Stopwatch();

    public static boolean bankMode = false;
    public static boolean dropMode = false;
    public static boolean cookMode = false;
    public static boolean noteMode = false;
    public static boolean cutMode = false;
    public static boolean hasToolEquipped = false;
    public static FishingMethod fishingMethod;
    public static FishingLocation fishingLocation;
    public static HandlingMode handlingMode;
    public static String menuHook;
    public static boolean alreadyCountedFish = false;

    public static double fishingXp = 0;
    public static double cookingXp = 0;
    private double lastXpValue = 0;

    public static long lastXpGained = System.currentTimeMillis() - 20000;

    // Trackers
    private final XPTracking xpTracking;
    public static int fish1Caught = 0;
    public static int fish2Caught = 0;
    public static int fish3Caught = 0;
    public static int fish4Caught = 0;
    public static int fish5Caught = 0;
    public static int fish6Caught = 0;
    public static int fish7Caught = 0;
    public static int fish8Caught = 0;
    public static int startAmount = 0;

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

    // Karambwan merged stuff
    public static final PolyArea fishingArea = new PolyArea(List.of(new WorldPosition(2896, 3119, 0),new WorldPosition(2894, 3118, 0),new WorldPosition(2893, 3116, 0),new WorldPosition(2894, 3115, 0),new WorldPosition(2895, 3114, 0),new WorldPosition(2895, 3113, 0),new WorldPosition(2895, 3112, 0),new WorldPosition(2895, 3110, 0),new WorldPosition(2897, 3109, 0),new WorldPosition(2898, 3108, 0),new WorldPosition(2899, 3107, 0),new WorldPosition(2900, 3106, 0),new WorldPosition(2909, 3106, 0),new WorldPosition(2912, 3108, 0),new WorldPosition(2916, 3111, 0),new WorldPosition(2914, 3115, 0),new WorldPosition(2914, 3116, 0),new WorldPosition(2913, 3117, 0),new WorldPosition(2913, 3118, 0),new WorldPosition(2911, 3118, 0),new WorldPosition(2910, 3117, 0),new WorldPosition(2909, 3116, 0),new WorldPosition(2908, 3115, 0),new WorldPosition(2907, 3115, 0),new WorldPosition(2906, 3116, 0),new WorldPosition(2905, 3117, 0),new WorldPosition(2904, 3118, 0),new WorldPosition(2903, 3119, 0),new WorldPosition(2901, 3119, 0),new WorldPosition(2900, 3119, 0),new WorldPosition(2899, 3118, 0),new WorldPosition(2897, 3119, 0),new WorldPosition(2898, 3118, 0)));
    public static int equippedCloakId = -1;
    public static int teleportCapeId = -1;
    public static String bankOption;
    public static String fairyOption;
    public static boolean doneBanking = false;
    public static double totalXpGained = 0.0;
    public static WorldPosition currentPos;

    // Minnows stuff
    public static SearchableImage minnowTileImageTop;
    public static SearchableImage minnowTileImageBottom;

    // Logo image
    private com.osmb.api.visual.image.Image logoImage = null;

    // Paint stuff
    private static final Font FONT_LABEL       = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_VALUE_BOLD  = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_VALUE_ITALIC= new Font("Arial", Font.ITALIC, 12);

    public static int currentFishingLevel = 1;
    public static int startFishingLevel = 1;
    public static int currentCookingLevel = 1;
    public static int startCookingLevel = 1;

    public static final Map<String, Set<Integer>> TOOL_EQUIVALENTS = Map.of(
            "harpoon", Set.of(ItemID.HARPOON, ItemID.BARBTAIL_HARPOON, ItemID.DRAGON_HARPOON, ItemID.DRAGON_HARPOON_OR, ItemID.DRAGON_HARPOON_OR_30349, ItemID.INFERNAL_HARPOON, ItemID.INFERNAL_HARPOON_UNCHARGED_25367, ItemID.INFERNAL_HARPOON_OR, ItemID.INFERNAL_HARPOON_UNCHARGED, ItemID.INFERNAL_HARPOON_OR_30342, ItemID.INFERNAL_HARPOON_UNCHARGED_30343, ItemID.CRYSTAL_HARPOON, ItemID.CRYSTAL_HARPOON_23864, ItemID.CRYSTAL_HARPOON_INACTIVE),
            "fishingrod", Set.of(ItemID.FISHING_ROD, ItemID.PEARL_FISHING_ROD),
            "oilyfishingrod", Set.of(ItemID.OILY_FISHING_ROD, ItemID.OILY_PEARL_FISHING_ROD),
            "flyfishingrod", Set.of(ItemID.FLY_FISHING_ROD, ItemID.PEARL_FLY_FISHING_ROD),
            "barbarianrod", Set.of(ItemID.BARBARIAN_ROD, ItemID.PEARL_BARBARIAN_ROD)
    );

    public static final Map<String, Set<Integer>> BAIT_EQUIVALENTS = Map.of(
            "sandworm", Set.of(ItemID.SANDWORMS, ItemID.DIABOLIC_WORMS),
            "barbbait", Set.of(ItemID.FISHING_BAIT, ItemID.FEATHER, ItemID.RED_FEATHER, ItemID.YELLOW_FEATHER, ItemID.ORANGE_FEATHER, ItemID.BLUE_FEATHER, ItemID.FISH_OFFCUTS)
    );

    private List<Task> tasks;

    public dAIOFisher(Object scriptCore) {
        super(scriptCore);
        this.xpTracking = new XPTracking(this);
    }

    @Override
    public int[] regionsToPrioritise() {
        if (fishingLocation == null) {
            return new int[0];
        }

        return switch (fishingLocation) {
            case Karambwans -> new int[]{
                    11568, 9541, 11571, 10804, 10290, 10546
            };
            case Barb_Village -> new int[]{
                    12341, 12342
            };
            case Ottos_Grotto -> new int[]{
                    10038, 10039, 9782, 9783
            };
            case Mount_Quidamortem_CoX -> new int[]{
                    4919
            };
            case Karamja_West -> new int[]{
                    11055, 11054, 11311
            };
            case Shilo_Village -> new int[]{
                    11310
            };
            case Lumbridge_Goblins, Lumbridge_Swamp -> new int[]{
                    12850, 12849
            };
            case Mor_Ul_Rek_East, Mor_Ul_Rek_West -> new int[]{
                    10064, 10063, 9808, 9807
            };
            case Zul_Andra -> new int[]{
                    8751, 8752
            };
            case Port_Piscarilius_East, Port_Piscarilius_West -> new int[]{
                    6971, 7227, 7226, 6970
            };
            case Kingstown -> new int[]{
                    6713
            };
            case Farming_Guild -> new int[]{
                    4922, 4921
            };
            case Chaos_Druid_Tower -> new int[]{
                    10292, 10036, 10037
            };
            case Seers_SinclairMansion -> new int[]{
                    10807, 10806
            };
            case Rellekka_MiddlePier, Rellekka_NorthPier, Rellekka_WestPier -> new int[]{
                    10553, 10554
            };
            case Jatizso -> new int[]{
                    9531
            };
            case Lands_End_East, Lands_End_West -> new int[]{
                    5941, 6197
            };
            case Isle_Of_Souls_East, Isle_Of_Souls_North, Isle_Of_Souls_South -> new int[]{
                    8491, 9004, 9006
            };
            case Burgh_de_Rott -> new int[]{
                    13874, 13873
            };
            case Tree_Gnome_Village -> new int[]{
                    9777
            };
            case Piscatoris -> new int[]{
                    9273
            };
            case Prifddinas_North, Prifddinas_South_NorthSide, Prifddinas_South_SouthSide -> new int[]{
                    12896, 13152, 13150, 13149, 8757, 9010
            };
            case Corsair_Cove -> new int[]{
                    10028
            };
            case Myths_Guild -> new int[]{
                    9773
            };
            case Varlamore -> new int[]{
                    6193
            };
            case Entrana_East, Entrana_Middle -> new int[]{
                    11316, 11572
            };
            case Catherby -> new int[]{
                    11317, 11061
            };
            case Fishing_Guild_South, Fishing_Guild_North, Minnows -> new int[]{
                    10293
            };
            case The_Onyx_Crest -> new int[]{
                    11811
            };
            default -> new int[0];
        };
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - startTime;
        double hours = Math.max(1e-9, elapsed / 3_600_000.0);
        String runtime = formatRuntime(elapsed);

        // ---- Totals & rates ----
        int caughtCount   = fish1Caught + fish2Caught + fish3Caught + fish4Caught + fish5Caught + fish6Caught + fish7Caught + fish8Caught;
        int caughtPerHour = (int) Math.round(caughtCount / hours);

        int cookingXpBanked = caughtCount * 190; // Karambwan XP note

        // Formatters
        java.text.DecimalFormat intFmt = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols sy = new java.text.DecimalFormatSymbols();
        sy.setGroupingSeparator('.');
        intFmt.setDecimalFormatSymbols(sy);

        // ===== Panel config =====
        final int x = 5;
        final int baseY = 40;
        final int width = 260;
        final int borderThickness = 2;
        final int paddingX = 10;
        final int topGap = 6;
        final int lineGap = 16;
        final int smallGap = 6;
        final int logoBottomGap = 8;

        final int labelGray   = new Color(180,180,180).getRGB();
        final int valueWhite  = Color.WHITE.getRGB();
        final int valueGreen  = new Color(80, 220, 120).getRGB();
        final int valueBlue   = new Color(70, 130, 180).getRGB();

        ensureLogoLoaded();
        com.osmb.api.visual.image.Image scaledLogo = (logoImage != null) ? logoImage : null;

        // ===== Live Fishing stats from tracker =====
        String fishTTL = "-";
        double fishETL = 0;
        double fishProgressFrac = 0.0;

        XPTracker fishTracker = (xpTracking != null) ? xpTracking.getFishingTracker() : null;
        if (fishTracker != null) {
            double curXp = fishTracker.getXp();
            fishingXp = fishTracker.getXpGained();

            final int MAX = 99;
            int guard = 0;
            while (currentFishingLevel < MAX
                    && curXp >= fishTracker.getExperienceForLevel(currentFishingLevel + 1)
                    && guard++ < 150) {
                currentFishingLevel++;
            }

            int curStart = fishTracker.getExperienceForLevel(currentFishingLevel);
            int nextGoal = fishTracker.getExperienceForLevel(Math.min(MAX, currentFishingLevel + 1));
            int span     = Math.max(1, nextGoal - curStart);

            fishETL = Math.max(0, nextGoal - curXp);
            fishTTL = fishTracker.timeToNextLevelString();
            fishProgressFrac = Math.max(0.0, Math.min(1.0, (curXp - curStart) / (double) span));
        }

        String fishProgressText = (Math.abs(fishProgressFrac * 100.0 - Math.rint(fishProgressFrac * 100.0)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", fishProgressFrac * 100.0)
                : String.format(java.util.Locale.US, "%.1f%%", fishProgressFrac * 100.0);

        // ===== Live Cooking stats from tracker (only if cookMode) =====
        boolean showCook = cookMode;
        String cookTTL = "-";
        double cookETL = 0;
        double cookProgressFrac = 0.0;

        XPTracker cookTracker = (showCook && xpTracking != null) ? xpTracking.getCookingTracker() : null;
        if (cookTracker != null) {
            double curXp = cookTracker.getXp();
            cookingXp = cookTracker.getXpGained();

            final int MAX = 99;
            int guard = 0;
            while (currentCookingLevel < MAX
                    && curXp >= cookTracker.getExperienceForLevel(currentCookingLevel + 1)
                    && guard++ < 150) {
                currentCookingLevel++;
            }

            int curStart = cookTracker.getExperienceForLevel(currentCookingLevel);
            int nextGoal = cookTracker.getExperienceForLevel(Math.min(MAX, currentCookingLevel + 1));
            int span     = Math.max(1, nextGoal - curStart);

            cookETL = Math.max(0, nextGoal - curXp);
            cookTTL = cookTracker.timeToNextLevelString();
            cookProgressFrac = Math.max(0.0, Math.min(1.0, (curXp - curStart) / (double) span));
        }

        int fishingXpPerHour = (int) Math.round(fishingXp / hours);
        int cookingXpPerHour = (int) Math.round(cookingXp / hours);

        String cookProgressText = (Math.abs(cookProgressFrac * 100.0 - Math.rint(cookProgressFrac * 100.0)) < 1e-9)
                ? String.format(java.util.Locale.US, "%.0f%%", cookProgressFrac * 100.0)
                : String.format(java.util.Locale.US, "%.1f%%", cookProgressFrac * 100.0);

        // ===== Dynamic height =====
        boolean isMinnows = (fishingLocation == FishingLocation.Minnows);
        boolean isKaramb  = (fishingLocation == FishingLocation.Karambwans);

        int totalLines = 1 + 2; // Runtime + Catches
        if (isMinnows) totalLines += 2; // Sharks
        totalLines += 6; // Fishing XP, XP/hr, progress, level, TTL, ETL
        if (isKaramb) totalLines += 3; // Karamb extras
        if (showCook) totalLines += 6; // Cooking XP, XP/hr, progress, level, TTL, ETL
        totalLines += 4; // Footer

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

        // ===== Lines =====
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Runtime", runtime, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Catches", intFmt.format(caughtCount), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Catches/hr", intFmt.format(caughtPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        if (isMinnows) {
            int sharks = caughtCount / 40;
            int sharksPerHr = caughtPerHour / 40;
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Sharks", intFmt.format(sharks), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Sharks/hr", intFmt.format(sharksPerHr), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);
        }

        // --- Fishing XP + Stats ---
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing XP", intFmt.format(fishingXp), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing XP/hr", intFmt.format(fishingXpPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing Progress", fishProgressText, labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing Level", (startFishingLevel > 0 && currentFishingLevel > startFishingLevel)
                        ? (currentFishingLevel + " (+" + (currentFishingLevel - startFishingLevel) + ")")
                        : String.valueOf(currentFishingLevel),
                labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing TTL", fishTTL, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Fishing ETL", intFmt.format(Math.round(fishETL)), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        // Karamb extras
        if (isKaramb) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking XP banked", intFmt.format(cookingXpBanked), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Bank method", String.valueOf(bankOption), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Travel method", String.valueOf(fairyOption), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        }

        // --- Cooking XP + Stats ---
        if (showCook) {
            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking XP", intFmt.format(cookingXp), labelGray, valueBlue, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking XP/hr", intFmt.format(cookingXpPerHour), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking Progress", cookProgressText, labelGray, valueGreen, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking Level", (startCookingLevel > 0 && currentCookingLevel > startCookingLevel)
                            ? (currentCookingLevel + " (+" + (currentCookingLevel - startCookingLevel) + ")")
                            : String.valueOf(currentCookingLevel),
                    labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking TTL", cookTTL, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

            curY += lineGap;
            drawStatLine(c, innerX, innerWidth, paddingX, curY,
                    "Cooking ETL", intFmt.format(Math.round(cookETL)), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);
        }

        // --- Footer ---
        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Task", String.valueOf(task), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Location", fishingLocation.name() + " (" + fishingMethod.getMenuEntry() + ")", labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Handling mode", handlingMode.name(), labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        curY += lineGap;
        drawStatLine(c, innerX, innerWidth, paddingX, curY,
                "Version", scriptVersion, labelGray, valueWhite, FONT_VALUE_BOLD, FONT_LABEL);

        // --- Track XP gain timestamp ---
        double totalXpNow = fishingXp + cookingXp;
        if (totalXpNow > lastXpValue) {
            lastXpValue = totalXpNow;
            lastXpGained = System.currentTimeMillis();
        }

        try { lastCanvasFrame.set(c.toImageCopy()); } catch (Exception ignored) {}
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
        log(getClass().getSimpleName(), "Starting dAIOFisher v" + scriptVersion);

        ScriptUI ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dAIOFisher Options", false);

        bankMode = ui.getSelectedHandlingMethod().equals(HandlingMode.BANK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnBANK);
        dropMode = ui.getSelectedHandlingMethod().equals(HandlingMode.DROP) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOK);
        cookMode = ui.getSelectedHandlingMethod().equals(HandlingMode.COOK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnBANK) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnNOTE);
        noteMode = ui.getSelectedHandlingMethod().equals(HandlingMode.NOTE) || ui.getSelectedHandlingMethod().equals(HandlingMode.COOKnNOTE);
        cutMode = ui.getSelectedHandlingMethod().equals(HandlingMode.CUT);
        fishingMethod = ui.getSelectedMethod();
        fishingLocation = ui.getSelectedLocation();
        menuHook = fishingMethod.getMenuEntry();
        handlingMode = ui.getSelectedHandlingMethod();
        skipMinnowDelay = ui.isSkippingMinnowDelay();
        useBarehand = ui.isUseBarehanded();

        if (fishingLocation.equals(FishingLocation.Karambwans)) {
            KarambwanUI wambamUI = new KarambwanUI(this);
            Scene wambamScene = wambamUI.buildScene(this);
            getStageController().show(wambamScene, "Karambwan Options", false);

            bankOption = wambamUI.getSelectedBankingOption();
            fairyOption = wambamUI.getSelectedFairyRingOption();

            log(getClass(), "Bank option: " + bankOption + " | Fairy ring option: " + fairyOption);
        }

        if (fishingLocation.equals(FishingLocation.Minnows)) {
            SearchableImage[] itemImages = getItemManager().getItem(ItemID.MINNOW, true);
            minnowTileImageTop = itemImages[itemImages.length - 1];
            minnowTileImageBottom = new SearchableImage(minnowTileImageTop.copy(), minnowTileImageTop.getToleranceComparator(), minnowTileImageTop.getColorModel());
            makeHalfTransparent(minnowTileImageTop, true);
            makeHalfTransparent(minnowTileImageBottom, false);
        }

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            queueSendWebhook();
        }

        if (checkForUpdates()) {
            stop();
            return;
        }

        List<Task> taskList = new ArrayList<>();

        taskList.add(new Setup(this));
        if (fishingLocation == FishingLocation.Karambwans) {
            taskList.add(new dkTravel(this));
            taskList.add(new dkFish(this));
            taskList.add(new dkBank(this));
        } else if (fishingLocation == FishingLocation.Minnows) {
            taskList.add(new dmFish(this));
        } //else if (fishingLocation == FishingLocation.Wilderness_Resource_Area) {
         //   taskList.add(new dCrabs(this));
        //}
        else {
            taskList.add(new Travel(this));
            taskList.add(new Fish(this));
            taskList.add(new Cut(this));
            taskList.add(new Cook(this));
            taskList.add(new Drop(this));
            taskList.add(new Bank(this));
            taskList.add(new FailSafe(this));
        }

        // Build a readable list of task class names
        List<String> taskNames = taskList.stream()
                .map(task -> task.getClass().getSimpleName())
                .collect(Collectors.toList());

        log(getClass(), "Loaded " + taskList.size() + " task(s) for location: " + fishingLocation +
                " -> " + String.join(", ", taskNames));

        tasks = taskList;
    }

    @Override
    public int poll() {
        if (webhookEnabled && webhookTimer.hasFinished()) {
            queueSendWebhook();
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastStatsSent >= STATS_INTERVAL_MS) {
            long elapsed = nowMs - startTime;
            sendStats(0, (long) (fishingXp + cookingXp), elapsed);
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

    private boolean checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dAIOFisher/src/main/java/main/dAIOFisher.java");

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
        return 0;
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

    private void makeHalfTransparent(SearchableImage image, boolean topHalf) {
        int startY = topHalf ? 0 : image.getHeight() / 2;
        int endY = topHalf ? image.getHeight() / 2 : image.getHeight();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = startY; y < endY; y++) {
                image.setRGB(x, y, ColorUtils.TRANSPARENT_PIXEL);
            }
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
