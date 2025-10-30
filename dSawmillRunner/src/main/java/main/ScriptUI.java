package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_SELECTED_LOCATION = "dteleporter_selected_location";
    private static final String PREF_SELECTED_PLANK = "dteleporter_selected_plank";
    private static final String PREF_USE_RING_OF_ELEMENTS = "dteleporter_use_ring_of_elements";

    private static final String PREF_WEBHOOK_ENABLED = "dteleporter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dteleporter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dteleporter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dteleporter_webhook_include_user";

    private final Script script;

    private ComboBox<Location> locationComboBox;
    private ComboBox<Integer> plankComboBox;
    private CheckBox ringOfElementsCheckBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public enum Location {
        AUBURNVALE,
        PRIFDDINAS_NORTH,
        PRIFDDINAS_SOUTH,
        VARROCK,
        WOODCUTTING_GUILD
    }

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        // Location dropdown
        Label locationLabel = new Label("Select Location");
        locationComboBox = new ComboBox<>();
        locationComboBox.getItems().addAll(Location.values());
        String savedLocation = prefs.get(PREF_SELECTED_LOCATION, Location.WOODCUTTING_GUILD.name());
        locationComboBox.getSelectionModel().select(Location.valueOf(savedLocation));
        script.log("SAVESETTINGS", "Loaded saved location: " + savedLocation);

        // Ring of Elements checkbox (initially created, visibility handled below)
        ringOfElementsCheckBox = new CheckBox("Use Ring of Elements for teleport");
        ringOfElementsCheckBox.setSelected(prefs.getBoolean(PREF_USE_RING_OF_ELEMENTS, false));
        ringOfElementsCheckBox.setVisible(locationComboBox.getValue() == Location.VARROCK);

        // Listener to show/hide ring checkbox based on location
        locationComboBox.setOnAction(e -> {
            boolean isVarrock = locationComboBox.getValue() == Location.VARROCK;
            ringOfElementsCheckBox.setVisible(isVarrock);
        });

        // Plank dropdown
        Label plankLabel = new Label("Select Plank Type");
        plankComboBox = createPlankComboBox(core);

        int savedPlank = prefs.getInt(PREF_SELECTED_PLANK, ItemID.TEAK_PLANK);
        for (Integer option : plankComboBox.getItems()) {
            if (option.equals(savedPlank)) {
                plankComboBox.getSelectionModel().select(option);
                break;
            }
        }
        script.log("SAVESETTINGS", "Loaded saved plank ID: " + savedPlank);

        mainBox.getChildren().addAll(locationLabel, locationComboBox, ringOfElementsCheckBox, plankLabel, plankComboBox);
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
        prefs.put(PREF_SELECTED_LOCATION, getSelectedLocation().name());
        prefs.putInt(PREF_SELECTED_PLANK, getSelectedPlank());
        prefs.putBoolean(PREF_USE_RING_OF_ELEMENTS, isRingOfElementsEnabled());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        script.log("SAVESETTINGS", "Saved location, plank, ring, and webhook config.");
        ((Stage) locationComboBox.getScene().getWindow()).close();
    }

    private ComboBox<Integer> createPlankComboBox(ScriptCore core) {
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

        comboBox.getItems().addAll(
                ItemID.MAHOGANY_PLANK,
                ItemID.TEAK_PLANK,
                ItemID.OAK_PLANK,
                ItemID.PLANK
        );
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

    // === Getters ===
    public Location getSelectedLocation() {
        return locationComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedPlank() {
        return plankComboBox.getSelectionModel().getSelectedItem();
    }

    public boolean isRingOfElementsEnabled() {
        return ringOfElementsCheckBox != null && ringOfElementsCheckBox.isVisible() && ringOfElementsCheckBox.isSelected();
    }

    public int getLogs() {
        int plank = getSelectedPlank();
        return switch (plank) {
            case ItemID.MAHOGANY_PLANK -> ItemID.MAHOGANY_LOGS;
            case ItemID.TEAK_PLANK -> ItemID.TEAK_LOGS;
            case ItemID.OAK_PLANK -> ItemID.OAK_LOGS;
            case ItemID.PLANK -> ItemID.LOGS;
            default -> ItemID.LOGS;
        };
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