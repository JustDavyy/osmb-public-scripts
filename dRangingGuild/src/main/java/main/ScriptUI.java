package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);
    private final Script script;
    private static ScriptUI instance;

    private static final String[] LEVELS = { "50", "60", "70", "77" };
    private static final String[] SLOTS = { "weapon", "gloves", "body", "legs" };
    private static final int NONE = -1;

    private final ComboBox<Integer>[][] gearSelections = new ComboBox[LEVELS.length][SLOTS.length];

    private static final String PREF_WEBHOOK_ENABLED = "drangingguild_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "drangingguild_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "drangingguild_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "drangingguild_webhook_include_user";

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public ScriptUI(Script script) {
        this.script = script;
        instance = this;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        VBox gearBox = new VBox(15);
        gearBox.setStyle("-fx-background-color: #2d3436; -fx-padding: 15; -fx-alignment: center;");

        Label gearDescription = new Label("Select the gear you'd like to switch to when reaching\nlevel thresholds, make sure to have them in your inventory.");
        gearDescription.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");
        gearDescription.setMaxWidth(300);
        gearDescription.setWrapText(true);
        gearBox.getChildren().add(gearDescription);

        for (int i = 0; i < LEVELS.length; i++) {
            String level = LEVELS[i];
            VBox levelBox = new VBox(5);
            levelBox.setStyle("-fx-alignment: center;");

            Label levelLabel = new Label("Ranged Level " + level);
            levelLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
            levelBox.getChildren().add(levelLabel);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);

            int gridRow = 0;
            int gridCol = 0;

            for (int j = 0; j < SLOTS.length; j++) {
                String slot = SLOTS[j];

                if ((level.equals("70") && slot.equals("weapon")) ||
                        (level.equals("77") && !slot.equals("weapon"))) {
                    continue;
                }

                VBox slotBox = new VBox(2);
                Label slotLabel = new Label(capitalize(slot));
                slotLabel.setStyle("-fx-text-fill: lightgray;");
                Integer[] options = getOptionsForLevelAndSlot(level, slot);
                ComboBox<Integer> comboBox = createItemComboBox(core, options);

                comboBox.setPrefWidth(140);

                String prefKey = "drangingguild_gear_level_" + level + "_" + slot;
                int saved = prefs.getInt(prefKey, NONE);
                boolean found = false;
                for (Integer item : comboBox.getItems()) {
                    if (item != null && item.equals(saved)) {
                        comboBox.getSelectionModel().select(item);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    comboBox.getSelectionModel().select(NONE);
                }

                slotBox.getChildren().addAll(slotLabel, comboBox);
                grid.add(slotBox, gridCol, gridRow);

                gearSelections[i][j] = comboBox;

                if (++gridCol >= 2) {
                    gridCol = 0;
                    gridRow++;
                }
            }

            levelBox.getChildren().add(grid);
            gearBox.getChildren().add(levelBox);
        }

        tabPane.getTabs().add(new Tab("Gear", gearBox));

        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #2d3436; -fx-padding: 15; -fx-alignment: center;");

        webhookEnabledCheckBox = new CheckBox("Enable Webhooks");
        webhookEnabledCheckBox.setStyle("-fx-text-fill: white;");
        webhookEnabledCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_ENABLED, false));

        webhookUrlField = new TextField(prefs.get(PREF_WEBHOOK_URL, ""));
        webhookUrlField.setPromptText("Enter Webhook URL...");

        webhookIntervalComboBox = new ComboBox<>();
        for (int i = 1; i <= 60; i++) webhookIntervalComboBox.getItems().add(i);
        webhookIntervalComboBox.getSelectionModel().select(prefs.getInt(PREF_WEBHOOK_INTERVAL, 5) - 1);
        webhookIntervalComboBox.setStyle("-fx-text-fill: white;");

        includeUsernameCheckBox = new CheckBox("Include Username");
        includeUsernameCheckBox.setStyle("-fx-text-fill: white;");
        includeUsernameCheckBox.setSelected(prefs.getBoolean(PREF_WEBHOOK_INCLUDE_USER, true));

        Label intervalLabel = new Label("Send interval (minutes)");
        intervalLabel.setStyle("-fx-text-fill: white;");

        webhookEnabledCheckBox.setOnAction(e -> {
            boolean enabled = webhookEnabledCheckBox.isSelected();
            webhookUrlField.setDisable(!enabled);
            webhookIntervalComboBox.setDisable(!enabled);
            includeUsernameCheckBox.setDisable(!enabled);
        });

        webhookBox.getChildren().addAll(webhookEnabledCheckBox, webhookUrlField,
                intervalLabel, webhookIntervalComboBox,
                includeUsernameCheckBox);

        tabPane.getTabs().add(new Tab("Webhooks", webhookBox));

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");
        layout.getStylesheets().add("style.css");
        return new Scene(layout, 340, 650);
    }

    private ComboBox<Integer> createItemComboBox(ScriptCore core, Integer[] options) {
        ComboBox<Integer> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(options);

        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer itemId) {
                return itemId == null || itemId == NONE ? "None" : core.getItemManager().getItemName(itemId);
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        comboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (itemId == null || empty) {
                    setText(null);
                    setGraphic(null);
                } else if (itemId == NONE) {
                    setText("None");
                    setGraphic(null);
                } else {
                    ImageView icon = JavaFXUtils.getItemImageView(core, itemId);
                    if (icon != null) {
                        icon.setFitWidth(16);
                        icon.setFitHeight(16);
                    }
                    setText(core.getItemManager().getItemName(itemId));
                    setGraphic(icon);
                }
            }
        });

        comboBox.setButtonCell(comboBox.getCellFactory().call(null));
        return comboBox;
    }

    private void saveSettings() {
        for (int i = 0; i < LEVELS.length; i++) {
            for (int j = 0; j < SLOTS.length; j++) {
                String slot = SLOTS[j];

                if ((LEVELS[i].equals("70") && slot.equals("weapon")) ||
                        (LEVELS[i].equals("77") && !slot.equals("weapon"))) {
                    continue;
                }

                ComboBox<Integer> cb = gearSelections[i][j];
                if (cb == null) continue;

                Integer selected = cb.getSelectionModel().getSelectedItem();
                String prefKey = "drangingguild_gear_level_" + LEVELS[i] + "_" + slot;
                prefs.putInt(prefKey, selected != null ? selected : NONE);
            }
        }

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) webhookEnabledCheckBox.getScene().getWindow()).close();
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public boolean isWebhookEnabled() { return webhookEnabledCheckBox.isSelected(); }
    public String getWebhookUrl() { return webhookUrlField.getText().trim(); }
    public int getWebhookInterval() { return webhookIntervalComboBox.getValue() != null ? webhookIntervalComboBox.getValue() : 5; }
    public boolean isUsernameIncluded() { return includeUsernameCheckBox.isSelected(); }

    private Integer[] getOptionsForLevelAndSlot(String level, String slot) {
        return switch (level) {
            case "50" -> switch (slot) {
                case "weapon" -> new Integer[]{ NONE, ItemID.MAGIC_LONGBOW, ItemID.MAGIC_SHORTBOW };
                case "gloves" -> new Integer[]{ NONE, ItemID.BLUE_DHIDE_VAMBRACES };
                case "body" -> new Integer[]{ NONE, ItemID.BLUE_DHIDE_BODY };
                case "legs" -> new Integer[]{ NONE, ItemID.BLUE_DHIDE_CHAPS };
                default -> new Integer[]{ NONE };
            };
            case "60" -> switch (slot) {
                case "weapon" -> new Integer[]{ NONE, ItemID.DARK_BOW };
                case "gloves" -> new Integer[]{ NONE, ItemID.RED_DHIDE_VAMBRACES };
                case "body" -> new Integer[]{ NONE, ItemID.RED_DHIDE_BODY };
                case "legs" -> new Integer[]{ NONE, ItemID.RED_DHIDE_CHAPS };
                default -> new Integer[]{ NONE };
            };
            case "70" -> switch (slot) {
                case "gloves" -> new Integer[]{ NONE, ItemID.BLACK_DHIDE_VAMBRACES };
                case "body" -> new Integer[]{ NONE, ItemID.BLACK_DHIDE_BODY };
                case "legs" -> new Integer[]{ NONE, ItemID.BLACK_DHIDE_CHAPS };
                default -> new Integer[]{ NONE };
            };
            case "77" -> "weapon".equals(slot) ? new Integer[]{ NONE, ItemID.SCORCHING_BOW } : new Integer[]{ NONE };
            default -> new Integer[]{ NONE };
        };
    }

    public static int getSelectedGearItemId(String level, String slot) {
        if (instance == null) return -1;

        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equals(level)) {
                for (int j = 0; j < SLOTS.length; j++) {
                    if (SLOTS[j].equals(slot)) {
                        ComboBox<Integer> cb = instance.gearSelections[i][j];
                        return cb != null && cb.getValue() != null ? cb.getValue() : NONE;
                    }
                }
            }
        }
        return NONE;
    }
}