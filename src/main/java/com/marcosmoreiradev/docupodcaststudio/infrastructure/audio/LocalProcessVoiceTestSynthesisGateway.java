package com.marcosmoreiradev.docupodcaststudio.infrastructure.audio;

import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Runs a short voice test through the same local process configuration used by document audio jobs. */
public final class LocalProcessVoiceTestSynthesisGateway implements VoiceTestSynthesisGateway {
    private final LocalTtsProcessConfiguration configuration;
    private final ExternalProcessRunner runner;

    public LocalProcessVoiceTestSynthesisGateway(LocalTtsProcessConfiguration configuration) {
        this(configuration, new DefaultExternalProcessRunner());
    }

    public LocalProcessVoiceTestSynthesisGateway(LocalTtsProcessConfiguration configuration, ExternalProcessRunner runner) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.runner = runner == null ? new DefaultExternalProcessRunner() : runner;
    }

    @Override
    public VoiceTestSynthesisResult synthesize(VoiceTestSynthesisRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        if (request.textFile() == null || request.outputFile() == null) {
            return VoiceTestSynthesisResult.failed("No se pudo preparar la prueba de voz: faltan rutas de entrada o salida.", "");
        }
        LocalTtsPreflightReport preflight = configuration.preflightReport();
        if (!preflight.ready()) {
            return VoiceTestSynthesisResult.blocked(preflight.userMessage(), true);
        }
        Path output = request.outputFile().toAbsolutePath().normalize();
        Path text = request.textFile().toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        Files.createDirectories(text.getParent());
        String sanitizedText = TtsTextPreprocessor.sanitize(request.phrase());
        if (sanitizedText.isBlank()) {
            return VoiceTestSynthesisResult.failed(
                    "La prueba no contiene texto narrable despues de retirar formato y enlaces.",
                    "empty-voice-payload");
        }
        Files.writeString(text, sanitizedText, StandardCharsets.UTF_8);
        List<String> command = configuration.commandForVoiceTest(
                request.segmentId(),
                text,
                output,
                request.language(),
                request.voiceProfileId(),
                request.referenceSampleFile());
        ExternalProcessResult result;
        try {
            result = runCommand(command, workDirectory(request));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return VoiceTestSynthesisResult.failed("La prueba de voz fue interrumpida antes de terminar.", ex.getMessage());
        }
        long outputBytes = Files.exists(output) ? Files.size(output) : 0L;
        if (result.exitCode() == 0 && outputBytes > 44L) {
            return VoiceTestSynthesisResult.generated(outputBytes,
                    "Prueba de voz real generada con el motor local configurado.", tail(result.combinedOutputTail()));
        }
        String message = result.timedOut()
                ? "La prueba de voz superó el tiempo límite del motor local. Revisa Configuración o prueba una frase más corta."
                : "El motor de voz no generó un WAV válido para la prueba. Revisa Soporte y diagnóstico en Configuración.";
        return VoiceTestSynthesisResult.failed(message + diagnosticSuffix(result, command), tail(result.combinedOutputTail()));
    }

    private ExternalProcessResult runCommand(List<String> command, Path workingDirectory) throws IOException, InterruptedException {
        if (command == null || command.isEmpty()) {
            return new ExternalProcessResult(-1, false, false, "Comando TTS vacío.", "",
                    "", Duration.ZERO);
        }
        Files.createDirectories(workingDirectory);
        ExternalProcessRequest request = ExternalProcessRequest.of(command, "voice-test-synthesis",
                        Duration.ofSeconds(configuration.timeoutSeconds()))
                .withWorkingDirectory(workingDirectory)
                .withEnvironment(Map.of("PYTHONUNBUFFERED", "1", "PYTHONIOENCODING", "utf-8", "PYTHONUTF8", "1"))
                .redirectingErrorStream();
        return runner.run(request);
    }

    private static Path workDirectory(VoiceTestSynthesisRequest request) {
        if (request.workingDirectory() != null) {
            return request.workingDirectory().toAbsolutePath().normalize();
        }
        Path parent = request.outputFile().toAbsolutePath().normalize().getParent();
        return parent == null ? Path.of(".").toAbsolutePath().normalize() : parent;
    }

    private static String diagnosticSuffix(ExternalProcessResult result, List<String> command) {
        String summary = command == null ? "" : command.stream()
                .map(token -> token.contains(" ") ? "\"" + token.replace("\"", "'") + "\"" : token)
                .collect(Collectors.joining(" "));
        String tail = tail(result.combinedOutputTail());
        StringBuilder message = new StringBuilder(" Código de salida: ").append(result.exitCode()).append('.');
        if (!summary.isBlank()) {
            message.append(" Comando auditado en Soporte y diagnóstico.");
        }
        if (!tail.isBlank()) {
            message.append(" Última salida: ").append(tail);
        }
        return message.toString();
    }

    private static String tail(String output) {
        String normalized = output == null ? "" : output.replace("\r", " ").strip();
        if (normalized.length() <= 900) {
            return normalized;
        }
        return normalized.substring(normalized.length() - 900);
    }
}
