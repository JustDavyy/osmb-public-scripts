package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import data.MineArea;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {

    private final Script script;
    private final Preferences prefs = Preferences.userRoot().node("main");

    // === Pref keys ===
    private static final String PREF_MLM_AREA = "aiominer_mlm_area";
    private static final String PREF_MLM_USE_UPPER_HOPPER = "aiominer_mlm_use_upper_hopper";

    private static final String PREF_WEBHOOK_ENABLED = "aiominer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "aiominer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "aiominer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "aiominer_webhook_include_user";

    // === Main tab ===
    private ComboBox<MineArea> areaComboBox;
    private CheckBox useUpperHopperCheckBox;

    // === Webhook tab ===
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // =====================================================
        // MAIN TAB (Motherlode Mine)
        // =====================================================
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(12));
        mainBox.setStyle("-fx-background-color: #2d3436;");

        Label areaLabel = new Label("Select Motherlode Mine area:");
        areaLabel.setStyle("-fx-text-fill: #e6edf3;");

        areaComboBox = new ComboBox<>();
        areaComboBox.getItems().addAll(MineArea.values());
        areaComboBox.setValue(loadSelectedArea());

        // --- Upper hopper checkbox ---
        useUpperHopperCheckBox = new CheckBox("Use upper hopper");
        styleCheckbox(useUpperHopperCheckBox);
        useUpperHopperCheckBox.setSelected(
                prefs.getBoolean(PREF_MLM_USE_UPPER_HOPPER, false)
        );

        // Enable only when TOP is selected
        useUpperHopperCheckBox.setDisable(areaComboBox.getValue() != MineArea.TOP);

        // React to area changes
        areaComboBox.setOnAction(e -> {
            boolean isTop = areaComboBox.getValue() == MineArea.TOP;
            useUpperHopperCheckBox.setDisable(!isTop);

            // Optional safety: auto-disable when leaving TOP
            if (!isTop) {
                useUpperHopperCheckBox.setSelected(false);
            }
        });

        mainBox.getChildren().addAll(
                areaLabel,
                areaComboBox,
                useUpperHopperCheckBox
        );

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // =====================================================
        // WEBHOOK TAB (unchanged logic)
        // =====================================================
        VBox webhookBox = buildWebhookTab();
        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        tabPane.getTabs().addAll(mainTab, webhookTab);

        // =====================================================
        // CONFIRM BUTTON
        // =====================================================
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox root = new VBox(tabPane, new Separator(), confirmButton);
        root.setSpacing(8);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: #2d3436;");
        root.getStylesheets().add("style.css");

        return new Scene(root, 420, 300);
    }

    // =====================================================
    // WEBHOOK TAB
    // =====================================================
    private VBox buildWebhookTab() {
        VBox webhookBox = new VBox(10);
        webhookBox.setPadding(new Insets(12));
        webhookBox.setStyle("-fx-background-color: #2d3436;");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        styleCheckbox(webhookEnabledCheckBox);
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());
        HBox.setHgrow(webhookUrlField, Priority.ALWAYS);

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(
                Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5))
        );
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username");
        styleCheckbox(includeUsernameCheckBox);
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));
        includeUsernameCheckBox.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(
                webhookEnabledCheckBox,
                new HBox(8, new Label("Webhook URL:"), webhookUrlField),
                new HBox(8, new Label("Send interval (minutes):"), webhookIntervalComboBox),
                includeUsernameCheckBox
        );

        return webhookBox;
    }

    // =====================================================
    // SAVE / LOAD
    // =====================================================
    private void saveSettings() {
        prefs.put(PREF_MLM_AREA, getSelectedArea().name());
        prefs.putBoolean(
                PREF_MLM_USE_UPPER_HOPPER,
                isUseUpperHopperEnabled()
        );

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        try { prefs.flush(); } catch (Exception ignored) {}
        try { prefs.sync(); } catch (Exception ignored) {}

        ((Stage) areaComboBox.getScene().getWindow()).close();
    }

    private MineArea loadSelectedArea() {
        try {
            return MineArea.valueOf(
                    prefs.get(PREF_MLM_AREA, MineArea.TOP.name())
            );
        } catch (Exception e) {
            return MineArea.TOP;
        }
    }

    // =====================================================
    // GETTERS
    // =====================================================
    public MineArea getSelectedArea() {
        return areaComboBox.getValue();
    }

    public boolean isUseUpperHopperEnabled() {
        return useUpperHopperCheckBox.isSelected()
                && areaComboBox.getValue() == MineArea.TOP;
    }

    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox.isSelected();
    }

    public String getWebhookUrl() {
        return webhookUrlField.getText().trim();
    }

    public int getWebhookInterval() {
        return webhookIntervalComboBox.getValue();
    }

    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox.isSelected();
    }

    // =====================================================
    // UTILITY
    // =====================================================
    private void styleCheckbox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: #e6edf3;");
    }
}