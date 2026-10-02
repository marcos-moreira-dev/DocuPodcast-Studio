package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobRepository;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobStatus;

import java.time.Instant;
import java.util.Objects;

/** Reconciles only neutral jobs; legacy stores remain read-only. */
public final class GenerationJobRecoveryService {
    private final GenerationJobRepository repository;

    public GenerationJobRecoveryService(GenerationJobRepository repository) {
        this.repository = Objects.requireNonNull(repository, "generation job repository");
    }

    public int reconcileInterrupted() {
        int recovered = 0;
        for (GenerationJobSnapshot snapshot : repository.list()) {
            boolean interruptedState = snapshot.status() == GenerationJobStatus.RUNNING
                    || "staging".equalsIgnoreCase(snapshot.stage());
            if (snapshot.request().schemaVersion() < 2 || !interruptedState) continue;
            repository.save(new GenerationJobSnapshot(snapshot.request(), GenerationJobStatus.INTERRUPTED,
                    "recoverable", snapshot.progress(),
                    "Execution was interrupted. Retry creates a new attempt without executing stored commands.",
                    snapshot.artifacts(), "Interrupted during previous application session",
                    snapshot.attempt(), Instant.now()));
            recovered++;
        }
        return recovered;
    }
}
