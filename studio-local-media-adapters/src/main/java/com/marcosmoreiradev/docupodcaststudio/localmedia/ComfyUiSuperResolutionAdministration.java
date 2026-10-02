package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.GenerationArtifact;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageSuperResolutionEngine;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadAdministration;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadDecision;
import com.marcosmoreiradev.docupodcaststudio.media.api.ManagedDownloadPreflight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Explicit, recoverable installation of the official RealESRGAN_x4plus weight. */
final class ComfyUiSuperResolutionAdministration extends AbstractLocalEngineAdministration
        implements ManagedDownloadAdministration {
    static final String MODEL_FILE = "RealESRGAN_x4plus.pth";
    static final String OFFICIAL_URL =
            "https://github.com/xinntao/Real-ESRGAN/releases/download/v0.1.0/RealESRGAN_x4plus.pth";
    static final long MODEL_BYTES = 67_040_989L;
    static final String MODEL_SHA256 =
            "4fa0d38905f75ac06eb49a7951b426670021be3018265fd191d2125df9d682f1";
    private static final String RELATIVE_MODEL =
            "tools/image/ComfyUI/models/upscale_models/" + MODEL_FILE;

    private final ThreadLocal<ManagedDownloadDecision> downloadDecision =
            ThreadLocal.withInitial(() -> ManagedDownloadDecision.USE_EXISTING);
    private final ComfyUiManagedProcess managedProcess;

    ComfyUiSuperResolutionAdministration(
            ImageSuperResolutionEngine engine,
            RuntimeAssetCatalog assets,
            ComfyUiManagedProcess managedProcess) {
        super(engine, assets, List.of(
                new EngineActionDescriptor(
                        EngineActionId.INSTALL,
                        "Instalar Real-ESRGAN x4plus",
                        "Descarga explícitamente el modelo oficial de máxima calidad (BSD-3-Clause).",
                        List.of(),
                        true,
                        MODEL_BYTES,
                        Map.of("license", "BSD-3-Clause", "sha256", MODEL_SHA256,
                                "source", OFFICIAL_URL, "managedDownload", "true")),
                action(
                        EngineActionId.IMPORT,
                        "Importar modelo Real-ESRGAN",
                        "Valida y copia un peso oficial ya descargado.",
                        true,
                        file("modelFile", "RealESRGAN_x4plus.pth", true)),
                new EngineActionDescriptor(
                        EngineActionId.REPAIR,
                        "Reintentar instalación",
                        "Limpia exclusivamente el staging incompleto y repite la descarga oficial.",
                        List.of(),
                        true,
                        MODEL_BYTES,
                        Map.of("license", "BSD-3-Clause", "sha256", MODEL_SHA256,
                                "source", OFFICIAL_URL, "managedDownload", "true")),
                action(
                        EngineActionId.SMOKE_TEST,
                        "Comprobar modelo",
                        "Valida el peso y comprueba que el motor local lo enumere.",
                        false)));
        this.managedProcess = managedProcess;
    }

    @Override
    public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        if (request == null || !engineId().equals(request.engineId())) {
            throw new IllegalArgumentException("La acción no pertenece a este motor.");
        }
        Path model = runtime.target(RELATIVE_MODEL);
        Path partial = runtime.staging(engineId().value()).resolve(MODEL_FILE + ".partial");
        return ManagedDownloadPreflightInspector.inspect(
                MODEL_FILE, model, partial, MODEL_BYTES, MODEL_SHA256,
                "BSD-3-Clause", OFFICIAL_URL);
    }

    @Override
    public EngineActionResult executeDownload(
            EngineActionRequest request,
            ManagedDownloadDecision decision,
            ExecutionContext context) throws IOException, InterruptedException {
        downloadDecision.set(decision == null ? ManagedDownloadDecision.CANCEL : decision);
        try {
            return execute(request, context);
        } finally {
            downloadDecision.remove();
        }
    }

    @Override
    protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.INSTALL.equals(request.actionId())) {
            runtime.downloadVerified(
                    OFFICIAL_URL,
                    RELATIVE_MODEL,
                    engineId().value(),
                    context,
                    downloadDecision.get(),
                    ComfyUiSuperResolutionAdministration::verify);
            if (downloadDecision.get() == ManagedDownloadDecision.REDOWNLOAD) {
                reloadManagedCatalog(context);
            }
        } else if (EngineActionId.IMPORT.equals(request.actionId())) {
            runtime.importFileVerified(
                    inputPath(request, "modelFile"),
                    RELATIVE_MODEL,
                    context,
                    ComfyUiSuperResolutionAdministration::verify);
            reloadManagedCatalog(context);
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
            runtime.downloadVerified(
                    OFFICIAL_URL,
                    RELATIVE_MODEL,
                    engineId().value(),
                    context,
                    downloadDecision.get(),
                    ComfyUiSuperResolutionAdministration::verify);
            reloadManagedCatalog(context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            verify(runtime.target(RELATIVE_MODEL));
        }
        Path model = runtime.target(RELATIVE_MODEL);
        return Files.isRegularFile(model)
                ? List.of(new GenerationArtifact(
                        "model",
                        model.toUri(),
                        Map.of("model", MODEL_FILE, "sha256", MODEL_SHA256, "license", "BSD-3-Clause")))
                : List.of();
    }

    private void reloadManagedCatalog(ExecutionContext context) throws InterruptedException {
        if (managedProcess != null) managedProcess.reloadAfterManagedCatalogChange(context);
    }

    static void verify(Path model) throws IOException {
        if (!isOfficial(model)) {
            throw new IOException("El modelo no coincide con el peso oficial esperado (tamaño/SHA-256).");
        }
    }

    static boolean isOfficial(Path model) throws IOException {
        return Files.isRegularFile(model)
                && Files.size(model) == MODEL_BYTES
                && MODEL_SHA256.equalsIgnoreCase(ManagedDownloadPreflightInspector.sha256(model));
    }

    static String sha256(Path path) throws IOException {
        return ManagedDownloadPreflightInspector.sha256(path);
    }
}
