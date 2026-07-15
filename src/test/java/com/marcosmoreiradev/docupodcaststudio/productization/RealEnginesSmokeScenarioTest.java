package com.marcosmoreiradev.docupodcaststudio.productization;

import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationProfile;
import com.marcosmoreiradev.docupodcaststudio.application.media.AudioNormalizationResult;
import com.marcosmoreiradev.docupodcaststudio.application.media.VideoAudioExtractionResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegToolDiscovery;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.FfmpegAudioNormalizationGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.media.FfmpegVideoAudioExtractionGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt-in smoke that executes real local engines. It is skipped during the normal test suite.
 *
 * <p>Run with {@code scripts\\19-smoke-motores-reales.bat}. The test intentionally avoids UI/JavaFX
 * and validates only the real process boundary: Coqui/XTTS, Piper and FFmpeg producing auditable
 * local artifacts. Whisper/STT is not part of the DocuPodcast product smoke.</p>
 */
final class RealEnginesSmokeScenarioTest {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(120);

    @Test
    void runsOptInRealEngineSmokeForCoquiPiperAndFfmpeg() throws Exception {
        Path evidenceDir = Path.of("target", "docupodcast-real-engines-smoke").toAbsolutePath().normalize();
        Files.createDirectories(evidenceDir);
        SmokeConfig config = SmokeConfig.fromSystem(Path.of(".").toAbsolutePath().normalize(), evidenceDir);
        if (!config.enabled()) {
            writeReport(evidenceDir, List.of(SmokeStep.skipped("opt-in", "Smoke real no habilitado",
                    "Ejecuta scripts\\19-smoke-motores-reales.bat o usa -Ddocupodcast.realEnginesSmoke.enabled=true.")), config);
            Assumptions.assumeTrue(false, "Smoke real de motores deshabilitado por defecto.");
        }

        List<SmokeStep> steps = new ArrayList<>();
        if (config.shouldRun("coqui")) {
            steps.add(runCoquiXttsSmoke(config));
        }
        if (config.shouldRun("piper")) {
            steps.add(runPiperSmoke(config));
        }
        if (config.shouldRun("ffmpeg")) {
            steps.add(runFfmpegSmoke(config));
        }
        if (steps.isEmpty()) {
            steps.add(SmokeStep.skipped("none", "Sin motores seleccionados", "Configura docupodcast.realEnginesSmoke.required=coqui,piper,ffmpeg o usa scripts modulares."));
        }
        Path report = writeReport(evidenceDir, steps, config);

        List<String> failedRequired = steps.stream()
                .filter(step -> config.requiredEngines().contains(step.engine()))
                .filter(step -> step.status() != SmokeStatus.PASSED)
                .map(step -> step.engine() + ": " + step.detail())
                .toList();
        assertTrue(failedRequired.isEmpty(), "Falló smoke real obligatorio. Ver " + report + ": " + failedRequired);
    }

    private static SmokeStep runCoquiXttsSmoke(SmokeConfig config) throws IOException {
        String engine = "coqui";
        Instant start = Instant.now();
        Path script = config.xttsScript();
        Path wrapper = config.xttsWrapper();
        Path modelDir = config.xttsModelDir();
        Path speaker = config.xttsSpeakerWav();
        Path text = config.evidenceDir().resolve("input/coqui-xtts-text.txt");
        Path output = config.evidenceDir().resolve("output/coqui-xtts-smoke.wav");
        Files.createDirectories(text.getParent());
        Files.createDirectories(output.getParent());
        Files.writeString(text, "DocuPodcast prueba Coqui XTTS con voz local.", StandardCharsets.UTF_8);

        List<String> missing = missingFiles(script, wrapper, speaker);
        if (!Files.isDirectory(modelDir)) {
            missing.add("modelDir=" + modelDir);
        }
        if (!missing.isEmpty()) {
            return missing(engine, "Coqui/XTTS", missing, start, config);
        }
        List<String> command = new ArrayList<>();
        command.add(powerShellExecutable());
        command.addAll(List.of("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                "-Python", config.xttsPython(),
                "-Wrapper", wrapper.toString(),
                "-ModelDir", modelDir.toString(),
                "-SpeakerWav", speaker.toString(),
                "-Text", text.toString(),
                "-Output", output.toString(),
                "-Language", "es"));
        CommandResult result = runProcess(command, config.evidenceDir().resolve("logs/coqui-xtts"));
        if (result.success() && wavLooksValid(output)) {
            return SmokeStep.passed(engine, "Coqui/XTTS genera WAV", "Salida: " + relativize(config.evidenceDir(), output), elapsed(start));
        }
        return SmokeStep.failed(engine, "Coqui/XTTS genera WAV", result.summary() + " · salida=" + output, elapsed(start));
    }

    private static SmokeStep runPiperSmoke(SmokeConfig config) throws IOException {
        String engine = "piper";
        Instant start = Instant.now();
        Path script = config.piperScript();
        Path piper = config.piperExecutable();
        Path model = config.piperModel();
        Path text = config.evidenceDir().resolve("input/piper-text.txt");
        Path output = config.evidenceDir().resolve("output/piper-smoke.wav");
        Files.createDirectories(text.getParent());
        Files.createDirectories(output.getParent());
        Files.writeString(text, "DocuPodcast prueba Piper como respaldo local.", StandardCharsets.UTF_8);

        List<String> missing = missingFiles(script, piper, model);
        if (!missing.isEmpty()) {
            return missing(engine, "Piper", missing, start, config);
        }
        List<String> command = new ArrayList<>();
        command.add(powerShellExecutable());
        command.addAll(List.of("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                "-Piper", piper.toString(),
                "-Model", model.toString(),
                "-Text", text.toString(),
                "-Output", output.toString()));
        CommandResult result = runProcess(command, config.evidenceDir().resolve("logs/piper"));
        if (result.success() && wavLooksValid(output)) {
            return SmokeStep.passed(engine, "Piper genera WAV", "Salida: " + relativize(config.evidenceDir(), output), elapsed(start));
        }
        return SmokeStep.failed(engine, "Piper genera WAV", result.summary() + " · salida=" + output, elapsed(start));
    }

    private static SmokeStep runFfmpegSmoke(SmokeConfig config) throws IOException {
        String engine = "ffmpeg";
        Instant start = Instant.now();
        Path ffmpeg = config.ffmpegExecutable();
        if (!Files.isRegularFile(ffmpeg)) {
            return missing(engine, "FFmpeg", List.of("ffmpeg=" + ffmpeg), start, config);
        }
        Files.createDirectories(config.evidenceDir().resolve("input"));
        Files.createDirectories(config.evidenceDir().resolve("output"));
        Path tone = config.evidenceDir().resolve("input/ffmpeg-tone.wav");
        Path video = config.evidenceDir().resolve("input/ffmpeg-video.mp4");
        Path normalized = config.evidenceDir().resolve("output/ffmpeg-normalized.wav");
        Path extracted = config.evidenceDir().resolve("output/ffmpeg-extracted-from-video.wav");

        CommandResult toneResult = runProcess(List.of(ffmpeg.toString(), "-y", "-f", "lavfi", "-i",
                "sine=frequency=880:duration=1", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2", tone.toString()),
                config.evidenceDir().resolve("logs/ffmpeg-generate-tone"));
        if (!toneResult.success() || !wavLooksValid(tone)) {
            return SmokeStep.failed(engine, "FFmpeg genera fixture de audio", toneResult.summary(), elapsed(start));
        }
        CommandResult videoResult = runProcess(List.of(ffmpeg.toString(), "-y",
                "-f", "lavfi", "-i", "color=c=black:s=640x360:d=1",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=1",
                "-shortest", "-c:v", "mpeg4", "-c:a", "aac", video.toString()), config.evidenceDir().resolve("logs/ffmpeg-generate-video"));
        if (!videoResult.success() || !Files.isRegularFile(video)) {
            return SmokeStep.failed(engine, "FFmpeg genera fixture de video", videoResult.summary(), elapsed(start));
        }

        FfmpegToolDiscovery discovery = new FfmpegToolDiscovery(ffmpeg, null, Files.isRegularFile(ffmpeg), true, "Smoke T90");
        AudioNormalizationResult normalizedResult = new FfmpegAudioNormalizationGateway(discovery)
                .normalize(tone, normalized, AudioNormalizationProfile.ASSIGNABLE_AUDIO);
        if (!normalizedResult.successful() || !wavLooksValid(normalized)) {
            return SmokeStep.failed(engine, "FFmpeg normaliza audio", normalizedResult.message(), elapsed(start));
        }
        VideoAudioExtractionResult extractedResult = new FfmpegVideoAudioExtractionGateway(discovery)
                .extractAudio(video, extracted);
        if (!extractedResult.successful() || !wavLooksValid(extracted)) {
            return SmokeStep.failed(engine, "FFmpeg extrae audio de video", extractedResult.message(), elapsed(start));
        }
        return SmokeStep.passed(engine, "FFmpeg normaliza y extrae audio", "Evidencia: "
                + relativize(config.evidenceDir(), normalized) + " / " + relativize(config.evidenceDir(), extracted), elapsed(start));
    }

    private static SmokeStep missing(String engine, String name, List<String> missing, Instant start, SmokeConfig config) {
        String detail = "Faltan artefactos locales: " + String.join(", ", missing);
        if (config.requiredEngines().contains(engine)) {
            return SmokeStep.failed(engine, name, detail, elapsed(start));
        }
        return SmokeStep.warning(engine, name, detail, elapsed(start));
    }

    private static CommandResult runProcess(List<String> command, Path logStem) throws IOException {
        Files.createDirectories(logStem.getParent());
        Path stdout = Path.of(logStem + "-stdout.log");
        Path stderr = Path.of(logStem + "-stderr.log");
        ExternalProcessResult result;
        try {
            result = new DefaultExternalProcessRunner().run(
                    ExternalProcessRequest.of(command, logStem.getFileName().toString(), PROCESS_TIMEOUT));
        } catch (IOException ex) {
            Files.writeString(stderr, ex.getMessage() == null ? ex.toString() : ex.getMessage(), StandardCharsets.UTF_8);
            return new CommandResult(false, -1, stdout, stderr, "No se pudo iniciar proceso: " + command.get(0));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(false, -1, stdout, stderr, "Proceso interrumpido");
        }
        Files.writeString(stdout, result.stdout(), StandardCharsets.UTF_8);
        Files.writeString(stderr, result.stderr(), StandardCharsets.UTF_8);
        if (result.timedOut()) {
            return new CommandResult(false, -1, stdout, stderr, "Timeout de " + PROCESS_TIMEOUT.toSeconds() + " segundos");
        }
        return new CommandResult(result.succeeded(), result.exitCode(), stdout, stderr, "exit=" + result.exitCode());
    }

    private static Path writeReport(Path evidenceDir, List<SmokeStep> steps, SmokeConfig config) throws IOException {
        Path report = evidenceDir.resolve("T90_REAL_ENGINES_SMOKE_REPORT.md");
        Files.createDirectories(evidenceDir);
        StringBuilder out = new StringBuilder();
        out.append("# T90 — Smoke real de motores locales\n\n");
        out.append("- Estado: ").append(steps.stream().allMatch(SmokeStep::successful) ? "OK" : "REVISAR").append("\n");
        out.append("- Habilitado: ").append(config.enabled()).append("\n");
        out.append("- Raíz de app: `").append(config.appRoot()).append("`\n");
        out.append("- Motores seleccionados/obligatorios: `").append(String.join(",", config.requiredEngines())).append("`\n");
        out.append("- Alcance: motor avanzado, voz local simple y FFmpeg. No incluye transcripcion de audio.\n");
        out.append("- Modo modular T90G: solo se ejecutan los motores listados como requeridos.\n\n");
        out.append("## Pasos\n\n");
        out.append("| Motor | Paso | Estado | Duración | Detalle |\n");
        out.append("|---|---|---|---:|---|\n");
        for (SmokeStep step : steps) {
            out.append("| ").append(step.engine()).append(" | ").append(escape(step.name()))
                    .append(" | ").append(step.status().displayName())
                    .append(" | ").append(step.duration().toMillis()).append(" ms")
                    .append(" | ").append(escape(step.detail())).append(" |\n");
        }
        out.append("\n## Evidencia esperada\n\n");
        out.append("- `input/` textos y fixtures generados.\n");
        out.append("- `output/` WAVs generados por motores reales.\n");
        out.append("- `logs/` stdout/stderr por proceso.\n");
        out.append("- sidecars FFmpeg `*-ffmpeg-*.log` y `*-ffmpeg-*.txt`.\n");
        Files.writeString(report, out.toString(), StandardCharsets.UTF_8);
        return report;
    }

    private static List<String> missingFiles(Path... paths) {
        ArrayList<String> missing = new ArrayList<>();
        for (Path path : paths) {
            if (path == null || !Files.isRegularFile(path)) {
                missing.add(String.valueOf(path));
            }
        }
        return missing;
    }

    private static boolean wavLooksValid(Path path) {
        try {
            return Files.isRegularFile(path) && Files.size(path) > 44;
        } catch (IOException ex) {
            return false;
        }
    }

    private static String powerShellExecutable() {
        return isWindows() ? "powershell" : "pwsh";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Duration elapsed(Instant start) {
        return Duration.between(start, Instant.now());
    }

    private static String relativize(Path root, Path path) {
        try {
            return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (RuntimeException ex) {
            return path.toString().replace('\\', '/');
        }
    }

    private static String escape(String value) {
        return Objects.toString(value, "").replace("|", "\\|").replace("\n", " ");
    }

    private enum SmokeStatus {
        PASSED("OK"), WARNING("Advertencia"), FAILED("Fallo"), SKIPPED("Omitido");

        private final String displayName;

        SmokeStatus(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }

    private record SmokeStep(String engine, String name, SmokeStatus status, String detail, Duration duration) {
        static SmokeStep passed(String engine, String name, String detail, Duration duration) {
            return new SmokeStep(engine, name, SmokeStatus.PASSED, detail, duration);
        }

        static SmokeStep warning(String engine, String name, String detail, Duration duration) {
            return new SmokeStep(engine, name, SmokeStatus.WARNING, detail, duration);
        }

        static SmokeStep failed(String engine, String name, String detail, Duration duration) {
            return new SmokeStep(engine, name, SmokeStatus.FAILED, detail, duration);
        }

        static SmokeStep skipped(String engine, String name, String detail) {
            return new SmokeStep(engine, name, SmokeStatus.SKIPPED, detail, Duration.ZERO);
        }

        boolean successful() {
            return status == SmokeStatus.PASSED || status == SmokeStatus.WARNING || status == SmokeStatus.SKIPPED;
        }
    }

    private record CommandResult(boolean success, int exitCode, Path stdout, Path stderr, String message) {
        String summary() {
            return message + " · stdout=" + stdout.getFileName() + " · stderr=" + stderr.getFileName();
        }
    }

    private record SmokeConfig(Path appRoot, Path evidenceDir, boolean enabled, Set<String> requiredEngines,
                               Path piperScript, Path piperExecutable, Path piperModel,
                               Path xttsScript, String xttsPython, Path xttsWrapper, Path xttsModelDir, Path xttsSpeakerWav,
                               Path ffmpegExecutable) {
        boolean shouldRun(String engineId) {
            return requiredEngines.contains(engineId);
        }

        static SmokeConfig fromSystem(Path appRoot, Path evidenceDir) throws IOException {
            Path root = appRoot == null ? Path.of(".").toAbsolutePath().normalize() : appRoot;
            Path evidence = evidenceDir == null ? root.resolve("target/docupodcast-real-engines-smoke") : evidenceDir;
            String enabledText = firstNonBlank(System.getProperty("docupodcast.realEnginesSmoke.enabled"),
                    System.getenv("DOCUPODCAST_REAL_ENGINES_SMOKE"));
            boolean enabled = "true".equalsIgnoreCase(enabledText) || "1".equals(enabledText) || "yes".equalsIgnoreCase(enabledText);
            Set<String> required = parseEngines(firstNonBlank(System.getProperty("docupodcast.realEnginesSmoke.required"),
                    System.getenv("DOCUPODCAST_REAL_ENGINES_REQUIRED"), "coqui,piper,ffmpeg"));
            Path modelsRoot = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.modelsRoot"),
                    System.getenv("DOCUPODCAST_MODELS_ROOT"), "models"));
            Path piperScript = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.piper.script"), "scripts/tts/piper-file-to-wav.ps1"));
            Path piperExe = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.piper.exe"),
                    System.getenv("DOCUPODCAST_PIPER_EXE"), "tools/piper/piper.exe"));
            Path piperModel = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.piper.model"),
                    System.getenv("DOCUPODCAST_PIPER_MODEL"), firstOnnx(modelsRoot.resolve("tts/piper/voices"))));
            Path xttsScript = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.xtts.script"), "scripts/tts/xtts-file-to-wav.ps1"));
            String python = root.resolve("tools/xtts-wrapper/.venv/Scripts/python.exe").toString();
            Path xttsWrapper = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.xtts.wrapper"), "tools/xtts-wrapper/synthesize_xtts.py"));
            Path xttsModelDir = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.xtts.modelDir"),
                    System.getenv("DOCUPODCAST_XTTS_MODEL_DIR"), modelsRoot.resolve("tts/xtts").toString()));
            Path xttsSpeaker = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.xtts.speaker"),
                    System.getenv("DOCUPODCAST_XTTS_SPEAKER_WAV"), modelsRoot.resolve("tts/xtts/speakers/voz-por-defecto.wav").toString()));
            Path ffmpeg = path(root, firstNonBlank(System.getProperty("docupodcast.smoke.ffmpeg"),
                    System.getenv("DOCUPODCAST_FFMPEG"), root.resolve("tools/ffmpeg/bin/ffmpeg.exe").toString()));
            return new SmokeConfig(root, evidence, enabled, required, piperScript, piperExe, piperModel,
                    xttsScript, python, xttsWrapper, xttsModelDir, xttsSpeaker, ffmpeg);
        }

        private static Set<String> parseEngines(String text) {
            LinkedHashSet<String> out = new LinkedHashSet<>();
            Arrays.stream(Objects.toString(text, "").split(","))
                    .map(value -> value.strip().toLowerCase(Locale.ROOT))
                    .filter(value -> !value.isBlank())
                    .forEach(out::add);
            return out.isEmpty() ? Set.of("coqui", "piper", "ffmpeg") : out;
        }

        private static Path path(Path root, String value) {
            Path path = Path.of(Objects.toString(value, "").strip());
            return path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
        }

        private static String firstOnnx(Path voicesDir) throws IOException {
            if (!Files.isDirectory(voicesDir)) {
                return voicesDir.resolve("es_ES-default-medium.onnx").toString();
            }
            try (Stream<Path> stream = Files.walk(voicesDir, 3)) {
                return stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".onnx"))
                        .findFirst()
                        .map(Path::toString)
                        .orElse(voicesDir.resolve("es_ES-default-medium.onnx").toString());
            }
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value.strip();
                }
            }
            return "";
        }
    }
}
