package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Voice profile available to the narration script.
 *
 * <p>A voice may be predefined, owned by the user, imported from a local model,
 * or authorized by another person. The profile stores references only; samples
 * and models stay as project assets.</p>
 */
public record VoiceProfile(
        String id,
        String displayName,
        VoiceProfileType type,
        VoiceEngineType engineType,
        String language,
        String sampleAssetId,
        String modelAssetId,
        VoiceQualityPreset qualityPreset,
        boolean supportsStyleTransfer,
        String consentNote,
        Map<String, String> metadata
) {
    public VoiceProfile {
        id = token(id, "id");
        displayName = requiredText(displayName, "displayName");
        type = Objects.requireNonNullElse(type, VoiceProfileType.UNKNOWN);
        engineType = Objects.requireNonNullElse(engineType, VoiceEngineType.UNKNOWN);
        language = normalizeLanguage(language);
        sampleAssetId = normalize(sampleAssetId);
        modelAssetId = normalize(modelAssetId);
        qualityPreset = Objects.requireNonNullElse(qualityPreset, VoiceQualityPreset.BALANCED);
        consentNote = normalize(consentNote);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        validateEthicalBoundary(type, sampleAssetId, consentNote);
    }

    public static VoiceProfile predefinedNarrator() {
        return new VoiceProfile(
                "VOC-NARRATOR",
                "Narrador prediseñado",
                VoiceProfileType.PREDEFINED,
                VoiceEngineType.PIPER,
                "es",
                "",
                "",
                VoiceQualityPreset.BALANCED,
                false,
                "Voz neutral prediseñada para el motor local simple y documentos académicos.",
                Map.of("builtIn", "true")
        );
    }

    public static VoiceProfile ownVoicePlaceholder() {
        return new VoiceProfile(
                "VOC-OWN-PLACEHOLDER",
                "Narrador prediseñado avanzado",
                VoiceProfileType.PREDEFINED,
                VoiceEngineType.XTTS,
                "es",
                "voz-por-defecto.wav",
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Referencia Neutral prediseñada para Voz IA avanzada. Las emociones adicionales se agregan como tonos opcionales.",
                Map.of("builtInAdvancedReference", "true", "legacyVoiceSlot", "VOC-OWN-PLACEHOLDER")
        );
    }

    public boolean hasSample() {
        return !sampleAssetId.isBlank();
    }

    public boolean hasModel() {
        return !modelAssetId.isBlank();
    }

    public boolean usableForTts() {
        return engineType != VoiceEngineType.HUMAN_AUDIO || hasSample();
    }

    /** Extensible engine identity persisted alongside the format-v1 enum alias. */
    public EngineId engineId() {
        String persisted = normalize(metadata.get("engineId"));
        return persisted.isBlank() ? engineIdFor(engineType) : new EngineId(persisted);
    }

    public static EngineId engineIdFor(VoiceEngineType engineType) {
        VoiceEngineType safe = Objects.requireNonNullElse(engineType, VoiceEngineType.UNKNOWN);
        return new EngineId(switch (safe) {
            case PIPER -> "piper";
            case XTTS -> "xtts";
            case LOCAL_TTS_PROCESS -> "local-tts-process";
            case HUMAN_AUDIO -> "human-audio";
            case MOCK -> "mock";
            case UNKNOWN -> "unknown";
        });
    }

    /** Compatibility mapping used only by format-v1 readers. Unknown ids remain extensible. */
    public static VoiceEngineType engineTypeFor(String engineId) {
        String normalized = normalize(engineId).toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "piper" -> VoiceEngineType.PIPER;
            case "xtts", "coqui", "coqui-xtts" -> VoiceEngineType.XTTS;
            case "local-tts-process", "external" -> VoiceEngineType.LOCAL_TTS_PROCESS;
            case "human-audio" -> VoiceEngineType.HUMAN_AUDIO;
            case "mock" -> VoiceEngineType.MOCK;
            default -> VoiceEngineType.UNKNOWN;
        };
    }

    private static void validateEthicalBoundary(VoiceProfileType type, String sampleAssetId, String consentNote) {
        if (type.requiresConsentNote() && !sampleAssetId.isBlank() && consentNote.isBlank()) {
            throw new IllegalArgumentException("Voces autorizadas/importadas con muestra requieren nota de consentimiento");
        }
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String requiredText(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static String normalizeLanguage(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "es" : normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
