package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;

import java.util.List;
import java.util.function.Consumer;
import java.time.Duration;

/** Port for local audio generation engines. */
public interface AudioGenerationGateway {
    /** Describes the engine currently behind this gateway. */
    default AudioEngineDescriptor engineDescriptor() {
        return AudioEngineDescriptor.unavailable("unconfigured", "Motor de voz", "No hay un motor de voz registrado.");
    }

    String submit(AudioGenerationRequest request, Consumer<AudioJobStatusDto> statusConsumer);

    /**
     * Resumes a persisted job snapshot using the same output directory and skipping completed segments.
     *
     * <p>The method is intentionally part of the engine port before the real TTS gateway exists so the
     * mock and XTTS/Piper implementations share one recovery contract.</p>
     */
    String resume(AudioGenerationRequest request, AudioJobSnapshot snapshot, Consumer<AudioJobStatusDto> statusConsumer);

    boolean cancel(String jobId);

    List<AudioJobStatusDto> listStatuses();

    default boolean awaitTermination(String jobId, Duration timeout) throws InterruptedException {
        String target = jobId == null ? "" : jobId.strip();
        if (target.isBlank()) return true;
        long deadline = System.nanoTime() + Math.max(1L, timeout == null ? Duration.ofSeconds(30).toNanos() : timeout.toNanos());
        while (System.nanoTime() < deadline) {
            AudioJobStatusDto status = listStatuses().stream().filter(item -> item.jobId().equals(target)).findFirst().orElse(null);
            if (status == null || status.state().terminal()) return true;
            Thread.sleep(50L);
        }
        return false;
    }
}
