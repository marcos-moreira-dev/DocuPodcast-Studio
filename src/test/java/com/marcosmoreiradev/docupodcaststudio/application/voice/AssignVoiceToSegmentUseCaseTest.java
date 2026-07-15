package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AssignVoiceToSegmentUseCaseTest {
    @Test
    void assignsExistingVoiceCharacterAndStyleToSegment() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Word", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto de prueba", List.of("B001"))
        ));

        NarrationScriptDocument updated = new AssignVoiceToSegmentUseCase().assign(
                script, VoiceLibrary.defaults(), "SEG-001", "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL");

        assertEquals("VOC-NARRATOR", updated.segmentById("SEG-001").orElseThrow().voiceProfileId());
        assertEquals("CHR-NARRATOR", updated.segmentById("SEG-001").orElseThrow().characterId());
    }

    @Test
    void rejectsMissingVoiceCombination() {
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Word", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto", List.of("B001"))
        ));

        assertThrows(IllegalArgumentException.class, () -> new AssignVoiceToSegmentUseCase().assign(
                script, VoiceLibrary.defaults(), "SEG-001", "CHR-NARRATOR", "VOC-MISSING", "STY-NEUTRAL"));
    }
}
