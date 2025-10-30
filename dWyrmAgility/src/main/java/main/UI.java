package main;

import com.osmb.api.ScriptCore;
import courses.WyrmAdvanced;
import courses.WyrmBasic;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class UI {
    private final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_SELECTED_COURSE = "dwyrmagility_selected_course";
    private static final String PREF_WEBHOOK_ENABLED = "dwyrmagility_webhook_enabled";
    private static final String PREF_WEBHOOK_URL = "dwyrmagility_webhook_url";
    private static final String PREF_WEBHOOK_INTERVAL = "dwyrmagility_webhook_interval";
    private static final String PREF_WEBHOOK_INCLUDE_USER = "dwyrmagility_webhook_include_user";

    private ComboBox<Course> selectCourseComboBox;
    private CheckBox webhookEnabledCheckBox;
    private TextField webhookUrlField;
    private ComboBox<Integer> webhookIntervalComboBox;
    private CheckBox includeUsernameCheckBox;

    public Scene buildScene(ScriptCore core) {
        TabPane tabPane = new TabPane();

        // === Main Tab ===
        VBox mainBox = new VBox(10);
        mainBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15;");

        Label selectCourseLabel = new Label("Select Course");
        selectCourseComboBox = new ComboBox<>();
        selectCourseComboBox.getItems().addAll(
                new WyrmBasic((dWyrmAgility) core),
                new WyrmAdvanced((dWyrmAgility) core)
        );

        selectCourseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.name() : "";
            }

            @Override
            public Course fromString(String string) {
                return selectCourseComboBox.getItems().stream()
                        .filter(course -> course.name().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        loadSelectedCourse();

        HBox selectCourseBox = new HBox(10, selectCourseLabel, selectCourseComboBox);
        selectCourseBox.setAlignment(Pos.CENTER_LEFT);

        mainBox.getChildren().addAll(selectCourseBox);
        Tab mainTab = new Tab("Main", mainBox);
        mainTab.setClosable(false);

        // === Webhook Tab ===
        VBox webhookBox = new VBox(10);
        webhookBox.setStyle("-fx-background-color: #636E72; -fx-padding: 15;");

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

        // === Scene & Confirm Button ===
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            if (selectedCourse() != null) {
                saveSelectedCourse();
                saveWebhookSettings();
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        VBox layout = new VBox(tabPane, confirmButton);
        layout.setSpacing(10);
        layout.setStyle("-fx-background-color: #2d3436; -fx-padding: 10;");

        tabPane.getTabs().addAll(mainTab, webhookTab);
        Scene scene = new Scene(layout, 300, 400);
        scene.getStylesheets().add("style.css");
        return scene;
    }

    private void loadSelectedCourse() {
        String savedCourse = prefs.get(PREF_SELECTED_COURSE, "");
        if (!savedCourse.isEmpty()) {
            Course match = selectCourseComboBox.getItems().stream()
                    .filter(course -> course.name().equals(savedCourse))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                selectCourseComboBox.getSelectionModel().select(match);
                System.out.println("[SAVESETTINGS] Loaded course: " + savedCourse);
            }
        }
    }

    private void saveSelectedCourse() {
        Course course = selectCourseComboBox.getSelectionModel().getSelectedItem();
        if (course != null) {
            prefs.put(PREF_SELECTED_COURSE, course.name());
            System.out.println("[SAVESETTINGS] Saved selected course: " + course.name());
        }
    }

    private void saveWebhookSettings() {
        prefs.putBoolean(PREF_WEBHOOK_ENABLED, isWebhookEnabled());
        prefs.put(PREF_WEBHOOK_URL, getWebhookUrl());
        prefs.putInt(PREF_WEBHOOK_INTERVAL, getWebhookInterval());
        prefs.putBoolean(PREF_WEBHOOK_INCLUDE_USER, isUsernameIncluded());
    }

    public Course selectedCourse() {
        return selectCourseComboBox.getSelectionModel().getSelectedItem();
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