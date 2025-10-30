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
    private static final String PREF_SELECTED_WINE = "selected_wine";

    private static final String PREF_WEBHOOK_ENABLED = "dwinemaker_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dwinemaker_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dwinemaker_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dwinemaker_webhook_include_user";

    private final Script script;
    private ComboBox<Integer> wineComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final Integer[] WINE_OPTIONS = {
            ItemID.JUG_OF_WINE,
            ItemID.WINE_OF_ZAMORAK
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label wineLabel = new Label("Choose wine to make");
        wineComboBox = createWineComboBox(core);

        int savedItemId = prefs.getInt(PREF_SELECTED_WINE, ItemID.JUG_OF_WINE);
        for (Integer option : WINE_OPTIONS) {
            if (option.equals(savedItemId)) {
                wineComboBox.getSelectionModel().select(option);
                break;
            }
        }

        script.log("SAVESETTINGS", "Loaded selected wine ID from preferences: " + savedItemId);
        mainBox.getChildren().addAll(wineLabel, wineComboBox);
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

        // Confirm Button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        Scene scene = new Scene(layout, 300, 320);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createWineComboBox(ScriptCore core) {
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
        comboBox.getItems().addAll(WINE_OPTIONS);
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
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    private void saveSettings() {
        Integer selected = wineComboBox.getSelectionModel().getSelectedItem();
        if (selected != null) {
            prefs.putInt(PREF_SELECTED_WINE, selected);
            script.log("SAVESETTINGS", "Saved selected wine ID to preferences: " + selected);
        }

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) wineComboBox.getScene().getWindow()).close();
    }

    // Getters
    public int getSelectedWineId() {
        return wineComboBox.getSelectionModel().getSelectedItem();
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
