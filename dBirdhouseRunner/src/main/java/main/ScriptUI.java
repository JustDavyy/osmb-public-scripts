package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    // Birdhouse
    private static final String PREF_SELECTED_BIRDHOUSE = "dbirdhouse_selected_tier";
    private static final String PREF_SELECTED_SEED_ID = "dbirdhouse_selected_seed_id";

    // Seaweed
    private static final String PREF_ENABLE_SEAWEED = "dbirdhouse_enable_seaweed";
    private static final String PREF_SELECTED_COMPOST = "dbirdhouse_selected_compost";

    // Webhooks
    private static final String PREF_WEBHOOK_ENABLED = "dbirdhouse_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dbirdhouse_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dbirdhouse_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dbirdhouse_webhook_include_user";
    private static final String PREF_WEBHOOK_INCLUDE_STATS = "dbirdhouse_webhook_include_stats";

    private final Script script;

    // Birdhouse
    private ComboBox<Integer> birdhouseComboBox;
    private int selectedSeedId = ItemID.BARLEY_SEED;
    private ImageView seedImageView;

    // Seaweed
    private CheckBox seaweedCheckbox;
    private ComboBox<String> compostComboBox;

    // Webhooks
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;
    private CheckBox includeStatsCheckBox;

    private static final int[] BIRDHOUSE_TIERS = {
            ItemID.BIRD_HOUSE,
            ItemID.OAK_BIRD_HOUSE,
            ItemID.WILLOW_BIRD_HOUSE,
            ItemID.TEAK_BIRD_HOUSE,
            ItemID.MAPLE_BIRD_HOUSE,
            ItemID.MAHOGANY_BIRD_HOUSE,
            ItemID.YEW_BIRD_HOUSE,
            ItemID.MAGIC_BIRD_HOUSE,
            ItemID.REDWOOD_BIRD_HOUSE
    };

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Birdhouse Tab ===
        VBox birdBox = new VBox(10);
        birdBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        birdhouseComboBox = new ComboBox<>();
        for (int id : BIRDHOUSE_TIERS) birdhouseComboBox.getItems().add(id);
        birdhouseComboBox.setCellFactory(param -> createItemCell(core));
        birdhouseComboBox.setButtonCell(createItemCell(core));
        birdhouseComboBox.getSelectionModel().select((Integer) prefs.getInt(PREF_SELECTED_BIRDHOUSE, ItemID.BIRD_HOUSE));

        seedImageView = JavaFXUtils.getItemImageView(core, selectedSeedId = prefs.getInt(PREF_SELECTED_SEED_ID, ItemID.BARLEY_SEED));
        if (seedImageView == null) seedImageView = new ImageView();
        seedImageView.setFitWidth(32); seedImageView.setFitHeight(32);
        Button seedSearch = new Button("\uD83D\uDD0E Search");
        seedSearch.setOnAction(event -> {
            int id = ItemSearchDialogue.show(core, (Stage) seedSearch.getScene().getWindow());
            if (id != -1) {
                selectedSeedId = id;
                ImageView view = JavaFXUtils.getItemImageView(core, id);
                if (view != null) seedImageView.setImage(view.getImage());
            }
        });
        HBox seedBox = new HBox(seedImageView, seedSearch);
        seedBox.setSpacing(8); seedBox.setStyle("-fx-alignment: center");

        birdBox.getChildren().addAll(new Label("Choose birdhouse tier"), birdhouseComboBox,
                new Label("Seed to feed birdhouse"), seedBox);
        tabPane.getTabs().add(new Tab("Birdhouse", birdBox));

        // === Seaweed Tab ===
        VBox seaweedBox = new VBox(10);
        seaweedBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        seaweedCheckbox = new CheckBox("Enable seaweed runs");
        seaweedCheckbox.setSelected(prefs.getBoolean(PREF_ENABLE_SEAWEED, false));

        compostComboBox = new ComboBox<>();
        compostComboBox.getItems().addAll("None", "Compost", "Supercompost", "Ultracompost");
        compostComboBox.setCellFactory(param -> createCompostCell(core));
        compostComboBox.setButtonCell(createCompostCell(core));
        compostComboBox.getSelectionModel().select(prefs.get(PREF_SELECTED_COMPOST, "Ultracompost")); // default: Ultracompost

        compostComboBox.setDisable(!seaweedCheckbox.isSelected());
        seaweedCheckbox.setOnAction(e -> compostComboBox.setDisable(!seaweedCheckbox.isSelected()));

        seaweedBox.getChildren().addAll(
                seaweedCheckbox,
                new Label("Compost Type"),
                compostComboBox
        );
        tabPane.getTabs().add(new Tab("Seaweed", seaweedBox));

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 15; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5)) - 1);

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));

        includeStatsCheckBox = new CheckBox("Include Stats");
        includeStatsCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_STATS, true));

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
            includeStatsCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(webhookEnabledCheckBox, webhookUrlField,
                new Label("Send interval (minutes)"), webhookIntervalComboBox,
                includeUsernameCheckBox, includeStatsCheckBox);
        tabPane.getTabs().add(new Tab("Webhooks", webhookBox));

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");
        layout.getStylesheets().add("style.css");
        return new Scene(layout, 300, 440);
    }

    private ListCell<Integer> createItemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId != null && !empty) {
                    setGraphic(JavaFXUtils.getItemImageView(core, itemId));
                    setText(core.getItemManager().getItemName(itemId));
                } else {
                    setGraphic(null); setText(null);
                }
            }
        };
    }

    private ListCell<String> createCompostCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (type != null && !empty) {
                    int itemId = switch (type) {
                        case "Compost" -> ItemID.COMPOST;
                        case "Supercompost" -> ItemID.SUPERCOMPOST;
                        case "Ultracompost" -> ItemID.ULTRACOMPOST;
                        default -> ItemID.EMPTY_BUCKET;
                    };
                    String label = type.equals("None") ? "None" : core.getItemManager().getItemName(itemId);
                    ImageView imageView = JavaFXUtils.getItemImageView(core, itemId);
                    if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                    }
                    setGraphic(imageView);
                    setText(label);
                    setStyle("-fx-alignment: center-left;");
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        };
    }

    private void saveSettings() {
        prefs.putInt(PREF_SELECTED_BIRDHOUSE, birdhouseComboBox.getSelectionModel().getSelectedItem());
        prefs.putBoolean(PREF_ENABLE_SEAWEED, seaweedCheckbox.isSelected());
        prefs.putInt(PREF_SELECTED_SEED_ID, selectedSeedId);
        prefs.put(PREF_SELECTED_COMPOST, compostComboBox.getSelectionModel().getSelectedItem());
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_STATS, isStatsIncluded());

        ((Stage) birdhouseComboBox.getScene().getWindow()).close();
    }

    public int getRequiredLogId() {
        return switch (getSelectedBirdhouseTier()) {
            case ItemID.BIRD_HOUSE -> ItemID.LOGS;
            case ItemID.OAK_BIRD_HOUSE -> ItemID.OAK_LOGS;
            case ItemID.WILLOW_BIRD_HOUSE -> ItemID.WILLOW_LOGS;
            case ItemID.TEAK_BIRD_HOUSE -> ItemID.TEAK_LOGS;
            case ItemID.MAPLE_BIRD_HOUSE -> ItemID.MAPLE_LOGS;
            case ItemID.MAHOGANY_BIRD_HOUSE -> ItemID.MAHOGANY_LOGS;
            case ItemID.YEW_BIRD_HOUSE -> ItemID.YEW_LOGS;
            case ItemID.MAGIC_BIRD_HOUSE -> ItemID.MAGIC_LOGS;
            case ItemID.REDWOOD_BIRD_HOUSE -> ItemID.REDWOOD_LOGS;
            default -> ItemID.LOGS;
        };
    }

    public int getSelectedBirdhouseTier() { return birdhouseComboBox.getSelectionModel().getSelectedItem(); }
    public int getSelectedSeedId() { return selectedSeedId; }
    public boolean isSeaweedRunEnabled() { return seaweedCheckbox.isSelected(); }
    public int getCompostId() {
        String selected = compostComboBox.getSelectionModel().getSelectedItem();
        return switch (selected) {
            case "Ultracompost" -> ItemID.ULTRACOMPOST;
            case "Supercompost" -> ItemID.SUPERCOMPOST;
            case "Compost" -> ItemID.COMPOST;
            default -> 0;
        };
    }
    public boolean isWebhookEnabled() { return webhookEnabledCheckBox.isSelected(); }
    public String getWebhookUrl() { return webhookUrlField.getText().trim(); }
    public int getWebhookInterval() { return webhookIntervalComboBox.getValue() != null ? webhookIntervalComboBox.getValue() : 5; }
    public boolean isUsernameIncluded() { return includeUsernameCheckBox.isSelected(); }
    public boolean isStatsIncluded() { return includeStatsCheckBox.isSelected(); }
}