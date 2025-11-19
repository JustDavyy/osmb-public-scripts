package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptUI {

    // === PREFS ===
    private final Preferences prefs = Preferences.userRoot().node("dconstructioneer");
    private static final String PREF_ITEM_TYPE = "dconstructioneer_item_type";
    private static final String PREF_ITEM_TIER = "dconstructioneer_item_tier";
    private static final String PREF_WEBHOOK_ENABLED = "dconstructioneer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dconstructioneer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dconstructioneer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dconstructioneer_webhook_include_user";

    // =========================
    // Construction Item Data
    // =========================

    private static final String[] ITEM_TYPES = {"Hull parts", "Repair kits"};
    private static final String DEFAULT_ITEM_TYPE = "Repair kits";

    private static final String[] TIERS = {
            "Normal", "Oak", "Teak", "Mahogany", "Camphor", "Ironwood", "Rosewood"
    };

    // Hull parts (Normal → Rosewood)
    private static final int[] HULL_PART_IDS = {
            32041, 32044, 32047, 32050, 32053, 32056, 32059
    };

    // Repair kits (Normal → Rosewood)
    private static final int[] REPAIR_KIT_IDS = {
            31964, 31967, 31970, 31973, 31976, 31979, 31982
    };

    private final Script script;

    private ComboBox<String> itemTypeComboBox;
    private ComboBox<String> itemTierComboBox;

    // Webhook UI
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {

        TabPane tabPane = new TabPane();

        // ======================
        // === MAIN TAB UI ===
        // ======================

        VBox mainBox = new VBox(12);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        // Item Type
        Label typeLabel = new Label("Item type to make");
        itemTypeComboBox = new ComboBox<>();
        itemTypeComboBox.getItems().addAll(ITEM_TYPES);
        itemTypeComboBox.getSelectionModel().select(
                prefs.get(PREF_ITEM_TYPE, DEFAULT_ITEM_TYPE)
        );

        // Item Tier
        Label tierLabel = new Label("Item tier");
        itemTierComboBox = new ComboBox<>();
        itemTierComboBox.getItems().addAll(TIERS);
        itemTierComboBox.getSelectionModel().select(
                prefs.get(PREF_ITEM_TIER, TIERS[0])
        );

        // === ICON SUPPORT FOR TYPE & TIER DROPDOWNS ===

        // --- Cell factory for item TYPE dropdown ---
        itemTypeComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);

                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                int tierIndex = itemTierComboBox.getSelectionModel().getSelectedIndex();
                int itemId = getPreviewIconId(type, tierIndex);

                var img = JavaFXUtils.getItemImageView(core, itemId);
                if (img != null) {
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                }

                setText(type);
                setGraphic(img);
            }
        });

        // Show icon when selected (button cell)
        itemTypeComboBox.setButtonCell(itemTypeComboBox.getCellFactory().call(null));

        // --- Cell factory for item TIER dropdown ---
        itemTierComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String tier, boolean empty) {
                super.updateItem(tier, empty);

                if (empty || tier == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                int tierIndex = getIndex();
                String type = itemTypeComboBox.getSelectionModel().getSelectedItem();

                int itemId = getPreviewIconId(type, tierIndex);

                var img = JavaFXUtils.getItemImageView(core, itemId);
                if (img != null) {
                    img.setFitWidth(18);
                    img.setFitHeight(18);
                }

                setText(tier);
                setGraphic(img);
            }
        });

        // Show icon in selected tier cell
        itemTierComboBox.setButtonCell(itemTierComboBox.getCellFactory().call(null));

        // Refresh icons when selection changes
        itemTypeComboBox.setOnAction(e -> {
            itemTierComboBox.setButtonCell(itemTierComboBox.getCellFactory().call(null));
        });

        itemTierComboBox.setOnAction(e -> {
            itemTypeComboBox.setButtonCell(itemTypeComboBox.getCellFactory().call(null));
        });

        mainBox.getChildren().addAll(
                typeLabel, itemTypeComboBox,
                tierLabel, itemTierComboBox
        );

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // ======================
        // === WEBHOOK TAB ===
        // ======================

        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");
        webhookUrlField.setDisable(!webhookEnabledCheckBox.isSelected());

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(
                Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1
        );
        webhookIntervalComboBox.setDisable(!webhookEnabledCheckBox.isSelected());

        includeUsernameCheckBox = new CheckBox("Include Username in Webhook");
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

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 300, 360);
        scene.getStylesheets().add("style.css");

        return scene;
    }

    private void saveSettings() {

        prefs.put(PREF_ITEM_TYPE, itemTypeComboBox.getSelectionModel().getSelectedItem());
        prefs.put(PREF_ITEM_TIER, itemTierComboBox.getSelectionModel().getSelectedItem());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) itemTypeComboBox.getScene().getWindow()).close();
    }

    private int getPreviewIconId(String type, int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIERS.length) return -1;

        if (type.equalsIgnoreCase("Hull parts")) {
            return HULL_PART_IDS[tierIndex];
        } else {
            return REPAIR_KIT_IDS[tierIndex];
        }
    }

    // ======================
    // === GETTERS ===
    // ======================

    public String getSelectedType() {
        return itemTypeComboBox.getSelectionModel().getSelectedItem();
    }

    public String getSelectedTier() {
        return itemTierComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemId() {
        int tierIndex = itemTierComboBox.getSelectionModel().getSelectedIndex();
        if (tierIndex < 0 || tierIndex >= TIERS.length) return -1;

        if ("Hull parts".equals(getSelectedType())) {
            return HULL_PART_IDS[tierIndex];
        } else {
            return REPAIR_KIT_IDS[tierIndex];
        }
    }

    public int getSelectedPlankId() {
        int tierIndex = itemTierComboBox.getSelectionModel().getSelectedIndex();
        if (tierIndex < 0 || tierIndex >= TIERS.length) return -1;

        switch (tierIndex) {
            case 0:  // Normal
                return ItemID.PLANK;
            case 1:  // Oak
                return ItemID.OAK_PLANK;
            case 2:  // Teak
                return ItemID.TEAK_PLANK;
            case 3:  // Mahogany
                return ItemID.MAHOGANY_PLANK;
            case 4:  // Camphor
                return 31432;
            case 5:  // Ironwood
                return 31435;
            case 6:  // Rosewood
                return 31438;
            default:
                return -1;
        }
    }

    public int getSelectedTierNumber() {
        int tierIndex = itemTierComboBox.getSelectionModel().getSelectedIndex();
        if (tierIndex < 0 || tierIndex >= TIERS.length) return -1;
        return tierIndex + 1;
    }

    public int getSelectedNailId() {
        int tierIndex = itemTierComboBox.getSelectionModel().getSelectedIndex();
        if (tierIndex < 0 || tierIndex >= TIERS.length) return -1;

        switch (tierIndex) {
            case 0:  // Normal
                return ItemID.BRONZE_NAILS;
            case 1:  // Oak
                return ItemID.IRON_NAILS;
            case 2:  // Teak
                return ItemID.STEEL_NAILS;
            case 3:  // Mahogany
                return ItemID.MITHRIL_NAILS;
            case 4:  // Camphor
                return ItemID.ADAMANTITE_NAILS;
            case 5:  // Ironwood
                return ItemID.RUNE_NAILS;
            case 6:  // Rosewood
                return 31406;  // Dragon nails
            default:
                return -1;
        }
    }

    // --- Webhook getters ---
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
