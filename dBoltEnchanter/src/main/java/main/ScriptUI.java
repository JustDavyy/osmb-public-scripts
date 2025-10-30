package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptUI.class);

    private static final String PREF_WEBHOOK_ENABLED = "dboltenchanter_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dboltenchanter_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dboltenchanter_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dboltenchanter_webhook_include_user";

    // Persist selected bolt (base ID)
    private static final String PREF_SELECTED_BOLT_ID = "dboltenchanter_selected_bolt_id";

    private final Script script;

    // Webhook UI
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    // Bolt selector UI
    private ComboBox<BoltOption> boltComboBox;
    private ImageView boltImageView;

    public ScriptUI(Script script) {
        this.script = script;
    }

    // ----- Bolt model -----
    private static final class BoltOption {
        final String label;
        final int baseId;
        final int enchantedId;
        final Set<Integer> requiredRunes;
        final double xpPerCast;
        final int magicLevel;

        BoltOption(String label, int baseId, int enchantedId, Set<Integer> requiredRunes, double xpPerCast, int magicLevel) {
            this.label = label;
            this.baseId = baseId;
            this.enchantedId = enchantedId;
            this.requiredRunes = requiredRunes;
            this.xpPerCast = xpPerCast;
            this.magicLevel = magicLevel;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Runes by gem (normal & dragon variants use the same runes)
    private static final Set<Integer> RUNES_OPAL        = Set.of(ItemID.AIR_RUNE,   ItemID.COSMIC_RUNE);
    private static final Set<Integer> RUNES_SAPPHIRE    = Set.of(ItemID.WATER_RUNE, ItemID.COSMIC_RUNE, ItemID.MIND_RUNE);
    private static final Set<Integer> RUNES_JADE        = Set.of(ItemID.EARTH_RUNE, ItemID.COSMIC_RUNE);
    private static final Set<Integer> RUNES_PEARL       = Set.of(ItemID.WATER_RUNE, ItemID.COSMIC_RUNE);
    private static final Set<Integer> RUNES_EMERALD     = Set.of(ItemID.AIR_RUNE,   ItemID.COSMIC_RUNE, ItemID.NATURE_RUNE);
    private static final Set<Integer> RUNES_TOPAZ       = Set.of(ItemID.FIRE_RUNE,  ItemID.COSMIC_RUNE);
    private static final Set<Integer> RUNES_RUBY        = Set.of(ItemID.FIRE_RUNE,  ItemID.COSMIC_RUNE, ItemID.BLOOD_RUNE);
    private static final Set<Integer> RUNES_DIAMOND     = Set.of(ItemID.EARTH_RUNE, ItemID.COSMIC_RUNE, ItemID.LAW_RUNE);
    private static final Set<Integer> RUNES_DRAGONSTONE = Set.of(ItemID.EARTH_RUNE, ItemID.COSMIC_RUNE, ItemID.SOUL_RUNE);
    private static final Set<Integer> RUNES_ONYX        = Set.of(ItemID.FIRE_RUNE,  ItemID.COSMIC_RUNE, ItemID.DEATH_RUNE);

    private static final List<BoltOption> BOLT_OPTIONS = List.of(
            new BoltOption("Opal bolts",               ItemID.OPAL_BOLTS,               ItemID.OPAL_BOLTS_E,               RUNES_OPAL,        9.0,   4),
            new BoltOption("Opal dragon bolts",        ItemID.OPAL_DRAGON_BOLTS,        ItemID.OPAL_DRAGON_BOLTS_E,        RUNES_OPAL,        9.0,   4),

            new BoltOption("Sapphire bolts",           ItemID.SAPPHIRE_BOLTS,           ItemID.SAPPHIRE_BOLTS_E,           RUNES_SAPPHIRE,    17.5,  7),
            new BoltOption("Sapphire dragon bolts",    ItemID.SAPPHIRE_DRAGON_BOLTS,    ItemID.SAPPHIRE_DRAGON_BOLTS_E,    RUNES_SAPPHIRE,    17.5,  7),

            new BoltOption("Jade bolts",               ItemID.JADE_BOLTS,               ItemID.JADE_BOLTS_E,               RUNES_JADE,        19.0, 14),
            new BoltOption("Jade dragon bolts",        ItemID.JADE_DRAGON_BOLTS,        ItemID.JADE_DRAGON_BOLTS_E,        RUNES_JADE,        19.0, 14),

            new BoltOption("Pearl bolts",              ItemID.PEARL_BOLTS,              ItemID.PEARL_BOLTS_E,              RUNES_PEARL,       29.0, 24),
            new BoltOption("Pearl dragon bolts",       ItemID.PEARL_DRAGON_BOLTS,       ItemID.PEARL_DRAGON_BOLTS_E,       RUNES_PEARL,       29.0, 24),

            new BoltOption("Emerald bolts",            ItemID.EMERALD_BOLTS,            ItemID.EMERALD_BOLTS_E,            RUNES_EMERALD,     37.0, 27),
            new BoltOption("Emerald dragon bolts",     ItemID.EMERALD_DRAGON_BOLTS,     ItemID.EMERALD_DRAGON_BOLTS_E,     RUNES_EMERALD,     37.0, 27),

            new BoltOption("Topaz bolts",              ItemID.TOPAZ_BOLTS,              ItemID.TOPAZ_BOLTS_E,              RUNES_TOPAZ,       33.0, 29),
            new BoltOption("Topaz dragon bolts",       ItemID.TOPAZ_DRAGON_BOLTS,       ItemID.TOPAZ_DRAGON_BOLTS_E,       RUNES_TOPAZ,       33.0, 29),

            new BoltOption("Ruby bolts",               ItemID.RUBY_BOLTS,               ItemID.RUBY_BOLTS_E,               RUNES_RUBY,        59.0, 49),
            new BoltOption("Ruby dragon bolts",        ItemID.RUBY_DRAGON_BOLTS,        ItemID.RUBY_DRAGON_BOLTS_E,        RUNES_RUBY,        59.0, 49),

            new BoltOption("Diamond bolts",            ItemID.DIAMOND_BOLTS,            ItemID.DIAMOND_BOLTS_E,            RUNES_DIAMOND,     67.0, 57),
            new BoltOption("Diamond dragon bolts",     ItemID.DIAMOND_DRAGON_BOLTS,     ItemID.DIAMOND_DRAGON_BOLTS_E,     RUNES_DIAMOND,     67.0, 57),

            new BoltOption("Dragonstone bolts",        ItemID.DRAGONSTONE_BOLTS,        ItemID.DRAGONSTONE_BOLTS_E,        RUNES_DRAGONSTONE, 78.0, 68),
            new BoltOption("Dragonstone dragon bolts", ItemID.DRAGONSTONE_DRAGON_BOLTS, ItemID.DRAGONSTONE_DRAGON_BOLTS_E, RUNES_DRAGONSTONE, 78.0, 68),

            new BoltOption("Onyx bolts",               ItemID.ONYX_BOLTS,               ItemID.ONYX_BOLTS_E,               RUNES_ONYX,        97.0, 87),
            new BoltOption("Onyx dragon bolts",        ItemID.ONYX_DRAGON_BOLTS,        ItemID.ONYX_DRAGON_BOLTS_E,        RUNES_ONYX,        97.0, 87)
    );

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15; -fx-alignment: center");

        Label boltLabel = new Label("Bolts to enchant");

        boltComboBox = new ComboBox<>();
        boltComboBox.getItems().addAll(BOLT_OPTIONS);

        int savedBoltBaseId = prefs.getInt(PREF_SELECTED_BOLT_ID, BOLT_OPTIONS.get(0).baseId);
        BoltOption initial = BOLT_OPTIONS.stream()
                .filter(o -> o.baseId == savedBoltBaseId)
                .findFirst()
                .orElse(BOLT_OPTIONS.get(0));
        boltComboBox.getSelectionModel().select(initial);

        boltImageView = JavaFXUtils.getItemImageView(core, initial.baseId);
        if (boltImageView == null) boltImageView = new ImageView();
        boltImageView.setFitWidth(32);
        boltImageView.setFitHeight(32);

        boltComboBox.setOnAction(e -> {
            BoltOption sel = boltComboBox.getSelectionModel().getSelectedItem();
            if (sel != null) {
                ImageView iv = JavaFXUtils.getItemImageView(core, sel.baseId);
                if (iv != null) boltImageView.setImage(iv.getImage());
            }
        });

        HBox boltRow = new HBox(8, boltImageView, boltComboBox);
        boltRow.setStyle("-fx-alignment: center;");

        mainBox.getChildren().addAll(boltLabel, boltRow);

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhooks Tab ===
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

        // === Final Scene ===
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(event -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);

        Scene scene = new Scene(layout, 320, 290);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void saveSettings() {
        BoltOption sel = boltComboBox.getSelectionModel().getSelectedItem();
        if (sel != null) {
            prefs.putInt(PREF_SELECTED_BOLT_ID, sel.baseId);
        }

        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        // Close window
        Stage stage = (Stage) boltComboBox.getScene().getWindow();
        if (stage != null) stage.close();
    }

    // ===== Getters =====

    /** Returns the selected (normal or dragon) bolt base ID. */
    public int getSelectedBoltId() {
        BoltOption sel = boltComboBox != null ? boltComboBox.getSelectionModel().getSelectedItem() : null;
        return sel != null ? sel.baseId : BOLT_OPTIONS.get(0).baseId;
    }

    /** Returns the selected enchanted (_E) bolt ID. */
    public int getSelectedEnchantedBoltId() {
        BoltOption sel = boltComboBox != null ? boltComboBox.getSelectionModel().getSelectedItem() : null;
        return sel != null ? sel.enchantedId : BOLT_OPTIONS.get(0).enchantedId;
    }

    /** Returns the set of rune ItemIDs required to cast the enchant spell for the selected bolt. */
    public Set<Integer> getRequiredRunes() {
        BoltOption sel = boltComboBox != null ? boltComboBox.getSelectionModel().getSelectedItem() : null;
        return sel != null ? sel.requiredRunes : RUNES_OPAL;
    }

    /** Returns the magic xp gained from casting the enchant spell for the selected bolt. */
    public double getXpPerCast() {
        BoltOption sel = boltComboBox.getSelectionModel().getSelectedItem();
        return sel != null ? sel.xpPerCast : 9.0;
    }

    /** Returns the magic level required for the enchant spell for the selected bolt. */
    public int getRequiredMagicLevel() {
        BoltOption sel = boltComboBox.getSelectionModel().getSelectedItem();
        return sel != null ? sel.magicLevel : 4;
    }

    // Webhook getters
    public boolean isWebhookEnabled() {
        return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected();
    }
    public String getWebhookUrl() {
        return webhookUrlField != null ? webhookUrlField.getText().trim() : "";
    }
    public int getWebhookInterval() {
        return (webhookIntervalComboBox != null && webhookIntervalComboBox.getValue() != null)
                ? webhookIntervalComboBox.getValue()
                : 5;
    }
    public boolean isUsernameIncluded() {
        return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected();
    }
}