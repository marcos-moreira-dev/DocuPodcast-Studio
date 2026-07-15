package com.marcosmoreiradev.docupodcaststudio.presentation.script;

import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCue;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackCursor;
import com.marcosmoreiradev.docupodcaststudio.domain.playback.PlaybackManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssue;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptValidationIssueLevel;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ScriptSegmentPresentationTest {
    @Test
    void projectsVoiceAudioStoryboardValidationAndPlaybackStatus() {
        NarrationSegment segment = new NarrationSegment(
                "SEG-001",
                NarrationSegmentType.PARAGRAPH,
                "Intro",
                "Texto narrable",
                List.of("B001"),
                "CHR-NARRATOR",
                "VOC-NARRATOR",
                "STY-NEUTRAL",
                Map.of()
        );
        PlaybackManifest manifest = new PlaybackManifest(
                "PLAYBACK-001",
                "JOB-001",
                List.of(new PlaybackCue("SEG-001", 0, 2.5, "CLIP-001", "audio/SEG-001.wav", "IMG-001", "Intro")),
                "final/podcast.wav",
                Instant.now()
        );
        StoryboardDocument storyboard = new StoryboardDocument(
                "STORYBOARD-001",
                "Storyboard",
                "SCRIPT-001",
                List.of(StoryboardBinding.of("BIND-001", "SEG-001", "IMG-001", "Intro")),
                Map.of(),
                Instant.now(),
                Instant.now(),
                ""
        );

        ScriptSegmentPresentation presentation = ScriptSegmentPresentation.from(
                segment,
                VoiceLibrary.defaults(),
                manifest,
                storyboard,
                List.of(),
                new PlaybackCursor("SEG-001", 0.5, false)
        );

        assertTrue(presentation.voiceReady());
        assertTrue(presentation.audioReady());
        assertTrue(presentation.storyboardReady());
        assertEquals(ScriptValidationIssueLevel.INFO, presentation.validationLevel());
        assertTrue(presentation.playbackActive());
        assertEquals("Voz lista", presentation.voiceStatus());
        assertEquals("Audio generado", presentation.audioStatus());
        assertEquals("Imagen asociada", presentation.storyboardStatus());
    }

    @Test
    void warnsWhenVoiceIsMissingAudioIsPendingAndSegmentHasValidationIssues() {
        NarrationSegment segment = new NarrationSegment(
                "SEG-002",
                NarrationSegmentType.PARAGRAPH,
                "Cuerpo",
                "Texto narrable",
                List.of("B002"),
                "CHR-NARRATOR",
                "VOC-NO-EXISTE",
                "STY-NEUTRAL",
                Map.of()
        );

        ScriptSegmentPresentation presentation = ScriptSegmentPresentation.from(
                segment,
                VoiceLibrary.defaults(),
                PlaybackManifest.empty(),
                null,
                List.of(ScriptValidationIssue.warning("SEGMENT_NO_SOURCE", "Sin fuente", "SEG-002")),
                PlaybackCursor.stopped()
        );

        assertFalse(presentation.voiceReady());
        assertFalse(presentation.audioReady());
        assertFalse(presentation.storyboardReady());
        assertEquals(ScriptValidationIssueLevel.WARNING, presentation.validationLevel());
        assertEquals("Voz no encontrada", presentation.voiceStatus());
        assertEquals("Audio pendiente", presentation.audioStatus());
        assertEquals("Imagen pendiente", presentation.storyboardStatus());
    }
}
