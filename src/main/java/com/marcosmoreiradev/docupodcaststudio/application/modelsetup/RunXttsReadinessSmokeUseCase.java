package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettingsMigrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Runs a short local synthesis proof for Voz IA avanzada after model/runtime inspection passes.
 *
 * <p>This is intentionally separate from downloads: a model can be downloaded and still fail at runtime
 * because Python, wrapper, GPU/CPU, speaker sample or command arguments are wrong.</p>
 */
public final class RunXttsReadinessSmokeUseCase {
    public static final String DEFAULT_PHRASE = "Esta es una prueba corta de Voz IA avanzada en DocuPodcast Studio.";

    private final InspectXttsSetupReadinessUseCase readinessInspector;
    private final VoiceTestSynthesisGateway synthesisGateway;

    public RunXttsReadinessSmokeUseCase() {
        this(new InspectXttsSetupReadinessUseCase(), VoiceTestSynthesisGateway.unavailable());
    }

    public RunXttsReadinessSmokeUseCase(InspectXttsSetupReadinessUseCase readinessInspector,
                                        VoiceTestSynthesisGateway synthesisGateway) {
        this.readinessInspector = Objects.requireNonNull(readinessInspector, "readinessInspector");
        this.synthesisGateway = Objects.requireNonNull(synthesisGateway, "synthesisGateway");
    }

    public XttsSmokeTestReport run(OperationalSettings settings, Path applicationRoot) {
        return run(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public XttsSmokeTestReport run(OperationalSettings settings, Path applicationRoot,
                                   ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        OperationalSettings current = OperationalSettingsMigrationPolicy.repair(settings == null ? OperationalSettings.defaults() : settings);
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path dir = InspectXttsSmokeTestUseCase.smokeDirectory(root);
        progress.onProgress("Verificando que Voz IA avanzada tenga runtime, modelo y voz neutral...");
        XttsSetupReadinessReport readiness = readinessInspector.inspect(current, root);
        if (!readiness.canBeSelectedAsEngine()) {
            return XttsSmokeTestReport.failed(dir,
                    "No se puede generar prueba: Voz IA avanzada aun no esta seleccionable.",
                    readiness.missingRequirements());
        }
        try {
            Files.createDirectories(dir);
            Path text = dir.resolve("xtts-readiness-smoke.txt");
            Path audio = dir.resolve("xtts-readiness-smoke.wav");
            Path manifest = dir.resolve("xtts-readiness-smoke.json");
            Files.deleteIfExists(audio);
            Files.deleteIfExists(manifest);
            progress.onProgress("Generando WAV real de prueba con Voz IA avanzada...");
            VoiceTestSynthesisResult result = synthesisGateway.synthesize(new VoiceTestSynthesisRequest(
                    "xtts-readiness-smoke",
                    DEFAULT_PHRASE,
                    current.tts().language(),
                    current.tts().voiceProfileId(),
                    readiness.speakerWav(),
                    text,
                    audio,
                    dir,
                    AudioEngineDescriptor.process("Voz IA avanzada", true,
                            "runtime-local", "Prueba local de preparacion",
                            java.util.Set.of(EngineFeature.REFERENCE_VOICE,
                                    EngineFeature.EXPRESSIVE_STYLE))));
            long bytes = Files.isRegularFile(audio) ? Files.size(audio) : result.outputBytes();
            if (!result.generated() || bytes <= 44L) {
                String message = result.userMessage().isBlank()
                        ? "El motor no produjo un WAV valido para la prueba."
                        : result.userMessage();
                return XttsSmokeTestReport.failed(dir, message,
                        result.diagnosticTail().isBlank() ? List.of(message) : List.of(message, result.diagnosticTail()));
            }
            Instant now = Instant.now();
            Files.writeString(manifest, manifestJson(now, readiness, audio, bytes, result), StandardCharsets.UTF_8);
            progress.onProgress("Prueba WAV generada: " + audio.getFileName() + " (" + bytes + " bytes).");
            String evidence = voiceRuntimeEvidence(result.diagnosticTail());
            String userMessage = "Voz IA avanzada genero un WAV real."
                    + (evidence.isBlank() ? "" : " " + evidence);
            return new XttsSmokeTestReport(dir, text, audio, manifest, true, false, bytes, now, List.of(), userMessage);
        } catch (IOException | RuntimeException ex) {
            return XttsSmokeTestReport.failed(dir,
                    "No se pudo generar la prueba de Voz IA avanzada: " + readable(ex),
                    List.of(readable(ex)));
        }
    }

    private static String manifestJson(Instant createdAt, XttsSetupReadinessReport readiness, Path audio,
                                       long outputBytes, VoiceTestSynthesisResult result) {
        return "{\n"
                + "  \"schema\": \"docupodcast-xtts-readiness-smoke-v1\",\n"
                + "  \"generatedAt\": \"" + escape(createdAt.toString()) + "\",\n"
                + "  \"generated\": true,\n"
                + "  \"playbackConfirmed\": false,\n"
                + "  \"engineMode\": \"xtts\",\n"
                + "  \"speakerWav\": \"" + escape(readiness.speakerWav().toString()) + "\",\n"
                + "  \"audioFile\": \"" + escape(audio.getFileName().toString()) + "\",\n"
                + "  \"outputBytes\": " + outputBytes + ",\n"
                + "  \"message\": \"" + escape(result.userMessage()) + "\",\n"
                + "  \"runtimeEvidence\": \"" + escape(voiceRuntimeEvidence(result.diagnosticTail())) + "\",\n"
                + "  \"diagnosticTail\": \"" + escape(result.diagnosticTail()) + "\"\n"
                + "}\n";
    }

    static String voiceRuntimeEvidence(String diagnosticTail) {
        String output = diagnosticTail == null ? "" : diagnosticTail;
        String requested = xttsValue(output, "device_requested");
        String backend = xttsValue(output, "device_backend");
        String name = "";
        if (!backend.isBlank()) {
            String marker = "device_name=";
            int start = backend.indexOf(marker);
            if (start >= 0) {
                name = backend.substring(start + marker.length()).strip();
                backend = backend.substring(0, start).strip();
            }
        }
        String device = firstNonBlank(xttsValue(output, "asignando_device"), xttsValue(output, "device"));
        if (!device.isBlank()) {
            StringBuilder message = new StringBuilder("Dispositivo usado por wrapper: ").append(device).append('.');
            if (!requested.isBlank() && !"auto".equalsIgnoreCase(requested)) {
                message.append(" Solicitado: ").append(requested).append('.');
            }
            if (!backend.isBlank()) {
                message.append(" Backend: ").append(backend).append('.');
            }
            if (!name.isBlank()) {
                message.append(" GPU: ").append(name).append('.');
            }
            return message.toString();
        }
        if (!requested.isBlank()) {
            return "Dispositivo solicitado al wrapper: " + requested + ".";
        }
        return "";
    }

    private static String xttsValue(String output, String key) {
        if (output == null || output.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String marker = "DOCUPODCAST_XTTS: " + key + "=";
        for (String line : output.split("\\R")) {
            String stripped = line.strip();
            if (stripped.startsWith(marker)) {
                return stripped.substring(marker.length()).strip();
            }
        }
        return "";
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.strip();
        }
        return second == null ? "" : second.strip();
    }

    private static String readable(Throwable error) {
        if (error == null) {
            return "error desconocido";
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message.strip();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
