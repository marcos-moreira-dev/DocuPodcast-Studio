package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class XttsVoiceEngine extends AbstractProcessVoiceEngine {
    public static final EngineId ID = new EngineId("xtts");
    private static final String ACOUSTIC_TEXT_POLICY = "coqui-study-v3-dash-pauses";
    private static final String INFERENCE_PROFILE = "coqui-native-defaults-v2";
    private final EngineConfiguration configuration;
    private final Supplier<String> device;
    private final LocalProcessExecutor executor = new LocalProcessExecutor();

    public XttsVoiceEngine(EngineConfiguration configuration) {
        this(configuration, () -> "auto");
    }

    public XttsVoiceEngine(EngineConfiguration configuration, Supplier<String> device) {
        super(ID, "Coqui XTTS · voz IA avanzada",
                Set.of(EngineFeature.REFERENCE_VOICE, EngineFeature.EXPRESSIVE_STYLE, EngineFeature.BATCH),
                configuration);
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.device = device == null ? () -> "auto" : device;
    }

    @Override
    public String prepareText(String normalizedText) {
        String value = (normalizedText == null ? "" : normalizedText.strip())
                .replace("“", "")
                .replace("”", "")
                .replace("«", "")
                .replace("»", "")
                .replace("∀", " para todo ")
                .replace("∃", " existe ")
                .replace("∉", " no pertenece a ")
                .replace("∈", " pertenece a ")
                .replace("≥", " es mayor o igual que ")
                .replace("≤", " es menor o igual que ")
                .replace("≠", " es distinto de ")
                .replace("⇔", " si y solo si ")
                .replace("⇒", " implica ")
                .replace("→", " implica ")
                .replace("∧", " y ")
                .replace("∨", " o ")
                .replace("¬", " no ")
                .replace("=", " es igual a ")
                .replace("×", " por ")
                .replace("÷", " dividido para ");
        value = value.replaceAll("\\?\\s*\\.+", "?")
                .replaceAll("!\\s*\\.+", "!")
                .replaceAll("\\.{2,}", ".")
                // XTTS can vocalize separator dashes as noise. Only Coqui gets
                // this pause policy; lexical hyphens, ranges and minus signs stay intact.
                .replaceAll("^\\s*—\\s*", "")
                .replaceAll("\\s*—\\s*$", ".")
                .replaceAll("\\s*—\\s*", ", ")
                .replaceAll("\\s+[–-]{1,3}\\s+", ", ")
                .replaceAll("(?:,\\s*){2,}", ", ")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .strip();
        return containsLetterOrDigit(value) ? value : "";
    }

    @Override
    public String acousticFingerprint() {
        return super.acousticFingerprint()
                + "|text-policy=" + ACOUSTIC_TEXT_POLICY
                + "|inference=" + INFERENCE_PROFILE;
    }

    private static boolean containsLetterOrDigit(String value) {
        return value != null && value.codePoints().anyMatch(Character::isLetterOrDigit);
    }

    @Override
    public ComputeResourceDemand resourceDemand(ComputePreference requested) {
        final long mib = 1024L * 1024L;
        ComputePreference preference = requested == null
                ? ComputePreference.automatic() : requested;
        ComputeDeviceId computeDevice = computeDevice(preference);
        if (preference.mode() == ComputePreference.Mode.CPU_ONLY) {
            ModelResidencyDemand residency = new ModelResidencyDemand(
                    new ModelResidencyKey(ID.value(), "xtts-v2", "cpu", computeDevice),
                    2_500L * mib, 0L);
            return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                    512L * mib, 0L, true, computeDevice, 0,
                    residency, EncoderResourceDemand.none());
        }
        ModelResidencyDemand residency = new ModelResidencyDemand(
                new ModelResidencyKey(ID.value(), "xtts-v2", "gpu", computeDevice),
                1_500L * mib, 1_300L * mib);
        return new ComputeResourceDemand(Map.of(ResourceId.CPU_HEAVY, 1),
                384L * mib, 512L * mib, true, computeDevice, 1,
                residency, EncoderResourceDemand.none());
    }

    private static ComputeDeviceId computeDevice(ComputePreference preference) {
        if (preference.mode() == ComputePreference.Mode.CPU_ONLY) {
            return ComputeDeviceId.CPU_0;
        }
        return preference.mode() == ComputePreference.Mode.SPECIFIC_DEVICE
                ? ComputeDeviceId.parse(preference.deviceId())
                : ComputeDeviceId.AUTO_GPU_0;
    }

    @Override
    public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        EngineReadiness configured = super.inspectReadiness(ignored);
        if (!configured.ready()) return configured;
        List<String> missing = new ArrayList<>();
        requireFile("script", "script individual XTTS", missing);
        requireFile("batchScript", "script por lotes XTTS", missing);
        requireFile("python", "Python XTTS", missing);
        requireFile("wrapper", "wrapper individual XTTS", missing);
        requireFile("batchWrapper", "wrapper por lotes XTTS", missing);
        requireFile("workerWrapper", "worker persistente XTTS", missing);
        requireDirectory("modelDirectory", "directorio del modelo XTTS", missing);
        requireFile("modelConfig", "config.json", missing);
        requireFile("modelCheckpoint", "model.pth", missing);
        requireFile("vocabulary", "vocab.json", missing);
        requireFile("speakers", "speakers_xtts.pth", missing);
        requireFile("dvae", "dvae.pth", missing);
        requireFile("melStats", "mel_stats.pth", missing);
        requireFile("defaultSpeaker", "muestra de voz predeterminada", missing);
        if (!missing.isEmpty()) {
            return EngineReadiness.unavailable(ID,
                    "Voz IA avanzada no esta lista: faltan artefactos de XTTS.",
                    "Restaura o importa: " + String.join(", ", missing) + ".");
        }
        try {
            prepareManagedRuntime();
            return EngineReadiness.ready(ID,
                    "Voz IA avanzada lista con runtime portable, wrappers y modelo XTTS completo.");
        } catch (IOException invalidRuntime) {
            return EngineReadiness.unavailable(ID,
                    "El entorno Python de la voz avanzada no es portable.",
                    invalidRuntime.getMessage());
        }
    }

    private void requireFile(String key, String label, List<String> missing) {
        requirePath(key, label, false, missing);
    }

    private void requireDirectory(String key, String label, List<String> missing) {
        requirePath(key, label, true, missing);
    }

    private void requirePath(String key, String label, boolean directory,
                             List<String> missing) {
        String configured = configuration.value(key);
        if (configured.isBlank()) {
            missing.add(label + " (ruta no configurada)");
            return;
        }
        try {
            Path path = Path.of(configured);
            boolean exists = directory ? Files.isDirectory(path) : Files.isRegularFile(path);
            if (!exists) missing.add(label + " (" + configured + ")");
        } catch (InvalidPathException invalid) {
            missing.add(label + " (ruta invalida)");
        }
    }

    @Override
    public VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        prepareManagedRuntime();
        VoiceSynthesisResult result = super.synthesize(request, context);
        java.util.LinkedHashMap<String, String> diagnostics =
                new java.util.LinkedHashMap<>(result.diagnostics());
        String requestedDevice = selectedDevice();
        diagnostics.put("requestedDevice", requestedDevice);
        diagnostics.put("effectiveDevice", effectiveDeviceFromOutput(
                diagnostics.getOrDefault("output", ""), requestedDevice));
        return new VoiceSynthesisResult(result.audioFile(), result.durationSeconds(), diagnostics);
    }

    @Override public VoiceSynthesisBatchResult synthesizeBatch(VoiceSynthesisBatchRequest request,
                                                               ExecutionContext context)
            throws IOException, InterruptedException {
        prepareManagedRuntime();
        String workerWrapper = configuration.value("workerWrapper");
        if (!workerWrapper.isBlank()
                && Files.isRegularFile(Path.of(workerWrapper))
                && !request.units().isEmpty()) {
            return synthesizeWithWorker(request, context, Path.of(workerWrapper));
        }
        String template = configuration.value("batchCommandTemplate");
        if (template.isBlank() || request.units().size() == 1) return super.synthesizeBatch(request, context);
        ExecutionContext current = context == null ? ExecutionContext.defaults("voice-batch") : context;
        Path parent = request.units().getFirst().outputFile().toAbsolutePath().normalize().getParent();
        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, ".voice-batch-").toAbsolutePath().normalize();
        try {
            Path manifest = staging.resolve("batch.json");
            Files.writeString(manifest, manifest(request, staging), StandardCharsets.UTF_8);
            String command = template.replace("{manifestFile}", manifest.toString())
                    .replace("{language}", request.language())
                    .replace("{device}", selectedDevice());
            current.progress().report("SYNTHESIZING", 0.1, "Cargando una vez el modelo para el lote de voz.");
            LocalProcessExecutor.Result executed = executor.run(new ArrayList<>(CommandLineTokenizer.split(command)),
                    staging, current);
            if (executed.exitCode() != 0) throw new IOException("El lote de voz terminó con código "
                    + executed.exitCode() + ": " + tail(executed.output()));
            ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
            int completed = 0;
            for (VoiceSynthesisUnit unit : request.units()) {
                if (!Files.isRegularFile(unit.outputFile()) || Files.size(unit.outputFile()) == 0) {
                    throw new IOException("El lote no produjo la unidad " + unit.id());
                }
                results.add(new VoiceSynthesisResult(unit.outputFile(), 0,
                        Map.of("engineId", ID.value(), "batch", "true",
                                "requestedDevice", selectedDevice(),
                                "effectiveDevice", effectiveDeviceFromOutput(
                                        executed.output(), selectedDevice()))));
                current.progress().report("voice-unit", ++completed / (double) request.units().size(), unit.id());
            }
            return new VoiceSynthesisBatchResult(results, Map.of(
                    "mode", "optimized-batch", "engineId", ID.value(),
                    "requestedDevice", selectedDevice(),
                    "effectiveDevice", effectiveDeviceFromOutput(
                            executed.output(), selectedDevice())));
        } finally {
            deleteStaging(staging, parent);
        }
    }

    private VoiceSynthesisBatchResult synthesizeWithWorker(
            VoiceSynthesisBatchRequest request, ExecutionContext context,
            Path workerWrapper) throws IOException, InterruptedException {
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("voice-worker") : context;
        Path parent = request.units().getFirst().outputFile()
                .toAbsolutePath().normalize().getParent();
        Files.createDirectories(parent);
        Path staging = Files.createTempDirectory(parent, ".voice-worker-")
                .toAbsolutePath().normalize();
        ArrayList<VoiceSynthesisResult> results = new ArrayList<>();
        boolean yielded = false;
        boolean cancelled = false;
        VoiceSynthesisUnit activeUnit = null;
        XttsBatchWorker activeWorker = null;
        try {
            Path python = Path.of(configuration.value("python"));
            Path modelDirectory = Path.of(configuration.value("modelDirectory"));
            current.progress().report("voice-model-loading", 0.0,
                    "Cargando la voz IA avanzada en el dispositivo de cómputo seleccionado…");
            try (XttsBatchWorker worker = new XttsBatchWorker(
                    python, workerWrapper, modelDirectory, selectedDevice(),
                    staging, current.cancellation(), current.policy().timeout())) {
                activeWorker = worker;
                current.progress().report("voice-model-ready", 0.0,
                        "Voz IA avanzada lista. Preparando el primer fragmento…");
                for (int index = 0; index < request.units().size(); index++) {
                    VoiceSynthesisUnit unit = request.units().get(index);
                    activeUnit = unit;
                    Path text = staging.resolve(String.format(
                            "unit-%04d.txt", index + 1));
                    Files.writeString(text, unit.text(), StandardCharsets.UTF_8);
                    Path speaker = speaker(unit);
                    worker.synthesize(unit.id(), text, unit.outputFile(), speaker,
                            request.language(), current.cancellation(),
                            current.policy().timeout());
                    results.add(new VoiceSynthesisResult(unit.outputFile(), 0,
                            Map.of("engineId", ID.value(),
                                    "worker", "persistent",
                                    "requestedDevice", selectedDevice(),
                                    "effectiveDevice", worker.effectiveDevice())));
                    current.progress().report("voice-unit",
                            results.size() / (double) request.units().size(),
                            unit.id());
                    if (current.resourceLease().cancellationRequested()) {
                        cancelled = true;
                        break;
                    }
                    if (index + 1 < request.units().size()
                            && current.resourceLease().yieldRequested()) {
                        yielded = true;
                        break;
                    }
                }
            }
            return new VoiceSynthesisBatchResult(results,
                    Map.of("mode", "persistent-worker",
                            "engineId", ID.value(),
                            "requestedDevice", selectedDevice(),
                            "effectiveDevice", activeWorker == null
                                    ? selectedDevice() : activeWorker.effectiveDevice(),
                            "yielded", Boolean.toString(yielded),
                            "cancelled", Boolean.toString(cancelled),
                            "modelReleased", "true"));
        } catch (EngineExecutionException failure) {
            throw preserveWorkerFailure(parent, activeWorker, activeUnit, failure);
        } catch (IOException failure) {
            Path diagnostic = preserveWorkerFailureFiles(
                    parent, activeWorker == null ? null : activeWorker.logPath(),
                    activeUnit);
            if (diagnostic == null) throw failure;
            IOException preserved = new IOException(failure.getMessage()
                    + " Se conservó un diagnóstico en " + diagnostic + ".", failure);
            throw preserved;
        } finally {
            deleteStaging(staging, parent);
        }
    }

    private static EngineExecutionException preserveWorkerFailure(
            Path parent, XttsBatchWorker worker, VoiceSynthesisUnit unit,
            EngineExecutionException failure) {
        Path diagnostic = preserveWorkerFailureFiles(
                parent, worker == null ? null : worker.logPath(), unit);
        if (diagnostic == null) return failure;
        Map<String, String> details = new java.util.LinkedHashMap<>(
                failure.diagnostics());
        details.put("diagnosticFolder", diagnostic.toString());
        if (unit != null) details.put("failedUnit", unit.id());
        return new EngineExecutionException(failure.code(),
                failure.getMessage() + " Se conservó un diagnóstico en "
                        + diagnostic + ".",
                details, failure);
    }

    static Path preserveWorkerFailureFiles(
            Path parent, Path workerLog, VoiceSynthesisUnit unit) {
        if (parent == null) return null;
        try {
            String unitId = safeFileName(unit == null ? "inicio" : unit.id());
            Path diagnostic = parent.resolve("diagnostics")
                    .resolve("voz-" + unitId).toAbsolutePath().normalize();
            Files.createDirectories(diagnostic);
            if (unit != null) {
                Files.writeString(diagnostic.resolve("texto.txt"), unit.text(),
                        StandardCharsets.UTF_8);
            }
            if (workerLog != null && Files.isRegularFile(workerLog)) {
                Files.copy(workerLog, diagnostic.resolve("motor.log"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return diagnostic;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll(
                "[^\\p{L}\\p{N}._-]+", "_");
        return safe.isBlank() ? "fragmento" : safe;
    }

    private Path speaker(VoiceSynthesisUnit unit) throws IOException {
        String defaultSpeaker = configuration.value("defaultSpeaker");
        Path selected = unit.referenceAudio() == null
                ? (defaultSpeaker.isBlank() ? null
                : Path.of(defaultSpeaker).toAbsolutePath().normalize())
                : unit.referenceAudio();
        if (selected == null || !Files.isRegularFile(selected)) {
            throw new IOException("Falta la referencia de voz para " + unit.id());
        }
        return selected;
    }

    @Override
    protected String expandCommand(String command, VoiceSynthesisRequest request) {
        return command.replace("{device}", selectedDevice());
    }

    private String selectedDevice() {
        String selected = device.get();
        return selected == null || selected.isBlank() ? "auto" : selected.strip();
    }

    static String effectiveDeviceFromOutput(String output, String fallback) {
        String effective = fallback == null || fallback.isBlank()
                ? "auto" : fallback.strip();
        if (output == null || output.isBlank()) return effective;
        for (String line : output.split("\\R")) {
            String normalized = line.strip();
            int marker = normalized.lastIndexOf("device=");
            if (marker < 0) continue;
            String value = normalized.substring(marker + "device=".length()).strip();
            int separator = value.indexOf(' ');
            if (separator >= 0) value = value.substring(0, separator);
            if (!value.isBlank()) effective = value;
        }
        return effective;
    }

    private String manifest(VoiceSynthesisBatchRequest request, Path staging) throws IOException {
        String defaultSpeaker = configuration.value("defaultSpeaker");
        StringBuilder json = new StringBuilder("{\"segments\":[");
        int index = 0;
        for (VoiceSynthesisUnit unit : request.units()) {
            if (index > 0) json.append(',');
            Path text = staging.resolve(String.format("unit-%04d.txt", ++index));
            Files.writeString(text, unit.text(), StandardCharsets.UTF_8);
            Path speaker = unit.referenceAudio() == null
                    ? (defaultSpeaker.isBlank() ? null : Path.of(defaultSpeaker).toAbsolutePath().normalize())
                    : unit.referenceAudio();
            if (speaker == null || !Files.isRegularFile(speaker)) {
                throw new IOException("Falta la referencia de voz para " + unit.id());
            }
            json.append("{\"segmentId\":\"").append(escape(unit.id()))
                    .append("\",\"textFile\":\"").append(escape(text.toString()))
                    .append("\",\"outputFile\":\"").append(escape(unit.outputFile().toString()))
                    .append("\",\"speakerWav\":\"").append(escape(speaker.toString()))
                    .append("\",\"language\":\"").append(escape(request.language())).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static void deleteStaging(Path root, Path parent) {
        if (root == null || parent == null || !root.startsWith(parent)
                || !managedStagingDirectory(root) || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException ignored) { }
    }

    static boolean managedStagingDirectory(Path root) {
        String name = root.getFileName().toString();
        return name.startsWith(".voice-batch-")
                || name.startsWith(".voice-worker-");
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String tail(String value) {
        String text = value == null ? "" : value.strip();
        return text.length() <= 1200 ? text : text.substring(text.length() - 1200);
    }

    private void prepareManagedRuntime() throws IOException {
        String root = configuration.value("runtimeRoot");
        String python = configuration.value("python");
        if (root.isBlank() || python.isBlank()) return;
        ManagedPythonEnvironmentRepair.repair(Path.of(root), Path.of(python), descriptor().displayName());
    }
}
