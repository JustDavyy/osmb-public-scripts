package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_SPELL = "dteleporter_selected_spell";
    private static final String PREF_WEBHOOK_ENABLED = "dteleporter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dteleporter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dteleporter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dteleporter_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "dteleporter_webhook_include_stats";

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    private static final StandardSpellbook[] TELEPORT_SPELLS = {
            StandardSpellbook.VARROCK_TELEPORT,
            StandardSpellbook.LUMBRIDGE_TELEPORT,
            StandardSpellbook.FALADOR_TELEPORT,
            StandardSpellbook.CAMELOT_TELEPORT,
            StandardSpellbook.KOUREND_TELEPORT,
            StandardSpellbook.ARDOUGNE_TELEPORT,
            StandardSpellbook.CIVITAS_ILLA_FORTIS_TELEPORT,
            StandardSpellbook.WATCHTOWER_TELEPORT,
            StandardSpellbook.TROLLHEIM_TELEPORT
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label spellLabel = new Label("Choose teleport to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(TELEPORT_SPELLS);

        String savedSpell = prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.CAMELOT_TELEPORT.name());
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(savedSpell));
        script.log("SAVESETTINGS", "Loaded saved teleport spell from preferences: " + savedSpell);

        mainBox.getChildren().addAll(spellLabel, spellComboBox);
        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField();
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setText(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 15; i++) webhookIntervalComboBox.getItems().add(i);
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
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 300, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        prefs.put(PREF_SELECTED_SPELL, getSelectedSpell().name());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        script.log("SAVESETTINGS", "Saved teleport spell and webhook config.");
        ((Stage) spellComboBox.getScene().getWindow()).close();
    }

    // === Getters ===
    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
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

    public boolean isStatsIncluded() {
        return includeStatsCheckBox != null && includeStatsCheckBox.isSelected();
    }
}
