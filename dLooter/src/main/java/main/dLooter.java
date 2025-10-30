package main;

import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.visual.drawing.Canvas;
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
import java.util.*;
import java.util.List;

@ScriptDefinition(
        name = "dLooter",
        description = "Loots WT/GOTR/Tempoross, seed packs and cwars supply crates",
        skillCategory = SkillCategory.OTHER,
        version = 1.1,
        author = "JustDavyy"
)
public class dLooter extends Script {
    public static final String scriptVersion = "1.1";
    public static boolean setupDone = false;
    public static boolean needToStop = false;
    public static boolean RNGEnabled = false;
    public static String location = "N/A";
    public static int lootsLeft = 0;

    public static Map<Integer, Integer> totalGained = new HashMap<>();
    public static Map<Integer, Integer> lastSeenInventory = new HashMap<>();
    public static boolean inventoryProcessedThisCycle = true;

    public static String task = "Initialize";
    public static long startTime = System.currentTimeMillis();
    private static final Font ARIAL        = new Font("Arial", Font.PLAIN, 14);
    private static final Font ARIAL_BOLD   = new Font("Arial", Font.BOLD, 14);
    private static final Font ARIAL_ITALIC = new Font("Arial", Font.ITALIC, 14);

    private static boolean webhookEnabled = false;
    private static boolean webhookShowUser = false;
    private static boolean webhookShowStats = false;
    private static String webhookUrl = "";
    private static int webhookIntervalMinutes = 5;
    private static long lastWebhookSent = 0;
    private static String user = "";

    // RNG meme spammer state
    private long lastRngSent = 0L;
    private long rngNextIntervalMs = 0L;
    private final java.util.Random rng = new java.util.Random();

    private List<Task> tasks;
    private ScriptUI ui;

    public dLooter(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{

                // GOTR
                14484, 14483,

                // WT
                6461,

                // Tempoross
                12588,

                // Castle wars
                9776,

                // Banks
                13104, // Shantay Pass
                13105, // Al Kharid
                13363, // Duel Arena / PvP Arena
                12850, // Lumbridge Castle
                12338, // Draynor
                12853, // Varrock East
                12597, // Varrock West + Cooks Guild
                12598, // Grand Exchange
                12342, // Edgeville
                12084, // Falador East + Mining GUild
                11828, // Falador West
                11571, // Crafting Guild
                11319, // Warriors Guild
                11061, // Catherby
                10806, // Seers
                11310, // Shilo
                10284, // Corsair Cove
                9772,  // Myths Guild
                10288, // Yanille
                10545, // Port Khazard
                10547, // Ardougne East/South
                10292, // Ardougne East/North
                10293, // Fishing Guild
                10039, // Barbarian Assault
                9782,  // Grand Tree
                9781,  // Tree Gnome Stronghold
                9265,  // Lletya
                8748,  // Soul Wars
                8253,  // Lunar Isle
                9275,  // Neitiznot
                9531,  // Jatiszo
                6461,  // Wintertodt
                7227,  // Port Piscarilius
                6458,  // Arceeus
                6457,  // Kourend Castle
                6968,  // Hosidius
                7223,  // Vinery
                6710,  // Sand Crabs Chest
                6198,  // Woodcutting Guild
                5941,  // Land's End
                5944,  // Shayzien
                5946,  // Lovakengj South
                5691,  // Lovekengj North
                4922,  // Farming Guild
                4919,  // Chambers of Xeric
                5938,  // Quetzacalli
                6448,  // Varlamore West
                6960,  // Varlamore East
                6191,  // Hunter Guild
                5421,  // Aldarin
                5420,  // Mistrock
                14638, // Mos'le Harmless
                14642, // TOB
                14646, // Port Phasmatys
                12344, // Ferox Enclave
                12895, // Priff North
                13150, // Priff South
                13907, // Museum Camp
                14908, // Fossil Bank Chest island
        };
    }

    @Override
    public void onStart() {
        log("INFO", "Starting dLooter v" + scriptVersion);
        checkForUpdates();

        ui = new ScriptUI(this);
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "dLooter Options", false);

        RNGEnabled = ui.isAdditionalRngEnabled();
        location = ui.getSelectedLocation();
        log(getClass(), "Selected location: " + location);

        lastRngSent = System.currentTimeMillis();
        rngNextIntervalMs = (45_000L + rng.nextInt(46_000)); // 45–90s

        webhookEnabled = ui.isWebhookEnabled();
        webhookUrl = ui.getWebhookUrl();
        webhookIntervalMinutes = ui.getWebhookInterval();
        webhookShowUser = ui.isUsernameIncluded();
        webhookShowStats = ui.isStatsIncluded();

        if (webhookEnabled) {
            user = getWidgetManager().getChatbox().getUsername();
            log("WEBHOOK", "✅ Webhook enabled. Interval: " + webhookIntervalMinutes + "min. Username: " + user);
            lastWebhookSent = System.currentTimeMillis();
            sendWebhook();
        }

        tasks = new ArrayList<>();
        tasks.add(new Setup(this));

        if (location.equals("Guardians of the Rift")) {
            tasks.add(new GOTR(this));
        }

        if (location.equals("Wintertodt")) {
            tasks.add(new WT(this));
        }

        if (location.equals("Tempoross")) {
            tasks.add(new Tempoross(this));
        }

        if (location.equals("Bank/Seed Pack")){
            tasks.add(new sPacks(this));
        }

        if (location.equals("Castle wars supply crate")){
            tasks.add(new cWarsCrate(this));
        }
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public int poll() {
        if (webhookEnabled && System.currentTimeMillis() - lastWebhookSent >= webhookIntervalMinutes * 60_000L) {
            sendWebhook();
            lastWebhookSent = System.currentTimeMillis();
        }

        if (RNGEnabled) {
            long now = System.currentTimeMillis();
            if (now - lastRngSent >= rngNextIntervalMs) {
                sendRngBurst(); // logs 10–20 cheeky lines
                lastRngSent = now;
                rngNextIntervalMs = (45_000L + rng.nextInt(46_000)); // schedule next 45–90s
            }
        }

        if (needToStop) {
            // --- Print loot summary before stopping ---
            StringBuilder lootSummary = new StringBuilder("Final Loot Summary:\n");
            java.util.List<Integer> sortedIds = new java.util.ArrayList<>(totalGained.keySet());

            // Sort alphabetically by item name
            sortedIds.sort(java.util.Comparator.comparing(id -> getItemManager().getItemName(id)));

            for (int id : sortedIds) {
                int count = totalGained.getOrDefault(id, 0);
                if (count > 0) {
                    lootSummary.append(getItemManager().getItemName(id))
                            .append(": ")
                            .append(count)
                            .append("\n");
                }
            }
            log(getClass(), lootSummary.toString().trim());

            // --- Send final webhook and stop ---
            sendWebhook();
            stop();
            return 0;
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
        // ---- Formatters ----
        java.text.DecimalFormat f = new java.text.DecimalFormat("#,###");
        java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        f.setDecimalFormatSymbols(s);

        // ---- Fonts & metrics (prevents jitter, enables dynamic sizing) ----
        FontMetrics fm       = c.getFontMetrics(ARIAL);
        FontMetrics fmBold   = c.getFontMetrics(ARIAL_BOLD);
        FontMetrics fmItalic = c.getFontMetrics(ARIAL_ITALIC);

        final int x = 5;
        final int yTop = 40;
        final int borderThickness = 2;
        final int headerHeight = 25;

        final int paddingLeft = 10, paddingRight = 10;
        final int contentTopPad = 5, contentBottomPad = 8;
        final int groupGap = 10;
        final int lootHeaderGap = 16; // space under "Loot gained:"
        final int cols = 3;
        final int colGap = 8;

        // ---- Build & filter the item list ----
        java.util.List<Integer> itemIds = new java.util.ArrayList<>(totalGained.keySet());
        if ("Guardians of the Rift".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isGotrItemVisible(id));
        } else if ("Wintertodt".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isWtItemVisible(id));
        } else if ("Tempoross".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isTemporossItemVisible(id));
        } else if ("Bank/Seed Pack".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isSPackItemVisible(id));
        } else if ("Castle wars supply crate".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isCwscItemVisible(id));
        }

        // Sort alphabetically by item name
        itemIds.sort(java.util.Comparator.comparing(id -> getItemManager().getItemName(id).toLowerCase(java.util.Locale.ROOT)));

        // ---- Prepare strings for measurement ----
        String title = "dLooter";
        String lootHeader = "Loot gained:";

        String footerLeftLabel;
        if ("Guardians of the Rift".equals(location))       footerLeftLabel = "Searches left: ";
        else if ("Wintertodt".equals(location))             footerLeftLabel = "Rewards left: ";
        else if ("Tempoross".equals(location))              footerLeftLabel = "Permits left: ";
        else if ("Bank/Seed Pack".equals(location))         footerLeftLabel = "Packs left: ";
        else if ("Castle wars supply crate".equals(location)) footerLeftLabel = "Crates left: ";
        else                                                footerLeftLabel = "Left: ";

        String lineLeft    = footerLeftLabel + lootsLeft;
        String lineTask    = "Task: " + task;
        String lineVersion = "Version: " + scriptVersion;

        // ---- Compute loot grid text, row/col sizing ----
        int lootRows = (itemIds.size() + cols - 1) / cols;
        int lootLineHeight = fm.getHeight();

        // For dynamic width: measure max width per column
        int[] colMax = new int[Math.max(1, cols)];
        for (int i = 0; i < itemIds.size(); i++) {
            int id = itemIds.get(i);
            int count = totalGained.getOrDefault(id, 0);
            String name = getItemManager().getItemName(id);
            String text = name + ": " + f.format(count);
            int col = i % cols;
            colMax[col] = Math.max(colMax[col], fm.stringWidth(text));
        }

        // Total content width needed by loot grid
        int lootGridWidth = 0;
        for (int cIdx = 0; cIdx < Math.max(1, cols); cIdx++) {
            lootGridWidth += colMax[cIdx];
        }
        lootGridWidth += Math.max(0, (cols - 1)) * colGap;

        // ---- Compute inner width (max of header/title, loot grid, and footers) ----
        int maxLineWidth = 0;
        maxLineWidth = Math.max(maxLineWidth, fmBold.stringWidth(lootHeader));
        maxLineWidth = Math.max(maxLineWidth, lootGridWidth);
        maxLineWidth = Math.max(maxLineWidth, fm.stringWidth(lineLeft));
        maxLineWidth = Math.max(maxLineWidth, fmBold.stringWidth(lineTask));
        maxLineWidth = Math.max(maxLineWidth, fmItalic.stringWidth(lineVersion));
        maxLineWidth = Math.max(maxLineWidth, fmBold.stringWidth(title)); // header title also constrains width

        int innerWidth = paddingLeft + maxLineWidth + paddingRight;

        // ---- Compute inner height dynamically ----
        int innerHeight = 0;
        innerHeight += headerHeight + contentTopPad;
        innerHeight += fmBold.getHeight();       // "Loot gained:"
        innerHeight += lootHeaderGap;
        innerHeight += lootRows * lootLineHeight;
        innerHeight += groupGap;
        innerHeight += fmBold.getHeight();       // Task
        innerHeight += fm.getHeight();           // Left
        innerHeight += fmItalic.getHeight();     // Version
        innerHeight += contentBottomPad;

        // ---- Outer white border highlight ----
        c.fillRect(x - borderThickness, yTop - borderThickness,
                innerWidth + (borderThickness * 2), innerHeight + (borderThickness * 2),
                Color.WHITE.getRGB(), 1);

        // ---- Black background box ----
        int innerX = x;
        int innerY = yTop;
        c.fillRect(innerX, innerY, innerWidth, innerHeight, Color.BLACK.getRGB(), 1);

        // ---- White inner border ----
        c.drawRect(innerX, innerY, innerWidth, innerHeight, Color.WHITE.getRGB());

        // ---- Gradient header (violet/blue style retained) ----
        for (int i = 0; i < headerHeight; i++) {
            int r = Math.min(110 + (i * 2), 255);
            int g = Math.min(90  + (i * 2), 255);
            int b = Math.min(200 + (i * 2), 255);
            int gradientColor = new Color(r, g, b, 255).getRGB();
            c.drawLine(innerX + 1, innerY + 1 + i, innerX + innerWidth - 2, innerY + 1 + i, gradientColor);
        }
        // Header bottom border
        for (int i = 0; i < borderThickness; i++) {
            c.drawLine(innerX + 1, innerY + headerHeight + i + 1, innerX + innerWidth - 2, innerY + headerHeight + i + 1, Color.WHITE.getRGB());
        }

        // ---- Title centered via FontMetrics ----
        int titleWidth = fmBold.stringWidth(title);
        int titleX = innerX + (innerWidth / 2) - (titleWidth / 2);
        c.drawText(title, titleX, innerY + 18, Color.BLACK.getRGB(), ARIAL_BOLD);

        // ---- Content drawing ----
        final int cx = innerX + paddingLeft;
        int y = innerY + headerHeight + contentTopPad;

        // Loot table header
        y += fmBold.getHeight();
        c.drawText(lootHeader, cx, y, new Color(220, 220, 220).getRGB(), ARIAL_BOLD);

        y += lootHeaderGap;

        // Alternating row text colors
        final int colorYellow = new Color(190, 170, 40).getRGB();
        final int colorGreen  = new Color(150, 220, 160).getRGB();

        // Draw loot grid using measured column widths
        for (int row = 0; row < lootRows; row++) {
            int baselineY = y + row * lootLineHeight;
            int drawX = cx;

            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                if (idx >= itemIds.size()) break;

                int id = itemIds.get(idx);
                int count = totalGained.getOrDefault(id, 0);
                String name = getItemManager().getItemName(id);
                String text = name + ": " + f.format(count);

                int textColor = (row % 2 == 0) ? colorYellow : colorGreen;
                c.drawText(text, drawX, baselineY, textColor, ARIAL);

                // advance by the measured width of this column + gap
                drawX += colMax[col] + colGap;
            }
        }

        // Move Y to end of loot grid
        y += lootRows * lootLineHeight;

        // Footer (grouped)
        y += groupGap;

        // Task (bold cyan)
        y += fmBold.getHeight();
        c.drawText(lineTask, cx, y, new Color(0, 255, 255).getRGB(), ARIAL_BOLD);

        // Left (regular)
        y += fm.getHeight();
        c.drawText(lineLeft, cx, y, new Color(180, 220, 255).getRGB(), ARIAL);

        // Version (italic grey)
        y += fmItalic.getHeight();
        c.drawText(lineVersion, cx, y, new Color(180, 180, 180).getRGB(), ARIAL_ITALIC);
    }

    private void sendWebhook() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            BufferedImage image = getScreen().getImage().toBufferedImage();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            long elapsed = System.currentTimeMillis() - startTime;
            String runtime = formatRuntime(elapsed);

            // number formatting with '.' as thousands separator
            DecimalFormat f = new DecimalFormat("#,###");
            DecimalFormatSymbols s = new DecimalFormatSymbols();
            s.setGroupingSeparator('.');
            f.setDecimalFormatSymbols(s);

            // Build loot table blocks (3 per row, code block), split to respect Discord field limits
            java.util.List<String> lootBlocks = buildLootBlocks(f);

            StringBuilder json = new StringBuilder();
            json.append("{\"embeds\":[{")
                    .append("\"title\":\"📊 dLooter Stats - ")
                    .append(webhookShowUser && user != null ? escapeJson(user) : "anonymous")
                    .append("\",")
                    .append("\"color\":15844367,");

            // fields start
            json.append("\"fields\":[");
            // Add loot blocks as full-width (non-inline) fields
            for (int i = 0; i < lootBlocks.size(); i++) {
                String name = (lootBlocks.size() == 1) ? "Loot gained" : ("Loot gained (" + (i + 1) + "/" + lootBlocks.size() + ")");
                if (i > 0) json.append(",");
                json.append("{\"name\":\"").append(escapeJson(name)).append("\",")
                        .append("\"value\":\"").append(escapeJson(lootBlocks.get(i))).append("\",")
                        .append("\"inline\":false}");
            }

            // Footer fields (keep as before)
            json.append(",")
                    .append("{\"name\":\"Task\",\"value\":\"").append(escapeJson(task)).append("\",\"inline\":true},")
                    .append("{\"name\":\"Runtime\",\"value\":\"").append(runtime).append("\",\"inline\":true},")
                    .append("{\"name\":\"Version\",\"value\":\"").append(scriptVersion).append("\",\"inline\":true}")
                    .append("],"); // end fields

            // attach screenshot
            json.append("\"image\":{\"url\":\"attachment://screen.png\"}}]}");

            String boundary = "----Boundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n\r\n".getBytes());
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"screen.png\"\r\n".getBytes());
                out.write("Content-Type: image/png\r\n\r\n".getBytes());
                out.write(imageBytes);
                out.write("\r\n".getBytes());
                out.write(("--" + boundary + "--\r\n").getBytes());
            }

            int code = conn.getResponseCode();
            if (code == 200 || code == 204) {
                log("WEBHOOK", "✅ Sent webhook successfully.");
            } else {
                log("WEBHOOK", "⚠ Failed to send webhook: HTTP " + code);
            }
        } catch (Exception e) {
            log("WEBHOOK", "❌ Error sending webhook: " + e.getMessage());
        }
    }

    // Builds a monospaced loot table with 3 columns per row, wrapped in ``` code block.
    // Splits into multiple blocks to stay under Discord's 1024-char field value limit.
    private java.util.List<String> buildLootBlocks(DecimalFormat f) {
        // Build & filter
        java.util.List<Integer> itemIds = new java.util.ArrayList<>(totalGained.keySet());
        if ("Guardians of the Rift".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isGotrItemVisible(id));
        } else if ("Wintertodt".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isWtItemVisible(id));
        } else if ("Tempoross".equals(location)) { // NEW
            itemIds.removeIf(id -> !ScriptUI.isTemporossItemVisible(id));
        } else if ("Bank/Seed Pack".equals(location)) {
            itemIds.removeIf(id -> !ScriptUI.isSPackItemVisible(id));
        }

        // Sort alphabetically by item name (case-insensitive)
        itemIds.sort(java.util.Comparator.comparing(id -> getItemManager().getItemName(id).toLowerCase()));

        // Build cells "Name: 1.234"
        java.util.List<String> cells = new java.util.ArrayList<>(itemIds.size());
        for (int id : itemIds) {
            String name = getItemManager().getItemName(id);
            int count = totalGained.getOrDefault(id, 0);
            cells.add(name + ": " + f.format(count));
        }

        final int cols = 3;
        final int colWidth = 26; // padding width per column for alignment in monospace
        StringBuilder current = new StringBuilder("```"); // start code block
        java.util.List<String> blocks = new java.util.ArrayList<>();
        int col = 0;

        for (int i = 0; i < cells.size(); i++) {
            String cell = padRight(cells.get(i), colWidth);
            current.append(cell);
            col++;

            boolean endOfRow = (col == cols);
            boolean lastItem = (i == cells.size() - 1);

            if (endOfRow || lastItem) {
                current.append("\n");
                col = 0;
            }

            // If we're getting close to Discord's 1024 char field limit, flush the block.
            // Leave headroom for closing ``` and JSON escapes.
            if (current.length() >= 900 || (lastItem)) {
                current.append("```");
                blocks.add(current.toString());

                // Prepare next block if there are more items to add
                if (!lastItem) current = new StringBuilder("```");
            }
        }

        // Handle empty list edge case
        if (blocks.isEmpty()) {
            blocks.add("```(no items)```");
        }
        return blocks;
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) return s.substring(0, Math.min(width, s.length()));
        StringBuilder sb = new StringBuilder(width);
        sb.append(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "unknown";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String formatRuntime(long millis) {
        long seconds = millis / 1000;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private void checkForUpdates() {
        String latest = getLatestVersion("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dLooter/src/main/java/main/dLooter.java");
        if (latest == null) {
            log("VERSION", "⚠ Could not fetch latest version info.");
            return;
        }
        if (compareVersions(scriptVersion, latest) < 0) {
            log("VERSION", "⏬ New version v" + latest + " found! Updating...");
            try {
                File dir = new File(System.getProperty("user.home") + File.separator + ".osmb" + File.separator + "Scripts");

                File[] old = dir.listFiles((d, n) -> n.equals("dLooter.jar") || n.startsWith("dLooter-"));
                if (old != null) for (File f : old) if (f.delete()) log("UPDATE", "🗑 Deleted old: " + f.getName());

                File out = new File(dir, "dLooter-" + latest + ".jar");
                URL url = new URL("https://raw.githubusercontent.com/JustDavyy/osmb-scripts/main/dLooter/jar/dLooter.jar");

                try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) fos.write(buf, 0, n);
                }

                log("UPDATE", "✅ Downloaded: " + out.getName());
                stop();
            } catch (Exception e) {
                log("UPDATE", "❌ Error downloading new version: " + e.getMessage());
            }
        } else {
            log("SCRIPTVERSION", "✅ You are running the latest version (v" + scriptVersion + ").");
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

    private void sendRngBurst() {
        // 2–3 messages per burst
        int n = 2 + rng.nextInt(2); // 2 or 3

        String[] pool = new String[] {
                // Seed Packs
                "Seed Pack says: ‘not today’. Re-rolling entropy…",
                "Injecting compost into the RNG for your next Seed Pack.",
                "Calibrating seed rarity curve. Magic seed, you’re on notice.",
                "Your Seed Pack pity timer just winked. That’s a good sign.",
                "Shaking the Seed Pack like a loot piñata. Stand back.",
                "Adding ‘Farming Guild blessing’ to the next Seed Pack roll.",
                "The Seed Pack coughed—quick, roll again before it changes its mind.",
                "Buffing herb seed odds by a scientifically vague amount.",
                "Applying anti-‘10x acorn’ patch to Seed Packs.",
                "Rerouting bad luck from Seed Packs to someone in world 301. You’re welcome.",
                "Installing premium seed coatings. +Luck, +Shine, +Drama.",
                "Seed Pack algorithm upgraded to pseudo-quantum. Probably legal.",
                "‘No Magic Seed’ error encountered. Retrying with optimism.",
                "Your Seed Pack was empty? Consider it pre-looted; trying again.",
                // GOTR Guardian
                "Rewards Guardian consulted. It shrugged, then handed over a surprise.",
                "Whispered to the Rewards Guardian: ‘pearls please’. It nodded… menacingly.",
                "Abyssal pearls spawned in the code path. Prepare your pockets.",
                "Painting the Rewards Guardian with lucky dye #777.",
                "Your catalytic rolls got a caffeine boost at the Guardian.",
                "Guardian agreed to a best-of-3 reroll clause. We take those.",
                "Guardian mumbled something about pity. I heard ‘bigger pouch’?",
                "Upgrading rune multipliers by a suspicious percentage.",
                "Rewards Guardian’s lantern brightness increased = more luck. Science.",
                "Guardian checked your diary. It says ‘destined for lantern’.",
                "Splicing your rune rolls with streamer odds. Pog.",
                // Wintertodt (Reward Crate/Cart)
                "WT cart rattled ominously. That’s usually rare drop protocol.",
                "Heating up the reward crate—hot loot cooks faster.",
                "WT supply cart fitted with luck-rails. Next stop: Phoenix town.",
                "Pyromancers approved extra embers in your crate math.",
                "Smuggling a Dragon axe blueprint into the loot table.",
                "Burnt pages forged from luck-only logs. Totally canon.",
                "Crate QA says ‘too many coins’. I replaced them with gems. Maybe.",
                "WT cart switched tracks: Gem Junction → Phoenix Peak.",
                "Your next crate has a 100% chance to exist. Progress!",
                "Torch.flameIntensity++ ; rareRollChance += flameIntensity;",
                // Tempoross (Rewards Pool)
                "Tempoross pool is bubbling like it heard ‘spirit flakes’.",
                "Fishing the RNG out of the Rewards Pool—double hooking engaged.",
                "Harpoon boost uploaded. Rerouting the rare fish vein to you.",
                "Tempoross waves whisper: ‘tome, tome, tome’.",
                "Netting probability currents… your bucket looks heavier already.",
                "Rewards Pool oxygenated for optimal rare-spawn conditions.",
                "Current changed. It’s flowing toward unique drops. Surf’s up.",
                "Double-dip protocol: two rolls, one splash. Don’t tell the kraken.",
                "Tempoross approved a luck tide. Brace for flake storm.",
                "Anchoring the loot table so it stops drifting to ‘raw bass’.",
                // Cross-activity / generic RNG
                "Turning the RNG dial to ‘hero arc’.",
                "Installing hotfix: ‘Not Enough Purples’ (works on seeds too).",
                "Rolling d100… nat 1 detected. Initiating Reroll™.",
                "Borrowed some luck from a speedrunner. Pay it back with PBs.",
                "Enabling PityTimer.v2: now with confetti.",
                "Reality patch: ‘random’ now biased toward ‘nice’.",
                "Cosigned your loot with the Drop Fairy. Very official.",
                "If luck were a skill, you’d be 200m. I’ll pretend it is.",
                "Quantum coin flip returns ‘yes’. Applying to next reward.",
                "Analytics show you’re due. The graphs don’t lie.",
                "Exporting luck from another timeline. Expect desync.",
                "Summoning circle drawn with chaos runes. Totally safe."
        };

        // pick unique lines for this burst
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int i = 0; i < n; i++) {
            int idx;
            do { idx = rng.nextInt(pool.length); } while (!used.add(idx));
            log(getClass(), "🎲 " + pool[idx]);
            submitTask(() -> false, 10 + rng.nextInt(25)); // tiny jitter
        }
    }
}
