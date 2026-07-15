package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MockAudioGenerationGatewayRenderPlanTest {
    @TempDir
    Path tempDir;

    @Test
    void writesWavsByRenderUnitAndSkipsVisualSilentUnits() throws Exception {
        MockAudioGenerationGateway gateway = new MockAudioGenerationGateway(new InMemoryAudioJobQueue(), 0);
        NarrationScriptDocument script = NarrationScriptDocument.create("Prueba TI2", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Uno", "Texto uno. Texto dos.", List.of("B001"))
        ));
        RenderUnit spoken = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 9), null,
                "Uno", "Texto uno", RenderUnitKind.SPOKEN_ONLY,
                "VOC-NARRATOR", "STY-NEUTRAL", "", "", 5.0, List.of());
        RenderUnit externalAudio = new RenderUnit("SEG-001-U002", "SEG-001-U002", "SEG-001", 1,
                new ScriptTextRange("SEG-001", 10, 19), null,
                "Dos", "Texto dos", RenderUnitKind.SPOKEN_ONLY,
                "", "", "AUD-EXTERNO", "", 5.0, List.of("LAYER-AUDIO"));
        RenderUnit visual = RenderUnit.visualSilent("VISUAL-B001", 2,
                new DocumentTextRange("B001", 0, 0), "Visual", "IMG-001", 5.0, List.of("LAYER-IMG"));
        RenderUnitPlan plan = new RenderUnitPlan("UNITPLAN-SCRIPT-001", script.id(),
                List.of(spoken, externalAudio, visual), 5.0, Instant.EPOCH);
        CountDownLatch completed = new CountDownLatch(1);
        ArrayList<AudioJobStatusDto> statuses = new ArrayList<>();

        String jobId = gateway.submit(new AudioGenerationRequest(script, plan, tempDir, "RenderPlan audio"), status -> {
            statuses.add(status);
            if (status.state() == AudioJobState.COMPLETED) {
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS), "El mock debe completar rápido");
        Path jobDir = tempDir.resolve("jobs").resolve(jobId);
        assertTrue(Files.exists(jobDir.resolve("audio/SEG-001-U001.wav")));
        assertFalse(Files.exists(jobDir.resolve("audio/SEG-001-U002.wav")), "El audio externo no se regenera por TTS");
        assertFalse(Files.exists(jobDir.resolve("audio/VISUAL-B001.wav")), "Los visuales silenciosos no entran al job TTS");
        assertEquals(1, statuses.get(statuses.size() - 1).totalSegments());
    }
}
