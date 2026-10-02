package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimeArtifactPaths;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return build(library, engine, reservedVoiceIds, currentVoiceId, null);
    }

    public List<VoiceAssignmentOption> build(
            VoiceLibrary library,
            AudioEngineDescriptor engine,
            Collection<String> reservedVoiceIds,
            String currentVoiceId,
            VoiceAssignmentReadinessContext readinessContext) {
        if (library == null) {
            return List.of();
        }
        Set<String> reserved = normalizeSet(reservedVoiceIds);
        String current = normalize(currentVoiceId);
        return library.voices().stream()
                .map(voice -> option(library, voice, engine, reserved, current, readinessContext))
                .toList();
    }

    private VoiceAssignmentOption option(
            VoiceLibrary library,
            VoiceProfile voice,
            AudioEngineDescriptor engine,
            Set<String> reservedVoiceIds,
            String currentVoiceId,
            VoiceAssignmentReadinessContext readinessContext) {
        VoiceProfileCapability capability = capabilityPolicy.evaluateVoice(voice, engine);
        boolean current = voice != null && voice.id().equals(currentVoiceId);
        boolean reservedByOther = voice != null && !current && reservedVoiceIds.contains(voice.id());
        VoiceReferenceAvailability availability = availability(voice, capability);
        String status = capability.status();
        String detail = capability.message();
        if (availability.usable() && readinessContext != null && requiresResolvedNeutral(library, voice)) {
            var neutral = library.referenceSampleSetByVoiceId(voice.id())
                    .flatMap(set -> set.sampleFor(VoiceReferenceTone.NEUTRAL));
            if (neutral.isEmpty() && legacyAdvancedDefaultFile(readinessContext, voice).isPresent()) {
                // Legacy advanced narrator stores its reference in models/tts/xtts/speakers.
            } else if (neutral.isEmpty()) {
                availability = VoiceReferenceAvailability.MISSING_SAMPLE;
                status = "Falta muestra Neutral";
                detail = "Registra o restaura la muestra Neutral desde Biblioteca de voces.";
            } else {
                try {
                    readinessContext.samplePathResolver().resolve(
                            readinessContext.projectRoot(), neutral.orElseThrow(), "muestra Neutral de voz");
                } catch (IOException | RuntimeException unavailable) {
                    availability = VoiceReferenceAvailability.MISSING_SAMPLE;
                    status = "Muestra no disponible";
                    detail = unavailable.getMessage() == null || unavailable.getMessage().isBlank()
                            ? "La muestra Neutral registrada no puede resolverse. Repárala desde Biblioteca de voces."
                            : unavailable.getMessage() + " Repárala desde Biblioteca de voces.";
                }
            }
        }
        return new VoiceAssignmentOption(
                capability.voiceId(),
                capability.displayName(),
                availability,
                capability.assignable() && availability.usable(),
                current,
                reservedByOther,
                status,
                detail);
    }

    private static boolean requiresResolvedNeutral(VoiceLibrary library, VoiceProfile voice) {
        if (voice == null || library == null) {
            return false;
        }
        return library.referenceSampleSetByVoiceId(voice.id()).isPresent()
                || voice.engineType() == com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType.XTTS
                || voice.engineType() == com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType.HUMAN_AUDIO;
    }

    private static java.util.Optional<Path> legacyAdvancedDefaultFile(
            VoiceAssignmentReadinessContext context, VoiceProfile voice) {
        if (context == null || voice == null
                || !"VOC-OWN-PLACEHOLDER".equalsIgnoreCase(voice.id())
                || voice.metadata().containsKey("userManaged")) {
            return java.util.Optional.empty();
        }
        Path installed = RuntimeArtifactPaths.fromRoot(context.installationRoot())
                .xttsDefaultSpeakerWav();
        if (Files.isRegularFile(installed)) {
            return java.util.Optional.of(installed);
        }
        Path runtime = RuntimeArtifactPaths.fromRoot(context.runtimeRoot())
                .xttsDefaultSpeakerWav();
        return Files.isRegularFile(runtime) ? java.util.Optional.of(runtime)
                : java.util.Optional.empty();
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
