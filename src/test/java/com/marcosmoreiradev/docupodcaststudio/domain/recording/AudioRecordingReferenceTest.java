package com.marcosmoreiradev.docupodcaststudio.domain.recording;

import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioRecordingReferenceTest {
    @Test
    void keepsManagedRecordingPurposeWithoutSpeechToText() {
        AudioRecordingReference reference = new AudioRecordingReference("REC-001", RecordingPurpose.NOTE_OR_REFERENCE,
                "AUD-001", null, null, "nota");

        assertEquals(RecordingPurpose.NOTE_OR_REFERENCE, reference.purpose());
    }
}
