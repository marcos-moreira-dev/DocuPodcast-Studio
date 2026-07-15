package com.marcosmoreiradev.docupodcaststudio.application.recording;

import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrepareRecordingActionUseCaseTest {
    @Test
    void preparesHumanVoiceRecordingForSelectedText() {
        RecordingActionPlan plan = new PrepareRecordingActionUseCase()
                .prepare(RecordingPurpose.HUMAN_VOICE_FOR_TEXT, new ScriptTextRange("SEG-010", 0, 42));

        assertEquals(RecordingPurpose.HUMAN_VOICE_FOR_TEXT, plan.purpose());
        assertEquals("SEG-010-human-voice.wav", plan.suggestedFileName());
        assertTrue(plan.requiresProjectFile());
        assertTrue(plan.requiresMicrophone());
    }

    @Test
    void preparesNoteOrReferenceRecordingWithoutSpeechToText() {
        RecordingActionPlan plan = new PrepareRecordingActionUseCase()
                .prepare(RecordingPurpose.NOTE_OR_REFERENCE, new ScriptTextRange("SEG-011", 3, 99));

        assertFalse(plan.userMessage().isBlank());
        assertTrue(plan.suggestedFileName().contains("recording-note"));
    }
}
