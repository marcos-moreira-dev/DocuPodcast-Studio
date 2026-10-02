package com.marcosmoreiradev.docupodcaststudio.ink.controls;

import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputCursor;
import com.marcosmoreiradev.docupodcaststudio.ink.input.InkInputStatus;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.util.Locale;
import java.util.Objects;

/** Shared fixed-geometry pressure readout for every ink editor. */
public final class InkPressureIndicator extends Label implements AutoCloseable {
    private static final double FIXED_WIDTH = 118.0;

    private final ObservableValue<InkInputStatus> status;
    private final ChangeListener<InkInputStatus> listener;
    private String pendingText;
    private boolean updateQueued;

    public InkPressureIndicator(ObservableValue<InkInputStatus> status) {
        this.status = Objects.requireNonNull(status, "ink input status");
        getStyleClass().add("ink-pressure-indicator");
        setWrapText(false);
        setMinWidth(FIXED_WIDTH);
        setPrefWidth(FIXED_WIDTH);
        setMaxWidth(FIXED_WIDTH);
        setTooltip(new Tooltip("Presión detectada del lápiz digital."));
        setAccessibleText("Presión del lápiz digital");
        pendingText = textFor(status.getValue());
        setText(pendingText);
        listener = (observable, previous, current) -> queue(current);
        status.addListener(listener);
    }

    private void queue(InkInputStatus current) {
        pendingText = textFor(current);
        if (updateQueued) return;
        updateQueued = true;
        Platform.runLater(() -> {
            updateQueued = false;
            if (!pendingText.equals(getText())) setText(pendingText);
        });
    }

    static String textFor(InkInputStatus status) {
        if (status == null || !status.nativeActive()
                || (status.cursor() != InkInputCursor.PEN && status.cursor() != InkInputCursor.ERASER)) {
            return "Presión:   --";
        }
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, status.pressure())) * 100.0);
        return String.format(Locale.ROOT, "Presión: %3d%%", percent);
    }

    @Override
    public void close() {
        status.removeListener(listener);
    }
}
