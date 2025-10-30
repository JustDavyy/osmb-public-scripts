package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    // dlooter prefs
    private static final String PREF_SELECTED_LOCATION = "dlooter_selected_location";

    // Webhook prefs (kept)
    private static final String PREF_WEBHOOK_ENABLED = "dlooter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dlooter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dlooter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dlooter_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "dlooter_webhook_include_stats";

    // Fun switch
    private static final String PREF_RNG_ENABLED = "dlooter_rng_enabled";

    private final Script script;

    // Main tab
    private ComboBox<String> locationComboBox;
    private CheckBox rngCheckBox;

    // Webhook controls
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    // Locations
    private static final String GOTR  = "Guardians of the Rift";
    private static final String WT    = "Wintertodt";
    private static final String SPACK = "Bank/Seed Pack";
    private static final String TEMP = "Tempoross";
    private static final String CWSC = "Castle wars supply crate";

    // GOTR
    private static final String PREF_GOTR_ITEM_VISIBLE_PREFIX = "dlooter_gotr_item_visible_";
    private VBox gotrFilterBox; // content box
    private ScrollPane gotrScroll; // scroll wrapper
    private final java.util.Map<Integer, CheckBox> gotrItemChecks = new java.util.HashMap<>();

    // Wintertodt
    private static final String PREF_WT_ITEM_VISIBLE_PREFIX = "dlooter_wt_item_visible_";
    private VBox wtFilterBox;    // content box
    private ScrollPane wtScroll; // scroll wrapper
    private final java.util.Map<Integer, CheckBox> wtItemChecks = new java.util.HashMap<>();

    // Seed Pack (Bank)
    private static final String PREF_SPACK_ITEM_VISIBLE_PREFIX = "dlooter_spack_item_visible_";
    private VBox spackFilterBox;    // content box
    private ScrollPane spackScroll; // scroll wrapper
    private final java.util.Map<Integer, CheckBox> spackItemChecks = new java.util.HashMap<>();
    private Label spackDisclaimerLabel;

    // Tempoross
    private static final String PREF_TEMP_ITEM_VISIBLE_PREFIX = "dlooter_temp_item_visible_";
    private VBox tempFilterBox;    // content box
    private ScrollPane tempScroll; // scroll wrapper
    private final java.util.Map<Integer, CheckBox> tempItemChecks = new java.util.HashMap<>();

    // Castle Wars Supply Crate
    private static final String PREF_CWSC_ITEM_VISIBLE_PREFIX = "dlooter_cwsc_item_visible_";
    private VBox cwscFilterBox;    // content box
    private ScrollPane cwscScroll; // scroll wrapper
    private final java.util.Map<Integer, CheckBox> cwscItemChecks = new java.util.HashMap<>();

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15;");

        Label locationLabel = new Label("Select Location");

        locationComboBox = createLocationComboBox(core);
        String savedLocation = prefs.get(PREF_SELECTED_LOCATION, GOTR);
        if (savedLocation == null || savedLocation.isBlank()) savedLocation = GOTR;
        locationComboBox.getSelectionModel().select(savedLocation);
        script.log("SAVESETTINGS", "Loaded saved location: " + savedLocation);

        gotrFilterBox  = createGotrFilterBox(core);
        wtFilterBox    = createWtFilterBox(core);
        spackFilterBox = createSpackFilterBox(core);
        tempFilterBox  = createTemporossFilterBox(core);
        cwscFilterBox  = createCwscFilterBox(core);

        // Wrap filter boxes in scroll panes so long lists are accessible
        gotrScroll  = wrapInScroll(gotrFilterBox);
        wtScroll    = wrapInScroll(wtFilterBox);
        spackScroll = wrapInScroll(spackFilterBox);
        tempScroll  = wrapInScroll(tempFilterBox);
        cwscScroll     = wrapInScroll(cwscFilterBox);

        spackDisclaimerLabel = new Label(
                "IMPORTANT: Make sure your Bank 'Withdraw-X' quantity is set to 3\n" +
                        "BEFORE starting the script."
        );
        spackDisclaimerLabel.setWrapText(true);
        spackDisclaimerLabel.setStyle("-fx-text-fill: #ff7675; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        spackDisclaimerLabel.setMaxWidth(Double.MAX_VALUE);
        spackDisclaimerLabel.prefWidthProperty().bind(mainBox.widthProperty().subtract(20));

        // Let the scroll areas expand and take available space
        VBox.setVgrow(gotrScroll, Priority.ALWAYS);
        VBox.setVgrow(wtScroll, Priority.ALWAYS);
        VBox.setVgrow(spackScroll, Priority.ALWAYS);
        VBox.setVgrow(tempScroll, Priority.ALWAYS);
        VBox.setVgrow(cwscScroll, Priority.ALWAYS);

        // Always-visible "additional RNG" at the bottom of the main tab
        rngCheckBox = new CheckBox("Enable additional RNG");
        rngCheckBox.setSelected(prefs.getBoolean(PREF_RNG_ENABLED, false));
        rngCheckBox.setStyle("-fx-text-fill: white;");

        updateFilterVisibility(savedLocation);
        locationComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateFilterVisibility(newVal);
        });

        mainBox.getChildren().addAll(
                locationLabel,
                locationComboBox,
                gotrScroll,
                wtScroll,
                tempScroll,
                spackScroll,
                cwscScroll,
                spackDisclaimerLabel,
                new Separator(),
                rngCheckBox
        );
        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15;");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeStatsCheckBox = new CheckBox("Include Stats");
        includeStatsCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_STATS, true));
        includeStatsCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
            includeStatsCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(
                webhookEnabledCheckBox,
                webhookUrlField,
                new Label("Send interval (minutes)"),
                webhookIntervalComboBox,
                includeUsernameCheckBox,
                includeStatsCheckBox
        );
        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        // === Final UI Setup ===
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            // If Seed Pack is selected, ask the user to confirm Withdraw-X = 3
            if (SPACK.equals(getSelectedLocation())) {
                if (!showSpackConfirmDialog()) {
                    script.log(getClass(), "User cancelled Seed Pack Withdraw-X confirmation. Stopping script.");
                    script.stop();
                    // close this UI window too
                    ((Stage) locationComboBox.getScene().getWindow()).close();
                    return;
                }
            }
            saveSettings();
        });

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        // Let the TabPane expand so inner tabs (and scroll panes) can use extra height
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 550, 500);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ScrollPane wrapInScroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return sp;
    }

    private void updateFilterVisibility(String location) {
        boolean isGotr  = GOTR.equals(location);
        boolean isWt    = WT.equals(location);
        boolean isSpack = SPACK.equals(location);
        boolean isTemp  = TEMP.equals(location);
        boolean isCwsc = CWSC.equals(location);

        if (gotrScroll != null) { gotrScroll.setVisible(isGotr); gotrScroll.setManaged(isGotr); }
        if (wtScroll   != null) { wtScroll.setVisible(isWt);   wtScroll.setManaged(isWt);   }
        if (tempScroll != null) { tempScroll.setVisible(isTemp); tempScroll.setManaged(isTemp); }
        if (spackScroll!= null) { spackScroll.setVisible(isSpack); spackScroll.setManaged(isSpack); }
        if (cwscScroll != null) { cwscScroll.setVisible(isCwsc); cwscScroll.setManaged(isCwsc); }

        if (spackDisclaimerLabel != null) {
            spackDisclaimerLabel.setVisible(isSpack);
            spackDisclaimerLabel.setManaged(isSpack);
        }
    }

    private void saveSettings() {
        prefs.put(PREF_SELECTED_LOCATION, getSelectedLocation());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        // Save GOTR loot visibility flags
        for (var entry : gotrItemChecks.entrySet()) {
            int id = entry.getKey();
            boolean visible = entry.getValue().isSelected();
            prefs.putBoolean(PREF_GOTR_ITEM_VISIBLE_PREFIX + id, visible);
        }

        // Save Wintertodt loot visibility flags
        for (var entry : wtItemChecks.entrySet()) {
            int id = entry.getKey();
            boolean visible = entry.getValue().isSelected();
            prefs.putBoolean(PREF_WT_ITEM_VISIBLE_PREFIX + id, visible);
        }

        // Save Seed Pack loot visibility flags
        for (var entry : spackItemChecks.entrySet()) {
            int id = entry.getKey();
            boolean visible = entry.getValue().isSelected();
            prefs.putBoolean(PREF_SPACK_ITEM_VISIBLE_PREFIX + id, visible);
        }

        // Save Tempoross loot visibility flags
        for (var entry : tempItemChecks.entrySet()) {
            int id = entry.getKey();
            boolean visible = entry.getValue().isSelected();
            prefs.putBoolean(PREF_TEMP_ITEM_VISIBLE_PREFIX + id, visible);
        }

        // Save Castle Wars Supply Crate loot visibility flags
        for (var entry : cwscItemChecks.entrySet()) {
            int id = entry.getKey();
            boolean visible = entry.getValue().isSelected();
            prefs.putBoolean(PREF_CWSC_ITEM_VISIBLE_PREFIX + id, visible);
        }

        // Save RNG toggle
        prefs.putBoolean(PREF_RNG_ENABLED, isAdditionalRngEnabled());

        script.log("SAVESETTINGS", "Saved location, filters, webhook config, and RNG toggle.");
        ((Stage) locationComboBox.getScene().getWindow()).close();
    }

    private ComboBox<String> createLocationComboBox(ScriptCore core) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core));
        comboBox.getItems().addAll(GOTR, WT, TEMP, SPACK, CWSC);
        return comboBox;
    }

    private ListCell<String> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String locationName, boolean empty) {
                super.updateItem(locationName, empty);
                if (locationName != null && !empty) {
                    ImageView imageView = null;
                    if (GOTR.equals(locationName)) {
                        imageView = JavaFXUtils.getItemImageView(core, ItemID.GREATISH_GUARDIAN);
                    } else if (WT.equals(locationName)) {
                        imageView = JavaFXUtils.getItemImageView(core, ItemID.BRUMA_TORCH);
                    } else if (TEMP.equals(locationName)) { // NEW
                        imageView = JavaFXUtils.getItemImageView(core, ItemID.TINY_TEMPOR);
                        if (imageView == null) imageView = JavaFXUtils.getItemImageView(core, ItemID.TOME_OF_WATER_EMPTY);
                    } else if (SPACK.equals(locationName)) {
                        imageView = JavaFXUtils.getItemImageView(core, ItemID.SEED_PACK);
                        if (imageView == null) imageView = JavaFXUtils.getItemImageView(core, ItemID.WATERMELON_SEED);
                    } else if (CWSC.equals(locationName)) { // NEW
                    imageView = JavaFXUtils.getItemImageView(core, ItemID.CASTLE_WARS_SUPPLY_CRATE);
                    if (imageView == null) imageView = JavaFXUtils.getItemImageView(core, ItemID.COINS_995);
                    }
                if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                    }
                    setGraphic(imageView);
                    setText(locationName);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    private VBox createGotrFilterBox(ScriptCore core) {
        VBox root = new VBox(8);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 12; -fx-background-radius: 6;");

        Label lbl = new Label("Loot visibility (GOTR):");
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button selectAll = new Button("Select all");
        Button deselectAll = new Button("Deselect all");
        selectAll.setOnAction(e -> gotrItemChecks.values().forEach(cb -> cb.setSelected(true)));
        deselectAll.setOnAction(e -> gotrItemChecks.values().forEach(cb -> cb.setSelected(false)));
        ToolBar tb = new ToolBar(selectAll, deselectAll);
        tb.setStyle("-fx-background-color: #636e72;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        java.util.List<Integer> sorted = new java.util.ArrayList<>(getGotrItemIds());
        sorted.sort(java.util.Comparator.comparing(id -> core.getItemManager().getItemName(id).toLowerCase()));

        final int cols = 3;
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            String name = core.getItemManager().getItemName(id);

            boolean show = prefs.getBoolean(PREF_GOTR_ITEM_VISIBLE_PREFIX + id, true);
            CheckBox cb = new CheckBox(name);
            cb.setSelected(show);
            cb.setStyle("-fx-text-fill: white;");

            ImageView iv = JavaFXUtils.getItemImageView(core, id);
            if (iv != null) {
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }

            gotrItemChecks.put(id, cb);

            int row = i / cols;
            int col = i % cols;
            grid.add(cb, col, row);
        }

        root.getChildren().addAll(lbl, tb, grid);
        return root;
    }

    private VBox createWtFilterBox(ScriptCore core) {
        VBox root = new VBox(8);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 12; -fx-background-radius: 6;");

        Label lbl = new Label("Loot visibility (Wintertodt):");
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button selectAll = new Button("Select all");
        Button deselectAll = new Button("Deselect all");
        selectAll.setOnAction(e -> wtItemChecks.values().forEach(cb -> cb.setSelected(true)));
        deselectAll.setOnAction(e -> wtItemChecks.values().forEach(cb -> cb.setSelected(false)));
        ToolBar tb = new ToolBar(selectAll, deselectAll);
        tb.setStyle("-fx-background-color: #636e72;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        java.util.List<Integer> sorted = new java.util.ArrayList<>(getWTItemIds());
        sorted.sort(java.util.Comparator.comparing(id -> core.getItemManager().getItemName(id).toLowerCase()));

        final int cols = 3;
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            String name = core.getItemManager().getItemName(id);

            boolean show = prefs.getBoolean(PREF_WT_ITEM_VISIBLE_PREFIX + id, true);
            CheckBox cb = new CheckBox(name);
            cb.setSelected(show);
            cb.setStyle("-fx-text-fill: white;");

            ImageView iv = JavaFXUtils.getItemImageView(core, id);
            if (iv != null) {
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }

            wtItemChecks.put(id, cb);

            int row = i / cols;
            int col = i % cols;
            grid.add(cb, col, row);
        }

        root.getChildren().addAll(lbl, tb, grid);
        return root;
    }

    private VBox createTemporossFilterBox(ScriptCore core) {
        VBox root = new VBox(8);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 12; -fx-background-radius: 6;");

        Label lbl = new Label("Loot visibility (Tempoross):");
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button selectAll = new Button("Select all");
        Button deselectAll = new Button("Deselect all");
        selectAll.setOnAction(e -> tempItemChecks.values().forEach(cb -> cb.setSelected(true)));
        deselectAll.setOnAction(e -> tempItemChecks.values().forEach(cb -> cb.setSelected(false)));
        ToolBar tb = new ToolBar(selectAll, deselectAll);
        tb.setStyle("-fx-background-color: #636e72;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        java.util.List<Integer> sorted = new java.util.ArrayList<>(getTemporossItemIds());
        sorted.sort(java.util.Comparator.comparing(id -> core.getItemManager().getItemName(id).toLowerCase()));

        final int cols = 3;
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            String name = core.getItemManager().getItemName(id);

            boolean show = prefs.getBoolean(PREF_TEMP_ITEM_VISIBLE_PREFIX + id, true);
            CheckBox cb = new CheckBox(name);
            cb.setSelected(show);
            cb.setStyle("-fx-text-fill: white;");

            ImageView iv = JavaFXUtils.getItemImageView(core, id);
            if (iv != null) {
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }

            tempItemChecks.put(id, cb);

            int row = i / cols;
            int col = i % cols;
            grid.add(cb, col, row);
        }

        root.getChildren().addAll(lbl, tb, grid);
        return root;
    }

    private VBox createSpackFilterBox(ScriptCore core) {
        VBox root = new VBox(8);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 12; -fx-background-radius: 6;");

        Label lbl = new Label("Loot visibility (Seed Pack):");
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button selectAll = new Button("Select all");
        Button deselectAll = new Button("Deselect all");
        selectAll.setOnAction(e -> spackItemChecks.values().forEach(cb -> cb.setSelected(true)));
        deselectAll.setOnAction(e -> spackItemChecks.values().forEach(cb -> cb.setSelected(false)));
        ToolBar tb = new ToolBar(selectAll, deselectAll);
        tb.setStyle("-fx-background-color: #636e72;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        java.util.List<Integer> sorted = new java.util.ArrayList<>(getSPackItemIds());
        sorted.sort(java.util.Comparator.comparing(id -> core.getItemManager().getItemName(id).toLowerCase()));

        final int cols = 3;
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            String name = core.getItemManager().getItemName(id);

            boolean show = prefs.getBoolean(PREF_SPACK_ITEM_VISIBLE_PREFIX + id, true);
            CheckBox cb = new CheckBox(name);
            cb.setSelected(show);
            cb.setStyle("-fx-text-fill: white;");

            ImageView iv = JavaFXUtils.getItemImageView(core, id);
            if (iv != null) {
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }

            spackItemChecks.put(id, cb);

            int row = i / cols;
            int col = i % cols;
            grid.add(cb, col, row);
        }

        root.getChildren().addAll(lbl, tb, grid);
        return root;
    }

    private VBox createCwscFilterBox(ScriptCore core) {
        VBox root = new VBox(8);
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 12; -fx-background-radius: 6;");

        Label lbl = new Label("Loot visibility (Castle wars supply crate):");
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Button selectAll = new Button("Select all");
        Button deselectAll = new Button("Deselect all");
        selectAll.setOnAction(e -> cwscItemChecks.values().forEach(cb -> cb.setSelected(true)));
        deselectAll.setOnAction(e -> cwscItemChecks.values().forEach(cb -> cb.setSelected(false)));
        ToolBar tb = new ToolBar(selectAll, deselectAll);
        tb.setStyle("-fx-background-color: #636e72;");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        java.util.List<Integer> sorted = new java.util.ArrayList<>(getCwscItemIds());
        // Sort by item name for a neat grid
        sorted.sort(java.util.Comparator.comparing(id -> {
            String n = core.getItemManager().getItemName(id);
            return n == null ? "" : n.toLowerCase();
        }));

        final int cols = 3;
        for (int i = 0; i < sorted.size(); i++) {
            int id = sorted.get(i);
            String name = core.getItemManager().getItemName(id);

            boolean show = prefs.getBoolean(PREF_CWSC_ITEM_VISIBLE_PREFIX + id, true);
            CheckBox cb = new CheckBox(name);
            cb.setSelected(show);
            cb.setStyle("-fx-text-fill: white;");

            ImageView iv = JavaFXUtils.getItemImageView(core, id);
            if (iv != null) {
                iv.setFitWidth(16);
                iv.setFitHeight(16);
                cb.setGraphic(iv);
            }

            cwscItemChecks.put(id, cb);

            int row = i / cols;
            int col = i % cols;
            grid.add(cb, col, row);
        }

        // If list is empty, show a small note (won’t break layout)
        if (sorted.isEmpty()) {
            Label empty = new Label("No items configured yet.");
            empty.setStyle("-fx-text-fill: #ddd;");
            root.getChildren().addAll(lbl, tb, empty);
        } else {
            root.getChildren().addAll(lbl, tb, grid);
        }
        return root;
    }

    private boolean showSpackConfirmDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Seed Pack Setup");
        alert.setHeaderText("Confirm Withdraw-X is set to 3");

        Stage owner = (Stage) locationComboBox.getScene().getWindow();
        alert.initOwner(owner);

        Label msg = new Label(
                "You selected Bank/Seed Pack.\n\n" +
                        "Please ensure your bank 'Withdraw-X' quantity is set to 3 BEFORE starting.\n\n" +
                        "Proceed?"
        );
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        DialogPane dp = alert.getDialogPane();

        // === Style background and borders directly ===
        dp.setStyle(
                "-fx-background-color: #2d3436;" +   // dark gray background
                        "-fx-padding: 15;" +
                        "-fx-border-color: #555;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        // Header text styling
        if (dp.lookup(".header-panel") != null) {
            dp.lookup(".header-panel").setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");
        }
        if (dp.lookup(".header-panel .label") != null) {
            dp.lookup(".header-panel .label").setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        }

        dp.setContent(new VBox(10, msg));

        // Style the buttons
        dp.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        ButtonBar buttonBar = (ButtonBar) dp.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setStyle("-fx-background-color: #2d3436;");
        }
        dp.lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: #0984e3;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 12;"
        );
        dp.lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: #636e72;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 12;"
        );

        return alert.showAndWait()
                .filter(btn -> btn == ButtonType.OK)
                .isPresent();
    }

    // === Getter (returns plain location string) ===
    public String getSelectedLocation() {
        return locationComboBox.getSelectionModel().getSelectedItem();
    }

    // === Webhook getters ===
    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected();
    }

    public String getWebhookUrl() {
        return webhookUrlField != null ? webhookUrlField.getText().trim() : "";
    }

    public int getWebhookInterval() {
        return webhookIntervalComboBox != null && webhookIntervalComboBox.getValue() != null
                ? webhookIntervalComboBox.getValue()
                : 5;
    }

    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected();
    }

    public boolean isStatsIncluded() {
        return includeStatsCheckBox != null && includeStatsCheckBox.isSelected();
    }

    // Additional RNG getter
    public boolean isAdditionalRngEnabled() {
        return rngCheckBox != null && rngCheckBox.isSelected();
    }

    // Keep the lists close to the UI so it can render names/images:
    private static java.util.List<Integer> getGotrItemIds() {
        return java.util.List.of(
                // Rare reward table
                ItemID.ATLAXS_DIARY,
                ItemID.CATALYTIC_TALISMAN,
                ItemID.ABYSSAL_NEEDLE,
                ItemID.ABYSSAL_LANTERN,
                ItemID.ABYSSAL_RED_DYE,
                ItemID.ABYSSAL_GREEN_DYE,
                ItemID.ABYSSAL_BLUE_DYE,

                // Essence pouches
                ItemID.SMALL_POUCH,
                ItemID.MEDIUM_POUCH,
                ItemID.LARGE_POUCH,
                ItemID.GIANT_POUCH,

                // Runes
                ItemID.AIR_RUNE,
                ItemID.WATER_RUNE,
                ItemID.EARTH_RUNE,
                ItemID.FIRE_RUNE,
                ItemID.MIND_RUNE,
                ItemID.BODY_RUNE,
                ItemID.CHAOS_RUNE,
                ItemID.COSMIC_RUNE,
                ItemID.NATURE_RUNE,
                ItemID.LAW_RUNE,
                ItemID.DEATH_RUNE,
                ItemID.BLOOD_RUNE,

                // Talismans
                ItemID.ELEMENTAL_TALISMAN,
                ItemID.LAW_TALISMAN,
                ItemID.DEATH_TALISMAN + 1, // noted variant (keep +1s as requested)

                // Other
                ItemID.ABYSSAL_PEARLS,
                ItemID.INTRICATE_POUCH,
                ItemID.ABYSSAL_ASHES,
                ItemID.NEEDLE,

                // Tertiary
                ItemID.ABYSSAL_PROTECTOR
        );
    }

    private static java.util.List<Integer> getWTItemIds() {
        return java.util.List.of(
                // Rare reward table
                ItemID.BURNT_PAGE,
                ItemID.WARM_GLOVES,
                ItemID.BRUMA_TORCH,
                ItemID.PYROMANCER_HOOD,
                ItemID.PYROMANCER_GARB,
                ItemID.PYROMANCER_ROBE,
                ItemID.PYROMANCER_BOOTS,
                ItemID.TOME_OF_FIRE_EMPTY,
                ItemID.PHOENIX,
                ItemID.DRAGON_AXE,

                // Logs (noted)
                ItemID.OAK_LOGS + 1,
                ItemID.WILLOW_LOGS + 1,
                ItemID.TEAK_LOGS + 1,
                ItemID.MAPLE_LOGS + 1,
                ItemID.MAHOGANY_LOGS + 1,
                ItemID.YEW_LOGS + 1,
                ItemID.MAGIC_LOGS + 1,

                // Gems (noted)
                ItemID.UNCUT_SAPPHIRE + 1,
                ItemID.UNCUT_EMERALD + 1,
                ItemID.UNCUT_RUBY + 1,
                ItemID.UNCUT_DIAMOND + 1,

                // Ores (noted)
                ItemID.PURE_ESSENCE + 1,
                ItemID.LIMESTONE + 1,
                ItemID.SILVER_ORE + 1,
                ItemID.IRON_ORE + 1,
                ItemID.COAL + 1,
                ItemID.GOLD_ORE + 1,
                ItemID.MITHRIL_ORE + 1,
                ItemID.ADAMANTITE_ORE + 1,
                ItemID.RUNITE_ORE + 1,

                // Herbs (noted)
                ItemID.GRIMY_RANARR_WEED + 1,
                ItemID.GRIMY_IRIT_LEAF + 1,
                ItemID.GRIMY_AVANTOE + 1,
                ItemID.GRIMY_KWUARM + 1,
                ItemID.GRIMY_CADANTINE + 1,
                ItemID.GRIMY_LANTADYME + 1,
                ItemID.GRIMY_DWARF_WEED + 1,
                ItemID.GRIMY_TORSTOL + 1,

                // Seeds (unnoted)
                ItemID.ACORN,
                ItemID.WILLOW_SEED,
                ItemID.MAPLE_SEED,
                ItemID.BANANA_TREE_SEED,
                ItemID.TEAK_SEED,
                ItemID.MAHOGANY_SEED,
                ItemID.YEW_SEED,
                ItemID.WATERMELON_SEED,
                ItemID.SNAPE_GRASS_SEED,
                ItemID.SPIRIT_SEED,
                ItemID.MAGIC_SEED,

                // Fish (noted)
                ItemID.RAW_ANCHOVIES + 1,
                ItemID.RAW_TROUT + 1,
                ItemID.RAW_SALMON + 1,
                ItemID.RAW_TUNA + 1,
                ItemID.RAW_LOBSTER + 1,
                ItemID.RAW_SWORDFISH + 1,
                ItemID.RAW_SHARK + 1,

                // Other
                ItemID.COINS_995,
                ItemID.SALTPETRE + 1,
                ItemID.DYNAMITE + 1
        );
    }

    private static java.util.List<Integer> getTemporossItemIds() {
        return java.util.List.of(
                // Uniques / special
                ItemID.SPIRIT_FLAKES,
                ItemID.CASKET_25590,
                ItemID.SOAKED_PAGE,
                ItemID.FISH_BARREL,
                ItemID.TACKLE_BOX,
                ItemID.BIG_HARPOONFISH,
                ItemID.TOME_OF_WATER_EMPTY,
                ItemID.TINY_TEMPOR,
                ItemID.DRAGON_HARPOON,

                // Fish
                ItemID.RAW_HERRING + 1,
                ItemID.RAW_MACKEREL + 1,
                ItemID.RAW_PIKE + 1,
                ItemID.RAW_SALMON + 1,
                ItemID.RAW_TUNA + 1,
                ItemID.RAW_LOBSTER + 1,
                ItemID.RAW_BASS + 1,
                ItemID.RAW_SWORDFISH + 1,
                ItemID.RAW_SHARK + 1,
                ItemID.RAW_SEA_TURTLE + 1,
                ItemID.RAW_MANTA_RAY + 1,

                // Other
                ItemID.PLANK + 1,
                ItemID.OAK_PLANK + 1,
                ItemID.SEAWEED + 1,
                ItemID.STEEL_NAILS,
                ItemID.FEATHER,
                ItemID.FISHING_BAIT
        );
    }

    private static java.util.List<Integer> getSPackItemIds() {
        return java.util.List.of(
                // Low Rewards
                ItemID.ACORN,
                ItemID.APPLE_TREE_SEED,
                ItemID.BANANA_TREE_SEED,
                ItemID.ORANGE_TREE_SEED,
                ItemID.CURRY_TREE_SEED,

                // Medium Rewards
                ItemID.LIMPWURT_SEED,
                ItemID.WATERMELON_SEED,
                ItemID.SNAPE_GRASS_SEED,
                ItemID.POTATO_CACTUS_SEED,
                ItemID.WILLOW_SEED,
                ItemID.PINEAPPLE_SEED,
                ItemID.CALQUAT_TREE_SEED,
                ItemID.TEAK_SEED,
                ItemID.WHITE_LILY_SEED,

                // High Rewards
                ItemID.PAPAYA_TREE_SEED,
                ItemID.PALM_TREE_SEED,
                ItemID.HESPORI_SEED,
                ItemID.MAPLE_SEED,
                ItemID.MAHOGANY_SEED,
                ItemID.YEW_SEED,
                ItemID.DRAGONFRUIT_TREE_SEED,
                ItemID.CELASTRUS_SEED,
                ItemID.MAGIC_SEED,
                ItemID.SPIRIT_SEED,
                ItemID.REDWOOD_TREE_SEED
        );
    }

    private static java.util.List<Integer> getCwscItemIds() {
        return java.util.List.of(
                ItemID.BLIGHTED_MANTA_RAY +1,
                ItemID.BLIGHTED_ANGLERFISH +1,
                ItemID.BLIGHTED_KARAMBWAN +1,
                ItemID.BLIGHTED_SUPER_RESTORE4 +1,
                ItemID.BLIGHTED_ANCIENT_ICE_SACK,
                ItemID.BLIGHTED_VENGEANCE_SACK,
                ItemID.CASTLE_WARS_ARROW,
                ItemID.CASTLE_WARS_BOLTS,
                ItemID.RUNE_ARROW,
                ItemID.RUNE_JAVELIN,
                ItemID.CASTLE_WARS_TICKET
        );
    }

    public static boolean isGotrItemVisible(int itemId) {
        Preferences p = Preferences.userNodeForPackage(ScriptUI.class);
        return p.getBoolean(PREF_GOTR_ITEM_VISIBLE_PREFIX + itemId, true);
    }

    public static boolean isWtItemVisible(int itemId) {
        Preferences p = Preferences.userNodeForPackage(ScriptUI.class);
        return p.getBoolean(PREF_WT_ITEM_VISIBLE_PREFIX + itemId, true);
    }

    public static boolean isTemporossItemVisible(int itemId) {
        Preferences p = Preferences.userNodeForPackage(ScriptUI.class);
        return p.getBoolean(PREF_TEMP_ITEM_VISIBLE_PREFIX + itemId, true);
    }

    public static boolean isSPackItemVisible(int itemId) {
        Preferences p = Preferences.userNodeForPackage(ScriptUI.class);
        return p.getBoolean(PREF_SPACK_ITEM_VISIBLE_PREFIX + itemId, true);
    }

    public static boolean isCwscItemVisible(int itemId) {
        Preferences p = Preferences.userNodeForPackage(ScriptUI.class);
        return p.getBoolean(PREF_CWSC_ITEM_VISIBLE_PREFIX + itemId, true);
    }
}