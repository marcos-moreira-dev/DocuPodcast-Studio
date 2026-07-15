package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MockAudioGenerationGatewayTest {
    @TempDir
    Path tempDir;

    @Test
    void writesMockWavsAndManifestWhileReportingCompletion() throws Exception {
        MockAudioGenerationGateway gateway = new MockAudioGenerationGateway(new InMemoryAudioJobQueue(), 0);
        NarrationScriptDocument script = NarrationScriptDocument.create("Prueba", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno", List.of("B001")),
                NarrationSegment.of("SEG-002", NarrationSegmentType.PARAGRAPH, "Dos", "Texto dos", List.of("B002"))
        ));
        CountDownLatch completed = new CountDownLatch(1);
        ArrayList<AudioJobStatusDto> statuses = new ArrayList<>();

        String jobId = gateway.submit(new AudioGenerationRequest(script, tempDir, "Prueba"), status -> {
            statuses.add(status);
            if (status.state() == AudioJobState.COMPLETED) {
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS), "El mock debe completar rápido");
        Path jobDir = tempDir.resolve("jobs").resolve(jobId);
        assertTrue(Files.exists(jobDir.resolve("audio/SEG-001.wav")));
        assertTrue(Files.exists(jobDir.resolve("audio/SEG-002.wav")));
        assertTrue(Files.exists(jobDir.resolve("final/podcast-mock.wav")));
        assertTrue(Files.exists(jobDir.resolve("audio-manifest.json")));
        assertEquals(AudioJobState.COMPLETED, statuses.get(statuses.size() - 1).state());
        assertEquals(1.0, statuses.get(statuses.size() - 1).progress(), 0.001);
    }
}
