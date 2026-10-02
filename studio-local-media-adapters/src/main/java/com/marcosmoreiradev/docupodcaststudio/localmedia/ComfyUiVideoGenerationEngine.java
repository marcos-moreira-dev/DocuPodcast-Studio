package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Independent ComfyUI adapter for native video generation. */
public final class ComfyUiVideoGenerationEngine implements VideoGenerationEngine {
    public static final EngineId ID = new EngineId("comfyui-video");
    public static final EnginePresetId WAN_BALANCED = new EnginePresetId("wan22-ti2v-5b-balanced");
    public static final EnginePresetId WAN_QUALITY = new EnginePresetId("wan22-i2v-14b-quality");
    public static final EnginePresetId LTX_PORTRAIT = new EnginePresetId("ltx23-i2v-portrait");
    private final EngineConfiguration configuration;
    private final ComfyUiTransport transport;

    public ComfyUiVideoGenerationEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.transport = new ComfyUiTransport(this.configuration.value("baseUrl"));
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.VIDEO_GENERATION, "Video generativo local", "local", "http",
                Set.of(EngineFeature.TEXT_TO_VIDEO, EngineFeature.IMAGE_TO_VIDEO, EngineFeature.HARDWARE_ACCELERATION), false);
    }

    @Override public List<EnginePresetDescriptor> presets() {
        return List.of(
                preset(WAN_BALANCED, "WAN 2.2 equilibrado", "Texto o imagen a video con un coste moderado.", "wanBalancedWorkflow", true),
                preset(WAN_QUALITY, "WAN 2.2 calidad", "Imagen a video con mayor demanda de memoria.", "wanQualityWorkflow", false),
                preset(LTX_PORTRAIT, "LTX 2.3 retrato", "Imagen a video optimizada para formato vertical.", "ltxPortraitWorkflow", false));
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Dirección local", "Endpoint HTTP del runtime local.",
                        ConfigurationFieldType.URL, true, "http://127.0.0.1:8188")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        boolean hasWorkflow = presets().stream().map(p -> workflowFor(p.id())).anyMatch(Files::isRegularFile);
        if (!hasWorkflow) {
            return EngineReadiness.unavailable(ID, "No hay workflows de video generativo instalados.",
                    "Instala o importa un workflow WAN/LTX compatible.");
        }
        try {
            return transport.isReady()
                    ? EngineReadiness.ready(ID, "Motor local de video generativo disponible.")
                    : EngineReadiness.unavailable(ID, "El motor rechazó la prueba HTTP.", "Inicia el runtime local.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "El motor local de video no responde.", "Inicia el runtime y revisa su dirección.");
        }
    }

    @Override public VideoGenerationResult generate(VideoGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        EnginePresetId preset = EnginePresetId.AUTO.equals(request.presetId()) ? WAN_BALANCED : request.presetId();
        Path workflowFile = workflowFor(preset);
        if (!Files.isRegularFile(workflowFile)) {
            throw new IOException("El preset " + preset.value() + " no tiene un workflow instalado: " + workflowFile);
        }
        int frames = Math.max(1, (int) Math.round(request.framesPerSecond() * request.durationSeconds()));
        String initialImage = request.initialImage() == null ? "" : request.initialImage().getFileName().toString();
        String workflow = ComfyWorkflowTemplate.video(workflowFile, request.prompt(), request.negativePrompt(),
                request.width(), request.height(), request.framesPerSecond(), frames, request.seed(),
                request.filenamePrefix(), initialImage);
        List<ComfyUiTransport.DownloadedArtifact> artifacts = transport.executeAll(workflow, context, "video generativo");
        ComfyUiTransport.DownloadedArtifact clip = artifacts.stream().filter(item -> isVideo(item.filename()))
                .findFirst().orElseThrow(() -> new IOException("El workflow no produjo un clip de video."));
        ComfyUiTransport.DownloadedArtifact frame = artifacts.stream().filter(item -> isImage(item.filename()))
                .findFirst().orElseThrow(() -> new IOException("El workflow no produjo el frame de continuidad."));
        Files.createDirectories(request.outputDirectory());
        Path target = request.outputDirectory().resolve(request.filenamePrefix() + extension(clip.filename(), ".mp4"));
        Path continuation = request.outputDirectory().resolve(request.filenamePrefix() + "-continuity"
                + extension(frame.filename(), ".png"));
        Files.write(target, clip.bytes());
        Files.write(continuation, frame.bytes());
        return new VideoGenerationResult(target, continuation, request.durationSeconds(), Map.of(
                "engineId", ID.value(), "presetId", preset.value(), "promptId", clip.promptId()));
    }

    private EnginePresetDescriptor preset(EnginePresetId id, String name, String description, String key, boolean primary) {
        Path workflow = configuredPath(key);
        return new EnginePresetDescriptor(id, name, description,
                Set.of(EngineFeature.IMAGE_TO_VIDEO), Map.of(
                "workflowResource", workflow.toString(),
                "resourceState", Files.isRegularFile(workflow) ? "available" : "missing"), primary);
    }

    private Path workflowFor(EnginePresetId id) {
        if (WAN_BALANCED.equals(id)) return configuredPath("wanBalancedWorkflow");
        if (WAN_QUALITY.equals(id)) return configuredPath("wanQualityWorkflow");
        if (LTX_PORTRAIT.equals(id)) return configuredPath("ltxPortraitWorkflow");
        return Path.of("");
    }

    private Path configuredPath(String key) {
        String value = configuration.value(key);
        return value.isBlank() ? Path.of("") : Path.of(value).toAbsolutePath().normalize();
    }

    private static String extension(String filename, String fallback) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? fallback : filename.substring(dot);
    }

    private static boolean isVideo(String filename) {
        String value = filename == null ? "" : filename.toLowerCase(java.util.Locale.ROOT);
        return value.endsWith(".mp4") || value.endsWith(".webm") || value.endsWith(".mov") || value.endsWith(".mkv");
    }

    private static boolean isImage(String filename) {
        String value = filename == null ? "" : filename.toLowerCase(java.util.Locale.ROOT);
        return value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".webp");
    }
}
