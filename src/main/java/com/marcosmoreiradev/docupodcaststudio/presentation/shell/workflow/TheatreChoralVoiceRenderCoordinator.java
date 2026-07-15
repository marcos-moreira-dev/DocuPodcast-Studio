package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.RenderTheatreChoralVoiceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceRenderProgress;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceRenderRequest;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceRenderResult;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Keeps background execution and option validation out of the shell view-model. */
public final class TheatreChoralVoiceRenderCoordinator {
    private final BooleanProperty running = new SimpleBooleanProperty(false);
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Sin render multipersona activo.");

    public ReadOnlyBooleanProperty runningProperty() { return running; }
    public ReadOnlyDoubleProperty progressProperty() { return progress; }
    public ReadOnlyStringProperty statusProperty() { return status.getReadOnlyProperty(); }

    public boolean canRender(List<TheatreChoralVoiceWorkflow.Option> options,
                             List<String> participantIds,
                             boolean theatreMode,
                             boolean interventionSelected,
                             boolean anotherAudioJobRunning) {
        if (!theatreMode || !interventionSelected || anotherAudioJobRunning) return false;
        List<String> selected = normalize(participantIds);
        if (selected.size() < 2) return false;
        Map<String, TheatreChoralVoiceWorkflow.Option> byId = options.stream()
                .collect(Collectors.toMap(TheatreChoralVoiceWorkflow.Option::characterId, option -> option));
        return selected.stream().allMatch(id -> byId.containsKey(id) && byId.get(id).voiceReady());
    }

    public void start(RenderTheatreChoralVoiceUseCase useCase,
                      TheatreChoralVoiceRenderRequest request,
                      Consumer<TheatreChoralVoiceRenderResult> success,
                      Consumer<String> statusSink) {
        Objects.requireNonNull(useCase, "useCase");
        Objects.requireNonNull(request, "request");
        Consumer<TheatreChoralVoiceRenderResult> successListener = success == null ? ignored -> { } : success;
        Consumer<String> sink = statusSink == null ? ignored -> { } : statusSink;
        begin("Preparando voces de " + request.interventionId() + "...", sink);
        Thread worker = new Thread(() -> {
            try {
                TheatreChoralVoiceRenderResult result = useCase.execute(request,
                        update -> Platform.runLater(() -> acceptProgress(update, sink)));
                Platform.runLater(() -> successListener.accept(result));
            } catch (Throwable ex) {
                Platform.runLater(() -> fail("No se pudo renderizar la voz multipersona: " + rootCauseMessage(ex), sink));
            }
        }, "theatre-choral-voice-render");
        worker.setDaemon(true);
        worker.start();
    }

    public void complete(String message, Consumer<String> statusSink) {
        running.set(false);
        progress.set(1.0);
        setStatus(message, statusSink);
    }

    public void fail(String message, Consumer<String> statusSink) {
        running.set(false);
        progress.set(0.0);
        setStatus(message, statusSink);
    }

    private void begin(String message, Consumer<String> statusSink) {
        running.set(true);
        progress.set(0.0);
        setStatus(message, statusSink);
    }

    private void acceptProgress(TheatreChoralVoiceRenderProgress update, Consumer<String> statusSink) {
        progress.set(update.fraction());
        setStatus(update.message(), statusSink);
    }

    private void setStatus(String message, Consumer<String> statusSink) {
        String normalized = message == null || message.isBlank() ? "Sin estado multipersona." : message.strip();
        status.set(normalized);
        if (statusSink != null) statusSink.accept(normalized);
    }

    private static String rootCauseMessage(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null && current.getCause() != current) current = current.getCause();
        String message = current == null ? "Error desconocido." : current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message.strip();
    }

    public static List<String> normalize(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}
