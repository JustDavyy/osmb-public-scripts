package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_DEPOSIT_ACTION = "dcamtorumminer_deposit_action";
    private static final String PREF_DROP_ALSO_GEMS_CLUES = "dcamtorumminer_drop_also_gems_clues";

    private static final String PREF_WEBHOOK_ENABLED = "dcamtorumminer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dcamtorumminer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dcamtorumminer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dcamtorumminer_webhook_include_user";

    private final Script script;
    private ComboBox<String> depositActionComboBox;
    private CheckBox dropAlsoGemsCluesCheckBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label actionLabel = new Label("What to do with Calcified Deposits:");
        depositActionComboBox = new ComboBox<>();
        depositActionComboBox.getItems().addAll("Drop", "Smith");
        depositActionComboBox.getSelectionModel().select(prefs.get(PREF_DEPOSIT_ACTION, "Drop"));

        // Conditionally shown when "Drop" is selected
        dropAlsoGemsCluesCheckBox = new CheckBox("Also drop gems & keys");
        dropAlsoGemsCluesCheckBox.setSelected(prefs.getBoolean(PREF_DROP_ALSO_GEMS_CLUES, false));

        // Show/hide logic tied to deposit action
        boolean showExtraDrop = "Drop".equals(depositActionComboBox.getValue());
        dropAlsoGemsCluesCheckBox.setVisible(showExtraDrop);
        dropAlsoGemsCluesCheckBox.setManaged(showExtraDrop);

        depositActionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean show = "Drop".equals(newVal);
            dropAlsoGemsCluesCheckBox.setVisible(show);
            dropAlsoGemsCluesCheckBox.setManaged(show);
        });

        mainBox.getChildren().addAll(actionLabel, depositActionComboBox, dropAlsoGemsCluesCheckBox);

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5) - 1);
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

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        Scene scene = new Scene(layout, 300, 340);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        prefs.put(PREF_DEPOSIT_ACTION, getDepositAction());
        prefs.putBoolean(PREF_DROP_ALSO_GEMS_CLUES, isAlsoDropGemsAndClues());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) depositActionComboBox.getScene().getWindow()).close();
    }

    // Getters
    public String getDepositAction() {
        return depositActionComboBox.getSelectionModel().getSelectedItem();
    }

    public boolean isAlsoDropGemsAndClues() {
        return "Drop".equals(getDepositAction())
                && dropAlsoGemsCluesCheckBox != null
                && dropAlsoGemsCluesCheckBox.isSelected();
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