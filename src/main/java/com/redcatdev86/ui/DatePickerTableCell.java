package com.redcatdev86.ui;

import com.redcatdev86.model.Injury;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.util.StringConverter;
import javafx.util.converter.LocalDateStringConverter;

import java.time.LocalDate;

public class DatePickerTableCell extends TableCell<Injury, LocalDate> {

    private final DatePicker datePicker = new DatePicker();
    private final StringConverter<LocalDate> converter = new LocalDateStringConverter();

    public DatePickerTableCell() {
        setContentDisplay(ContentDisplay.TEXT_ONLY);

        datePicker.setOnAction(e -> {
            LocalDate newDate = datePicker.getValue();
            commitEdit(newDate);
        });

        // Se perde focus, conferma edit
        datePicker.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitEdit(datePicker.getValue());
            }
        });
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            datePicker.setValue(getItem());
            setGraphic(datePicker);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            datePicker.requestFocus();
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setContentDisplay(ContentDisplay.TEXT_ONLY);
        setGraphic(null);
    }

    @Override
    protected void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        } else {
            if (isEditing()) {
                datePicker.setValue(item);
                setGraphic(datePicker);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(item == null ? "" : converter.toString(item));
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
    }
}