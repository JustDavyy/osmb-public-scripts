package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import data.SandstoneData;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {

    private final Preferences prefs = Preferences.userRoot().node("main");

    // Webhook prefs
    private static final String PREF_WEBHOOK_ENABLED = "ddesertminer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "ddesertminer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "ddesertminer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "ddesertminer_webhook_include_user";

    // Mining prefs
    private static final String PREF_MINING_LOCATION = "ddesertminer_location";
    private static final String PREF_USE_HUMIDIFY = "ddesertminer_use_humidify";
    private static final String PREF_USE_CIRCLET = "ddesertminer_use_circlet";
    private static final String PREF_HOP_ENABLED = "ddesertminer_hop_enabled";
    private static final String PREF_HOP_RADIUS = "ddesertminer_hop_radius";

    private final Script script;

    // Main tab controls
    private ComboBox<SandstoneData.MiningLocation> locationDropdown;
    private CheckBox humidifyCheckBox;
    private CheckBox circletCheckBox;
    private CheckBox hopCheckBox;
    private TextField hopTilesField;

    // Webhook controls
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {

        TabPane tabPane = new TabPane();

        // =============================
        // MAIN TAB
        // =============================

        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        locationDropdown = new ComboBox<>();
        locationDropdown.getItems().addAll(SandstoneData.MiningLocation.values());

        String savedLocation = prefs.get(PREF_MINING_LOCATION, SandstoneData.MiningLocation.NORTH.name());
        locationDropdown.getSelectionModel().select(SandstoneData.MiningLocation.valueOf(savedLocation));

        hopCheckBox = new CheckBox("Hop when player within tiles");
        hopCheckBox.setSelected(prefs.getBoolean(PREF_HOP_ENABLED, false));

        hopTilesField = new TextField();
        hopTilesField.setPromptText("Tiles radius");
        hopTilesField.setText(String.valueOf(prefs.getInt(PREF_HOP_RADIUS, 0)));
        hopTilesField.setDisable(!hopCheckBox.isSelected());

        hopCheckBox.setOnAction(e -> hopTilesField.setDisable(!hopCheckBox.isSelected()));

        humidifyCheckBox = new CheckBox("Use Humidify spell");
        humidifyCheckBox.setSelected(prefs.getBoolean(PREF_USE_HUMIDIFY, false));

        circletCheckBox = new CheckBox("Use Circlet of Water");
        circletCheckBox.setSelected(prefs.getBoolean(PREF_USE_CIRCLET, false));

        circletCheckBox.setOnAction(e -> {
            boolean circletEnabled = circletCheckBox.isSelected();

            if (circletEnabled) {
                humidifyCheckBox.setSelected(false);
                humidifyCheckBox.setDisable(true);
            } else {
                humidifyCheckBox.setDisable(false);
            }
        });

        mainBox.getChildren().addAll(
                new Label("Mining Location"),
                locationDropdown,
                humidifyCheckBox,
                circletCheckBox,
                hopCheckBox,
                hopTilesField
        );

        if (circletCheckBox.isSelected()) {
            humidifyCheckBox.setSelected(false);
            humidifyCheckBox.setDisable(true);
        }

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // =============================
        // WEBHOOK TAB
        // =============================

        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);

        webhookIntervalComboBox.getSelectionModel()
                .select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);

        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username");
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
                webhookUrlField,
                new Label("Send interval (minutes)"),
                webhookIntervalComboBox,
                includeUsernameCheckBox
        );

        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        // =============================
        // FINAL UI
        // =============================

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 320, 420);
        scene.getStylesheets().add("style.css");

        return scene;
    }

    private void saveSettings() {

        prefs.put(PREF_MINING_LOCATION, getMiningLocation().name());
        prefs.putBoolean(PREF_USE_HUMIDIFY, isHumidifyEnabled());
        prefs.putBoolean(PREF_USE_CIRCLET, isCircletEnabled());
        prefs.putBoolean(PREF_HOP_ENABLED, isHopEnabled());
        prefs.putInt(PREF_HOP_RADIUS, getHopRadius());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        script.log("SAVESETTINGS", "Saved settings");

        ((Stage) webhookEnabledCheckBox.getScene().getWindow()).close();
    }

    // =============================
    // GETTERS
    // =============================

    public SandstoneData.MiningLocation getMiningLocation() {
        return locationDropdown.getValue();
    }

    public boolean isHumidifyEnabled() {
        return humidifyCheckBox != null && humidifyCheckBox.isSelected();
    }

    public boolean isCircletEnabled() {
        return circletCheckBox != null && circletCheckBox.isSelected();
    }

    public boolean isHopEnabled() {
        return hopCheckBox != null && hopCheckBox.isSelected();
    }

    public int getHopRadius() {
        try {
            return Integer.parseInt(hopTilesField.getText());
        } catch (Exception e) {
            return 0;
        }
    }

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
}