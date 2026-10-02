package com.marcosmoreiradev.docupodcaststudio.application.voice;

import java.util.Optional;

/** Resolves the same effective voice precedence for preparation, playback and export. */
public final class EffectiveVoiceAssignmentResolver {
    public static final String DEFAULT_NARRATOR_VOICE_ID = "VOC-NARRATOR";

    public enum Source {
        FRAGMENT,
        CHARACTER,
        GLOBAL,
        ENGINE_DEFAULT
    }

    public record Resolution(String voiceProfileId, Source source) {
        public Resolution {
            voiceProfileId = normalize(voiceProfileId);
            source = source == null ? Source.ENGINE_DEFAULT : source;
            if (voiceProfileId.isBlank()) {
                voiceProfileId = DEFAULT_NARRATOR_VOICE_ID;
            }
        }
    }

    public Resolution resolve(
            Optional<String> fragmentVoiceId,
            Optional<String> characterVoiceId,
            String globalVoiceId,
            String engineDefaultVoiceId) {
        String fragment = normalized(fragmentVoiceId);
        if (!fragment.isBlank()) {
            return new Resolution(fragment, Source.FRAGMENT);
        }
        String character = normalized(characterVoiceId);
        if (!character.isBlank()) {
            return new Resolution(character, Source.CHARACTER);
        }
        String global = normalize(globalVoiceId);
        if (!global.isBlank() && !DEFAULT_NARRATOR_VOICE_ID.equalsIgnoreCase(global)) {
            return new Resolution(global, Source.GLOBAL);
        }
        String engineDefault = normalize(engineDefaultVoiceId);
        return new Resolution(engineDefault.isBlank() ? DEFAULT_NARRATOR_VOICE_ID : engineDefault,
                Source.ENGINE_DEFAULT);
    }

    private static String normalized(Optional<String> value) {
        return value == null ? "" : value.map(EffectiveVoiceAssignmentResolver::normalize).orElse("");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
