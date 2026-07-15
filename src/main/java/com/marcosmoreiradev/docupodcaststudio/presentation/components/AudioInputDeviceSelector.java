package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import com.marcosmoreiradev.docupodcaststudio.application.recording.AudioInputDevice;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import java.util.ArrayList;
import java.util.List;

/** Shared microphone selector for JavaSound-backed recordings. */
public final class AudioInputDeviceSelector extends ComboBox<AudioInputDevice> {
    public AudioInputDeviceSelector(List<AudioInputDevice> devices) {
        ArrayList<AudioInputDevice> choices = new ArrayList<>();
        if (devices != null) {
            choices.addAll(devices);
        }
        if (choices.isEmpty()) {
            choices.add(AudioInputDevice.systemDefault());
        }
        getItems().setAll(choices);
        getSelectionModel().select(choices.stream()
                .filter(AudioInputDevice::defaultDevice)
                .findFirst()
                .orElse(choices.get(0)));
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("voice-library-combo");
        setCellFactory(list -> cell());
        setButtonCell(cell());
    }

    public AudioInputDevice selectedDevice() {
        AudioInputDevice selected = getSelectionModel().getSelectedItem();
        return selected == null ? AudioInputDevice.systemDefault() : selected;
    }

    public String selectedDeviceId() {
        return selectedDevice().id();
    }

    private static ListCell<AudioInputDevice> cell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AudioInputDevice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        };
    }
}
