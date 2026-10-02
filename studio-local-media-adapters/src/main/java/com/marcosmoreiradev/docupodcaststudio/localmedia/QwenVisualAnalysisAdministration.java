package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Guided preparation for the private Ollama runtime and Qwen3-VL model. */
final class QwenVisualAnalysisAdministration extends AbstractLocalEngineAdministration
        implements ManagedDownloadAdministration {
    static final String RUNTIME_VERSION = "0.32.5";
    static final String RUNTIME_URL =
            "https://github.com/ollama/ollama/releases/download/v0.32.5/ollama-windows-amd64.zip";
    static final long RUNTIME_BYTES = 1_457_824_795L;
    static final String RUNTIME_SHA256 =
            "7c941ae084569d298062d29f8139163a3187c76dbca0479c70d085e78fd8c7bb";
    static final long Q8_BYTES = 5_100_000_000L;
    static final long Q8_8B_BYTES = 9_800_000_000L;
    static final String Q8_MANIFEST_DIGEST_PREFIX = "3c4e71051a81";
    static final String Q8_8B_MANIFEST_DIGEST_PREFIX = "eff3eb825b32";
    private static final String RUNTIME_ARCHIVE =
            "tools/document-ai/downloads/ollama-windows-amd64-" + RUNTIME_VERSION + ".zip";
    private static final String RUNTIME_DIRECTORY = "tools/document-ai/ollama";
    private final ManagedOllamaProcess process;
    private final QwenVisualAnalysisEngine visualEngine;
    private final DiskSpaceProbe diskSpace;
    private final EngineCertificationStore certifications;
    private final ThreadLocal<ManagedDownloadDecision> decision =
            ThreadLocal.withInitial(() -> ManagedDownloadDecision.USE_EXISTING);

    QwenVisualAnalysisAdministration(QwenVisualAnalysisEngine engine,
                                      RuntimeAssetCatalog assets,
                                      ManagedOllamaProcess process) {
        this(engine, assets, process, DiskSpaceProbe.system());
    }

    QwenVisualAnalysisAdministration(QwenVisualAnalysisEngine engine,
                                      RuntimeAssetCatalog assets,
                                      ManagedOllamaProcess process,
                                      DiskSpaceProbe diskSpace) {
        this(engine, assets, process, diskSpace, EngineCertificationStore.none());
    }

    QwenVisualAnalysisAdministration(QwenVisualAnalysisEngine engine,
                                      RuntimeAssetCatalog assets,
                                      ManagedOllamaProcess process,
                                      DiskSpaceProbe diskSpace,
                                      EngineCertificationStore certifications) {
        super(engine, assets, List.of(
                new EngineActionDescriptor(EngineActionId.INSTALL,
                        "Preparar descripción visual local",
                        "Prepara el runtime privado y el perfil Qwen PDF configurado. No instala servicios ni modifica PATH.",
                        List.of(),
                        true, RUNTIME_BYTES + modelBytes(engine.selectedModel()),
                        Map.of("license", "Apache-2.0", "source",
                                "https://github.com/ollama/ollama; https://ollama.com/library/qwen3-vl/tags",
                                "managedDownload", "true")),
                action(EngineActionId.IMPORT, "Importar runtime visual",
                        "Importa el ZIP standalone oficial ya descargado.", true,
                        file("runtimeZip", "ZIP standalone de Ollama", true)),
                action(EngineActionId.START, "Iniciar análisis visual",
                        "Inicia el proceso privado en loopback.", false),
                action(EngineActionId.STOP, "Detener análisis visual",
                        "Detiene únicamente el árbol de procesos iniciado por DocuPodcast.", false),
                action(EngineActionId.SMOKE_TEST, "Probar descripción",
                        "Ejecuta una prueba visual real; una respuesta solo textual no se acepta.", false,
                        file("testImage", "Imagen de prueba", true))));
        this.process = process;
        this.visualEngine = engine;
        this.diskSpace = java.util.Objects.requireNonNull(diskSpace);
        this.certifications = java.util.Objects.requireNonNullElse(
                certifications, EngineCertificationStore.none());
    }

    @Override
    public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        String model = visualEngine.selectedModel();
        Path executable = assets.require(QwenVisualAnalysisEngine.ID, "executable");
        Path archive = runtime.target(RUNTIME_ARCHIVE);
        OllamaModelStoreInspector.Inspection inspection =
                OllamaModelStoreInspector.inspect(process.modelsRoot(), model);
        String expectedDigest = expectedManifestDigestPrefix(model);
        boolean digestValid = expectedDigest.isBlank()
                || inspection.manifestSha256().startsWith(expectedDigest);
        boolean runtimeValid = Files.isRegularFile(executable);
        boolean partial = Files.exists(runtime.staging(engineId().value()));
        ManagedDownloadState state = partial ? ManagedDownloadState.PARTIAL
                : runtimeValid && inspection.valid() && digestValid ? ManagedDownloadState.VALID
                : (Files.exists(executable) || Files.exists(archive) || inspection.bytes() > 0)
                ? ManagedDownloadState.INVALID : ManagedDownloadState.MISSING;
        long expected = RUNTIME_BYTES + modelBytes(model);
        long actual = (Files.isRegularFile(archive) ? Files.size(archive) : 0L) + inspection.bytes();
        return new ManagedDownloadPreflight(
                "Descripción visual local · " + model, state, executable.getParent(),
                expected, actual, RUNTIME_SHA256, inspection.manifestSha256(),
                "Apache-2.0", RUNTIME_URL + " | https://ollama.com/library/qwen3-vl/tags",
                diagnosis(runtimeValid, inspection, digestValid));
    }

    @Override
    public EngineActionResult executeDownload(EngineActionRequest request,
                                              ManagedDownloadDecision selected,
                                              ExecutionContext context)
            throws IOException, InterruptedException {
        decision.set(selected == null ? ManagedDownloadDecision.CANCEL : selected);
        try {
            return execute(request, context);
        } finally {
            decision.remove();
        }
    }

    @Override
    protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.INSTALL.equals(request.actionId())) {
            ensureSpace();
            prepareRuntime(context);
            String model = visualEngine.selectedModel();
            process.pull(model,
                    decision.get() == ManagedDownloadDecision.REDOWNLOAD, context);
            process.rememberPreferredModel(model);
        } else if (EngineActionId.IMPORT.equals(request.actionId())) {
            Path archive = inputPath(request, "runtimeZip");
            verifyRuntimeArchive(archive);
            runtime.installZipDirectoryPreservingRootVerified(
                    archive, RUNTIME_DIRECTORY, engineId().value(), context,
                    QwenVisualAnalysisAdministration::verifyRuntimeDirectory);
        } else if (EngineActionId.START.equals(request.actionId())) {
            process.ensureReady(context);
        } else if (EngineActionId.STOP.equals(request.actionId())) {
            process.stop();
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path image = inputPath(request, "testImage");
            QwenVisualAnalysisEngine visual = (QwenVisualAnalysisEngine) engine;
            ContentAnalysisResult result = visual.analyze(new ContentAnalysisRequest(
                    ContentAnalysisOperation.IMAGE_DESCRIPTION,
                    List.of(new AnalysisVisualInput(image, "smoke-real-image", "")),
                    "Describe brevemente los elementos visibles de esta imagen.",
                    "", "es", "", Map.of(
                    "model", visual.selectedModel(),
                    "maxOutputTokens", "100",
                    "certificationSmoke", "true")), context);
            if (result.text().isBlank() || result.confidence() <= 0.0) {
                throw new IOException("La prueba visual no produjo una descripción estructurada válida.");
            }
            String model = visual.selectedModel();
            Map<String, String> diagnostics = new java.util.LinkedHashMap<>(result.diagnostics());
            diagnostics.put("input", image.toAbsolutePath().normalize().toString());
            certifications.save(new EngineCertificationRecord(
                    QwenVisualAnalysisEngine.ID, RUNTIME_VERSION, model,
                    EngineHardwareFingerprint.current(context.computePreference()),
                    "qwen-real-visual-smoke", java.time.Instant.now(), true,
                    diagnostics));
        }
        return List.of();
    }

    private void ensureSpace() throws IOException {
        long payload = RUNTIME_BYTES + modelBytes(visualEngine.selectedModel());
        long required = Math.addExact(payload, Math.max(512L * 1024L * 1024L, payload * 15L / 100L));
        long available = diskSpace.usableBytes(assets.root());
        if (available < required) {
            throw new EngineExecutionException(EngineDiagnosticCode.NO_SPACE,
                    "No hay espacio suficiente para preparar la descripción visual local.",
                    Map.of("requiredBytes", Long.toString(required),
                            "availableBytes", Long.toString(available),
                            "margin", "15%"));
        }
    }

    private void prepareRuntime(ExecutionContext context) throws IOException, InterruptedException {
        Path executable = assets.require(QwenVisualAnalysisEngine.ID, "executable");
        if (Files.isRegularFile(executable) && decision.get() == ManagedDownloadDecision.USE_EXISTING) return;
        runtime.downloadVerified(RUNTIME_URL, RUNTIME_ARCHIVE, engineId().value(), context,
                decision.get(), QwenVisualAnalysisAdministration::verifyRuntimeArchive);
        runtime.installZipDirectoryPreservingRootVerified(
                runtime.target(RUNTIME_ARCHIVE), RUNTIME_DIRECTORY, engineId().value(), context,
                QwenVisualAnalysisAdministration::verifyRuntimeDirectory);
    }

    private static String diagnosis(boolean runtimeValid,
                                    OllamaModelStoreInspector.Inspection model,
                                    boolean digestValid) {
        if (!runtimeValid) return "Falta el runtime privado.";
        if (!model.valid()) return "El modelo no está completo: " + String.join(", ", model.issues());
        if (!digestValid) return "El manifest del modelo no coincide con la revisión fijada.";
        return "Runtime y modelo validados localmente.";
    }

    private static void verifyRuntimeArchive(Path archive) throws IOException {
        if (!Files.isRegularFile(archive) || Files.size(archive) != RUNTIME_BYTES
                || !RUNTIME_SHA256.equalsIgnoreCase(ManagedDownloadPreflightInspector.sha256(archive))) {
            throw new IOException("El ZIP standalone no coincide con la versión oficial fijada.");
        }
    }

    private static void verifyRuntimeDirectory(Path directory) throws IOException {
        if (!Files.isRegularFile(directory.resolve("ollama.exe"))) {
            throw new IOException("El ZIP no contiene ollama.exe en la raíz esperada.");
        }
    }

    static String expectedManifestDigestPrefix(String model) {
        return switch (java.util.Objects.toString(model, "")) {
            case PdfVlmRuntimeProfile.MODEL_4B_Q8 -> Q8_MANIFEST_DIGEST_PREFIX;
            case PdfVlmRuntimeProfile.MODEL_8B_Q8 -> Q8_8B_MANIFEST_DIGEST_PREFIX;
            default -> "";
        };
    }

    private static long modelBytes(String model) {
        return PdfVlmRuntimeProfile.MODEL_8B_Q8.equals(model)
                ? Q8_8B_BYTES : Q8_BYTES;
    }

    private static long totalPhysicalBytes() {
        var bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean extended) {
            return Math.max(0L, extended.getTotalMemorySize());
        }
        return Runtime.getRuntime().maxMemory();
    }
}
