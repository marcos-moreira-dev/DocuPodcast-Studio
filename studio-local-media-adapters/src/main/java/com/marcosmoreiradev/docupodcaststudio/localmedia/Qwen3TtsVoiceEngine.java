package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/** Local Qwen3-TTS 1.7B Base Q8 adapter backed by llama.cpp. */
public final class Qwen3TtsVoiceEngine implements VoiceSynthesisEngine {
    public static final EngineId ID = new EngineId("qwen3-tts-local");
    private static final String MODEL_ID = "qwen3-tts-12hz-1.7b-base-q8_0";
    private final EngineConfiguration configuration;
    private final Supplier<String> device;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();
    private final QwenEmotionProsodyProcessor prosody = new QwenEmotionProsodyProcessor();
    private final EngineDescriptor descriptor = new EngineDescriptor(ID, CapabilityId.VOICE_SYNTHESIS,
            "Qwen3-TTS local · 1.7B Q8", "1.7B-Q8_0", "llama.cpp-local-process",
            Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE,
                    EngineFeature.BATCH, EngineFeature.HARDWARE_ACCELERATION), false);

    public Qwen3TtsVoiceEngine(EngineConfiguration configuration, Supplier<String> device) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.device = device == null ? () -> "auto" : device;
    }

    @Override public EngineDescriptor descriptor() { return descriptor; }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of());
    }

    @Override public String prepareText(String normalizedText) {
        return normalizedText == null ? "" : normalizedText.strip()
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ");
    }

    @Override public ComputeResourceDemand resourceDemand(ComputePreference requested) {
        final long mib = 1024L * 1024L;
        ComputePreference preference = requested == null ? ComputePreference.automatic() : requested;
        boolean cpuOnly = preference.mode() == ComputePreference.Mode.CPU_ONLY;
        ComputeDeviceId computeDevice = cpuOnly ? ComputeDeviceId.CPU_0
                : preference.mode() == ComputePreference.Mode.SPECIFIC_DEVICE
                ? ComputeDeviceId.parse(preference.deviceId()) : ComputeDeviceId.AUTO_GPU_0;
        ModelResidencyDemand residency = new ModelResidencyDemand(
                new ModelResidencyKey(ID.value(), MODEL_ID,
                        cpuOnly ? "cpu" : "gpu-ram-offload", computeDevice),
                5_400L * mib, cpuOnly ? 0L : 2_570L * mib);
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                768L * mib, cpuOnly ? 0L : 256L * mib, true, computeDevice,
                cpuOnly ? 0 : 1, residency, EncoderResourceDemand.none());
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        List<String> missing = new ArrayList<>();
        requireFile("executable", "llama-tts.exe", missing);
        requireFile("model", "Qwen3-TTS 1.7B Base Q8_0", missing);
        requireFile("codec", "codec/mmproj Q8_0", missing);
        requireFile("defaultSpeaker", "muestra de voz predeterminada", missing);
        if (!missing.isEmpty()) return EngineReadiness.unavailable(ID,
                "Qwen3-TTS local no está listo.", "Prepara en Configuración: " + String.join(", ", missing) + ".");
        if (!configuration.value("model").toLowerCase(java.util.Locale.ROOT).contains("q8_0")
                || !configuration.value("codec").toLowerCase(java.util.Locale.ROOT).contains("q8_0")) {
            return EngineReadiness.unavailable(ID,
                    "La instalación Qwen3-TTS no cumple la política Q8.",
                    "Selecciona el talker y codec 1.7B Base Q8_0 administrados.");
        }
        return EngineReadiness.ready(ID,
                "Qwen3-TTS 1.7B Base Q8 listo para síntesis local con GPU + RAM offload.");
    }

    @Override public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        String style = request.options().getOrDefault("styleId",
                request.options().getOrDefault("tone", "neutral"));
        return synthesizeUnit(request.text(), request.language(), request.referenceAudio(),
                request.outputFile(), style, request.options(), context);
    }

    @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                               ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("qwen3-tts-batch") : context;
        ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
        boolean yielded = false;
        boolean cancelled = false;
        ResidentWorker worker = null;
        Path workerPath = workerExecutable();
        if (!request.units().isEmpty() && Files.isRegularFile(workerPath)) {
            try {
                current.progress().report("voice-model-loading", 0.01,
                        "Cargando Qwen3-TTS una sola vez para el lote actual.");
                worker = ResidentWorker.start(workerPath, residentCommand(workerPath), current);
            } catch (IOException unavailable) {
                current.progress().report("voice-model-fallback", 0.01,
                        "El trabajador residente no inició; se usará el ejecutable compatible por fragmento.");
            }
        }
        boolean resident = worker != null;
        try {
            for (int index = 0; index < request.units().size(); index++) {
                current.cancellation().throwIfCancellationRequested();
                VoiceSynthesisUnit unit = request.units().get(index);
                LinkedHashMap<String, String> options = new LinkedHashMap<>(request.options());
                options.putAll(unit.metadata());
                options.put("styleId", unit.styleId());
                try {
                    results.add(synthesizeUnit(unit.text(), request.language(), unit.referenceAudio(),
                            unit.outputFile(), unit.styleId(), options, current, worker));
                    current.progress().report("voice-unit",
                            results.size() / (double) request.units().size(), unit.id());
                } catch (AudioQualityException rejected) {
                    results.add(new VoiceSynthesisResult(unit.outputFile(), 0.0,
                            Map.of("qualityStatus", "REVIEW_REQUIRED", "qualityReason", rejected.getMessage())));
                    current.progress().report("voice-quality-review", index / (double) request.units().size(),
                            "Fragmento " + unit.id() + " pendiente de revisión; se continúa con los demás.");
                }
                if (current.resourceLease().cancellationRequested()) {
                    cancelled = true;
                    break;
                }
                if (index + 1 < request.units().size() && current.resourceLease().yieldRequested()) {
                    yielded = true;
                    break;
                }
            }
        } finally {
            if (worker != null) worker.close();
        }
        return new VoiceSynthesisBatchResult(results, Map.of(
                "mode", resident ? "resident-local-worker" : "bounded-local-process",
                "engineId", ID.value(),
                "model", MODEL_ID,
                "prosodyPolicy", QwenEmotionProsodyProcessor.POLICY_VERSION,
                "computeDevice", selectedDevice(),
                "yielded", Boolean.toString(yielded),
                "cancelled", Boolean.toString(cancelled),
                "modelReleased", "true"));
    }

    private VoiceSynthesisResult synthesizeUnit(String text, String language,
                                                Path requestedSpeaker, Path output,
                                                String style, Map<String, String> options,
                                                ExecutionContext context)
            throws IOException, InterruptedException {
        return synthesizeUnit(text, language, requestedSpeaker, output, style, options, context, null);
    }

    private VoiceSynthesisResult synthesizeUnit(String text, String language,
                                                Path requestedSpeaker, Path output,
                                                String style, Map<String, String> options,
                                                ExecutionContext context, ResidentWorker worker)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("qwen3-tts") : context;
        for (int attempt = 0; attempt < 2; attempt++) {
            current.cancellation().throwIfCancellationRequested();
            try {
                return synthesizeAttempt(text, language, requestedSpeaker, output, style, options, current, worker, attempt);
            } catch (AudioQualityException rejected) {
                if (attempt == 1) throw rejected;
                current.progress().report("voice-quality-retry", 0.0,
                        "Reintentando una vez el fragmento sospechoso: " + rejected.getMessage());
            }
        }
        throw new AssertionError("bounded quality retry");
    }

    private VoiceSynthesisResult synthesizeAttempt(String text, String language,
                                                Path requestedSpeaker, Path output,
                                                String style, Map<String, String> options,
                                                ExecutionContext context, ResidentWorker worker, int attempt)
            throws IOException, InterruptedException {
        EngineReadiness readiness = inspectReadiness(configuration);
        if (!readiness.ready()) throw new IOException(readiness.summary() + " "
                + String.join(" ", readiness.recommendedActions()));
        ExecutionContext current = context == null ? ExecutionContext.defaults("qwen3-tts") : context;
        Path target = output.toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Path speaker = resolveSpeaker(requestedSpeaker);
        // Keep the native worker protocol on a short path. Batch projects can live
        // several directories below a long user-selected title; placing the request
        // manifest beside the final WAV can then cross the Win32 MAX_PATH boundary
        // before the worker has a chance to read it.
        Path staging = createStagingDirectory();
        try {
            Path prompt = staging.resolve("prompt.txt");
            Path raw = staging.resolve("raw.wav");
            Files.writeString(prompt, text, StandardCharsets.UTF_8);
            String processOutput;
            if (worker == null) {
                current.progress().report("voice-model-loading", 0.02,
                        "Cargando Qwen3-TTS 1.7B Q8 con el perfil local GPU + RAM.");
                List<String> invocation = new ArrayList<>(command(prompt, raw, speaker, language, text));
                if (attempt > 0) invocation.set(invocation.indexOf("--seed") + 1, "1235");
                LocalProcessExecutor.Result executed = executor.run(invocation,
                        staging, current);
                if (executed.exitCode() != 0) {
                    throw new IOException("Qwen3-TTS terminó con código " + executed.exitCode()
                            + ": " + tail(executed.output()));
                }
                processOutput = executed.output();
            } else {
                processOutput = worker.synthesize(prompt, raw, speaker,
                        language == null || language.isBlank() ? "es" : language,
                        maxFrames(text), staging, current);
            }
            current.progress().report("voice-prosody", 0.92,
                    "Preparando el tono, comprobando la señal y ajustando el silencio inicial del fragmento.");
            Path candidate = staging.resolve("candidate.wav");
            PcmAudioQuality.Report quality;
            try {
                prosody.process(raw, candidate, style);
                current.progress().report("voice-quality-check", 0.96, "Comprobando la calidad acústica del fragmento.");
                quality = PcmAudioQuality.inspect(candidate, text);
                if (quality.verdict() != PcmAudioQuality.Verdict.ACCEPT)
                    throw new AudioQualityException(quality.reason());
            } catch (AudioQualityException rejected) {
                // Preserve rejected candidates separately, never as valid output or over the user's WAV.
                Path review = Files.createTempDirectory(target.getParent(), ".audio-review-");
                if (Files.isRegularFile(raw)) Files.copy(raw, review.resolve("raw.wav"));
                if (Files.isRegularFile(candidate)) Files.copy(candidate, review.resolve("candidate.wav"));
                Files.writeString(review.resolve("review.txt"), "Fragmento: " + target.getFileName()
                        + "\nTexto: " + text + "\nIntento: " + (attempt + 1) + "\n" + rejected.getMessage());
                throw new AudioQualityException(rejected.getMessage() + " Diagnóstico: " + review);
            }
            current.cancellation().throwIfCancellationRequested();
            Files.writeString(target.resolveSibling(target.getFileName() + ".quality.txt"),
                    "Validación acústica (no verifica palabras): " + quality
                            + "\nIntentos: " + (attempt + 1));
            if (quality.repetitionWarning()) current.progress().report("voice-quality-warning", .98, quality.reason());
            try {
                Files.move(candidate, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(candidate, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
            diagnostics.put("engineId", ID.value());
            diagnostics.put("qualityStatus", quality.repetitionWarning() ? "WARNING" : "ACCEPTED");
            diagnostics.put("qualityReason", quality.reason());
            diagnostics.put("qualityMeasurements", quality.toString());
            diagnostics.put("qualityAttempts", Integer.toString(attempt + 1));
            diagnostics.put("model", MODEL_ID);
            diagnostics.put("quantization", "Q8_0");
            diagnostics.put("prosodyPolicy", QwenEmotionProsodyProcessor.POLICY_VERSION);
            diagnostics.put("prosodyFamily", QwenEmotionProsodyProcessor.profileFor(style).family());
            diagnostics.put("requestedTone", options.getOrDefault("requestedTone",
                    style == null ? "" : style));
            diagnostics.put("appliedTone", options.getOrDefault("appliedTone", "engine-default"));
            diagnostics.put("referenceAudio", speaker.toString());
            diagnostics.put("computeDevice", selectedDevice());
            diagnostics.put("cancelled", "false");
            diagnostics.put("referenceTranscriptUsed", "false");
            diagnostics.put("referenceTranscriptAvailable",
                    Boolean.toString(!options.getOrDefault("referenceTranscript", "").isBlank()));
            diagnostics.put("processReuse", worker == null ? "one-shot" : "resident-batch");
            diagnostics.put("output", tail(processOutput));
            return new VoiceSynthesisResult(target, 0.0, diagnostics);
        } finally {
            deleteTree(staging);
        }
    }

    static Path createStagingDirectory() throws IOException {
        return Files.createTempDirectory("docupodcast-qwen-");
    }

    private List<String> command(Path prompt, Path raw, Path speaker, String language, String text) {
        return List.of(configuration.value("executable"),
                "-m", configuration.value("model"),
                "-mm", configuration.value("codec"),
                "-f", prompt.toString(),
                "--tts-lang", language == null || language.isBlank() ? "es" : language,
                "--tts-speaker-file", speaker.toString(),
                "-o", raw.toString(),
                "-ngl", gpuLayers(),
                "--no-mmproj-offload",
                "-t", "4",
                "--seed", "1234",
                "--temp", "0.75",
                "--top-k", "30",
                "--top-p", "0.90",
                "-n", Integer.toString(maxFrames(text)));
    }

    private List<String> residentCommand(Path worker) {
        return List.of(worker.toString(),
                "-m", configuration.value("model"),
                "-mm", configuration.value("codec"),
                "-ngl", gpuLayers(),
                "--no-mmproj-offload",
                "-t", "4",
                "--seed", "1234",
                "--temp", "0.75",
                "--top-k", "30",
                "--top-p", "0.90");
    }

    private Path workerExecutable() {
        Path executable = Path.of(configuration.value("executable")).toAbsolutePath().normalize();
        Path isolated = executable.getParent().resolve("resident-worker").resolve("llama-tts-worker.exe");
        if (Files.isRegularFile(isolated)) return isolated;
        return executable.resolveSibling("llama-tts-worker.exe");
    }

    private static int maxFrames(String text) {
        int words = Math.max(1, text == null || text.isBlank() ? 1 : text.strip().split("\\s+").length);
        return Math.max(220, Math.min(768, 96 + words * 12));
    }

    private String gpuLayers() {
        String selected = selectedDevice();
        return selected != null && selected.strip().equalsIgnoreCase("cpu") ? "0" : "99";
    }

    private String selectedDevice() {
        String selected = device.get();
        return selected == null || selected.isBlank() ? "auto" : selected.strip();
    }

    private Path resolveSpeaker(Path requested) throws IOException {
        Path selected = requested;
        if (selected == null && !configuration.value("defaultSpeaker").isBlank()) {
            selected = Path.of(configuration.value("defaultSpeaker"));
        }
        if (selected == null || !Files.isRegularFile(selected)) {
            throw new IOException("Qwen3-TTS requiere una muestra de voz WAV válida.");
        }
        return selected.toAbsolutePath().normalize();
    }

    private void requireFile(String key, String label, List<String> missing) {
        try {
            String value = configuration.value(key);
            if (value.isBlank() || !Files.isRegularFile(Path.of(value))) missing.add(label);
        } catch (InvalidPathException invalid) {
            missing.add(label);
        }
    }

    @Override public String acousticFingerprint() {
        return VoiceSynthesisEngine.super.acousticFingerprint()
                + "|model=" + MODEL_ID + "|prosody=" + QwenEmotionProsodyProcessor.POLICY_VERSION
                + "|mmproj=cpu|ngl=" + gpuLayers();
    }

    private static String tail(String value) {
        String text = value == null ? "" : value.strip();
        return text.length() <= 1200 ? text : text.substring(text.length() - 1200);
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) { }
    }

    /** One model-owning native process for one application batch. */
    private static final class ResidentWorker implements AutoCloseable {
        private final Process process;
        private final BufferedWriter input;
        private final BufferedReader output;
        private final Path diagnostics;

        private ResidentWorker(Process process, Path diagnostics) {
            this.process = process;
            this.diagnostics = diagnostics;
            input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            output = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        }

        static ResidentWorker start(Path executable, List<String> command, ExecutionContext context)
                throws IOException, InterruptedException {
            Path diagnostics = Files.createTempFile("docupodcast-qwen-worker-", ".log");
            Process process;
            try {
                process = new ProcessBuilder(command)
                    .directory(executable.getParent().toFile())
                    .redirectError(diagnostics.toFile())
                    .start();
            } catch (IOException | RuntimeException failure) {
                Files.deleteIfExists(diagnostics);
                throw failure;
            }
            ResidentWorker worker = new ResidentWorker(process, diagnostics);
            try {
                String ready = worker.awaitLine(context, Duration.ofMinutes(10));
                if (!"READY".equals(ready)) {
                    throw new IOException("El trabajador Qwen no confirmó la carga: " + ready);
                }
                return worker;
            } catch (IOException | InterruptedException | RuntimeException failure) {
                worker.close();
                throw failure;
            }
        }

        String synthesize(Path prompt, Path raw, Path speaker, String language, int frames,
                          Path staging, ExecutionContext context)
                throws IOException, InterruptedException {
            Path manifest = staging.resolve("worker-request.txt");
            Files.write(manifest, List.of(prompt.toAbsolutePath().normalize().toString(),
                    raw.toAbsolutePath().normalize().toString(), speaker.toAbsolutePath().normalize().toString(),
                    language, Integer.toString(frames)), StandardCharsets.UTF_8);
            input.write(manifest.toAbsolutePath().normalize().toString());
            input.newLine();
            input.flush();
            String response = awaitLine(context, Duration.ofHours(8));
            if (response == null || !response.startsWith("OK\t")) {
                throw new IOException("Qwen residente no produjo el fragmento: "
                        + (response == null ? diagnosticTail() : response));
            }
            return response;
        }

        private String awaitLine(ExecutionContext context, Duration timeout)
                throws IOException, InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                context.cancellation().throwIfCancellationRequested();
                if (output.ready()) return output.readLine();
                if (!process.isAlive()) return output.readLine();
                Thread.sleep(50L);
            }
            throw new IOException("Tiempo de espera agotado en Qwen residente. " + diagnosticTail());
        }

        private String diagnosticTail() {
            try { return tail(Files.readString(diagnostics, StandardCharsets.UTF_8)); }
            catch (IOException ignored) { return "sin diagnóstico nativo"; }
        }

        @Override public void close() {
            try {
                if (process.isAlive()) {
                    input.write("QUIT");
                    input.newLine();
                    input.flush();
                    if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroy();
                    if (process.isAlive() && !process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
                }
            } catch (IOException | InterruptedException ignored) {
                process.destroyForcibly();
                if (ignored instanceof InterruptedException) Thread.currentThread().interrupt();
            } finally {
                try { input.close(); } catch (IOException ignored) { }
                try { output.close(); } catch (IOException ignored) { }
                try { Files.deleteIfExists(diagnostics); } catch (IOException ignored) { }
            }
        }
    }
}
