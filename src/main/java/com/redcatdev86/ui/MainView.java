package com.redcatdev86.ui;

import com.redcatdev86.model.CareerSave;
import com.redcatdev86.model.Injury;
import com.redcatdev86.storage.AppPaths;
import com.redcatdev86.storage.JsonDataStore;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

public class MainView {

    // ----------------------------
    // Data (in-memory)
    // ----------------------------
    private final ObservableList<CareerSave> saves = FXCollections.observableArrayList();
    private final ObservableList<Injury> injuriesView = FXCollections.observableArrayList();

    // ----------------------------
    // Persistence
    // ----------------------------
    private final JsonDataStore dataStore = new JsonDataStore();
    private Path currentFile = AppPaths.defaultDataFile();

    // Debounced autosave
    private final PauseTransition saveDebounce = new PauseTransition(Duration.millis(500));

    // Dirty flag
    private boolean dirty = false;

    // Stage (for title)
    private Stage stage;

    // Title base
    private static final String BASE_TITLE = "FC Issuer Manager";

    // ----------------------------
    // UI Controls
    // ----------------------------
    private final ComboBox<CareerSave> savesCombo = new ComboBox<>();
    private final TableView<Injury> injuriesTable = new TableView<>();

    private final TextField playerField = new TextField();
    private final DatePicker recoveryPicker = new DatePicker();
    private final Button addInjuryBtn = new Button("Add");
    private final Button deleteInjuryBtn = new Button("Delete");
    private final Button newSaveBtn = new Button("+ New save");
    private final Button deleteSaveBtn = new Button("Delete save");

    private final Button loadBtn = new Button("Load...");
    private final Button saveAsBtn = new Button("Save as...");

    // Nice-to-have UI
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label();

    // ----------------------------
    // Public API
    // ----------------------------
    public Parent build(Stage stage) {
        this.stage = stage;

        configureDebouncedSave();
        loadFromCurrentFileOrEmpty();

        buildTopBar();
        buildTable();

        Parent root = layout();
        wireEvents();

        // Ensure UI selection consistent
        if (!saves.isEmpty() && savesCombo.getSelectionModel().getSelectedItem() == null) {
            savesCombo.getSelectionModel().selectFirst();
        }
        loadSelectedSaveInjuries();

        // Title initial
        setDirty(false);
        updateTitle();
        updateStatus();

        return root;
    }

    // ----------------------------
    // Debounced save config
    // ----------------------------
    private void configureDebouncedSave() {
        saveDebounce.setOnFinished(e -> saveNow());
    }

    private void markDirtyAndScheduleSave() {
        setDirty(true);
        saveDebounce.playFromStart();
        updateStatus();
    }

    private void saveNow() {
        try {
            dataStore.save(currentFile, saves);
            setDirty(false);
            statusLabel.setText("Saved");
        } catch (IOException e) {
            // keep dirty true if save fails
            setDirty(true);
            alert("Failed to save data:\n" + e.getMessage());
            statusLabel.setText("Save failed");
        } finally {
            updateTitle();
        }
    }

    private void setDirty(boolean value) {
        this.dirty = value;
        updateTitle();
    }

    private void updateTitle() {
        if (stage == null) return;

        String filePart = currentFile == null ? "" : " — " + currentFile.toAbsolutePath();
        String dirtyPart = dirty ? " *" : "";

        stage.setTitle(BASE_TITLE + dirtyPart + filePart);
    }

    // ----------------------------
    // UI Build
    // ----------------------------
    private void buildTopBar() {
        savesCombo.setItems(saves);
        savesCombo.setPromptText("Select a save...");
        if (!saves.isEmpty()) {
            savesCombo.getSelectionModel().selectFirst();
        }

        searchField.setPromptText("Search player…");
    }

    private void buildTable() {
        injuriesTable.setEditable(true);
        injuriesTable.setPlaceholder(new Label("No injuries for this career save."));

        TableColumn<Injury, String> playerCol = new TableColumn<>("Player");
        playerCol.setCellValueFactory(new PropertyValueFactory<>("playerName"));
        playerCol.setEditable(true);

        playerCol.setCellFactory(TextFieldTableCell.forTableColumn());
        playerCol.setOnEditCommit(e -> {
            Injury injury = e.getRowValue();
            String newVal = e.getNewValue() == null ? "" : e.getNewValue().trim();
            injury.setPlayerName(newVal);
            injuriesTable.refresh();
            markDirtyAndScheduleSave();
        });

        TableColumn<Injury, LocalDate> dateCol = new TableColumn<>("Recovery date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("recoveryDate"));
        dateCol.setEditable(true);

        // DatePicker in cell (separate class: com.redcatdev86.ui.DatePickerTableCell)
        dateCol.setCellFactory(col -> new DatePickerTableCell());
        dateCol.setOnEditCommit(e -> {
            Injury injury = e.getRowValue();
            injury.setRecoveryDate(e.getNewValue());
            injuriesTable.refresh();
            markDirtyAndScheduleSave();
        });

        injuriesTable.getColumns().setAll(playerCol, dateCol);
        injuriesTable.setItems(injuriesView);
        injuriesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Parent layout() {
        // --- Top bar
        Label title = new Label("CAREER INJURIES");
        title.getStyleClass().add("fc-title");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        HBox topLeft = new HBox(10, title);
        topLeft.setAlignment(Pos.CENTER_LEFT);

        HBox topRight = new HBox(10,
                new Label("Save:"),
                savesCombo,
                newSaveBtn,
                deleteSaveBtn,
                loadBtn,
                saveAsBtn
        );
        topRight.setAlignment(Pos.CENTER_RIGHT);

        HBox top = new HBox(12, topLeft, spacer1, topRight);
        top.getStyleClass().add("fc-topbar");

        // --- Center: search + table
        VBox center = new VBox(10);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("Filter:");
        searchRow.getChildren().addAll(searchLabel, searchField);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox.setVgrow(injuriesTable, Priority.ALWAYS);
        center.getChildren().addAll(searchRow, injuriesTable);
        center.getStyleClass().add("fc-center");

        // --- Bottom bar (add/delete injury)
        playerField.setPromptText("Player name");
        recoveryPicker.setPromptText("Recovery date");

        addInjuryBtn.getStyleClass().add("fc-accent");

        HBox bottom = new HBox(10,
                playerField,
                recoveryPicker,
                addInjuryBtn,
                deleteInjuryBtn
        );
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.getStyleClass().add("fc-bottombar");
        HBox.setHgrow(playerField, Priority.ALWAYS);

        // --- Footer/status
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Label fileHint = new Label();
        fileHint.setOpacity(0.75);
        fileHint.setText(currentFile == null ? "" : currentFile.toAbsolutePath().toString());

        HBox footer = new HBox(10, fileHint, spacer2, statusLabel);
        footer.setPadding(new Insets(0, 12, 10, 12));
        footer.setAlignment(Pos.CENTER_LEFT);

        // Root
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        BorderPane.setMargin(top, new Insets(0, 0, 12, 0));
        BorderPane.setMargin(center, new Insets(0, 0, 12, 0));

        root.setTop(top);
        root.setCenter(center);
        root.setBottom(new VBox(10, bottom, footer));

        top.setPadding(new Insets(12));
        bottom.setPadding(new Insets(12));

        return root;
    }

    private void wireEvents() {
        savesCombo.setOnAction(e -> {
            loadSelectedSaveInjuries();
            updateStatus();
        });

        addInjuryBtn.setOnAction(e -> addInjury());
        deleteInjuryBtn.setOnAction(e -> deleteSelectedInjury());

        newSaveBtn.setOnAction(e -> createNewSave());
        deleteSaveBtn.setOnAction(e -> deleteSelectedSave());

        // UX: disable if no save selected
        deleteSaveBtn.disableProperty().bind(savesCombo.getSelectionModel().selectedItemProperty().isNull());

        loadBtn.setOnAction(e -> chooseAndLoad(getWindow(loadBtn)));
        saveAsBtn.setOnAction(e -> chooseAndSaveAs(getWindow(saveAsBtn)));

        searchField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
    }

    // ----------------------------
    // Actions
    // ----------------------------
    private void loadSelectedSaveInjuries() {
        CareerSave selected = savesCombo.getSelectionModel().getSelectedItem();
        injuriesView.clear();
        if (selected != null) {
            injuriesView.addAll(selected.getInjuries());
        }
        applyFilter();
    }

    private void applyFilter() {
        CareerSave selected = savesCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            injuriesView.clear();
            return;
        }

        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        injuriesView.clear();

        if (q.isEmpty()) {
            injuriesView.addAll(selected.getInjuries());
            return;
        }

        for (Injury i : selected.getInjuries()) {
            String pn = i.getPlayerName() == null ? "" : i.getPlayerName().toLowerCase();
            if (pn.contains(q)) injuriesView.add(i);
        }
    }

    private void addInjury() {
        CareerSave selected = savesCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Select a career save first.");
            return;
        }

        String player = playerField.getText() == null ? "" : playerField.getText().trim();
        LocalDate date = recoveryPicker.getValue();

        if (player.isEmpty()) {
            alert("Player name is required.");
            return;
        }
        if (date == null) {
            alert("Recovery date is required.");
            return;
        }

        Injury injury = new Injury(player, date);
        selected.getInjuries().add(injury);

        applyFilter();

        playerField.clear();
        recoveryPicker.setValue(null);

        markDirtyAndScheduleSave();
    }

    private void deleteSelectedInjury() {
        CareerSave selectedSave = savesCombo.getSelectionModel().getSelectedItem();
        Injury selectedInjury = injuriesTable.getSelectionModel().getSelectedItem();

        if (selectedSave == null || selectedInjury == null) {
            alert("Select an injury to delete.");
            return;
        }

        selectedSave.getInjuries().remove(selectedInjury);
        applyFilter();

        markDirtyAndScheduleSave();
    }

    private void createNewSave() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New career save");
        dialog.setHeaderText("Create a new career save");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(nameRaw -> {
            String name = nameRaw.trim();
            if (name.isEmpty()) {
                alert("Name cannot be empty.");
                return;
            }

            boolean exists = saves.stream().anyMatch(s -> s.getName() != null && s.getName().equalsIgnoreCase(name));
            if (exists) {
                alert("A save with the same name already exists.");
                return;
            }

            CareerSave save = new CareerSave(name);
            saves.add(save);
            savesCombo.getSelectionModel().select(save);
            loadSelectedSaveInjuries();

            markDirtyAndScheduleSave();
        });
    }

    private void deleteSelectedSave() {
        CareerSave selected = savesCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert("Select a save to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete career save");
        confirm.setHeaderText("Delete save: " + selected.getName());
        confirm.setContentText("This will remove the save and all its injuries.\nThis action cannot be undone.");

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBtn, cancelBtn);

        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != deleteBtn) {
            return;
        }

        int index = saves.indexOf(selected);
        saves.remove(selected);

        if (saves.isEmpty()) {
            savesCombo.getSelectionModel().clearSelection();
            injuriesView.clear();
        } else {
            int newIndex = Math.min(index, saves.size() - 1);
            savesCombo.getSelectionModel().select(newIndex);
            loadSelectedSaveInjuries();
        }

        markDirtyAndScheduleSave();
    }

    // ----------------------------
    // File chooser (user choice)
    // ----------------------------
    private void chooseAndLoad(Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Load data");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        fc.setInitialFileName("fc-issuer-manager.json");

        var file = fc.showOpenDialog(owner);
        if (file == null) return;

        saveDebounce.stop();

        currentFile = file.toPath();
        loadFromCurrentFileOrEmpty();

        if (!saves.isEmpty()) {
            savesCombo.getSelectionModel().selectFirst();
        } else {
            savesCombo.getSelectionModel().clearSelection();
        }

        loadSelectedSaveInjuries();

        setDirty(false);
        updateTitle();
        updateStatus();
    }

    private void chooseAndSaveAs(Window owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save data as");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        fc.setInitialFileName("fc-issuer-manager.json");

        var file = fc.showSaveDialog(owner);
        if (file == null) return;

        currentFile = file.toPath();

        saveDebounce.stop();
        saveNow();

        updateTitle();
        updateStatus();
    }

    // ----------------------------
    // Persistence helpers
    // ----------------------------
    private void loadFromCurrentFileOrEmpty() {
        try {
            saves.setAll(dataStore.load(currentFile));
        } catch (IOException e) {
            alert("Failed to load data:\n" + e.getMessage());
            saves.clear();
        }

        dirty = false;
        updateTitle();
    }

    // ----------------------------
    // Status helpers
    // ----------------------------
    private void updateStatus() {
        CareerSave selected = savesCombo.getSelectionModel().getSelectedItem();
        int saveCount = saves.size();
        int injCount = selected == null ? 0 : selected.getInjuries().size();
        statusLabel.setText((dirty ? "Unsaved changes" : "OK") + " • Saves: " + saveCount + " • Injuries: " + injCount);
        statusLabel.setOpacity(0.85);
    }

    // ----------------------------
    // Small utilities
    // ----------------------------
    private Window getWindow(Control c) {
        Scene scene = c.getScene();
        return scene == null ? null : scene.getWindow();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}