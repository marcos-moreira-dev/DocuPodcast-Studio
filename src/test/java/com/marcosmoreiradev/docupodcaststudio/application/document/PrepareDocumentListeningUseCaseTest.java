package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobStatusDto;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackBufferPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrepareDocumentListeningUseCaseTest {
    private final PrepareDocumentListeningUseCase useCase = new PrepareDocumentListeningUseCase();

    @Test
    void startsFromDocumentAndBuildsProjectionBeforeAudio() {
        DocumentListenPlan plan = useCase.prepare(document(), null, PlaybackManifest.empty(), false, true,
                AudioJobStatusDto.idle(), PlaybackBufferPolicy.defaultPolicy());

        assertEquals(DocumentListenPhase.BUILD_NARRATION_PROJECTION, plan.phase());
        assertTrue(plan.requiresNarrationProjection());
        assertTrue(plan.keepReaderVisible());
        assertFalse(plan.requiresAudioGeneration());
    }

    @Test
    void asksToSaveBeforeGeneratingAudioAssets() {
        DocumentListenPlan plan = useCase.prepare(document(), projection(), PlaybackManifest.empty(), false, false,
                AudioJobStatusDto.idle(), PlaybackBufferPolicy.defaultPolicy());

        assertEquals(DocumentListenPhase.SAVE_PROJECT_REQUIRED, plan.phase());
        assertTrue(plan.userMessage().contains("Guarda el proyecto"));
    }

    @Test
    void reusesExistingPlaybackManifestBeforeGeneratingNewAudio() {
        PlaybackManifest manifest = new PlaybackManifest("PLAYBACK-001", "JOB-001",
                List.of(new PlaybackCue("SEG-001", 0, 3, "CLIP-001", "audio/seg-001.wav", "", "Intro")),
                "", Instant.now());

        DocumentListenPlan plan = useCase.prepare(document(), projection(), manifest, false, true,
                AudioJobStatusDto.idle(), PlaybackBufferPolicy.defaultPolicy());

        assertEquals(DocumentListenPhase.PLAY_EXISTING_AUDIO, plan.phase());
        assertTrue(plan.canPlayImmediately());
    }

    @Test
    void reportsBufferStatusWhileAudioJobIsRunning() {
        AudioJobStatusDto running = new AudioJobStatusDto("JOB-001", "Documento", AudioJobState.GENERATING_AUDIO,
                AudioGenerationStage.GENERATING_SEGMENTS, 2, 8, 0, 0.25, "SEG-002", "Bloque 2", 120,
                "Generando", "jobs/JOB-001", "", "");

        DocumentListenPlan plan = useCase.prepare(document(), projection(), PlaybackManifest.empty(), true, true,
                running, PlaybackBufferPolicy.defaultPolicy());

        assertEquals(DocumentListenPhase.WAIT_FOR_AUDIO_BUFFER, plan.phase());
        assertTrue(plan.userMessage().contains("Fragmentos listos: 2/8"));
    }

    private static ReadableDocument document() {
        return new ReadableDocument("Demo", SourceDocumentFormat.TXT, Path.of("demo.txt"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Este es un bloque narrable.", "paragraph")
        ));
    }

    private static NarrationScriptDocument projection() {
        return NarrationScriptDocument.create("Demo", "es", "Demo", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Este es un bloque narrable.", List.of("B001"))
        ));
    }
}
