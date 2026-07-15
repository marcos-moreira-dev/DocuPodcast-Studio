package com.marcosmoreiradev.docupodcaststudio.presentation.process;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.presentation.components.ActionButtonFactory;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.DocuPodcastShellViewModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Floating overlay for long-running document operations.
 *
 * <p>Audio jobs are generated outside JavaFX. This overlay only observes progress and must never
 * scan files or perform heavy work on the JavaFX application thread.</p>
 */
public final class LongProcessOverlayView extends VBox {
    private static final long SIZE_PROBE_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(3);
    private static final ExecutorService SIZE_PROBE_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "docupodcast-audio-size-probe");
        thread.setDaemon(true);
        return thread;
    });

    private final DocuPodcastShellViewModel viewModel;
    private final Label title = new Label();
    private final Label detail = new Label();
    private final Label eta = new Label();
    private final ProgressBar progress = new ProgressBar(0);
    private final BooleanProperty expanded;
    private final Button hide;
    private final Button cancel;
    private final AtomicBoolean sizeProbeRunning = new AtomicBoolean(false);
    private volatile String sizeProbeKey = "";
    private volatile String cachedSizeLabel = "audio generado en disco: calculando";
    private volatile long lastSizeProbeNanos = 0L;

    public LongProcessOverlayView(DocuPodcastShellViewModel viewModel, BooleanProperty expanded) {
        this.viewModel = viewModel;
        this.expanded = expanded;
        this.hide = ActionButtonFactory.secondary("Ocultar", () -> this.expanded.set(false));
        this.cancel = ActionButtonFactory.danger("Cancelar", viewModel::cancelActiveAudioJob);
        getStyleClass().add("process-overlay");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setMaxWidth(620);
        setMinWidth(520);
        setPickOnBounds(false);

        title.getStyleClass().add("process-overlay-title");
        detail.getStyleClass().add("process-overlay-detail");
        eta.getStyleClass().add("process-overlay-eta");
        detail.setWrapText(true);
        eta.setWrapText(true);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("process-overlay-progress");

        hide.getStyleClass().add("process-overlay-hide-button");
        HBox head = new HBox(10, title, spacer(), hide, cancel);
        head.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        getChildren().addAll(head, detail, progress, eta);

        viewModel.activeAudioJobStatusProperty().addListener((obs, oldValue, newValue) -> {
            if ((oldValue == null || !oldValue.running()) && newValue != null && newValue.running()) {
                expanded.set(true);
            }
            render(newValue);
        });
        expanded.addListener((obs, oldValue, newValue) -> render(viewModel.activeAudioJobStatusProperty().get()));
        render(viewModel.activeAudioJobStatusProperty().get());
    }

    private void render(AudioJobStatusDto status) {
        AudioJobStatusDto dto = status == null ? AudioJobStatusDto.idle() : status;
        boolean running = dto.running();
        boolean visible = running && expanded.get();
        setVisible(visible);
        setManaged(visible);
        hide.setDisable(!running);
        cancel.setDisable(!running);
        title.setText(running ? "Generando fragmentos de audio" : "Sin proceso activo");
        detail.setText(dto.statusLine());
        progress.setProgress(dto.progress());
        eta.setText(etaText(dto, cachedSizeLabel));
        requestSizeProbe(dto);
    }

    private String etaText(AudioJobStatusDto dto, String sizeLabel) {
        return dto.etaLabel() + " · " + dto.segmentCounterLabel() + " · " + sizeLabel
                + ". Puedes ocultar este panel; la generación continuará y podrás reabrir los detalles desde la barra de estado.";
    }

    private void requestSizeProbe(AudioJobStatusDto dto) {
        if (dto == null || !dto.running() || dto.outputDirectory().isBlank() || dto.totalSegments() <= 0) {
            return;
        }
        long now = System.nanoTime();
        String key = dto.jobId() + ":" + dto.completedSegments() + ":" + dto.totalSegments() + ":" + dto.outputDirectory();
        if (Objects.equals(key, sizeProbeKey) || now - lastSizeProbeNanos < SIZE_PROBE_INTERVAL_NANOS || !sizeProbeRunning.compareAndSet(false, true)) {
            return;
        }
        sizeProbeKey = key;
        lastSizeProbeNanos = now;
        SIZE_PROBE_EXECUTOR.submit(() -> {
            String label = generatedChunksSizeLabel(dto);
            Platform.runLater(() -> {
                cachedSizeLabel = label;
                AudioJobStatusDto current = viewModel.activeAudioJobStatusProperty().get();
                if (current != null && dto.jobId().equals(current.jobId())) {
                    eta.setText(etaText(current, cachedSizeLabel));
                }
                sizeProbeRunning.set(false);
            });
        });
    }

    private static String generatedChunksSizeLabel(AudioJobStatusDto dto) {
        long completedBytes = completedWavBytes(dto.outputDirectory());
        if (completedBytes <= 0L) {
            return dto.completedSegments() <= 0
                    ? "audio generado en disco: 0 B"
                    : "audio generado en disco: calculando";
        }
        return "audio generado en disco: " + humanSize(completedBytes);
    }

    private static long completedWavBytes(String outputDirectory) {
        try (Stream<Path> files = Files.walk(Path.of(outputDirectory))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wav"))
                    .mapToLong(LongProcessOverlayView::safeSize)
                    .sum();
        } catch (IllegalArgumentException | IOException | SecurityException ex) {
            return 0L;
        }
    }

    private static long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException | SecurityException ex) {
            return 0L;
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024.0) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
}
