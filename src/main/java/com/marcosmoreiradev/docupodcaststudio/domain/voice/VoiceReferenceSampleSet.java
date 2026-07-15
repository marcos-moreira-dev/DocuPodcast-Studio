package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Samples registered for one voice profile, keyed by theatrical/reference tone. */
public record VoiceReferenceSampleSet(
        String voiceProfileId,
        List<VoiceReferenceSample> samples
) {
    public VoiceReferenceSampleSet {
        voiceProfileId = token(voiceProfileId, "voiceProfileId");
        samples = samples == null ? List.of() : List.copyOf(samples);
        validateSameVoice(voiceProfileId, samples);
        validateUniqueTone(samples);
    }

    public static VoiceReferenceSampleSet forAdvancedVoice(String voiceProfileId, List<VoiceReferenceSample> samples) {
        VoiceReferenceSampleSet set = new VoiceReferenceSampleSet(voiceProfileId, samples);
        if (!set.hasNeutral()) {
            throw new IllegalArgumentException("La voz IA avanzada requiere una muestra neutral");
        }
        return set;
    }

    public boolean hasNeutral() {
        return sampleFor(VoiceReferenceTone.NEUTRAL).isPresent();
    }

    public Optional<VoiceReferenceSample> neutralSample() {
        return sampleFor(VoiceReferenceTone.NEUTRAL);
    }

    public Optional<VoiceReferenceSample> sampleFor(VoiceReferenceTone tone) {
        VoiceReferenceTone target = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        return samples.stream().filter(sample -> sample.tone() == target).findFirst();
    }

    public VoiceReferenceSample sampleForOrNeutral(VoiceReferenceTone tone) {
        return sampleFor(tone).or(() -> sampleFor(VoiceReferenceTone.NEUTRAL))
                .orElseThrow(() -> new IllegalStateException("No hay muestra neutral para la voz " + voiceProfileId));
    }

    public List<VoiceReferenceTone> registeredTones() {
        return samples.stream().map(VoiceReferenceSample::tone).distinct().toList();
    }

    public VoiceReferenceSampleSet withSample(VoiceReferenceSample sample) {
        if (sample == null) {
            throw new IllegalArgumentException("sample is required");
        }
        if (!voiceProfileId.equals(sample.voiceProfileId())) {
            throw new IllegalArgumentException("Sample " + sample.id() + " belongs to a different voice profile");
        }
        LinkedHashMap<VoiceReferenceTone, VoiceReferenceSample> updated = new LinkedHashMap<>();
        for (VoiceReferenceSample existing : samples) {
            updated.put(existing.tone(), existing);
        }
        updated.put(sample.tone(), sample);
        return new VoiceReferenceSampleSet(voiceProfileId, List.copyOf(updated.values()));
    }

    public VoiceReferenceSampleSet withoutTone(VoiceReferenceTone tone) {
        VoiceReferenceTone target = tone == null ? VoiceReferenceTone.NEUTRAL : tone;
        return new VoiceReferenceSampleSet(
                voiceProfileId,
                samples.stream().filter(sample -> sample.tone() != target).toList()
        );
    }

    public boolean missingToneUsesNeutral(VoiceReferenceTone tone) {
        return tone != null && tone != VoiceReferenceTone.NEUTRAL && sampleFor(tone).isEmpty() && hasNeutral();
    }

    private static void validateSameVoice(String voiceProfileId, List<VoiceReferenceSample> samples) {
        for (VoiceReferenceSample sample : samples) {
            if (!voiceProfileId.equals(sample.voiceProfileId())) {
                throw new IllegalArgumentException("Sample " + sample.id() + " belongs to a different voice profile");
            }
        }
    }

    private static void validateUniqueTone(List<VoiceReferenceSample> samples) {
        Map<VoiceReferenceTone, String> idsByTone = new LinkedHashMap<>();
        for (VoiceReferenceSample sample : samples) {
            String previous = idsByTone.put(sample.tone(), sample.id());
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate sample tone " + sample.tone());
            }
        }
    }

    private static String token(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }
}
