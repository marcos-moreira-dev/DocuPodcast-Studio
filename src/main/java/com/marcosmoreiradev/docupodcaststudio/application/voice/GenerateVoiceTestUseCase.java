package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Generates a short, cacheable voice test request from an editable phrase.
 *
 * <p>The use case keeps "Reproducir muestra" and "Generar prueba" as separate actions. A productive
 * generated test must go through a synthesis gateway backed by the selected local voice engine; synthetic WAV generation is intentionally not present here.</p>
 */
public final class GenerateVoiceTestUseCase {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final VoiceCapabilityPolicy capabilityPolicy;
    private final ResolveVoiceToneReferenceUseCase resolveToneReference;
    private final VoiceTestSynthesisGateway synthesisGateway;
    private final VoiceReferenceSamplePathResolver samplePathResolver;

    public GenerateVoiceTestUseCase() {
        this(new VoiceCapabilityPolicy(), new ResolveVoiceToneReferenceUseCase(),
                VoiceTestSynthesisGateway.unavailable(),
                VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot());
    }

    public GenerateVoiceTestUseCase(VoiceCapabilityPolicy capabilityPolicy,
                                    ResolveVoiceToneReferenceUseCase resolveToneReference,
                                    VoiceTestSynthesisGateway synthesisGateway) {
        this(capabilityPolicy, resolveToneReference, synthesisGateway,
                VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot());
    }

    public GenerateVoiceTestUseCase(VoiceCapabilityPolicy capabilityPolicy,
                                    ResolveVoiceToneReferenceUseCase resolveToneReference,
                                    VoiceTestSynthesisGateway synthesisGateway,
                                    VoiceReferenceSamplePathResolver samplePathResolver) {
        this.capabilityPolicy = Objects.requireNonNull(capabilityPolicy, "capabilityPolicy");
        this.resolveToneReference = Objects.requireNonNull(resolveToneReference, "resolveToneReference");
        this.synthesisGateway = Objects.requireNonNull(synthesisGateway, "synthesisGateway");
        this.samplePathResolver = Objects.requireNonNull(samplePathResolver, "samplePathResolver");
    }

    public VoiceGeneratedTestResult generate(Path projectFile, VoiceLibrary library, VoiceGeneratedTestRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        VoiceEngineCapabilityProfile engine = capabilityPolicy.activeEngineProfile(request.engineDescriptor());
        if (library == null) {
            return VoiceGeneratedTestResult.blocked(request.tone(), "Biblioteca de voces no disponible.", false);
        }
        Optional<VoiceProfile> voice = library.voiceById(request.voiceProfileId());
        if (voice.isEmpty()) {
            return VoiceGeneratedTestResult.blocked(request.tone(), "Selecciona una voz existente antes de generar la prueba.", false);
        }
        if (engine.diagnosticMode()) {
            return VoiceGeneratedTestResult.blocked(request.tone(),
                    "Modo de prueba no genera voces reales. Activa Voz IA avanzada o Voz local simple para generar una prueba audible.",
                    true);
        }
        VoiceProfileCapability compatibility = capabilityPolicy.evaluateVoice(
                voice.get(), request.engineDescriptor());
        if (!compatibility.synthesizableNow()) {
            return VoiceGeneratedTestResult.blocked(request.tone(),
                    compatibility.message(), compatibility.requiresEngineConfiguration());
        }
        if (engine.simpleLocalMode()) {
            return generateLocalSimpleTest(projectFile, voice.get(), request, engine);
        }
        if (!engine.advancedAiMode() || !engine.canSynthesizeNow()) {
            return VoiceGeneratedTestResult.blocked(request.tone(),
                    "Prepara Voz IA avanzada en Configuración antes de generar una prueba con muestra de referencia.", true);
        }
        VoiceToneReferenceResolution resolution = resolveToneReference.resolve(library, voice.get().id(), request.tone());
        boolean usesDefaultEngineVoice = !resolution.available() && voice.get().type() == VoiceProfileType.PREDEFINED;
        if (!resolution.available() && !usesDefaultEngineVoice) {
            return VoiceGeneratedTestResult.blocked(request.tone(), resolution.userMessage(), false);
        }
        Path root = projectRoot(projectFile);
        Path referenceSample = usesDefaultEngineVoice ? null : resolveReferenceSample(root, resolution.sample().orElseThrow());
        VoiceReferenceTone resolvedTone = usesDefaultEngineVoice ? VoiceReferenceTone.NEUTRAL : resolution.resolvedTone();
        Path testDir = root.resolve("voices").resolve("generated-tests").resolve(sanitize(voice.get().id())).normalize();
        Files.createDirectories(testDir);
        Instant now = Instant.now();
        String baseName = FILE_TIME.format(now) + "-" + sanitize(resolvedTone.name().toLowerCase(Locale.ROOT));
        Path text = testDir.resolve(baseName + ".txt");
        Path audio = testDir.resolve(baseName + ".wav");
        Path manifest = testDir.resolve(baseName + ".json");
        VoiceTestSynthesisResult synthesis = synthesize(request, voice.get(), resolvedTone, referenceSample, text, audio, testDir);
        if (!synthesis.generated()) {
            return VoiceGeneratedTestResult.blocked(request.tone(), synthesis.userMessage(), synthesis.requiresConfiguration());
        }
        VoiceToneReferenceResolution manifestResolution = usesDefaultEngineVoice
                ? new VoiceToneReferenceResolution(voice.get().id(), request.tone(), VoiceReferenceTone.NEUTRAL, Optional.empty(), false,
                "Se usará la voz base del motor configurado.")
                : resolution;
        Files.writeString(manifest, manifestJson(voice.get(), request, manifestResolution, audio, text, now, synthesis), StandardCharsets.UTF_8);
        if (usesDefaultEngineVoice) {
            return new VoiceGeneratedTestResult(true, false, false, request.tone(), VoiceReferenceTone.NEUTRAL,
                    Optional.of(audio), Optional.of(manifest), request.phrase(), "",
                    "Prueba generada con motor real y la voz base configurada del programa.");
        }
        return new VoiceGeneratedTestResult(true, false, resolution.fallbackToNeutral(), request.tone(), resolution.resolvedTone(),
                Optional.of(audio), Optional.of(manifest), request.phrase(), resolution.sampleAssetId(),
                resolution.fallbackToNeutral()
                        ? "Prueba generada con motor real; se usará Neutral. " + resolution.userMessage()
                        : "Prueba generada con motor real, la voz seleccionada y el tono " + resolution.resolvedTone().displayName() + ".");
    }

    private VoiceGeneratedTestResult generateLocalSimpleTest(Path projectFile, VoiceProfile voice,
                                                            VoiceGeneratedTestRequest request,
                                                            VoiceEngineCapabilityProfile engine) throws IOException {
        if (!engine.canSynthesizeNow()) {
            return VoiceGeneratedTestResult.blocked(request.tone(),
                    "Prepara Voz local simple en Configuración antes de generar una prueba de lectura simple.", true);
        }
        Path root = projectRoot(projectFile);
        Path testDir = root.resolve("voices").resolve("generated-tests").resolve("local-simple").normalize();
        Files.createDirectories(testDir);
        Instant now = Instant.now();
        String baseName = FILE_TIME.format(now) + "-lectura-simple";
        Path text = testDir.resolve(baseName + ".txt");
        Path audio = testDir.resolve(baseName + ".wav");
        Path manifest = testDir.resolve(baseName + ".json");
        VoiceToneReferenceResolution resolution = new VoiceToneReferenceResolution(
                voice.id(), request.tone(), VoiceReferenceTone.NEUTRAL, Optional.empty(), false,
                "Voz local simple usa el modelo local activo; no usa muestras humanas ni tonos avanzados.");
        VoiceTestSynthesisResult synthesis = synthesize(request, voice, VoiceReferenceTone.NEUTRAL, null, text, audio, testDir);
        if (!synthesis.generated()) {
            return VoiceGeneratedTestResult.blocked(request.tone(), synthesis.userMessage(), synthesis.requiresConfiguration());
        }
        Files.writeString(manifest, manifestJson(voice, request, resolution, audio, text, now, synthesis), StandardCharsets.UTF_8);
        return new VoiceGeneratedTestResult(true, false, false, request.tone(), VoiceReferenceTone.NEUTRAL,
                Optional.of(audio), Optional.of(manifest), request.phrase(), "",
                "Prueba simple generada con Voz local simple. No usa muestras humanas, tonos avanzados ni clonación.");
    }

    private VoiceTestSynthesisResult synthesize(VoiceGeneratedTestRequest request,
                                                VoiceProfile voice,
                                                VoiceReferenceTone tone,
                                                Path referenceSample,
                                                Path text,
                                                Path audio,
                                                Path workDir) throws IOException {
        return synthesisGateway.synthesize(new VoiceTestSynthesisRequest(
                "voice-test-" + sanitize(voice.id()) + "-" + sanitize(tone.name()),
                request.phrase(),
                "es",
                voice.id(),
                referenceSample,
                text,
                 audio,
                 workDir,
                 request.engineDescriptor(),
                 tone.name()));
    }

    private static Path projectRoot(Path projectFile) {
        if (projectFile == null) {
            throw new IllegalArgumentException("Guarda el proyecto antes de generar una prueba de voz.");
        }
        Path normalized = projectFile.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("La ruta del proyecto no tiene carpeta contenedora.");
        }
        return parent;
    }

    private Path resolveReferenceSample(Path projectRoot, VoiceReferenceSample sample) throws IOException {
        return samplePathResolver.resolve(projectRoot, sample, "muestra de voz");
    }

    private static String manifestJson(VoiceProfile voice, VoiceGeneratedTestRequest request,
                                       VoiceToneReferenceResolution resolution, Path audio, Path text,
                                       Instant createdAt, VoiceTestSynthesisResult synthesis) {
        return "{\n"
                + "  \"schema\": \"docupodcast-voice-generated-test-v1\",\n"
                + "  \"createdAt\": \"" + escape(createdAt.toString()) + "\",\n"
                + "  \"realSynthesis\": true,\n"
                + "  \"voiceProfileId\": \"" + escape(voice.id()) + "\",\n"
                + "  \"voiceDisplayName\": \"" + escape(voice.displayName()) + "\",\n"
                + "  \"requestedTone\": \"" + escape(request.tone().name()) + "\",\n"
                + "  \"resolvedTone\": \"" + escape(resolution.resolvedTone().name()) + "\",\n"
                + "  \"fallbackToNeutral\": " + resolution.fallbackToNeutral() + ",\n"
                + "  \"referenceSampleAssetId\": \"" + escape(resolution.sampleAssetId()) + "\",\n"
                + "  \"phrase\": \"" + escape(request.phrase()) + "\",\n"
                + "  \"textFile\": \"" + escape(text.getFileName().toString()) + "\",\n"
                + "  \"audioFile\": \"" + escape(audio.getFileName().toString()) + "\",\n"
                + "  \"outputBytes\": " + synthesis.outputBytes() + "\n"
                + "}\n";
    }

    private static String sanitize(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_-]+", "-").replaceAll("-+", "-");
        return normalized.isBlank() ? "voice-test" : normalized;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
