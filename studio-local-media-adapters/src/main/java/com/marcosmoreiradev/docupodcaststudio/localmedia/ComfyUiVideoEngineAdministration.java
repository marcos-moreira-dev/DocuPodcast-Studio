package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Independent video-generation administration backed by the shared ComfyUI process. */
final class ComfyUiVideoEngineAdministration extends AbstractLocalEngineAdministration {
    private static final EngineActionId IMPORT_WORKFLOWS = new EngineActionId("import-workflows");
    private final VideoGenerationEngine video;
    private final ComfyUiManagedProcess process;
    private final Function<String, ComfyUiLaunchProfile> launchProfiles;

    ComfyUiVideoEngineAdministration(VideoGenerationEngine video, RuntimeAssetCatalog assets,
                                     ComfyUiManagedProcess process,
                                     Function<String, ComfyUiLaunchProfile> launchProfiles) {
        super(video, assets, List.of(
                action(EngineActionId.INSTALL, "Instalar o importar ComfyUI",
                        "Copia un runtime local al destino fijo tools/image.", true,
                        directory("runtimeDirectory", "Runtime ComfyUI", true)),
                action(IMPORT_WORKFLOWS, "Importar workflows de video",
                        "Instala workflows WAN/LTX en los destinos publicados por el adaptador.", true,
                        file("balancedWorkflow", "Workflow equilibrado", true),
                        file("qualityWorkflow", "Workflow de calidad (opcional)", false),
                        file("portraitWorkflow", "Workflow vertical (opcional)", false)),
                action(EngineActionId.START, "Iniciar ComfyUI", "Inicia el runtime fijo y compartido.", false),
                action(EngineActionId.STOP, "Detener ComfyUI", "Detiene el proceso administrado compartido.", true),
                action(EngineActionId.REPAIR, "Reparar video generativo",
                        "Limpia exclusivamente staging incompleto de este adaptador.", true),
                action(EngineActionId.SMOKE_TEST, "Generar clip de prueba",
                        "Produce un clip real y su frame de continuidad.", false)));
        this.video = video;
        this.process = process;
        this.launchProfiles = launchProfiles == null
                ? requested -> ComfyUiLaunchProfile.automatic(ComfyUiMemoryProfile.from(requested))
                : launchProfiles;
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (EngineActionId.INSTALL.equals(request.actionId())) {
            runtime.importDirectory(inputPath(request, "runtimeDirectory"), "tools/image", context);
        } else if (IMPORT_WORKFLOWS.equals(request.actionId())) {
            runtime.importFile(inputPath(request, "balancedWorkflow"),
                    "models/video/workflows/workflow-wan22-ti2v-5b-api.json", context);
            importOptional(request, "qualityWorkflow",
                    "models/video/workflows/workflow-wan22-i2v-14b-api.json", context);
            importOptional(request, "portraitWorkflow",
                    "models/video/workflows/workflow-ltx23-i2v-portrait-api.json", context);
        } else if (EngineActionId.START.equals(request.actionId())) {
            process.start(assets, launchProfiles.apply(request.inputs().get("memoryProfile")), context);
        } else if (EngineActionId.STOP.equals(request.actionId())) {
            process.stop(context);
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/comfyui-video");
            Files.createDirectories(output);
            VideoGenerationResult result = video.generate(new VideoGenerationRequest(
                    "Movimiento de camara suave sobre una ilustracion educativa", "texto, marca de agua",
                    null, List.of(), 512, 288, 8, 1.0, 42,
                    ComfyUiVideoGenerationEngine.WAN_BALANCED, output, "video-smoke", Map.of()), context);
            ArrayList<GenerationArtifact> artifacts = new ArrayList<>();
            artifacts.add(new GenerationArtifact("video", result.videoFile().toUri(), Map.of("purpose", "smoke")));
            artifacts.add(new GenerationArtifact("continuity-frame", result.continuationFrame().toUri(),
                    Map.of("purpose", "smoke")));
            return artifacts;
        }
        return List.of();
    }

    private void importOptional(EngineActionRequest request, String key, String target, ExecutionContext context)
            throws IOException, InterruptedException {
        String value = request.inputs().getOrDefault(key, "").strip();
        if (!value.isBlank()) runtime.importFile(Path.of(value), target, context);
    }
}
