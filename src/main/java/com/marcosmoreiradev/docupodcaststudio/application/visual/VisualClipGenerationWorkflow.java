package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Runs one installed, category-neutral image-to-video ComfyUI workflow. */
public final class VisualClipGenerationWorkflow {
    private final ComfyUiVisualEngineClient client;
    private final VisualClipWorkflowPackageService packages;

    public VisualClipGenerationWorkflow(ComfyUiVisualEngineClient client) {
        this(client, new VisualClipWorkflowPackageService());
    }

    VisualClipGenerationWorkflow(ComfyUiVisualEngineClient client,
                                 VisualClipWorkflowPackageService packages) {
        this.client = Objects.requireNonNull(client, "client");
        this.packages = Objects.requireNonNull(packages, "packages");
    }

    public VisualClipGenerationResult generate(String baseUrl,
                                               Duration timeout,
                                               VisualClipGenerationRequest request,
                                               String customWorkflowPath,
                                               Consumer<String> progress) throws IOException {
        Objects.requireNonNull(request, "request");
        if (!Files.isRegularFile(request.startFrame())) {
            throw new IOException("La imagen inicial del clip no existe: " + request.startFrame());
        }
        VisualClipWorkflowPackageService.WorkflowPackage installed =
                packages.require(request.profile(), customWorkflowPath);
        Instant started = Instant.now();
        progress(progress, "Subiendo imagen inicial al motor local.");
        String uploaded = client.uploadImage(baseUrl, timeout, request.startFrame(), 1);
        String prefix = request.outputTarget().filenamePrefix();
        String workflow = fill(installed.template(), request, uploaded, prefix);
        progress(progress, "Enviando workflow " + installed.profile().displayName() + ".");
        String promptId = client.queueRawPrompt(baseUrl, timeout, workflow);
        progress(progress, "Generando clip local.");
        List<ComfyUiVisualEngineClient.OutputArtifact> artifacts = client.waitForArtifacts(
                baseUrl, timeout, promptId, timeout);
        ComfyUiVisualEngineClient.OutputArtifact video = artifacts.stream()
                .filter(ComfyUiVisualEngineClient.OutputArtifact::video)
                .findFirst()
                .orElseThrow(() -> new IOException(
                        "El workflow termino sin MP4. Revisa que el paquete guarde una salida de video."));
        ComfyUiVisualEngineClient.OutputArtifact lastFrame = artifacts.stream()
                .filter(ComfyUiVisualEngineClient.OutputArtifact::image)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IOException(
                        "El workflow termino sin ultimo frame PNG. El encadenamiento requiere esa salida."));
        Path directory = request.outputTarget().outputDirectory();
        Files.createDirectories(directory);
        Path clipPath = directory.resolve(prefix + ".mp4");
        Path lastFramePath = directory.resolve(prefix + "-last.png");
        progress(progress, "Descargando clip y frame de continuidad.");
        client.downloadArtifact(baseUrl, timeout, video, clipPath);
        client.downloadArtifact(baseUrl, timeout, lastFrame, lastFramePath);
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(request.consumerMetadata());
        metadata.put("workflowPath", installed.workflowPath().toString());
        metadata.put("sourceVideoFilename", video.filename());
        metadata.put("sourceLastFrameFilename", lastFrame.filename());
        return new VisualClipGenerationResult(
                clipPath,
                lastFramePath,
                request.width(),
                request.height(),
                request.framesPerSecond(),
                request.durationSeconds(),
                installed.modelId(),
                request.workflowStrategy().name(),
                promptId,
                request.computeBinding(),
                request.seed(),
                Duration.between(started, Instant.now()),
                Map.copyOf(metadata));
    }

    static String fill(String template,
                       VisualClipGenerationRequest request,
                       String uploadedImage,
                       String filenamePrefix) {
        return template
                .replace("{{START_IMAGE}}", json(uploadedImage))
                .replace("{{PROMPT}}", json(request.prompt()))
                .replace("{{NEGATIVE_PROMPT}}", json(request.negativePrompt()))
                .replace("{{WIDTH}}", Integer.toString(request.width()))
                .replace("{{HEIGHT}}", Integer.toString(request.height()))
                .replace("{{FPS}}", Integer.toString(request.framesPerSecond()))
                .replace("{{FRAME_COUNT}}", Integer.toString(request.frameCount()))
                .replace("{{SEED}}", Long.toString(request.seed()))
                .replace("{{FILENAME_PREFIX}}", json(filenamePrefix));
    }

    private static String json(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private static void progress(Consumer<String> consumer, String message) {
        if (consumer != null) {
            consumer.accept(message);
        }
    }
}
