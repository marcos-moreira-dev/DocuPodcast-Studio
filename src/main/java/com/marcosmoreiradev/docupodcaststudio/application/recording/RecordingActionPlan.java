package com.marcosmoreiradev.docupodcaststudio.application.recording;

import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

/**
 * Preview plan for a future workspace recording action.
 *
 * <p>This record describes the intent before capture starts: record human voice for selected text,
 * record a voice sample for TTS, or keep a note/reference audio associated with the project.</p>
 */
public record RecordingActionPlan(
        RecordingPurpose purpose,
        ScriptTextRange textRange,
        String suggestedFileName,
        String userMessage,
        boolean requiresProjectFile,
        boolean requiresMicrophone
) {
    public RecordingActionPlan {
        purpose = purpose == null ? RecordingPurpose.NOTE_OR_REFERENCE : purpose;
        textRange = textRange == null ? new ScriptTextRange("SEG-000", 0, 0) : textRange;
        suggestedFileName = sanitize(suggestedFileName);
        userMessage = userMessage == null ? "" : userMessage.strip();
    }


    private static String sanitize(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return "recording.wav";
        }
        String clean = normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
        return clean.toLowerCase(java.util.Locale.ROOT).endsWith(".wav") ? clean : clean + ".wav";
    }
}
