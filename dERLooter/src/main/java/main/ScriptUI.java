package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userRoot().node("main");
    private final Script script;
    
    private static final String PREF_WEBHOOK_ENABLED = "derlooter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "derlooter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "derlooter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "derlooter_webhook_include_user";

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setPadding(new Insets(12));
        webhookBox.setStyle("-fx-background-color: #2d3436;");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setStyle("-fx-text-fill: #e6edf3;");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());
        HBox.setHgrow(webhookUrlField, Priority.ALWAYS);

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);

        // Select by value (robust even if items change order/size)
        int savedInterval = prefs.getInt(PREF_WEBHOOK_INTERVAL, 5);
        if (webhookIntervalComboBox.getItems().contains(savedInterval)) {
            webhookIntervalComboBox.getSelectionModel().select(savedInterval);
        } else {
            webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(5));
        }
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(savedInterval));
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setStyle("-fx-text-fill: #e6edf3;");
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

        Tab webhookTab = new Tab("Webhooks", webhookBox);
        webhookTab.setClosable(false);

        // === Final Layout ===
        tabPane.getTabs().addAll(webhookTab);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox root = new VBox(tabPane, new Separator(), confirmButton);
        root.setSpacing(8);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: #2d3436;");
        root.getStylesheets().add("style.css");

        // wider, shorter
        return new Scene(root, 300, 275);
    }

    private void saveSettings() {
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        ((Stage) webhookIntervalComboBox.getScene().getWindow()).close();
    }

    // === Getters ===
    public boolean isWebhookEnabled() { return webhookEnabledCheckBox.isSelected(); }
    public String getWebhookUrl() { return webhookUrlField.getText().trim(); }
    public int getWebhookInterval() {
        Integer val = webhookIntervalComboBox.getValue();
        return val != null ? val : 5;
    }
    public boolean isUsernameIncluded() { return includeUsernameCheckBox.isSelected(); }
}
