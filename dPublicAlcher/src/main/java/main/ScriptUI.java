package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.StandardSpellbook;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userRoot().node("main");
    private static final String PREF_SELECTED_SPELL = "dpublicalcher_selected_alch_spell";
    private static final String PREF_SELECTED_ITEM_ID = "dpublicalcher_selected_item_id";
    private static final String PREF_SELECTED_MULTIPLE_ITEMS = "dpublicalcher_selected_multiple_item_ids";
    private static final String PREF_SELECTION_MODE = "dpublicalcher_selection_mode";
    private static final String PREF_FAST_ALCH = "dpublicalcher_fast_alch";
    private static final String PREF_WEBHOOK_ENABLED = "dpublicalcher_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dpublicalcher_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dpublicalcher_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dpublicalcher_webhook_include_user";

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private final Script script;
    private ComboBox<StandardSpellbook> spellComboBox;
    private ComboBox<String> selectionModeComboBox;
    private ImageView itemToAlchView;
    private ListView<Integer> multipleItemsView;
    private CheckBox fastAlchCheckBox;

    private int selectedItemID = ItemID.BANK_FILLER;
    private final ObservableList<Integer> multipleSelectedItemIDs = FXCollections.observableArrayList();

    private VBox itemSelectionBox;

    private static final StandardSpellbook[] ALCHEMY_SPELLS = {
            StandardSpellbook.LOW_ALCHEMY,
            StandardSpellbook.HIGH_LEVEL_ALCHEMY
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(ALCHEMY_SPELLS);
        spellComboBox.getSelectionModel().select(StandardSpellbook.valueOf(prefs.get(PREF_SELECTED_SPELL, StandardSpellbook.HIGH_LEVEL_ALCHEMY.name())));

        Label modeLabel = new Label("Selection mode");
        selectionModeComboBox = new ComboBox<>();
        selectionModeComboBox.getItems().addAll("Single Item", "Multiple Items");
        selectionModeComboBox.getSelectionModel().select(prefs.get(PREF_SELECTION_MODE, "Single Item"));

        itemSelectionBox = new VBox(8);
        itemSelectionBox.setStyle("-fx-alignment: center");
        updateItemSelectionUI(core);
        selectionModeComboBox.setOnAction(e -> updateItemSelectionUI(core));

        fastAlchCheckBox = new CheckBox("Enable faster alching");
        fastAlchCheckBox.setSelected(prefs.getBoolean(PREF_FAST_ALCH, false));

        mainBox.getChildren().addAll(spellLabel, spellComboBox, modeLabel, selectionModeComboBox, itemSelectionBox, fastAlchCheckBox);
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

        // === Final Scene Setup ===
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 300, 420);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void updateItemSelectionUI(ScriptCore core) {
        itemSelectionBox.getChildren().clear();
        if (isMultipleSelectionMode()) {
            setupMultipleItemSelection(core);
        } else {
            setupSingleItemSelection(core);
        }
    }

    private void setupSingleItemSelection(ScriptCore core) {
        Label itemLabel = new Label("Item to alch");

        selectedItemID = prefs.getInt(PREF_SELECTED_ITEM_ID, ItemID.BANK_FILLER);

        itemToAlchView = JavaFXUtils.getItemImageView(core, selectedItemID);
        if (itemToAlchView == null) {
            itemToAlchView = new ImageView();
        }
        itemToAlchView.setFitWidth(32);
        itemToAlchView.setFitHeight(32);

        Button searchButton = new Button("\uD83D\uDD0E Search");
        searchButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) searchButton.getScene().getWindow());
            if (itemID != -1) {
                selectedItemID = itemID;
                ImageView newImage = JavaFXUtils.getItemImageView(core, itemID);
                if (newImage != null) {
                    itemToAlchView.setImage(newImage.getImage());
                }
            }
        });

        HBox hbox = new HBox(itemToAlchView, searchButton);
        hbox.setSpacing(8);
        hbox.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(itemLabel, hbox);
    }

    private void setupMultipleItemSelection(ScriptCore core) {
        Label itemsLabel = new Label("Items to alch");

        multipleItemsView = new ListView<>(multipleSelectedItemIDs);
        multipleItemsView.setPrefHeight(320);
        multipleItemsView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Integer> call(ListView<Integer> param) {
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
                            setMaxWidth(250);
                            setStyle("-fx-alignment: center-left;");
                        } else {
                            setText(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        String savedMultiple = prefs.get(PREF_SELECTED_MULTIPLE_ITEMS, "");
        if (!savedMultiple.isEmpty()) {
            try {
                List<Integer> ids = Arrays.stream(savedMultiple.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
                multipleSelectedItemIDs.setAll(ids);
                script.log("SAVESETTINGS", "Loaded multiple selected items: " + savedMultiple);
            } catch (Exception e) {
                script.log("ERROR", "Failed parsing multiple item IDs: " + savedMultiple);
            }
        }

        Button addItemButton = new Button("\uD83D\uDD0E Add Item");
        addItemButton.setOnAction(event -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) addItemButton.getScene().getWindow());
            if (itemID != -1 && !multipleSelectedItemIDs.contains(itemID)) {
                multipleSelectedItemIDs.add(itemID);
            }
        });

        Button removeItemButton = new Button("Remove Selected");
        removeItemButton.setOnAction(event -> {
            Integer selectedItem = multipleItemsView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                multipleSelectedItemIDs.remove(selectedItem);
            }
        });

        HBox buttonBox = new HBox(addItemButton, removeItemButton);
        buttonBox.setSpacing(8);
        buttonBox.setStyle("-fx-alignment: center");

        itemSelectionBox.getChildren().addAll(itemsLabel, multipleItemsView, buttonBox);
    }

    private void saveSettings() {
        if (spellComboBox.getSelectionModel().isEmpty()) return;

        prefs.put(PREF_SELECTED_SPELL, spellComboBox.getSelectionModel().getSelectedItem().name());
        prefs.put(PREF_SELECTION_MODE, selectionModeComboBox.getSelectionModel().getSelectedItem()); // << Save mode

        if (isMultipleSelectionMode()) {
            String joinedIds = multipleSelectedItemIDs.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            prefs.put(PREF_SELECTED_MULTIPLE_ITEMS, joinedIds);
            script.log("SAVESETTINGS", "Saved multiple selected items: " + joinedIds);
        } else {
            prefs.putInt(PREF_SELECTED_ITEM_ID, selectedItemID);
            script.log("SAVESETTINGS", "Saved selected single item ID: " + selectedItemID);
        }
        prefs.putBoolean(PREF_FAST_ALCH, isFastAlchEnabled());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) itemSelectionBox.getScene().getWindow()).close();
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemId() {
        return selectedItemID;
    }

    public ObservableList<Integer> getMultipleSelectedItemIds() {
        return multipleSelectedItemIDs;
    }

    public boolean isMultipleSelectionMode() {
        return selectionModeComboBox.getSelectionModel().getSelectedItem().equals("Multiple Items");
    }

    public boolean isFastAlchEnabled() {
        return fastAlchCheckBox != null && fastAlchCheckBox.isSelected();
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
