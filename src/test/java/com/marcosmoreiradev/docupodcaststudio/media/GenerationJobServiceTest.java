package com.marcosmoreiradev.docupodcaststudio.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationArtifact;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobId;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobService;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationJobStatus;
import com.marcosmoreiradev.docupodcaststudio.media.api.InMemoryGenerationJobRepository;
import com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GenerationJobServiceTest {
    @Test
    void retriesPersistsProgressAndPublishesNeutralArtifacts() throws Exception {
        InMemoryGenerationJobRepository repository = new InMemoryGenerationJobRepository();
        AtomicInteger attempts = new AtomicInteger();
        GenerationJobRequest request = request();
        try (GenerationJobService jobs = new GenerationJobService(repository, 1)) {
            jobs.submit(request, context -> {
                context.progress().report("synthesis", .5, "half");
                if (attempts.incrementAndGet() == 1) throw new IllegalStateException("retry me");
                return List.of(new GenerationArtifact("audio", URI.create("file:///voice.wav"), Map.of()));
            }, new ExecutionPolicy(Duration.ofSeconds(2), 2), ResourceLease.NONE);

            GenerationJobSnapshot completed = awaitTerminal(repository, request.jobId());
            assertEquals(GenerationJobStatus.SUCCEEDED, completed.status());
            assertEquals(2, completed.attempt());
            assertEquals("audio", completed.artifacts().getFirst().kind());
        }
    }

    @Test
    void cancellationIsCoordinatedOutsideTheEngine() throws Exception {
        InMemoryGenerationJobRepository repository = new InMemoryGenerationJobRepository();
        GenerationJobRequest request = request();
        try (GenerationJobService jobs = new GenerationJobService(repository, 1)) {
            jobs.submit(request, context -> {
                while (!context.cancellation().cancellationRequested()) Thread.sleep(5);
                return List.of();
            }, new ExecutionPolicy(Duration.ofSeconds(3), 1), ResourceLease.NONE);
            assertTrue(jobs.cancel(request.jobId()));
            assertEquals(GenerationJobStatus.CANCELLED, awaitTerminal(repository, request.jobId()).status());
        }
    }

    @Test
    void timeoutIsPersistedEvenWhenTheWorkerIsInterrupted() throws Exception {
        InMemoryGenerationJobRepository repository = new InMemoryGenerationJobRepository();
        GenerationJobRequest request = request();
        try (GenerationJobService jobs = new GenerationJobService(repository, 1)) {
            jobs.submit(request, context -> {
                Thread.sleep(5_000);
                return List.of();
            }, new ExecutionPolicy(Duration.ofMillis(50), 1), ResourceLease.NONE);
            GenerationJobSnapshot failed = awaitTerminal(repository, request.jobId());
            assertEquals(GenerationJobStatus.FAILED, failed.status());
            assertEquals("timeout", failed.stage());
        }
    }

    private static GenerationJobRequest request() {
        return new GenerationJobRequest(GenerationJobId.create(), CapabilityId.VOICE_SYNTHESIS,
                new EngineId("fake"), Map.of("text", "hola"), Instant.now());
    }

    private static GenerationJobSnapshot awaitTerminal(InMemoryGenerationJobRepository repository,
                                                       GenerationJobId id) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (System.nanoTime() < deadline) {
            GenerationJobSnapshot snapshot = repository.find(id).orElseThrow();
            if (snapshot.status() == GenerationJobStatus.SUCCEEDED
                    || snapshot.status() == GenerationJobStatus.FAILED
                    || snapshot.status() == GenerationJobStatus.CANCELLED) return snapshot;
            Thread.sleep(10);
        }
        throw new AssertionError("job did not finish");
    }
}
