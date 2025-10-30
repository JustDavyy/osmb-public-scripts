package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import com.osmb.api.ui.spellbook.ArceuusSpellbook;
import com.osmb.api.ui.spellbook.Spell;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_SELECTED_SPELL = "doffering_selected_spell";
    private static final String PREF_SELECTED_ITEM = "doffering_selected_item";

    private static final String PREF_WEBHOOK_ENABLED = "doffering_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "doffering_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "doffering_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "doffering_webhook_include_user";

    private final Script script;

    private ComboBox<String> spellComboBox;
    private ComboBox<Integer> itemComboBox;

    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    private static final String DEMONIC_OFFERING = "Demonic Offering";
    private static final String SINISTER_OFFERING = "Sinister Offering";

    private static final Map<Integer, Integer> DEMONIC_ITEMS_XP = new HashMap<>() {{
        put(ItemID.FIENDISH_ASHES, 30);
        put(ItemID.VILE_ASHES, 75);
        put(ItemID.MALICIOUS_ASHES, 195);
        put(ItemID.ABYSSAL_ASHES, 255);
        put(ItemID.INFERNAL_ASHES, 330);
    }};

    private static final Map<Integer, Integer> SINISTER_ITEMS_XP = new HashMap<>() {{
        put(ItemID.BONES, 13);
        put(ItemID.MONKEY_BONES, 15);
        put(ItemID.BAT_BONES, 15);
        put(ItemID.BIG_BONES, 45);
        put(ItemID.JOGRE_BONES, 45);
        put(ItemID.WYRMLING_BONES, 63);
        put(ItemID.ZOGRE_BONES, 67);
        put(ItemID.SHAIKAHAN_BONES, 75);
        put(ItemID.BABYDRAGON_BONES, 90);
        put(ItemID.WYRM_BONES, 150);
        put(ItemID.DRAGON_BONES, 216);
        put(ItemID.WYVERN_BONES, 216);
        put(ItemID.DRAKE_BONES, 240);
        put(ItemID.FAYRG_BONES, 252);
        put(ItemID.LAVA_DRAGON_BONES, 255);
        put(ItemID.RAURG_BONES, 288);
        put(ItemID.HYDRA_BONES, 330);
        put(ItemID.DAGANNOTH_BONES, 375);
        put(ItemID.OURG_BONES, 420);
        put(ItemID.SUPERIOR_DRAGON_BONES, 450);
    }};

    private static LinkedHashMap<Integer, Integer> sortItemsByXp(Map<Integer, Integer> items) {
        return items.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        // Spell dropdown
        Label spellLabel = new Label("Select Offering Spell");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(DEMONIC_OFFERING, SINISTER_OFFERING);

        // Item dropdown
        Label itemLabel = new Label("Select Item to Offer");
        itemComboBox = createItemComboBox(core);

        // When spell is selected (either on launch or by user), populate items accordingly
        spellComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateItems(newVal);
            }
        });

        // Load and apply saved spell (triggers listener above)
        String savedSpell = prefs.get(PREF_SELECTED_SPELL, SINISTER_OFFERING);
        spellComboBox.getSelectionModel().select(savedSpell);

        mainBox.getChildren().addAll(spellLabel, spellComboBox, itemLabel, itemComboBox);
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

        Scene scene = new Scene(layout, 320, 375);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void populateItems(String spell) {
        itemComboBox.getItems().clear();

        Map<Integer, Integer> itemsMap;
        String itemPrefKey;
        int defaultItem;

        if (DEMONIC_OFFERING.equals(spell)) {
            itemsMap = DEMONIC_ITEMS_XP;
            itemPrefKey = PREF_SELECTED_ITEM + "_demonic";
            defaultItem = ItemID.VILE_ASHES;
        } else {
            itemsMap = SINISTER_ITEMS_XP;
            itemPrefKey = PREF_SELECTED_ITEM + "_sinister";
            defaultItem = ItemID.DRAGON_BONES;
        }

        LinkedHashMap<Integer, Integer> sortedItems = sortItemsByXp(itemsMap);
        itemComboBox.getItems().addAll(sortedItems.keySet());

        int savedItem = prefs.getInt(itemPrefKey, defaultItem);

        // Use index-based fallback if select() fails
        boolean selected = false;
        for (int i = 0; i < itemComboBox.getItems().size(); i++) {
            if (itemComboBox.getItems().get(i).equals(savedItem)) {
                itemComboBox.getSelectionModel().select(i);
                selected = true;
                break;
            }
        }

        if (!selected) {
            itemComboBox.getSelectionModel().selectFirst();
        }
    }

    private ComboBox<Integer> createItemComboBox(ScriptCore core) {
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
        prefs.put(PREF_SELECTED_SPELL, getSelectedSpell());
        String spell = getSelectedSpell();
        String itemPrefKey = DEMONIC_OFFERING.equals(spell)
                ? PREF_SELECTED_ITEM + "_demonic"
                : PREF_SELECTED_ITEM + "_sinister";
        prefs.putInt(itemPrefKey, getSelectedItem());

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        ((Stage) spellComboBox.getScene().getWindow()).close();
    }

    // === Getters ===
    public String getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public Integer getSelectedItem() {
        return itemComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemXP() {
        if (DEMONIC_OFFERING.equals(getSelectedSpell())) {
            return DEMONIC_ITEMS_XP.getOrDefault(getSelectedItem(), 0);
        } else {
            return SINISTER_ITEMS_XP.getOrDefault(getSelectedItem(), 0);
        }
    }

    public int getXpPerSpellCast() {
        String selectedSpell = getSelectedSpell();
        if (DEMONIC_OFFERING.equals(selectedSpell)) {
            return 175;
        } else {
            return 180;
        }
    }

    public Spell getSpellToCast() {
        String selectedSpell = getSelectedSpell();
        if (DEMONIC_OFFERING.equals(selectedSpell)) {
            script.log(getClass(), "Demonic offering spell is selected to be cast.");
            return ArceuusSpellbook.DEMONIC_OFFERING;
        } else {
            script.log(getClass(), "Sinister offering spell is selected to be cast.");
            return ArceuusSpellbook.SINISTER_OFFERING;
        }
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
