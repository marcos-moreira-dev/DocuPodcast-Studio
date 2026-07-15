package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import java.util.Locale;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Floating reader control used by the main document surface.
 *
 * <p>This component keeps the global read-aloud controls close to the page without turning the
 * document workspace into an audio/job dashboard. It deliberately composes existing transversal
 * components so workspaces do not recreate styled JavaFX buttons by hand.</p>
 */
public final class FloatingReadingControlBar extends HBox {
    private static final double VERTICAL_CONTROL_WIDTH = 132.0;
    private static final double VERTICAL_CONTROL_MAX_WIDTH = 144.0;
    private static final double VERTICAL_PRIMARY_MAX_WIDTH = 126.0;

    private final Node horizontalContent;
    private final Node verticalContent;
    private boolean verticalLayout = true;

    public FloatingReadingControlBar(
            ObservableValue<String> actionLabel,
            ObservableValue<String> hint,
            Runnable primaryAction,
            Runnable onPause,
            Runnable onResume,
            Runnable onStop,
            Runnable onPlayFromBeginning,
            Runnable onPreviousFragment,
            Runnable onNextFragment,
            ObservableValue<Boolean> previousAvailable,
            ObservableValue<Boolean> nextAvailable,
            ObservableValue<Boolean> speedAvailable,
            ObservableValue<? extends Number> playbackRate,
            Runnable onSpeed1x,
            Runnable onSpeed15x,
            Runnable onSpeed175x,
            String secondaryLabel,
            Runnable secondaryAction) {
        super(0);
        getStyleClass().addAll(AppStyles.UI_FLOATING_READING_CONTROL, "document-operation-strip", "document-reading-glass-bar");
        setAlignment(Pos.CENTER_LEFT);
        setMaxHeight(Region.USE_PREF_SIZE);
        setMinHeight(Region.USE_PREF_SIZE);
        setPickOnBounds(false);

        horizontalContent = buildContent(false, actionLabel, hint, primaryAction, onPause, onResume, onStop,
                onPreviousFragment, onNextFragment, previousAvailable, nextAvailable, speedAvailable,
                playbackRate, onSpeed1x, onSpeed15x, onSpeed175x, secondaryLabel, secondaryAction);
        verticalContent = buildContent(true, actionLabel, hint, primaryAction, onPause, onResume, onStop,
                onPreviousFragment, onNextFragment, previousAvailable, nextAvailable, speedAvailable,
                playbackRate, onSpeed1x, onSpeed15x, onSpeed175x, secondaryLabel, secondaryAction);
        setVerticalLayout(false);
    }

    public void setVerticalLayout(boolean vertical) {
        if (verticalLayout == vertical) {
            return;
        }
        verticalLayout = vertical;
        getChildren().setAll(vertical ? verticalContent : horizontalContent);
        getStyleClass().remove("document-reading-glass-bar-vertical");
        if (vertical) {
            getStyleClass().add("document-reading-glass-bar-vertical");
            setPrefWidth(VERTICAL_CONTROL_WIDTH);
            setMaxWidth(VERTICAL_CONTROL_MAX_WIDTH);
        } else {
            setPrefWidth(Region.USE_COMPUTED_SIZE);
            setMaxWidth(820);
        }
    }

    private Node buildContent(
            boolean vertical,
            ObservableValue<String> actionLabel,
            ObservableValue<String> hint,
            Runnable primaryAction,
            Runnable onPause,
            Runnable onResume,
            Runnable onStop,
            Runnable onPreviousFragment,
            Runnable onNextFragment,
            ObservableValue<Boolean> previousAvailable,
            ObservableValue<Boolean> nextAvailable,
            ObservableValue<Boolean> speedAvailable,
            ObservableValue<? extends Number> playbackRate,
            Runnable onSpeed1x,
            Runnable onSpeed15x,
            Runnable onSpeed175x,
            String secondaryLabel,
            Runnable secondaryAction) {
        ObservableValue<String> effectiveActionLabel = vertical ? verticalActionLabel(actionLabel) : actionLabel;
        PrimaryActionStrip primary = new PrimaryActionStrip(effectiveActionLabel, hint, primaryAction, false);
        primary.setMaxWidth(vertical ? VERTICAL_PRIMARY_MAX_WIDTH : Region.USE_COMPUTED_SIZE);

        if (vertical) {
            Button previous = previousButton(onPreviousFragment, previousAvailable);
            Button next = nextButton(onNextFragment, nextAvailable);
            Button refresh = refreshButton(secondaryLabel, secondaryAction);

            VBox box = new VBox(7);
            box.getStyleClass().add("document-reading-control-vertical-content");
            box.getChildren().addAll(
                    primary,
                    pauseButton(onPause),
                    resumeButton(onResume),
                    stopButton(onStop),
                    speedButton("1x", 1.0, onSpeed1x, speedAvailable, playbackRate),
                    speedButton("1.5x", 1.5, onSpeed15x, speedAvailable, playbackRate),
                    speedButton("1.75x", 1.75, onSpeed175x, speedAvailable, playbackRate),
                    previous,
                    next);
            if (refresh != null) {
                box.getChildren().add(refresh);
            }
            return box;
        }

        TransportControls transport = new TransportControls(onPause, onResume, onStop);
        transport.getStyleClass().add(AppStyles.UI_FLOATING_READING_TRANSPORT);

        HBox speed = new HBox(4, speedButton("1x", 1.0, onSpeed1x, speedAvailable, playbackRate),
                speedButton("1.5x", 1.5, onSpeed15x, speedAvailable, playbackRate),
                speedButton("1.75x", 1.75, onSpeed175x, speedAvailable, playbackRate));
        speed.getStyleClass().add("ui-playback-speed-controls");

        Button previous = previousButton(onPreviousFragment, previousAvailable);
        Button next = nextButton(onNextFragment, nextAvailable);
        HBox nav = new HBox(4, previous, next);
        nav.getStyleClass().add("document-reading-control-nav");

        Button refresh = refreshButton(secondaryLabel, secondaryAction);

        HBox box = new HBox(10);
        box.getStyleClass().add("document-reading-control-horizontal-content");
        HBox.setHgrow(primary, Priority.ALWAYS);
        box.getChildren().addAll(primary, transport, speed, nav);
        if (refresh != null) {
            box.getChildren().add(refresh);
        }
        return box;
    }

    private Button pauseButton(Runnable onPause) {
        return ActionButtonFactory.transportIcon(AppIcon.PAUSE, "Pausar: detener temporalmente la lectura actual.", onPause);
    }

    private Button resumeButton(Runnable onResume) {
        return ActionButtonFactory.transportIcon(AppIcon.RESUME, "Reanudar: continuar la lectura desde el punto actual.", onResume);
    }

    private Button stopButton(Runnable onStop) {
        return ActionButtonFactory.transportIcon(AppIcon.STOP, "Detener: cancelar la reproducci\u00f3n actual.", onStop);
    }

    private Button previousButton(Runnable onPreviousFragment, ObservableValue<Boolean> previousAvailable) {
        Button previous = ActionButtonFactory.transportIcon(AppIcon.PREVIOUS_FRAGMENT, "Fragmento anterior: detener el actual y reproducir el fragmento anterior disponible.", onPreviousFragment);
        bindAvailability(previous, previousAvailable);
        return previous;
    }

    private Button nextButton(Runnable onNextFragment, ObservableValue<Boolean> nextAvailable) {
        Button next = ActionButtonFactory.transportIcon(AppIcon.NEXT_FRAGMENT, "Siguiente fragmento: detener el actual y reproducir el siguiente fragmento disponible.", onNextFragment);
        bindAvailability(next, nextAvailable);
        return next;
    }

    private Button refreshButton(String secondaryLabel, Runnable secondaryAction) {
        if (secondaryLabel == null || secondaryLabel.isBlank()) {
            return null;
        }
        return ActionButtonFactory.transportIcon(AppIcon.REFRESH, "Refrescar contenido: actualizar la copia del documento dentro del proyecto.", secondaryAction);
    }

    private static ObservableValue<String> verticalActionLabel(ObservableValue<String> actionLabel) {
        return Bindings.createStringBinding(() -> compactVerticalActionLabel(actionLabel.getValue()), actionLabel);
    }

    private static String compactVerticalActionLabel(String label) {
        String text = label == null ? "" : label.strip();
        if (text.isBlank()) {
            return "Reproducir";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("selecci")) {
            return "Reproducir\nselección";
        }
        if (lower.contains("documento")) {
            return "Reproducir\ndocumento";
        }
        if (lower.contains("desde aqu")) {
            return "Reproducir\ndesde aquí";
        }
        return text.length() > 14 ? text.replaceFirst("\\s+", "\n") : text;
    }

    private Button speedButton(String label, double rate, Runnable action, ObservableValue<Boolean> available, ObservableValue<? extends Number> playbackRate) {
        Button button = ActionButtonFactory.transport(label, action);
        button.getStyleClass().add("ui-playback-speed-button");
        button.setMinWidth(44);
        button.setPrefWidth(52);
        button.setMaxWidth(58);
        button.disableProperty().bind(Bindings.createBooleanBinding(() -> !Boolean.TRUE.equals(available.getValue()), available));
        playbackRate.addListener((obs, oldValue, newValue) -> markActiveRate(button, rate, newValue));
        markActiveRate(button, rate, playbackRate.getValue());
        return button;
    }

    private void markActiveRate(Button button, double target, Number current) {
        boolean active = current != null && Math.abs(current.doubleValue() - target) < 0.01;
        button.getStyleClass().remove("ui-playback-speed-active");
        if (active) {
            button.getStyleClass().add("ui-playback-speed-active");
        }
    }

    private void bindAvailability(Button button, ObservableValue<Boolean> available) {
        if (button == null || available == null) {
            return;
        }
        button.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> !Boolean.TRUE.equals(available.getValue()), available));
    }
}
