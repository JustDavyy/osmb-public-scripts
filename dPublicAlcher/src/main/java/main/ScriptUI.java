package main;

// OSMB specific imports
import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.spellbook.StandardSpellbook;

// General java imports
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptUI {

    private static final StandardSpellbook[] ALCHEMY_SPELLS = {
            StandardSpellbook.LOW_ALCHEMY,
            StandardSpellbook.HIGH_LEVEL_ALCHEMY
    };

    private ComboBox<StandardSpellbook> spellComboBox;
    private TextField itemIdField;

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        // Spell selection
        Label spellLabel = new Label("Choose spell to cast");
        spellComboBox = new ComboBox<>();
        spellComboBox.getItems().addAll(ALCHEMY_SPELLS);
        spellComboBox.getSelectionModel().select(StandardSpellbook.HIGH_LEVEL_ALCHEMY);

        // Item ID entry
        Label itemIdLabel = new Label("Enter item ID to alch");
        itemIdField = new TextField("892"); // Default: Rune arrows
        itemIdField.setMaxWidth(100);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            if (spellComboBox.getSelectionModel().getSelectedIndex() >= 0)
                ((Stage) confirmButton.getScene().getWindow()).close();
        });

        // Add everything to the UI
        root.getChildren().addAll(spellLabel, spellComboBox, itemIdLabel, itemIdField, confirmButton);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public StandardSpellbook getSelectedSpell() {
        return spellComboBox.getSelectionModel().getSelectedItem();
    }

    public int getSelectedItemId() {
        try {
            return Integer.parseInt(itemIdField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}