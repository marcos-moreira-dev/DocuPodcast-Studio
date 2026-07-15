package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.playback.NoopSegmentAudioPlayer;
import com.marcosmoreiradev.docupodcaststudio.application.playback.SegmentAudioPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Plays the local advanced voice proof WAV through the app audio path and confirms it only after
 * the internal player reports a natural end-of-file event.
 */
public final class ConfirmXttsSmokePlaybackUseCase {
    private static final int PLAYBACK_CONFIRMATION_TIMEOUT_SECONDS = 90;

    private final InspectXttsSmokeTestUseCase inspector;
    private final SegmentAudioPlayer player;

    public ConfirmXttsSmokePlaybackUseCase() {
        this(new InspectXttsSmokeTestUseCase(), new NoopSegmentAudioPlayer());
    }

    public ConfirmXttsSmokePlaybackUseCase(InspectXttsSmokeTestUseCase inspector, SegmentAudioPlayer player) {
        this.inspector = Objects.requireNonNull(inspector, "inspector");
        this.player = Objects.requireNonNull(player, "player");
    }

    public XttsSmokeTestReport confirm(Path applicationRoot) {
        return confirm(applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsSmokeTestReport confirm(Path applicationRoot, ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        XttsSmokeTestReport current = inspector.inspect(applicationRoot);
        if (!current.generatedWavProof()) {
            return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                    false, false, current.outputBytes(), current.generatedAt(), current.issues(),
                    "Primero genera una prueba WAV real de Voz IA avanzada.");
        }
        if (current.playbackConfirmed()) {
            return current;
        }
        if (!player.available()) {
            return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                    current.generated(), false, current.outputBytes(), current.generatedAt(),
                    List.of("El reproductor interno no está disponible en este equipo."),
                    "No se pudo reproducir la prueba dentro de la app porque el reproductor interno no está disponible.");
        }
        if (!Files.isRegularFile(current.audioFile())) {
            return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                    false, false, current.outputBytes(), current.generatedAt(),
                    List.of("No existe el WAV de prueba: " + current.audioFile()),
                    "No se encontró el WAV de prueba para reproducirlo.");
        }

        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<RuntimeException> writeError = new AtomicReference<>();
        Path expected = current.audioFile().toAbsolutePath().normalize();
        player.setOnPlaybackFinished(completed -> {
            Path actual = completed == null ? null : completed.toAbsolutePath().normalize();
            if (expected.equals(actual)) {
                try {
                    markPlaybackConfirmed(current.manifestFile(), Instant.now());
                } catch (RuntimeException ex) {
                    writeError.set(ex);
                } finally {
                    finished.countDown();
                }
            }
        });

        try {
            progress.onProgress("Reproduciendo prueba WAV desde la app...");
            player.stop();
            player.setPlaybackRate(1.0);
            player.play(current.audioFile(), 0.0);
            boolean completed = finished.await(PLAYBACK_CONFIRMATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                player.stop();
                return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                        current.generated(), false, current.outputBytes(), current.generatedAt(),
                        List.of("La reproducción de prueba no terminó dentro del tiempo esperado."),
                        "La prueba empezó, pero no se pudo confirmar el final de reproducción dentro de la app.");
            }
            if (writeError.get() != null) {
                RuntimeException error = writeError.get();
                return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                        current.generated(), false, current.outputBytes(), current.generatedAt(), List.of(readable(error)),
                        "La prueba se reprodujo, pero no se pudo guardar la confirmación: " + readable(error));
            }
            progress.onProgress("Reproducción de prueba confirmada dentro de la app.");
            return inspector.inspect(applicationRoot);
        } catch (IOException | RuntimeException ex) {
            return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                    current.generated(), false, current.outputBytes(), current.generatedAt(), List.of(readable(ex)),
                    "No se pudo reproducir la prueba dentro de la app: " + readable(ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new XttsSmokeTestReport(current.smokeDirectory(), current.textFile(), current.audioFile(), current.manifestFile(),
                    current.generated(), false, current.outputBytes(), current.generatedAt(), List.of("interrumpido"),
                    "La reproducción de prueba fue interrumpida antes de confirmarse.");
        }
    }

    static void markPlaybackConfirmed(Path manifest, Instant confirmedAt) {
        if (manifest == null) {
            throw new IllegalArgumentException("manifest requerido");
        }
        try {
            String json = Files.isRegularFile(manifest) ? Files.readString(manifest) : "{\n}";
            String updated = json.contains("\"playbackConfirmed\": false")
                    ? json.replace("\"playbackConfirmed\": false", "\"playbackConfirmed\": true")
                    : json;
            if (!updated.contains("\"playbackConfirmed\": true")) {
                updated = insertJsonField(updated, "playbackConfirmed", "true", false);
            }
            if (!updated.contains("\"playbackConfirmedAt\"")) {
                updated = insertJsonField(updated, "playbackConfirmedAt", confirmedAt == null ? Instant.now().toString() : confirmedAt.toString(), true);
            }
            if (!updated.contains("\"playbackConfirmedBy\"")) {
                updated = insertJsonField(updated, "playbackConfirmedBy", "app-internal-player", true);
            }
            Files.writeString(manifest, updated, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("no se pudo escribir confirmación de reproducción", ex);
        }
    }

    private static String insertJsonField(String json, String key, String value, boolean quoted) {
        String clean = json == null || json.isBlank() ? "{\n}" : json.stripTrailing();
        int close = clean.lastIndexOf('}');
        String field = "  \"" + escape(key) + "\": " + (quoted ? "\"" + escape(value) + "\"" : value);
        if (close < 0) {
            return "{\n" + field + "\n}\n";
        }
        String before = clean.substring(0, close).stripTrailing();
        String after = clean.substring(close);
        boolean needsComma = before.contains(":") && !before.endsWith("{") && !before.endsWith(",");
        return before + (needsComma ? "," : "") + "\n" + field + "\n" + after + (clean.endsWith("\n") ? "" : "\n");
    }

    private static String readable(Throwable error) {
        if (error == null) {
            return "error desconocido";
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message.strip();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
