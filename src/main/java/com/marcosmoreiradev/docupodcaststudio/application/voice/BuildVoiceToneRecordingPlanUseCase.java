package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

/** Builds the capture plan for recording one reference tone. */
public final class BuildVoiceToneRecordingPlanUseCase {
    public VoiceToneRecordingPlan build(String voiceProfileId, String voiceDisplayName, VoiceReferenceTone tone) {
        VoiceReferenceTone target = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        return new VoiceToneRecordingPlan(
                voiceProfileId,
                voiceDisplayName,
                target,
                target.displayName(),
                target.suggestedRecordingPrompt(),
                voiceDisplayName + "-" + target.displayName() + ".wav",
                "Grabar muestra",
                "Cancelar",
                "Detener grabacion",
                "Guardar muestra",
                true
        );
    }
}
