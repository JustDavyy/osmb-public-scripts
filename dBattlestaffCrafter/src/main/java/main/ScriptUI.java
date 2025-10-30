package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.script.Script;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTED_STAFF = "dbattlestaffcrafter_selected_staff";
    private static final String PREF_WEBHOOK_ENABLED = "dbattlestaffcrafter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dbattlestaffcrafter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dbattlestaffcrafter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dbattlestaffcrafter_webhook_include_user";

    private final Script script;
    private ComboBox<Integer> staffComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final Integer[] STAFF_OPTIONS = {
            ItemID.AIR_BATTLESTAFF,
            ItemID.WATER_BATTLESTAFF,
            ItemID.EARTH_BATTLESTAFF,
            ItemID.FIRE_BATTLESTAFF
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label staffLabel = new Label("Choose battlestaff to make");
        staffComboBox = createStaffComboBox(core);

        int savedItemId = prefs.getInt(PREF_SELECTED_STAFF, ItemID.WATER_BATTLESTAFF);
        for (Integer option : STAFF_OPTIONS) {
            if (option.equals(savedItemId)) {
                staffComboBox.getSelectionModel().select(option);
                break;
            }
        }

        mainBox.getChildren().addAll(staffLabel, staffComboBox);
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
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
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

        // Confirm Button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 300, 300);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createStaffComboBox(ScriptCore core) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer itemId) {
                return itemId != null ? core.getItemManager().getItemName(itemId) : "";
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(param -> createItemCell(core));
        comboBox.setButtonCell(createItemCell(core));
        comboBox.getItems().addAll(STAFF_OPTIONS);
        return comboBox;
    }

    private ListCell<Integer> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView imageView = JavaFXUtils.getItemImageView(core, itemId);
                    if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                    }
                    setGraphic(imageView);
                    setText(name != null ? name : "Unknown");
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    private void saveSettings() {
        int selected = getSelectedStaffId();
        prefs.putInt(PREF_SELECTED_STAFF, selected);
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        script.log("SAVESETTINGS", "Saved selected battlestaff ID: " + selected);
        ((Stage) staffComboBox.getScene().getWindow()).close();
    }

    // === Getters ===
    public int getSelectedStaffId() {
        return staffComboBox.getSelectionModel().getSelectedItem();
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
