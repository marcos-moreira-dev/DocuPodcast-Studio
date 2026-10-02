package com.marcosmoreiradev.docupodcaststudio.application.compatibility.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;

/** Format-v1 voice-engine aliases kept outside product and presentation policies. */
public final class LegacyVoiceProfileEngineCompatibility {
    private LegacyVoiceProfileEngineCompatibility() { }

    public static boolean supports(VoiceProfile voice, boolean advanced, boolean simple, boolean diagnostic) {
        if (voice == null) return false;
        VoiceEngineType legacy = voice.engineType();
        if (simple) {
            return legacy == VoiceEngineType.PIPER
                    || legacy == VoiceEngineType.LOCAL_TTS_PROCESS
                    || legacy == VoiceEngineType.MOCK;
        }
        if (advanced) return legacy != VoiceEngineType.PIPER;
        if (diagnostic) return legacy == VoiceEngineType.MOCK;
        return true;
    }

    public static VoiceEngineType advancedEngineTypeAlias() { return VoiceEngineType.XTTS; }

    public static boolean isSimple(VoiceProfile voice) {
        return voice != null && (voice.engineType() == VoiceEngineType.PIPER
                || voice.engineType() == VoiceEngineType.LOCAL_TTS_PROCESS);
    }

    public static boolean isAdvanced(VoiceProfile voice) {
        return voice != null && (voice.engineType() == VoiceEngineType.XTTS
                || voice.engineType() == VoiceEngineType.HUMAN_AUDIO);
    }

    public static boolean isDiagnostic(VoiceProfile voice) {
        return voice != null && voice.engineType() == VoiceEngineType.MOCK;
    }
}
