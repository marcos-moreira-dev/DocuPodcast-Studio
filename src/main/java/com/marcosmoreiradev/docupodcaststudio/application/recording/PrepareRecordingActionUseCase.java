package com.marcosmoreiradev.docupodcaststudio.application.recording;

import com.marcosmoreiradev.docupodcaststudio.domain.recording.RecordingPurpose;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;

/** Builds a future recording plan without touching microphone or filesystem. */
public final class PrepareRecordingActionUseCase {
    public RecordingActionPlan prepare(RecordingPurpose purpose, ScriptTextRange textRange) {
        RecordingPurpose effectivePurpose = purpose == null ? RecordingPurpose.NOTE_OR_REFERENCE : purpose;
        ScriptTextRange effectiveRange = textRange == null ? new ScriptTextRange("SEG-000", 0, 0) : textRange;
        return new RecordingActionPlan(
                effectivePurpose,
                effectiveRange,
                suggestedFileName(effectivePurpose, effectiveRange.segmentId()),
                message(effectivePurpose),
                true,
                true
        );
    }

    private static String suggestedFileName(RecordingPurpose purpose, String segmentId) {
        String suffix = switch (purpose) {
            case HUMAN_VOICE_FOR_TEXT -> "human-voice";
            case VOICE_SAMPLE_FOR_TTS -> "voice-sample";
            case NOTE_OR_REFERENCE -> "recording-note";
        };
        return segmentId + "-" + suffix + ".wav";
    }

    private static String message(RecordingPurpose purpose) {
        return switch (purpose) {
            case HUMAN_VOICE_FOR_TEXT -> "Preparado para grabar voz humana asociada al texto seleccionado. Usa la Biblioteca de voces para iniciar/detener una muestra real.";
            case VOICE_SAMPLE_FOR_TTS -> "Preparado para grabar una muestra de voz y registrarla como referencia de Voz IA avanzada.";
            case NOTE_OR_REFERENCE -> "Preparado para grabar una nota de audio asociada al proyecto o selección.";
        };
    }
}
