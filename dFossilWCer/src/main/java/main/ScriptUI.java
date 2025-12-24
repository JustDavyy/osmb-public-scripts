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
    private final Preferences prefs = Preferences.userRoot().node("main");

    private static final String PREF_SELECTED_TREE = "dfossilwcer_selected_tree";
    private static final String PREF_MODE = "dfossilwcer_mode";
    private static final String PREF_KEEP_CLUES = "dfossilwcer_keep_clues";
    private static final String PREF_TREE_COUNT = "dfossilwcer_tree_count";

    private static final String PREF_WEBHOOK_ENABLED = "dfossilwcer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dfossilwcer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dfossilwcer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dfossilwcer_webhook_include_user";

    private final Script script;

    private ComboBox<Integer> treeComboBox;
    private ComboBox<String> modeComboBox;
    private ComboBox<Integer> treeCountComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final Integer[] TREE_OPTIONS = {
            ItemID.TEAK_LOGS,
            ItemID.MAHOGANY_LOGS,
            32904, // Camphor logs
            32907, // Ironwood logs
            32910, // Rosewood logs
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        // Tree type dropdown
        Label treeLabel = new Label("Select Tree Type");
        treeComboBox = createItemComboBox(core, TREE_OPTIONS);
        int savedTree = prefs.getInt(PREF_SELECTED_TREE, ItemID.TEAK_LOGS); // Default to Teak logs
        for (Integer option : TREE_OPTIONS) {
            if (option.equals(savedTree)) {
                treeComboBox.getSelectionModel().select(option);
                break;
            }
        }

        // Mode dropdown
        Label modeLabel = new Label("Mode");
        modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll("Drop", "Bank");
        modeComboBox.getSelectionModel().select(prefs.get(PREF_MODE, "Bank")); // Default to Bank

        // Trees planted dropdown
        Label treeCountLabel = new Label("Trees Planted");
        treeCountComboBox = new ComboBox<>();
        treeCountComboBox.getItems().addAll(1, 2, 3);

        // Load saved value (default = 3)
        treeCountComboBox.getSelectionModel().select(
                Integer.valueOf(prefs.getInt(PREF_TREE_COUNT, 3))
        );

        mainBox.getChildren().addAll(treeLabel, treeComboBox, treeCountLabel, treeCountComboBox, modeLabel, modeComboBox);
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

        Scene scene = new Scene(layout, 320, 360);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createItemComboBox(ScriptCore core, Integer[] items) {
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
        comboBox.getItems().addAll(items);
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
        Integer selectedTree = getSelectedTree();
        if (selectedTree != null) {
            prefs.putInt(PREF_SELECTED_TREE, selectedTree);
        }
        prefs.put(PREF_MODE, getMode());
        prefs.putInt(PREF_TREE_COUNT, getTreeCount());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) modeComboBox.getScene().getWindow()).close();
    }

    // Getters
    public int getTreeCount() {
        return treeCountComboBox != null && treeCountComboBox.getValue() != null
                ? treeCountComboBox.getValue()
                : 3;
    }

    public Integer getSelectedTree() {
        return treeComboBox.getSelectionModel().getSelectedItem();
    }

    public String getMode() {
        return modeComboBox.getSelectionModel().getSelectedItem();
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
