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

    private static final String PREF_MODE = "amethystminer_mode";
    private static final String PREF_AMETHYST_PRODUCT = "amethystminer_selected_product";
    private static final String PREF_AREA_MODE = "amethystminer_area_mode";

    private static final String PREF_WEBHOOK_ENABLED = "amethystminer_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "amethystminer_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "amethystminer_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "amethystminer_webhook_include_user";

    private final Script script;
    private ComboBox<String> modeComboBox;
    private ComboBox<Integer> productComboBox;
    private ComboBox<String> areaComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final Integer[] AMETHYST_PRODUCTS = {
            ItemID.AMETHYST_BOLT_TIPS,
            ItemID.AMETHYST_JAVELIN_HEADS,
            ItemID.AMETHYST_DART_TIP,
            ItemID.AMETHYST_ARROWTIPS
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll("Bank", "Craft");
        modeComboBox.getSelectionModel().select(prefs.get(PREF_MODE, "Bank"));

        productComboBox = createProductComboBox(core);
        int savedProductId = prefs.getInt(PREF_AMETHYST_PRODUCT, ItemID.AMETHYST_BOLT_TIPS);
        for (Integer product : AMETHYST_PRODUCTS) {
            if (product == savedProductId) {
                productComboBox.getSelectionModel().select(product);
                break;
            }
        }

        // Craft section container
        VBox craftSection = new VBox(5);
        craftSection.setStyle("-fx-alignment: center;");
        craftSection.getChildren().addAll(new Label("Craft into"), productComboBox);
        craftSection.setVisible("Craft".equals(modeComboBox.getValue()));

        // Toggle craft section visibility when mode changes
        modeComboBox.setOnAction(e -> craftSection.setVisible("Craft".equals(modeComboBox.getValue())));

        areaComboBox = new ComboBox<>();
        areaComboBox.getItems().addAll("Normal", "Diary");
        areaComboBox.getSelectionModel().select(prefs.get(PREF_AREA_MODE, "Normal"));

        mainBox.getChildren().addAll(
                new Label("Mode"), modeComboBox,
                craftSection,
                new Label("Area"), areaComboBox
        );
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

        Scene scene = new Scene(layout, 320, 340);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private ComboBox<Integer> createProductComboBox(ScriptCore core) {
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
        comboBox.getItems().addAll(AMETHYST_PRODUCTS);
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
        prefs.put(PREF_MODE, getMode());
        prefs.put(PREF_AREA_MODE, getSelectedAreaMode());

        Integer selectedProduct = getSelectedAmethystProductId();
        if (selectedProduct != null) {
            prefs.putInt(PREF_AMETHYST_PRODUCT, selectedProduct);
        }

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) modeComboBox.getScene().getWindow()).close();
    }

    // Getters
    public String getMode() {
        return modeComboBox.getSelectionModel().getSelectedItem();
    }

    public String getSelectedAreaMode() {
        return areaComboBox != null ? areaComboBox.getSelectionModel().getSelectedItem() : "Normal";
    }

    public Integer getSelectedAmethystProductId() {
        return productComboBox.getSelectionModel().getSelectedItem();
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