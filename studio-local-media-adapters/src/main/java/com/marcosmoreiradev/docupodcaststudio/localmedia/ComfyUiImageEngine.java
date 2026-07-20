package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ComfyUiImageEngine implements ImageGenerationEngine {
    public static final EngineId ID = new EngineId("comfyui");
    public static final EnginePresetId DRAFT = new EnginePresetId("draft");
    private final EngineConfiguration configuration;
    private final ComfyUiTransport transport;

    public ComfyUiImageEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.transport = new ComfyUiTransport(this.configuration.value("baseUrl"));
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.IMAGE_GENERATION, "Imagen local", "local", "http",
                Set.of(EngineFeature.CONDITIONING_IMAGE), false);
    }

    @Override public List<EnginePresetDescriptor> presets() {
        return List.of(new EnginePresetDescriptor(DRAFT, "Borrador local",
                "Generación SD 1.5 equilibrada para previsualización.", Set.of(),
                Map.of("workflowResource", configuration.value("draftWorkflow")), true));
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Dirección local", "Endpoint HTTP del runtime local.",
                        ConfigurationFieldType.URL, true, "http://127.0.0.1:8188")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        Path workflow = workflowFor(DRAFT);
        if (!Files.isRegularFile(workflow)) {
            return EngineReadiness.unavailable(ID, "Falta el workflow de imagen.", "Restaura los recursos del motor local.");
        }
        try {
            return transport.isReady()
                    ? EngineReadiness.ready(ID, "Motor local de imagen disponible.")
                    : EngineReadiness.unavailable(ID, "El motor de imagen rechazó la prueba HTTP.", "Inicia el runtime local.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "El motor local de imagen no responde.", "Inicia el runtime y revisa su dirección.");
        }
    }

    @Override public ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        EnginePresetId preset = EnginePresetId.AUTO.equals(request.presetId()) ? DRAFT : request.presetId();
        Path workflowFile = workflowFor(preset);
        if (!DRAFT.equals(preset)) throw new IOException("Preset de imagen no publicado por el motor: " + preset.value());
        String workflow = ComfyWorkflowTemplate.image(workflowFile, request.prompt(), request.negativePrompt(),
                request.width(), request.height(), request.seed(), request.batchSize(), request.filenamePrefix());
        ComfyUiTransport.DownloadedArtifact artifact = transport.execute(workflow, context, "imagen");
        Files.createDirectories(request.outputDirectory());
        String extension = extension(artifact.filename(), ".png");
        Path target = request.outputDirectory().resolve(request.filenamePrefix() + extension);
        Files.write(target, artifact.bytes());
        return new ImageGenerationResult(List.of(target), Map.of(
                "engineId", ID.value(), "presetId", preset.value(), "promptId", artifact.promptId()));
    }

    private Path workflowFor(EnginePresetId preset) {
        if (!DRAFT.equals(preset)) return Path.of("");
        String configured = configuration.value("draftWorkflow");
        return configured.isBlank() ? Path.of("") : Path.of(configured).toAbsolutePath().normalize();
    }

    private static String extension(String filename, String fallback) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? fallback : filename.substring(dot);
    }
}
