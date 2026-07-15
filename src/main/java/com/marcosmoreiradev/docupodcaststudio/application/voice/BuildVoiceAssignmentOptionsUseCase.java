package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds cross-mode voice assignment choices from the library and current engine readiness. */
public final class BuildVoiceAssignmentOptionsUseCase {
    private final VoiceCapabilityPolicy capabilityPolicy;

    public BuildVoiceAssignmentOptionsUseCase() {
        this(new VoiceCapabilityPolicy());
    }

    public BuildVoiceAssignmentOptionsUseCase(VoiceCapabilityPolicy capabilityPolicy) {
        this.capabilityPolicy = capabilityPolicy == null ? new VoiceCapabilityPolicy() : capabilityPolicy;
    }

    public List<VoiceAssignmentOption> build(
            VoiceLibrary library,
            AudioEngineDescriptor engine,
            Collection<String> reservedVoiceIds,
            String currentVoiceId) {
        if (library == null) {
            return List.of();
        }
        Set<String> reserved = normalizeSet(reservedVoiceIds);
        String current = normalize(currentVoiceId);
        return library.voices().stream()
                .map(voice -> option(voice, engine, reserved, current))
                .toList();
    }

    private VoiceAssignmentOption option(
            VoiceProfile voice,
            AudioEngineDescriptor engine,
            Set<String> reservedVoiceIds,
            String currentVoiceId) {
        VoiceProfileCapability capability = capabilityPolicy.evaluateVoice(voice, engine);
        boolean current = voice != null && voice.id().equals(currentVoiceId);
        boolean reservedByOther = voice != null && !current && reservedVoiceIds.contains(voice.id());
        VoiceReferenceAvailability availability = availability(voice, capability);
        return new VoiceAssignmentOption(
                capability.voiceId(),
                capability.displayName(),
                availability,
                capability.assignable(),
                current,
                reservedByOther,
                capability.status(),
                capability.message());
    }

    private static VoiceReferenceAvailability availability(VoiceProfile voice, VoiceProfileCapability capability) {
        if (voice == null || capability == null || !capability.assignable()) {
            return VoiceReferenceAvailability.UNSUPPORTED_FOR_MODE;
        }
        if (capability.synthesizableNow()) {
            return builtIn(voice) ? VoiceReferenceAvailability.BUILT_IN : VoiceReferenceAvailability.AVAILABLE;
        }
        if (capability.requiresSample()) {
            return VoiceReferenceAvailability.MISSING_SAMPLE;
        }
        if (capability.requiresEngineConfiguration()) {
            return VoiceReferenceAvailability.ENGINE_UNAVAILABLE;
        }
        return VoiceReferenceAvailability.UNSUPPORTED_FOR_MODE;
    }

    private static boolean builtIn(VoiceProfile voice) {
        return voice != null
                && (voice.type() == VoiceProfileType.PREDEFINED
                || Boolean.parseBoolean(voice.metadata().getOrDefault("builtIn", "false")));
    }

    private static Set<String> normalizeSet(Collection<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .map(BuildVoiceAssignmentOptionsUseCase::normalize)
                    .filter(value -> !value.isBlank())
                    .forEach(result::add);
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
