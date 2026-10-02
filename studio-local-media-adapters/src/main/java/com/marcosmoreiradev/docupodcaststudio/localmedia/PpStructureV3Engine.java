package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Explicit enhanced local layout analysis; the standard Tesseract path remains independent. */
final class PpStructureV3Engine implements ContentAnalysisEngine {
    static final EngineId ID = new EngineId("pp-structure-v3-local");
    private static final Duration TIMEOUT = Duration.ofMinutes(20);
    private final EngineConfiguration configuration;
    private final EngineCertificationStore certifications;
    private final ProcessFactory processFactory;

    PpStructureV3Engine(EngineConfiguration configuration) {
        this(configuration, EngineCertificationStore.none());
    }

    PpStructureV3Engine(EngineConfiguration configuration,
                        EngineCertificationStore certifications) {
        this(configuration, certifications, ProcessBuilder::start);
    }

    PpStructureV3Engine(EngineConfiguration configuration,
                        EngineCertificationStore certifications,
                        ProcessFactory processFactory) {
        this.configuration = configuration;
        this.certifications = java.util.Objects.requireNonNullElse(
                certifications, EngineCertificationStore.none());
        this.processFactory = java.util.Objects.requireNonNull(processFactory);
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.CONTENT_LAYOUT_ANALYSIS,
                "Análisis avanzado de contenido", "PP-StructureV3", "managed-python",
                Set.of(EngineFeature.HARDWARE_ACCELERATION), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of());
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        List<String> missing = List.of("runtime", "python", "script", "models").stream()
                .filter(key -> {
                    String value = configuration.value(key);
                    if (value.isBlank()) return true;
                    Path path = Path.of(value);
                    return "runtime".equals(key)
                            ? !Files.isDirectory(path)
                            : "models".equals(key)
                            ? !Files.isRegularFile(path.resolve("manifest.json"))
                            : !Files.isRegularFile(path);
                }).toList();
        if (!missing.isEmpty()) {
            return EngineReadiness.unavailable(ID,
                    "El análisis avanzado necesita un runtime y modelos locales validados.",
                    "Importar el paquete PP-StructureV3 desde Motores y dependencias.");
        }
        try {
            PpStructurePackageManifest.Verification verification =
                    PpStructurePackageManifest.verify(Path.of(configuration.value("runtime")));
            if (!verification.valid()) {
                return new EngineReadiness(ID, ReadinessState.UNAVAILABLE,
                        "El paquete de análisis avanzado no superó la validación.",
                        verification.issues(),
                        List.of("Importar o reconstruir el paquete PP-StructureV3."),
                        String.join(System.lineSeparator(), verification.issues()));
            }
        } catch (IOException failure) {
            return EngineReadiness.unavailable(ID,
                    "No se pudo validar el paquete PP-StructureV3.",
                    "Revisar o importar nuevamente el paquete.");
        }
        String model = "pp-structure-v3:" + verificationProfile();
        String hardware = EngineHardwareFingerprint.current(ComputePreference.automatic());
        try {
            if (certifications.find(ID, model)
                    .filter(record -> record.matches(
                            PpStructurePackageManifest.PADDLE_OCR_VERSION,
                            model, hardware, true))
                    .isEmpty()) {
                return new EngineReadiness(ID, ReadinessState.DEGRADED,
                        "El paquete PP-StructureV3 es válido, pero necesita una prueba física.",
                        List.of("Falta certificación vigente de layout real."),
                        List.of("Probar análisis avanzado desde Motores y dependencias."),
                        "model=" + model + System.lineSeparator() + "hardware=" + hardware);
            }
        } catch (IOException failure) {
            return new EngineReadiness(ID, ReadinessState.DEGRADED,
                    "No se pudo leer la certificación PP-StructureV3.",
                    List.of(failure.getMessage()), List.of("Repetir la prueba física."), "");
        }
        return EngineReadiness.ready(ID, "Análisis avanzado de contenido listo y certificado.");
    }

    String verificationProfile() {
        try {
            return PpStructurePackageManifest.verify(
                    Path.of(configuration.value("runtime"))).profile();
        } catch (IOException failure) {
            return "";
        }
    }

    @Override public Set<ContentAnalysisOperation> operations() {
        return Set.of(ContentAnalysisOperation.LAYOUT_ANALYSIS,
                ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION);
    }

    @Override
    public ContentAnalysisResult analyze(ContentAnalysisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (!operations().contains(request.operation())) {
            throw new IllegalArgumentException(
                    "PP-StructureV3 layout does not implement " + request.operation());
        }
        return analyzeShared(request, context);
    }

    ContentAnalysisResult analyzeShared(ContentAnalysisRequest request,
                                        ExecutionContext context)
            throws IOException, InterruptedException {
        if (!operations().contains(request.operation())) {
            if (request.operation() != ContentAnalysisOperation.MATH_RECOGNITION) {
                throw new IllegalArgumentException(
                        "Unsupported PP-Structure operation: " + request.operation());
            }
        }
        EngineReadiness readiness = inspectReadiness(null);
        if (readiness.state() == ReadinessState.UNAVAILABLE) {
            throw new IOException(readiness.summary());
        }
        Path output = Files.createTempFile("docupodcast-pp-structure-", ".json");
        Path log = output.resolveSibling(output.getFileName() + ".log");
        String operation = switch (request.operation()) {
            case MATH_RECOGNITION -> "math";
            case TABLE_STRUCTURE_RECOGNITION -> "table";
            default -> "layout";
        };
        String requestedDevice = switch (context.computePreference().mode()) {
            case CPU_ONLY -> "cpu";
            case AUTO -> "auto";
            case PREFER_GPU, SPECIFIC_DEVICE -> "gpu";
        };
        ProcessBuilder processBuilder = new ProcessBuilder(
                configuration.value("python"), configuration.value("script"),
                "--input", request.visualInputs().getFirst().file().toString(),
                "--output", output.toString(),
                "--models-root", configuration.value("models"),
                "--operation", operation,
                "--device", requestedDevice)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        Process process = processFactory.start(processBuilder);
        ManagedProcessContainment containment = ManagedProcessContainment.create();
        containment.attach(process);
        Instant processStarted = OwnedProcessDiagnostics.started("PP_STRUCTURE", process,
                processBuilder.command(), "PpStructureV3Engine",
                "single analysis completion or cancellation", containment.diagnostics());
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        try {
            while (process.isAlive() && System.nanoTime() < deadline) {
                context.cancellation().throwIfCancellationRequested();
                context.progress().report("ANALYZING", 0.6,
                        "Analizando layout, tablas y fórmulas localmente.");
                process.waitFor(350, TimeUnit.MILLISECONDS);
            }
            if (process.isAlive()) {
                process.descendants().toList().reversed().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new EngineExecutionException(EngineDiagnosticCode.REQUEST_TIMEOUT,
                        "PP-StructureV3 superó el tiempo máximo.",
                        Map.of("requestedDevice", requestedDevice));
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(output)) {
                String details = tail(log);
                String normalized = details.toLowerCase(java.util.Locale.ROOT);
                EngineDiagnosticCode code = normalized.contains("device_mismatch")
                        ? EngineDiagnosticCode.DEVICE_MISMATCH
                        : normalized.contains("out of memory")
                        || normalized.contains("oom")
                        ? EngineDiagnosticCode.OOM : EngineDiagnosticCode.CHILD_EXIT;
                throw new EngineExecutionException(code,
                        code == EngineDiagnosticCode.DEVICE_MISMATCH
                                ? "PP-StructureV3 no pudo usar la GPU solicitada."
                                : code == EngineDiagnosticCode.OOM
                                ? "PP-StructureV3 agotó la memoria en " + requestedDevice
                                + "; selecciona CPU explícitamente para reintentar."
                                : "PP-StructureV3 falló. " + details,
                        Map.of("requestedDevice", requestedDevice,
                                "exitCode", Integer.toString(process.exitValue()),
                                "log", details));
            }
            String json = Files.readString(output, StandardCharsets.UTF_8);
            String effectiveDevice = OllamaJson.stringProperty(json, "device");
            if ("gpu".equals(requestedDevice)
                    && !effectiveDevice.toLowerCase(java.util.Locale.ROOT)
                    .startsWith("gpu")) {
                throw new EngineExecutionException(
                        EngineDiagnosticCode.DEVICE_MISMATCH,
                        "PP-StructureV3 terminó sin usar la GPU solicitada.",
                        Map.of("requestedDevice", requestedDevice,
                                "effectiveDevice", effectiveDevice));
            }
            String recognizedText = request.operation() == ContentAnalysisOperation.MATH_RECOGNITION
                    ? firstFormula(json) : "";
            return new ContentAnalysisResult(recognizedText, json,
                    recognizedText.isBlank() && "math".equals(operation) ? 0.0 : 1.0, List.of(),
                    Map.of("engineId", ID.value(), "operation", operation,
                            "profile", "ENHANCED", "network", "disabled",
                            "requestedDevice", requestedDevice,
                            "effectiveDevice", effectiveDevice));
        } finally {
            if (process.isAlive()) {
                process.descendants().toList().reversed().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            containment.close();
            OwnedProcessDiagnostics.stopped("PP_STRUCTURE", process, "PpStructureV3Engine",
                    processStarted, "ANALYSIS_END", process.isAlive() ? null : process.exitValue(),
                    process.isAlive());
            Files.deleteIfExists(output);
            Files.deleteIfExists(log);
        }
    }

    static String firstFormula(String json) {
        for (String property : List.of("rec_formula", "latex", "formula")) {
            String value = OllamaJson.stringProperty(json, property).strip();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private static String tail(Path log) {
        try {
            if (!Files.isRegularFile(log)) return "Sin registro técnico.";
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            return String.join(" | ", lines.subList(Math.max(0, lines.size() - 20), lines.size()));
        } catch (IOException ignored) {
            return "No se pudo leer el registro técnico.";
        }
    }

    @FunctionalInterface
    interface ProcessFactory {
        Process start(ProcessBuilder builder) throws IOException;
    }
}
