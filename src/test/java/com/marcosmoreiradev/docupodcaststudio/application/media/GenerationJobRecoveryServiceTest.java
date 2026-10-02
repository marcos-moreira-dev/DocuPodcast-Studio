package com.marcosmoreiradev.docupodcaststudio.application.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenerationJobRecoveryServiceTest {
    @Test void marksOnlyNeutralRunningJobsAsRecoverableAndLeavesLegacyUntouched() {
        MemoryRepository repository = new MemoryRepository();
        repository.jobs.add(snapshot("neutral", 2, GenerationJobStatus.RUNNING));
        repository.jobs.add(snapshot("staging", 2, GenerationJobStatus.QUEUED, "staging"));
        repository.jobs.add(snapshot("legacy", 1, GenerationJobStatus.RUNNING));
        repository.jobs.add(snapshot("done", 2, GenerationJobStatus.SUCCEEDED));

        int recovered = new GenerationJobRecoveryService(repository).reconcileInterrupted();

        assertEquals(2, recovered);
        assertEquals(GenerationJobStatus.INTERRUPTED, repository.find(new GenerationJobId("neutral")).orElseThrow().status());
        assertEquals("recoverable", repository.find(new GenerationJobId("neutral")).orElseThrow().stage());
        assertEquals(GenerationJobStatus.INTERRUPTED, repository.find(new GenerationJobId("staging")).orElseThrow().status());
        assertEquals(GenerationJobStatus.RUNNING, repository.find(new GenerationJobId("legacy")).orElseThrow().status());
    }

    private static GenerationJobSnapshot snapshot(String id, int version, GenerationJobStatus status) {
        return snapshot(id, version, status, "running");
    }

    private static GenerationJobSnapshot snapshot(String id, int version, GenerationJobStatus status, String stage) {
        GenerationJobRequest request = new GenerationJobRequest(new GenerationJobId(id), CapabilityId.IMAGE_GENERATION,
                new EngineId("fake"), Map.of(), Instant.EPOCH, version, EnginePresetId.AUTO,
                new EmptyGenerationPayload(Map.of()));
        return new GenerationJobSnapshot(request, status, stage, .4, "", List.of(), "", 1, Instant.EPOCH);
    }

    private static final class MemoryRepository implements GenerationJobRepository {
        private final List<GenerationJobSnapshot> jobs = new ArrayList<>();
        @Override public void save(GenerationJobSnapshot snapshot) {
            jobs.removeIf(item -> item.request().jobId().equals(snapshot.request().jobId()));
            jobs.add(snapshot);
        }
        @Override public Optional<GenerationJobSnapshot> find(GenerationJobId id) {
            return jobs.stream().filter(item -> item.request().jobId().equals(id)).findFirst();
        }
        @Override public List<GenerationJobSnapshot> list() { return List.copyOf(jobs); }
    }
}
