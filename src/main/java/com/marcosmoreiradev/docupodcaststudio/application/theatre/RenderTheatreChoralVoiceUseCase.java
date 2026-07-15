package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeReport;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ResolveVoiceToneReferenceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceCapabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneReferenceResolution;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Synthesizes all selected theatre voices locally and atomically installs their FFmpeg mix. */
public final class RenderTheatreChoralVoiceUseCase {
    private static final Duration MIX_TIMEOUT = Duration.ofMinutes(30);

    private final VoiceTestSynthesisGateway synthesisGateway;
    private final VoiceCapabilityPolicy capabilityPolicy;
    private final ResolveVoiceToneReferenceUseCase resolveToneReference;
    private final EmbeddedFfmpegLocator ffmpegLocator;
    private final FfmpegRuntimeProbeUseCase ffmpegProbe;
    private final ExternalProcessRunner processRunner;
    private final Supplier<OperationalSettings> settingsSupplier;
    private final Path applicationRoot;

    public RenderTheatreChoralVoiceUseCase(VoiceTestSynthesisGateway synthesisGateway,
                                           VoiceCapabilityPolicy capabilityPolicy,
                                           ResolveVoiceToneReferenceUseCase resolveToneReference,
                                           EmbeddedFfmpegLocator ffmpegLocator,
                                           FfmpegRuntimeProbeUseCase ffmpegProbe,
                                           ExternalProcessRunner processRunner,
                                           Supplier<OperationalSettings> settingsSupplier,
                                           Path applicationRoot) {
        this.synthesisGateway = Objects.requireNonNull(synthesisGateway, "synthesisGateway");
        this.capabilityPolicy = Objects.requireNonNull(capabilityPolicy, "capabilityPolicy");
        this.resolveToneReference = Objects.requireNonNull(resolveToneReference, "resolveToneReference");
        this.ffmpegLocator = Objects.requireNonNull(ffmpegLocator, "ffmpegLocator");
        this.ffmpegProbe = Objects.requireNonNull(ffmpegProbe, "ffmpegProbe");
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner");
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
        this.applicationRoot = Objects.requireNonNull(applicationRoot, "applicationRoot").toAbsolutePath().normalize();
    }

    public TheatreChoralVoiceRenderResult execute(TheatreChoralVoiceRenderRequest request,
                                                   Consumer<TheatreChoralVoiceRenderProgress> progress) throws IOException {
        Objects.requireNonNull(request, "request");
        Consumer<TheatreChoralVoiceRenderProgress> listener = progress == null ? ignored -> { } : progress;
        if (request.interventionId().isBlank()) {
            throw new IOException("Selecciona una intervencion teatral antes de renderizar voces simultaneas.");
        }
        if (request.participantCharacterIds().size() < 2) {
            throw new IOException("Selecciona al menos dos personajes para renderizar voces simultaneas.");
        }
        if (!Files.isRegularFile(request.projectFile())) {
            throw new IOException("Guarda el proyecto antes de renderizar voces simultaneas.");
        }
        Path projectRoot = request.projectFile().getParent();
        if (projectRoot == null) {
            throw new IOException("La ruta del proyecto no tiene carpeta contenedora.");
        }
        NarrationSegment segment = segmentFor(request);
        if (!segment.narratable()) {
            throw new IOException("La intervencion seleccionada no tiene texto narrable.");
        }

        VoiceEngineCapabilityProfile engine = capabilityPolicy.activeEngineProfile(request.engineDescriptor());
        if (engine.mockMode() || !engine.canSynthesizeNow()) {
            throw new IOException("Configura e inicia un motor TTS local real antes de renderizar voces simultaneas.");
        }
        Map<String, PreparedVoice> voices = prepareVoices(request, segment, projectRoot, engine);
        FfmpegRuntimeReport ffmpeg = ffmpegRuntime();
        if (!ffmpeg.ffmpegReady() || ffmpeg.ffmpegExecutable() == null) {
            throw new IOException("FFmpeg local no esta disponible. Preparalo desde Configuracion antes de mezclar voces.");
        }

        int totalSteps = voices.size() + 1;
        Path tempRoot = projectRoot.resolve("generated").resolve("theatre-choral").resolve("tmp")
                .resolve(UUID.randomUUID().toString()).normalize();
        Path outputDirectory = projectRoot.resolve("media").resolve("audio").resolve("theatre-choral").normalize();
        Path finalOutput = outputDirectory.resolve(sanitize(request.interventionId()) + "-choral.wav");
        Path stagedOutput = tempRoot.resolve("mixed.wav");
        Files.createDirectories(tempRoot);
        Files.createDirectories(outputDirectory);
        ArrayList<Path> rendered = new ArrayList<>();
        try {
            int completed = 0;
            for (PreparedVoice voice : voices.values()) {
                listener.accept(new TheatreChoralVoiceRenderProgress(completed, totalSteps,
                        "Sintetizando " + voice.displayName() + "..."));
                Path voiceDirectory = tempRoot.resolve(sanitize(voice.characterId()));
                Path textFile = voiceDirectory.resolve("texto.txt");
                Path wavFile = voiceDirectory.resolve("voz.wav");
                Files.createDirectories(voiceDirectory);
                VoiceTestSynthesisResult synthesis = synthesisGateway.synthesize(new VoiceTestSynthesisRequest(
                        "choral-" + sanitize(request.interventionId()) + "-" + sanitize(voice.characterId()),
                        segment.narrationText(),
                        voice.language(),
                        voice.voiceProfileId(),
                        voice.referenceSample(),
                        textFile,
                        wavFile,
                        voiceDirectory,
                        request.engineDescriptor()));
                if (!synthesis.generated() || !Files.isRegularFile(wavFile) || Files.size(wavFile) <= 44L) {
                    String detail = synthesis.userMessage().isBlank() ? synthesis.diagnosticTail() : synthesis.userMessage();
                    throw new IOException("No se pudo sintetizar " + voice.displayName() + ": " + detail);
                }
                rendered.add(wavFile);
                completed++;
                listener.accept(new TheatreChoralVoiceRenderProgress(completed, totalSteps,
                        voice.displayName() + " listo."));
            }

            listener.accept(new TheatreChoralVoiceRenderProgress(rendered.size(), totalSteps,
                    "Mezclando voces con FFmpeg..."));
            ExternalProcessRequest mixRequest = ExternalProcessRequest.of(
                            mixCommand(ffmpeg.ffmpegExecutable(), rendered, stagedOutput),
                            "theatre-choral-amix", MIX_TIMEOUT)
                    .withWorkingDirectory(tempRoot)
                    .redirectingErrorStream();
            ExternalProcessResult mix = run(mixRequest);
            if (!mix.succeeded() || !Files.isRegularFile(stagedOutput) || Files.size(stagedOutput) <= 44L) {
                throw new IOException("FFmpeg no pudo crear la mezcla multipersona. " + mix.combinedOutputTail());
            }
            installAtomically(stagedOutput, finalOutput);
            listener.accept(new TheatreChoralVoiceRenderProgress(totalSteps, totalSteps, "Mezcla multipersona lista."));

            String assetId = "AUDIO-CHORAL-" + sanitizeToken(request.interventionId());
            String relativePath = projectRoot.relativize(finalOutput).toString().replace('\\', '/');
            ProjectAssetReference asset = new ProjectAssetReference(
                    assetId,
                    ProjectAssetKind.AUDIO_CLIP,
                    "Voces simultaneas - " + request.interventionId(),
                    relativePath,
                    "audio/wav",
                    "Mezcla teatral multipersona",
                    sha256(finalOutput),
                    "Generado localmente con TTS y FFmpeg amix.");
            TheatreProjectLayer.ChoralVoiceAssignment assignment = new TheatreProjectLayer.ChoralVoiceAssignment(
                    request.interventionId(),
                    request.participantCharacterIds(),
                    assetId,
                    TheatreChoralVoiceFingerprint.compute(request.project(), segment, request.participantCharacterIds()),
                    "Mezcla multipersona generada por DocuPodcast Studio.");
            TheatreProjectLayer theatre = request.project().theatre();
            ArrayList<TheatreProjectLayer.ChoralVoiceAssignment> assignments = new ArrayList<>(
                    theatre.choralVoiceAssignments().stream()
                            .filter(item -> !item.intervencionId().equals(request.interventionId()))
                            .toList());
            assignments.add(assignment);
            DocuPodcastProject updated = request.project().withAsset(asset)
                    .withTheatre(theatre.withChoralVoiceAssignments(assignments));
            return new TheatreChoralVoiceRenderResult(updated, assignment, finalOutput,
                    "Voz multipersona lista para " + request.interventionId() + ".");
        } finally {
            deleteTree(tempRoot);
        }
    }

    private Map<String, PreparedVoice> prepareVoices(TheatreChoralVoiceRenderRequest request,
                                                     NarrationSegment segment,
                                                     Path projectRoot,
                                                     VoiceEngineCapabilityProfile engine) throws IOException {
        TheatreProjectLayer theatre = request.project().theatre();
        Map<String, TheatreProjectLayer.CharacterProfile> characters = new LinkedHashMap<>();
        theatre.characters().forEach(character -> characters.put(character.id(), character));
        Map<String, String> voiceByCharacter = new LinkedHashMap<>();
        theatre.voiceRoleAliases().forEach(alias -> voiceByCharacter.putIfAbsent(alias.characterId(), alias.voiceProfileId()));
        VoiceReferenceTone tone = VoiceReferenceTone.fromLayerTargetId(segment.performanceStyleId())
                .orElse(VoiceReferenceTone.NEUTRAL);
        LinkedHashMap<String, PreparedVoice> result = new LinkedHashMap<>();
        for (String characterId : request.participantCharacterIds()) {
            TheatreProjectLayer.CharacterProfile character = Optional.ofNullable(characters.get(characterId))
                    .orElseThrow(() -> new IOException("El personaje ya no existe: " + characterId + "."));
            String voiceId = voiceByCharacter.getOrDefault(characterId, "");
            if (voiceId.isBlank()) {
                throw new IOException(character.displayName() + " no tiene una voz asignada.");
            }
            VoiceProfile voice = request.project().voiceLibrary().voiceById(voiceId)
                    .orElseThrow(() -> new IOException("No existe el perfil de voz " + voiceId + " para " + character.displayName() + "."));
            if (!capabilityPolicy.evaluateVoice(voice, request.engineDescriptor()).synthesizableNow()) {
                throw new IOException(character.displayName() + " no se puede sintetizar con el motor TTS actual.");
            }
            Path reference = null;
            if (engine.coquiXttsMode()) {
                VoiceToneReferenceResolution resolution = resolveToneReference.resolve(request.project().voiceLibrary(), voiceId, tone);
                if (resolution.available()) {
                    reference = VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot().resolve(
                            projectRoot, resolution.sample().orElseThrow(), "muestra de " + character.displayName());
                } else if (voice.type() != VoiceProfileType.PREDEFINED) {
                    throw new IOException(character.displayName() + ": " + resolution.userMessage());
                }
            }
            result.put(characterId, new PreparedVoice(characterId, character.displayName(), voiceId,
                    voice.language(), reference));
        }
        return result;
    }

    private FfmpegRuntimeReport ffmpegRuntime() {
        OperationalSettings settings = Optional.ofNullable(settingsSupplier.get()).orElseGet(OperationalSettings::defaults);
        Path configured = null;
        if (!settings.video().ffmpegExecutable().isBlank()) {
            try {
                configured = Path.of(settings.video().ffmpegExecutable());
            } catch (RuntimeException ignored) {
                configured = null;
            }
        }
        return ffmpegProbe.inspect(ffmpegLocator.locate(applicationRoot, configured));
    }

    private static NarrationSegment segmentFor(TheatreChoralVoiceRenderRequest request) throws IOException {
        String blockId = request.project().theatre().intervenciones().stream()
                .filter(item -> item.id().equals(request.interventionId()))
                .map(TheatreProjectLayer.Intervencion::blockId)
                .findFirst()
                .orElseThrow(() -> new IOException("No existe la intervencion " + request.interventionId() + "."));
        return request.script().segments().stream()
                .filter(segment -> segment.sourceBlockIds().contains(blockId))
                .findFirst()
                .orElseThrow(() -> new IOException("No se encontro el texto narrable de " + request.interventionId() + "."));
    }

    static List<String> mixCommand(Path ffmpeg, List<Path> inputs, Path output) {
        ArrayList<String> command = new ArrayList<>();
        command.add(ffmpeg.toString());
        command.add("-y");
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("error");
        for (Path input : inputs) {
            command.add("-i");
            command.add(input.toString());
        }
        StringBuilder filter = new StringBuilder();
        for (int index = 0; index < inputs.size(); index++) {
            filter.append('[').append(index).append(":a]")
                    .append("aresample=48000,aformat=sample_fmts=fltp:channel_layouts=mono[a")
                    .append(index).append("];");
        }
        for (int index = 0; index < inputs.size(); index++) {
            filter.append("[a").append(index).append(']');
        }
        filter.append("amix=inputs=").append(inputs.size())
                .append(":duration=longest:dropout_transition=0:normalize=1,")
                .append("alimiter=limit=0.95[mix]");
        command.add("-filter_complex");
        command.add(filter.toString());
        command.add("-map");
        command.add("[mix]");
        command.add("-ar");
        command.add("48000");
        command.add("-ac");
        command.add("1");
        command.add("-c:a");
        command.add("pcm_s16le");
        command.add(output.toString());
        return List.copyOf(command);
    }

    private ExternalProcessResult run(ExternalProcessRequest request) throws IOException {
        try {
            return processRunner.run(request);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("La mezcla multipersona fue interrumpida.", ex);
        }
    }

    private static void installAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 no esta disponible.", ex);
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static String sanitize(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_-]+", "-").replaceAll("-+", "-");
        return normalized.isBlank() ? "intervention" : normalized;
    }

    private static String sanitizeToken(String value) {
        String normalized = value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^A-Z0-9_-]+", "-").replaceAll("-+", "-");
        return normalized.isBlank() ? "INTERVENTION" : normalized;
    }

    private record PreparedVoice(String characterId, String displayName, String voiceProfileId,
                                 String language, Path referenceSample) {
    }
}
