package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.util.Objects;

/** Assigns character, voice and performance style to a single narration segment. */
public final class AssignVoiceToSegmentUseCase {
    public NarrationScriptDocument assign(
            NarrationScriptDocument script,
            VoiceLibrary voiceLibrary,
            String segmentId,
            String characterId,
            String voiceProfileId,
            String performanceStyleId
    ) {
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(voiceLibrary, "voiceLibrary");
        NarrationSegment segment = script.segmentById(segmentId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el segmento " + segmentId));
        if (!voiceLibrary.supportsSegmentVoice(characterId, voiceProfileId, performanceStyleId)) {
            throw new IllegalArgumentException("La combinación personaje/voz/estilo no existe en la biblioteca de voces");
        }
        return script.replaceSegment(segment.withVoice(characterId, voiceProfileId, performanceStyleId));
    }
}
