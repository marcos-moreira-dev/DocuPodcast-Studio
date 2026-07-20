package com.marcosmoreiradev.docupodcaststudio.infrastructure.media;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class FileGenerationJobRepositoryTest {
    @TempDir Path projectRoot;

    @Test
    void persistsVersionedTypedJobUnderCapabilityDirectory() throws Exception {
        FileGenerationJobRepository repository = new FileGenerationJobRepository(projectRoot);
        GenerationJobRequest request = new GenerationJobRequest(new GenerationJobId("job-image-1"),
                CapabilityId.IMAGE_GENERATION, new EngineId("fake-image"), Map.of("consumer", "test"),
                Instant.parse("2026-07-19T12:00:00Z"), 2, new EnginePresetId("draft"),
                new ImageJobPayload("una escena", 1280, 720, Map.of("seed", "7")));
        GenerationJobSnapshot snapshot = new GenerationJobSnapshot(request, GenerationJobStatus.SUCCEEDED,
                "completed", 1, "ok", List.of(), "", 1, Instant.parse("2026-07-19T12:00:01Z"));

        repository.save(snapshot);

        Path file = projectRoot.resolve("jobs/generation/image-generation/job-image-1/job.json");
        assertTrue(Files.isRegularFile(file));
        String json = Files.readString(file);
        assertTrue(json.contains("\"capabilityId\":\"image-generation\""));
        assertTrue(json.contains("\"kind\":\"image-generation\""));
        GenerationJobSnapshot loaded = repository.find(request.jobId()).orElseThrow();
        assertInstanceOf(ImageJobPayload.class, loaded.request().payload());
        assertEquals("draft", loaded.request().presetId().value());
    }

    @Test
    void discoversLegacyJobAsReadOnlySchemaOne() throws Exception {
        Path legacy = projectRoot.resolve("jobs/JOB-OLD/job.json");
        Files.createDirectories(legacy.getParent());
        String original = "{\"status\":\"COMPLETED\",\"progress\":1,\"attempt\":2}";
        Files.writeString(legacy, original);

        GenerationJobSnapshot loaded = new FileGenerationJobRepository(projectRoot)
                .find(new GenerationJobId("JOB-OLD")).orElseThrow();

        assertEquals(1, loaded.request().schemaVersion());
        assertEquals(GenerationJobStatus.SUCCEEDED, loaded.status());
        assertEquals(original, Files.readString(legacy));
    }

    @Test
    void promotesArtifactsAtomicallyAndCleansAttemptStaging() throws Exception {
        FileGenerationJobRepository repository = new FileGenerationJobRepository(projectRoot);
        GenerationJobRequest request = new GenerationJobRequest(new GenerationJobId("job-voice-1"),
                CapabilityId.VOICE_SYNTHESIS, new EngineId("fake-voice"), Map.of(), Instant.now());
        Path promoted;
        Path stagingDirectory;
        try (GenerationArtifactStaging staging = repository.openStaging(request, 1)) {
            stagingDirectory = staging.directory();
            Path staged = stagingDirectory.resolve("segment.wav");
            Files.write(staged, new byte[]{1, 2, 3});
            GenerationArtifact artifact = staging.promote(List.of(
                    new GenerationArtifact("audio", staged.toUri(), Map.of("unitId", "one")))).getFirst();
            promoted = Path.of(artifact.location());
            assertTrue(Files.isRegularFile(promoted));
            assertTrue(promoted.startsWith(projectRoot.resolve("jobs/generation/voice-synthesis/job-voice-1/artifacts")));
        }
        assertFalse(Files.exists(stagingDirectory));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(promoted));
    }
}
