package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Generates one intervention in isolation and patches the playable job only after full validation. */
public final class RegenerateTheatreInterventionAudioUseCase {
    private final SubmitAudioGenerationJobUseCase submit;
    private final LoadPersistedAudioJobUseCase load;
    private final ManualAudioSegmentJobUseCase patch;

    public RegenerateTheatreInterventionAudioUseCase(SubmitAudioGenerationJobUseCase submit,
                                                     LoadPersistedAudioJobUseCase load,
                                                     ManualAudioSegmentJobUseCase patch) {
        this.submit = Objects.requireNonNull(submit, "submit");
        this.load = Objects.requireNonNull(load, "load");
        this.patch = Objects.requireNonNull(patch, "patch");
    }

    public String regenerate(AudioGenerationRequest request, AudioJobSnapshot target, List<String> expectedUnitIds,
                             Consumer<AudioJobStatusDto> progress, Consumer<AudioJobSnapshot> success,
                             Consumer<Throwable> failure) {
        AtomicBoolean terminal = new AtomicBoolean(false);
        try {
            return submit.submit(request, status -> {
                if (progress != null) progress.accept(status);
                if (status == null || status.running() || !terminal.compareAndSet(false, true)) return;
                try {
                    if (!status.completed()) throw new IOException(status.message().isBlank()
                            ? "El motor de voz no completo la regeneracion puntual." : status.message());
                    AudioJobSnapshot staged = load.load(request.projectDirectory(), status.jobId())
                            .orElseThrow(() -> new IOException("No se encontro el job aislado " + status.jobId() + "."));
                    AudioJobSnapshot updated = patch.applyGeneratedBatch(request.projectDirectory(), target, staged, expectedUnitIds);
                    discardWorkspace(request.projectDirectory(), staged.jobRelativeDirectory());
                    if (success != null) success.accept(updated);
                } catch (Throwable ex) {
                    if (failure != null) failure.accept(ex);
                }
            });
        } catch (Throwable ex) {
            terminal.set(true);
            if (failure != null) failure.accept(ex);
            return "";
        }
    }

    private static void discardWorkspace(Path projectDirectory, String relative) {
        Path jobs = projectDirectory.toAbsolutePath().normalize().resolve("jobs").normalize();
        Path directory = projectDirectory.toAbsolutePath().normalize().resolve(relative).normalize();
        if (!directory.startsWith(jobs) || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }
}
