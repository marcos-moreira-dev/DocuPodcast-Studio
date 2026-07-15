package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobSnapshotMapper;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MockAudioGenerationGatewayPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsJobAndSegmentStatusFilesDuringMockGeneration() throws Exception {
        AudioJobFileRepository repository = new AudioJobFileRepository();
        MockAudioGenerationGateway gateway = new MockAudioGenerationGateway(new InMemoryAudioJobQueue(), repository, 0);
        NarrationScriptDocument script = NarrationScriptDocument.create("Prueba", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Texto dos", List.of("B002"))
        ));
        CountDownLatch completed = new CountDownLatch(1);

        String jobId = gateway.submit(new AudioGenerationRequest(script, tempDir, "Prueba"), status -> {
            if (status.state() == AudioJobState.COMPLETED) {
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        Path jobDir = tempDir.resolve("jobs").resolve(jobId);
        assertTrue(Files.exists(jobDir.resolve("job.json")));
        assertTrue(Files.exists(jobDir.resolve("segments-status.json")));
        assertTrue(Files.readString(jobDir.resolve("segments-status.json")).contains("COMPLETED"));
        AudioJobStatusDto restored = AudioJobSnapshotMapper.toStatusDto(repository.load(tempDir, jobId).orElseThrow(), tempDir);
        assertEquals(AudioJobState.COMPLETED, restored.state());
        assertEquals(1.0, restored.progress(), 0.001);
    }
}
