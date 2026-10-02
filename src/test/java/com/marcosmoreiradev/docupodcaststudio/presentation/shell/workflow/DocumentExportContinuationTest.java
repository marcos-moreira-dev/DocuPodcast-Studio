package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.ExportExecutionMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.export.PreparedExportIntent;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DocumentExportContinuationTest {
    @Test
    void completeCoverageInvokesRendererExactlyOnce() {
        AtomicInteger rendererCalls = new AtomicInteger();
        var continuation = new DocumentExportContinuation("CORR-1",
                java.util.stream.IntStream.rangeClosed(1, 17)
                        .mapToObj(index -> "SEG-" + index).toList(),
                rendererCalls::incrementAndGet);

        continuation.claimAfterCoverage(true).orElseThrow().run();
        assertTrue(continuation.claimAfterCoverage(true).isEmpty());

        assertEquals(1, rendererCalls.get());
        assertEquals(DocumentExportContinuation.State.EXPORT_STARTED,
                continuation.state());
    }

    @Test
    void incompleteCoverageIsTerminalAndCannotCreateDialogStyleLoop() {
        AtomicInteger rendererCalls = new AtomicInteger();
        var continuation = new DocumentExportContinuation("CORR-2",
                List.of("SEG-FAILED"), rendererCalls::incrementAndGet);

        assertTrue(continuation.claimAfterCoverage(false).isEmpty());
        assertTrue(continuation.claimAfterCoverage(true).isEmpty());

        assertEquals(0, rendererCalls.get());
        assertEquals(DocumentExportContinuation.State.FAILED, continuation.state());
    }

    @Test
    void keepsImmutableDestinationAndExecutionModeAcrossAudioPreparation() {
        PreparedExportIntent intent = new PreparedExportIntent(
                AppCommandId.EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO,
                ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT,
                Path.of("build", "exports", "night.mp4"),
                "Video documental");
        var continuation = new DocumentExportContinuation(
                "CORR-NIGHT", List.of("SEG-1"), intent, () -> { });

        assertEquals(ExportExecutionMode.PREPARE_FULL_DOCUMENT_AND_EXPORT,
                continuation.intent().orElseThrow().executionMode());
        assertEquals("night.mp4",
                continuation.intent().orElseThrow().targetFile().getFileName().toString());
    }

    @Test
    void tracksPhysicalTtsUnitsSeparatelyFromNarrationSegments() {
        var continuation = new DocumentExportContinuation(
                "CORR-TTS", List.of("SEG-1", "SEG-2"), () -> { });

        continuation.recordAudioPlan(5, 3);

        assertEquals(2, continuation.segmentIds().size());
        assertEquals(5, continuation.plannedTtsUnits());
        assertEquals(3, continuation.initiallyReusableTtsUnits());
    }
}
