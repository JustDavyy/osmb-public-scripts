package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import data.CookingItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private static final String PREF_SELECTION_MODE = "dcooker_selection_mode";
    private static final String PREF_SELECTED_ITEM = "dcooker_selected_item";
    private static final String PREF_SELECTED_MULTIPLE_ITEMS = "dcooker_selected_multiple_items";
    private static final String PREF_BANK_METHOD = "dcooker_bank_method";
    private static final String PREF_WEBHOOK_ENABLED = "dcooker_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dcooker_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dcooker_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dcooker_webhook_include_user";

    private final Script script;
    private ComboBox<String> selectionModeComboBox;
    private ComboBox<String> bankMethodComboBox;

    private ComboBox<CookingItem> singleItemComboBox;
    private ListView<Integer> multipleItemsListView;
    private final ObservableList<Integer> multipleItemIds = FXCollections.observableArrayList();

    private VBox itemSelectionBox;

    private CookingItem pendingSingleSelect = null;

    // Webhook UI
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final String[] BANK_METHOD_OPTIONS = {"Item by item", "Deposit all"};

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");
        mainBox.setFillWidth(true);

        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Item", "Multiple Items");
        selectionModeComboBox.getSelectionModel().select(prefs.get(PREF_SELECTION_MODE, "Single Item"));

        itemSelectionBox = new VBox(8);
        itemSelectionBox.setStyle("-fx-alignment: center");
        itemSelectionBox.setFillWidth(true);
        updateItemSelectionUI(core);
        selectionModeComboBox.setOnAction(e -> {
            boolean toMultiple = isMultipleSelectionMode();

            if (toMultiple) {
                if (singleItemComboBox != null) {
                    CookingItem sel = singleItemComboBox.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        int id = sel.getRawItemId();
                        if (!multipleItemIds.contains(id)) {
                            multipleItemIds.add(id);
                        }
                    }
                }
            } else {
                if (!multipleItemIds.isEmpty()) {
                    CookingItem ci = CookingItem.fromRawItemId(multipleItemIds.get(0));
                    if (ci != null) {
                        pendingSingleSelect = ci;
                    }
                }
            }

            updateItemSelectionUI(core);
        });

        Label bankLabel = new Label("Bank method");
        bankMethodComboBox = new ComboBox<>();
        bankMethodComboBox.getItems().addAll(BANK_METHOD_OPTIONS);
        bankMethodComboBox.getSelectionModel().select(prefs.get(PREF_BANK_METHOD, BANK_METHOD_OPTIONS[0]));

        mainBox.getChildren().addAll(modeLabel, selectionModeComboBox, itemSelectionBox, bankLabel, bankMethodComboBox);
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
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);
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
        Scene scene = new Scene(layout, 300, 380);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        prefs.put(PREF_SELECTION_MODE, selectionModeComboBox.getSelectionModel().getSelectedItem());
        prefs.put(PREF_BANK_METHOD, bankMethodComboBox.getSelectionModel().getSelectedItem());
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        if (isMultipleSelectionMode()) {
            String joined = multipleItemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            prefs.put(PREF_SELECTED_MULTIPLE_ITEMS, joined);
            script.log("SAVESETTINGS", "Saved multiple cooking item IDs: " + joined);
        } else {
            CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                prefs.putInt(PREF_SELECTED_ITEM, selected.getRawItemId());
                script.log("SAVESETTINGS", "Saved single cooking item ID: " + selected.getRawItemId());
            }
        }

        ((Stage) bankMethodComboBox.getScene().getWindow()).close();
    }

    private void updateItemSelectionUI(ScriptCore core) {
        itemSelectionBox.getChildren().clear();
        if (isMultipleSelectionMode()) {
            setupMultipleSelection(core);
        } else {
            setupSingleSelection(core);
        }

        javafx.application.Platform.runLater(() -> {
            itemSelectionBox.requestLayout();
            itemSelectionBox.autosize();
        });
    }

    private void setupSingleSelection(ScriptCore core) {
        Label label = new Label("Select item to cook");

        singleItemComboBox = new ComboBox<>();
        singleItemComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CookingItem item) {
                return item != null ? core.getItemManager().getItemName(item.getRawItemId()) : "";
            }

            @Override
            public CookingItem fromString(String string) {
                return null;
            }
        });
        singleItemComboBox.setCellFactory(param -> createItemCell(core));
        singleItemComboBox.setButtonCell(createItemCell(core));
        singleItemComboBox.getItems().addAll(CookingItem.values());

        int savedId = prefs.getInt(PREF_SELECTED_ITEM, ItemID.RAW_SHRIMPS);
        CookingItem savedItem = CookingItem.fromRawItemId(savedId);
        if (savedItem != null) {
            singleItemComboBox.getSelectionModel().select(savedItem);
        }

        itemSelectionBox.getChildren().addAll(label, singleItemComboBox);

        // If we're returning from Multiple mode, prefer the carried selection
        if (pendingSingleSelect != null) {
            singleItemComboBox.getSelectionModel().select(pendingSingleSelect);
            pendingSingleSelect = null;
        }
    }

    private void setupMultipleSelection(ScriptCore core) {
        Label label = new Label("Select multiple items to cook");

        multipleItemsListView = new ListView<>(multipleItemIds);
        multipleItemsListView.setMinHeight(75);
        multipleItemsListView.setPrefHeight(280);
        multipleItemsListView.setMaxWidth(Double.MAX_VALUE);
        multipleItemsListView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(multipleItemsListView, javafx.scene.layout.Priority.ALWAYS);
        multipleItemsListView.prefWidthProperty().bind(itemSelectionBox.widthProperty());
        multipleItemsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    String name = core.getItemManager().getItemName(itemId);
                    ImageView img = JavaFXUtils.getItemImageView(core, itemId);
                    if (img != null) {
                        img.setFitWidth(16);
                        img.setFitHeight(16);
                    }
                    setGraphic(img);
                    setText(name != null ? name : "Unknown");
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });

        String saved = prefs.get(PREF_SELECTED_MULTIPLE_ITEMS, "");
        if (!saved.isEmpty()) {
            try {
                List<Integer> ids = Arrays.stream(saved.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                multipleItemIds.setAll(ids);
            } catch (Exception e) {
                script.log("ERROR", "Failed to parse multiple item IDs: " + saved);
            }
        }

        Button addButton = new Button("➕ Add Item");
        addButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) addButton.getScene().getWindow());
            if (itemID != -1 && !multipleItemIds.contains(itemID)) {
                multipleItemIds.add(itemID);
            }
        });

        Button removeButton = new Button("❌ Remove Selected");
        removeButton.setOnAction(event -> {
            Integer selected = multipleItemsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                multipleItemIds.remove(selected);
            }
        });

        HBox buttons = new HBox(addButton, removeButton);
        buttons.setSpacing(10);
        buttons.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(label, multipleItemsListView, buttons);

        if (!multipleItemIds.isEmpty()) {
            multipleItemsListView.getSelectionModel().select(0);
        }
    }

    private ListCell<CookingItem> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(CookingItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    String name = core.getItemManager().getItemName(item.getRawItemId());
                    ImageView imageView = JavaFXUtils.getItemImageView(core, item.getRawItemId());
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

    public boolean isMultipleSelectionMode() {
        return "Multiple Items".equals(selectionModeComboBox.getSelectionModel().getSelectedItem());
    }

    public int getSelectedItemId() {
        CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getRawItemId() : -1;
    }

    public int getSelectedCookedItemId() {
        CookingItem selected = singleItemComboBox.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getCookedItemId() : -1;
    }

    public ObservableList<Integer> getMultipleSelectedItemIds() {
        return multipleItemIds;
    }

    public String getSelectedBankMethod() {
        return bankMethodComboBox.getSelectionModel().getSelectedItem();
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