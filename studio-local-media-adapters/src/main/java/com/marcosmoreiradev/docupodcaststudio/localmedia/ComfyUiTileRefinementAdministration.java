package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Managed installation and verification for the official SD 1.5 ControlNet Tile weight. */
final class ComfyUiTileRefinementAdministration extends AbstractLocalEngineAdministration
        implements ManagedDownloadAdministration {
    static final String MODEL_FILE = ComfyUiTileRefinementEngine.DEFAULT_CONTROL_NET;
    static final String OFFICIAL_URL =
            "https://huggingface.co/lllyasviel/ControlNet-v1-1/resolve/main/" + MODEL_FILE;
    static final long MODEL_BYTES = 1_445_235_023L;
    static final String MODEL_SHA256 =
            "8aa69b8d391e72c2fced6a650268137dc0ed594cafe8a2c0f8b994799a21979b";
    private static final String LICENSE = "CreativeML OpenRAIL-M";
    private static final String RELATIVE_MODEL =
            "tools/image/ComfyUI/models/controlnet/" + MODEL_FILE;

    private final ThreadLocal<ManagedDownloadDecision> downloadDecision =
            ThreadLocal.withInitial(() -> ManagedDownloadDecision.CANCEL);
    private final ComfyUiManagedProcess managedProcess;

    ComfyUiTileRefinementAdministration(
            ImageRefinementEngine engine,
            RuntimeAssetCatalog assets,
            ComfyUiManagedProcess managedProcess) {
        super(engine, assets, List.of(
                new EngineActionDescriptor(EngineActionId.INSTALL,
                        "Instalar ControlNet Tile",
                        "Descarga explícitamente el modelo oficial para mejora conservadora.",
                        List.of(), true, MODEL_BYTES,
                        Map.of("license", LICENSE, "sha256", MODEL_SHA256,
                                "source", OFFICIAL_URL, "managedDownload", "true")),
                action(EngineActionId.IMPORT,
                        "Importar modelo ControlNet Tile",
                        "Valida y copia un peso oficial ya descargado.", true,
                        file("modelFile", MODEL_FILE, true)),
                new EngineActionDescriptor(EngineActionId.REPAIR,
                        "Reintentar instalación de ControlNet Tile",
                        "Limpia el staging incompleto y repite la descarga oficial.",
                        List.of(), true, MODEL_BYTES,
                        Map.of("license", LICENSE, "sha256", MODEL_SHA256,
                                "source", OFFICIAL_URL, "managedDownload", "true")),
                action(EngineActionId.SMOKE_TEST,
                        "Comprobar ControlNet Tile",
                        "Valida el peso y comprueba la disponibilidad del motor.", false)));
        this.managedProcess = managedProcess;
    }

    @Override public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        Path model = runtime.target(RELATIVE_MODEL);
        Path partial = runtime.staging(engineId().value()).resolve(MODEL_FILE + ".partial");
        return ManagedDownloadPreflightInspector.inspect(
                MODEL_FILE, model, partial, MODEL_BYTES, MODEL_SHA256, LICENSE, OFFICIAL_URL);
    }

    @Override public EngineActionResult executeDownload(
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

    @Override protected List<GenerationArtifact> perform(
            EngineActionRequest request, ExecutionContext context) throws IOException, InterruptedException {
        if (EngineActionId.INSTALL.equals(request.actionId())) {
            runtime.downloadVerified(OFFICIAL_URL, RELATIVE_MODEL, engineId().value(), context,
                    downloadDecision.get(), ComfyUiTileRefinementAdministration::verify);
            reload(context);
        } else if (EngineActionId.IMPORT.equals(request.actionId())) {
            runtime.importFileVerified(inputPath(request, "modelFile"), RELATIVE_MODEL, context,
                    ComfyUiTileRefinementAdministration::verify);
            reload(context);
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
            runtime.downloadVerified(OFFICIAL_URL, RELATIVE_MODEL, engineId().value(), context,
                    downloadDecision.get(), ComfyUiTileRefinementAdministration::verify);
            reload(context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            verify(runtime.target(RELATIVE_MODEL));
        }
        Path model = runtime.target(RELATIVE_MODEL);
        return Files.isRegularFile(model)
                ? List.of(new GenerationArtifact("model", model.toUri(),
                Map.of("model", MODEL_FILE, "sha256", MODEL_SHA256, "license", LICENSE)))
                : List.of();
    }

    private void reload(ExecutionContext context) throws InterruptedException {
        if (managedProcess != null) managedProcess.reloadAfterManagedCatalogChange(context);
    }

    static boolean isOfficial(Path model) throws IOException {
        return Files.isRegularFile(model) && Files.size(model) == MODEL_BYTES
                && MODEL_SHA256.equalsIgnoreCase(ManagedDownloadPreflightInspector.sha256(model));
    }

    static void verify(Path model) throws IOException {
        if (!isOfficial(model)) {
            throw new IOException("ControlNet Tile no coincide con el tamaño/SHA-256 oficial.");
        }
    }
}
