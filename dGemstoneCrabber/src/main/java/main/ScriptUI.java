package main;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.script.Script;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptUI {
    private final Preferences prefs = Preferences.userRoot().node("main");

    // === Pref keys (webhook) ===
    private static final String PREF_WEBHOOK_ENABLED        = "dgemstonecrabber_webhook_enabled";
    private static final String PREF_WEBHOOK_URL            = "dgemstonecrabber_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL       = "dgemstonecrabber_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER   = "dgemstonecrabber_webhook_include_user";

    // === Pref keys (main tab) ===
    private static final String PREF_USE_FOOD               = "dgemstonecrabber_use_food";
    private static final String PREF_FOOD_ITEM_ID           = "dgemstonecrabber_food_item_id";
    private static final String PREF_FOOD_QTY               = "dgemstonecrabber_food_qty";
    private static final String PREF_FOOD_EAT_PCT           = "dgemstonecrabber_food_eat_pct";
    private static final String PREF_USE_POTIONS            = "dgemstonecrabber_use_potions";
    private static final String PREF_POTION_ITEM_ID         = "dgemstonecrabber_potion_item_id";
    private static final String PREF_POTION_QTY             = "dgemstonecrabber_potion_qty";
    private static final String PREF_ONLY_HOP_AFTER_KILL    = "dgemstonecrabber_only_hop_after_kill";

    // === New pref keys (always-visible potion-related options) ===
    private static final String PREF_USE_DBA_SPEC           = "dgemstonecrabber_use_dba_spec";
    private static final String PREF_USE_HEART              = "dgemstonecrabber_use_heart";

    private final Script script;

    // === Webhook controls ===
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    // === Main tab controls ===
    private CheckBox useFoodCheck;
    private ComboBox<Integer> foodCombo;
    private Spinner<Integer>   foodQtySpinner;
    private Spinner<Integer> foodEatPctSpinner;

    private CheckBox usePotionsCheck;
    private ComboBox<Integer> potionCombo;
    private Spinner<Integer>   potionQtySpinner;

    // Always-visible potion-related options with item images
    private CheckBox useDragonBattleaxeSpecCheck;
    private CheckBox useHeartCheck;

    private CheckBox onlyHopAfterKillCheck;
    private Label hopAfterKillDisclaimer;

    public ScriptUI(Script script) {
        this.script = script;
    }

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // ---------- MAIN TAB ----------
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15;");

        // Use food
        useFoodCheck = new CheckBox("Use food");
        useFoodCheck.setSelected(prefs.getBoolean(PREF_USE_FOOD, false));

        foodCombo = new ComboBox<>();
        foodCombo.getItems().addAll(
                ItemID.CAKE, ItemID.TROUT, ItemID.SALMON, ItemID.TUNA, ItemID.JUG_OF_WINE, ItemID.LOBSTER, ItemID.BASS,
                ItemID.SWORDFISH, ItemID.PLAIN_PIZZA, ItemID.POTATO_WITH_CHEESE, ItemID.MONKFISH, ItemID.COOKED_KYATT,
                ItemID.COOKED_KARAMBWAN, ItemID.SHARK, ItemID.SEA_TURTLE, ItemID.COOKED_SUNLIGHT_ANTELOPE, ItemID.MANTA_RAY,
                ItemID.TUNA_POTATO, ItemID.DARK_CRAB, ItemID.ANGLERFISH, ItemID.COOKED_DASHING_KEBBIT, ItemID.COOKED_MOONLIGHT_ANTELOPE
        );
        int savedFood = prefs.getInt(PREF_FOOD_ITEM_ID, ItemID.SHARK);
        if (!foodCombo.getItems().contains(savedFood)) savedFood = ItemID.SHARK;
        foodCombo.getSelectionModel().select((Integer) savedFood);
        foodCombo.setCellFactory(cb -> itemCell(core));
        foodCombo.setButtonCell(itemCell(core));
        foodCombo.setMaxWidth(Double.MAX_VALUE);

        foodQtySpinner = new Spinner<>(1, 28, prefs.getInt(PREF_FOOD_QTY, 20));
        styleSpinner(foodQtySpinner);

        foodEatPctSpinner = new Spinner<>(10, 99, prefs.getInt(PREF_FOOD_EAT_PCT, 60));
        styleSpinner(foodEatPctSpinner);

        // Labels above controls
        Label foodItemLbl   = label("Food item");
        Label foodQtyLbl    = label("Quantity");
        Label eatAtLbl      = label("Eat at (%)");

        VBox foodItemBox = new VBox(4, foodItemLbl, foodCombo);
        VBox foodQtyBox  = new VBox(4, foodQtyLbl, foodQtySpinner);
        VBox eatAtBox    = new VBox(4, eatAtLbl, foodEatPctSpinner);

        HBox foodRow = new HBox(12, foodItemBox, foodQtyBox, eatAtBox);
        foodRow.setStyle("-fx-alignment: center-left;");
        HBox.setHgrow(foodItemBox, Priority.ALWAYS);

        // Use potions
        usePotionsCheck = new CheckBox("Use potions");
        usePotionsCheck.setSelected(prefs.getBoolean(PREF_USE_POTIONS, false));

        potionCombo = new ComboBox<>();
        potionCombo.getItems().addAll(
                ItemID.STRENGTH_POTION4, ItemID.COMBAT_POTION4, ItemID.SUPER_ATTACK4, ItemID.DIVINE_SUPER_ATTACK_POTION4, ItemID.SUPER_STRENGTH4, ItemID.DIVINE_SUPER_STRENGTH_POTION4,
                ItemID.RANGING_POTION4, ItemID.DIVINE_RANGING_POTION4, ItemID.ZAMORAK_BREW4, ItemID.FORGOTTEN_BREW4, ItemID.BASTION_POTION4,
                ItemID.DIVINE_BASTION_POTION4, ItemID.SUPER_COMBAT_POTION4, ItemID.DIVINE_SUPER_COMBAT_POTION4
        );
        int savedPotion = prefs.getInt(PREF_POTION_ITEM_ID, ItemID.SUPER_STRENGTH4);
        if (!potionCombo.getItems().contains(savedPotion)) savedPotion = ItemID.SUPER_STRENGTH4;
        potionCombo.getSelectionModel().select((Integer) savedPotion);
        potionCombo.setCellFactory(cb -> itemCell(core));
        potionCombo.setButtonCell(itemCell(core));
        potionCombo.setMaxWidth(Double.MAX_VALUE);

        potionQtySpinner = new Spinner<>(1, 28, prefs.getInt(PREF_POTION_QTY, 4));
        styleSpinner(potionQtySpinner);

        // Labels above controls
        Label potItemLbl = label("Potion");
        Label potQtyLbl  = label("Quantity");

        VBox potItemBox = new VBox(4, potItemLbl, potionCombo);
        VBox potQtyBox  = new VBox(4, potQtyLbl, potionQtySpinner);

        HBox potionRow = new HBox(12, potItemBox, potQtyBox);
        potionRow.setStyle("-fx-alignment: center-left;");
        HBox.setHgrow(potItemBox, Priority.ALWAYS);

        // === Always-visible potion-related options with item images ===
        useDragonBattleaxeSpecCheck = new CheckBox();
        useDragonBattleaxeSpecCheck.setGraphic(
                graphicWithImagesAndText(core, "Use Dragon battleaxe special", ItemID.DRAGON_BATTLEAXE)
        );
        useDragonBattleaxeSpecCheck.setText(null);
        useDragonBattleaxeSpecCheck.setSelected(prefs.getBoolean(PREF_USE_DBA_SPEC, false));

        useHeartCheck = new CheckBox();
        useHeartCheck.setGraphic(
                graphicWithImagesAndText(core, "Use Imbued/Saturated heart", ItemID.IMBUED_HEART, ItemID.SATURATED_HEART)
        );
        useHeartCheck.setText(null);
        useHeartCheck.setSelected(prefs.getBoolean(PREF_USE_HEART, false));

        HBox alwaysVisiblePotionOptions = new HBox(12, useDragonBattleaxeSpecCheck, useHeartCheck);
        alwaysVisiblePotionOptions.setStyle("-fx-alignment: center-left;");

        // Only hop/break after kill
        onlyHopAfterKillCheck = new CheckBox("Only hop/break after a kill is done");
        onlyHopAfterKillCheck.setSelected(prefs.getBoolean(PREF_ONLY_HOP_AFTER_KILL, false));

        hopAfterKillDisclaimer = new Label(
                "Disclaimer: Waiting for a kill before hopping/breaking\nmay be a more recognizable pattern.");
        hopAfterKillDisclaimer.setWrapText(true);
        hopAfterKillDisclaimer.setStyle("-fx-text-fill: #ffd166; -fx-font-size: 12px; -fx-padding: 4 0 0 22;");

        // Toggle visibility/enabled states for grouped rows (always-visible options stay visible)
        toggleGroup(foodRow, useFoodCheck.isSelected());
        toggleGroup(potionRow, usePotionsCheck.isSelected());
        toggleNode(hopAfterKillDisclaimer, onlyHopAfterKillCheck.isSelected());

        useFoodCheck.selectedProperty().addListener((o,w,now)-> toggleGroup(foodRow, now));
        usePotionsCheck.selectedProperty().addListener((o,w,now)-> toggleGroup(potionRow, now));
        onlyHopAfterKillCheck.selectedProperty().addListener((o,w,now)-> toggleNode(hopAfterKillDisclaimer, now));

        mainBox.getChildren().addAll(
                useFoodCheck,
                foodRow,
                new Separator(),
                usePotionsCheck,
                potionRow,
                // always visible with images:
                alwaysVisiblePotionOptions,
                new Separator(),
                onlyHopAfterKillCheck,
                hopAfterKillDisclaimer
        );

        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);
        tabPane.getTabs().add(mainTab);

        // -----------------------------------------
        // Webhook tab
        // -----------------------------------------
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
        tabPane.getTabs().add(webhookTab);

        // -----------------------------------------
        // Final UI frame
        // -----------------------------------------
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> saveSettings());

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(layout, 500, 425);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private HBox graphicWithImagesAndText(ScriptCore core, String text, int... itemIds) {
        HBox box = new HBox();
        box.setStyle("-fx-alignment: center-left; -fx-spacing: 2; -fx-padding: 0 0 0 8;");

        for (int i = 0; i < itemIds.length; i++) {
            ImageView iv = JavaFXUtils.getItemImageView(core, itemIds[i]);
            if (iv != null) {
                iv.setFitWidth(18);
                iv.setFitHeight(18);
                box.getChildren().add(iv);
            }
            if (i < itemIds.length - 1) {
                Label slash = new Label("/");
                slash.setStyle("-fx-text-fill: white;");
                box.getChildren().add(slash);
            }
        }

        Label lbl = label(text);
        box.getChildren().add(lbl);

        return box;
    }

    private ListCell<Integer> itemCell(ScriptCore core) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer itemId, boolean empty) {
                super.updateItem(itemId, empty);
                if (empty || itemId == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String name = core.getItemManager().getItemName(itemId);
                ImageView iv = JavaFXUtils.getItemImageView(core, itemId);
                if (iv != null) {
                    iv.setFitWidth(18);
                    iv.setFitHeight(18);
                }
                setText(name != null ? name : ("Item " + itemId));
                setGraphic(iv);
            }
        };
    }

    private void styleSpinner(Spinner<Integer> sp) {
        sp.setEditable(true);

        SpinnerValueFactory<Integer> vf = sp.getValueFactory();
        if (vf instanceof SpinnerValueFactory.IntegerSpinnerValueFactory intVF) {
            intVF.setConverter(new StringConverter<Integer>() {
                @Override
                public String toString(Integer value) {
                    return value == null ? "" : value.toString();
                }

                @Override
                public Integer fromString(String s) {
                    try {
                        int n = Integer.parseInt(s.trim());
                        int min = intVF.getMin();
                        int max = intVF.getMax();
                        if (n < min) n = min;
                        if (n > max) n = max;
                        return n;
                    } catch (Exception ex) {
                        // on invalid input, keep current value
                        return intVF.getValue();
                    }
                }
            });
        } else if (vf != null) {
            // Fallback: no min/max available, just parse-or-keep
            vf.setConverter(new StringConverter<Integer>() {
                @Override
                public String toString(Integer value) {
                    return value == null ? "" : value.toString();
                }

                @Override
                public Integer fromString(String s) {
                    try {
                        return Integer.parseInt(s.trim());
                    } catch (Exception ex) {
                        return sp.getValue();
                    }
                }
            });
        }
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white;");
        return l;
    }

    private void toggleNode(javafx.scene.Node n, boolean on) {
        n.setDisable(!on); n.setManaged(on); n.setVisible(on);
    }
    private void toggleGroup(javafx.scene.Node group, boolean on) { toggleNode(group, on); }

    private void saveSettings() {
        // Webhook
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());

        // Main tab
        prefs.putBoolean(PREF_USE_FOOD, isUseFood());
        prefs.putInt(PREF_FOOD_ITEM_ID, getSelectedFoodItemId());
        prefs.putInt(PREF_FOOD_QTY, getFoodQuantity());
        prefs.putInt(PREF_FOOD_EAT_PCT, getFoodEatPercent());

        prefs.putBoolean(PREF_USE_POTIONS, isUsePotions());
        prefs.putInt(PREF_POTION_ITEM_ID, getSelectedPotionItemId());
        prefs.putInt(PREF_POTION_QTY, getPotionQuantity());

        prefs.putBoolean(PREF_USE_DBA_SPEC, isUseDragonBattleaxeSpec());
        prefs.putBoolean(PREF_USE_HEART, isUseHeart());

        prefs.putBoolean(PREF_ONLY_HOP_AFTER_KILL, isOnlyHopBreakAfterKill());

        script.log("SAVESETTINGS", "Saved main + webhook settings");
        ((Stage) webhookEnabledCheckBox.getScene().getWindow()).close();
    }

    // === Getters (webhook) ===
    public boolean isWebhookEnabled()               { return webhookEnabledCheckBox != null && webhookEnabledCheckBox.isSelected(); }
    public String  getWebhookUrl()                  { return webhookUrlField != null ? webhookUrlField.getText().trim() : ""; }
    public int     getWebhookInterval()             { return (webhookIntervalComboBox != null && webhookIntervalComboBox.getValue()!=null) ? webhookIntervalComboBox.getValue() : 5; }
    public boolean isUsernameIncluded()             { return includeUsernameCheckBox != null && includeUsernameCheckBox.isSelected(); }

    // === Getters (main) ===
    public boolean isUseFood()                      { return useFoodCheck != null && useFoodCheck.isSelected(); }
    public int     getSelectedFoodItemId()          { Integer v = (foodCombo != null) ? foodCombo.getValue() : null; return v != null ? v : ItemID.SHARK; }
    public int     getFoodQuantity()                { return (foodQtySpinner != null) ? foodQtySpinner.getValue() : 20; }
    public int     getFoodEatPercent()              { return (foodEatPctSpinner != null) ? foodEatPctSpinner.getValue() : 60; }

    public boolean isUsePotions()                   { return usePotionsCheck != null && usePotionsCheck.isSelected(); }
    public int     getSelectedPotionItemId()        { Integer v = (potionCombo != null) ? potionCombo.getValue() : null; return v != null ? v : ItemID.SUPER_STRENGTH4; }
    public int     getPotionQuantity()              { return (potionQtySpinner != null) ? potionQtySpinner.getValue() : 4; }

    public boolean isUseDragonBattleaxeSpec()       { return useDragonBattleaxeSpecCheck != null && useDragonBattleaxeSpecCheck.isSelected(); }
    public boolean isUseHeart()                     { return useHeartCheck != null && useHeartCheck.isSelected(); }

    public boolean isOnlyHopBreakAfterKill()        { return onlyHopAfterKillCheck != null && onlyHopAfterKillCheck.isSelected(); }
}