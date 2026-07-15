package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementProvider;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementRequest;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementResult;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Initial provider for the local ComfyUI enhancement lane.
 *
 * <p>Technical Java2D adaptation is allowed only for explicit non-AI aspect strategies.
 * The theatrical default 16:9 lane requires a real ComfyUI workflow and never reports
 * post-resize/padding as IA outpainting.</p>
 */
public final class ComfyUiImageEnhancementProvider implements ImageEnhancementProvider {
    @Override public String id() {
        return "comfyui-local";
    }

    @Override public String displayName() {
        return "Motor local de imagen IA";
    }

    @Override public boolean supports(ImageEnhancementRequest request) {
        return request != null && Files.isRegularFile(request.sourceImage());
    }

    @Override public ImageEnhancementResult enhance(ImageEnhancementRequest request) throws IOException {
        if (!supports(request)) {
            return ImageEnhancementResult.failure(request, "No se encontro la imagen origen.");
        }
        BufferedImage source = ImageIO.read(request.sourceImage().toFile());
        if (source == null) {
            return ImageEnhancementResult.failure(request, "La imagen origen no se pudo leer como PNG/JPG.");
        }
        if (request.aspectStrategy() == ImageAspectStrategy.OUTPAINT_TO_TARGET) {
            Optional<Path> workflow = realWorkflow(request);
            if (workflow.isEmpty()) {
                return ImageEnhancementResult.failure(request,
                        "El perfil " + request.outputProfile().displayName()
                                + " requiere un workflow ComfyUI real para 16:9 por IA. "
                                + "Importa un workflow valido; no se usara recorte, padding ni redimensionamiento posterior.");
            }
            return ImageEnhancementResult.failure(request,
                    "Workflow ComfyUI detectado en " + workflow.get()
                            + ", pero el runner image-to-image de mejora 16:9 aun no esta conectado. "
                            + "Usa Generar para producir una imagen 16:9 nativa o instala un proveedor de mejora compatible.");
        }

        Path root = request.projectRoot().resolve("generated/image-enhancement").resolve(safe(request.jobId()));
        Path inputDir = root.resolve("input");
        Path intermediateDir = root.resolve("intermediate");
        Path finalDir = root.resolve("final").resolve(request.outputProfile().workflowId());
        Path metadataDir = root.resolve("metadata");
        Files.createDirectories(inputDir);
        Files.createDirectories(intermediateDir);
        Files.createDirectories(finalDir);
        Files.createDirectories(metadataDir);

        Path inputCopy = inputDir.resolve(sourceFileName(request.sourceImage()));
        Files.copy(request.sourceImage(), inputCopy, StandardCopyOption.REPLACE_EXISTING);

        BufferedImage intermediate = adaptAspect(source, request, true);
        Path intermediatePath = intermediateDir.resolve("aspect-" + request.outputProfile().workflowId() + ".png");
        ImageIO.write(intermediate, "png", intermediatePath.toFile());

        BufferedImage finalImage = adaptAspect(source, request, false);
        Path finalPath = finalDir.resolve("enhanced-" + request.outputProfile().workflowId() + ".png");
        ImageIO.write(finalImage, "png", finalPath.toFile());
        if (!Files.isRegularFile(finalPath) || Files.size(finalPath) <= 0) {
            return ImageEnhancementResult.failure(request, "El proveedor no genero un PNG final valido.");
        }

        Path manifest = metadataDir.resolve("job-manifest.json");
        Files.writeString(manifest, manifestJson(request, inputCopy, intermediatePath, finalPath));
        String message = request.outputProfile().displayName() + " generado con estrategia tecnica no IA. "
                + "Para salida teatral 16:9 usa OUTPAINT_TO_TARGET con workflow ComfyUI real.";
        return ImageEnhancementResult.success(request, inputCopy, intermediatePath, finalPath, manifest, message);
    }

    private static BufferedImage adaptAspect(BufferedImage source, ImageEnhancementRequest request, boolean intermediate) {
        int targetWidth = intermediate ? Math.min(request.outputProfile().width(), 1280) : request.outputProfile().width();
        int targetHeight = intermediate ? Math.min(request.outputProfile().height(), 720) : request.outputProfile().height();
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        configure(g);
        g.setColor(new Color(9, 12, 22));
        g.fillRect(0, 0, targetWidth, targetHeight);

        if (request.aspectStrategy() == ImageAspectStrategy.CENTER_CROP) {
            Rectangle cover = coverRect(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
            g.drawImage(source, cover.x, cover.y, cover.width, cover.height, null);
        } else {
            Rectangle fit = fitRect(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
            g.drawImage(source, fit.x, fit.y, fit.width, fit.height, null);
        }
        g.dispose();
        return canvas;
    }

    private static void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static Rectangle fitRect(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = Math.min(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new Rectangle((targetWidth - width) / 2, (targetHeight - height) / 2, width, height);
    }

    private static Rectangle coverRect(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = Math.max(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new Rectangle((targetWidth - width) / 2, (targetHeight - height) / 2, width, height);
    }

    private static String manifestJson(ImageEnhancementRequest request, Path inputCopy, Path intermediate, Path finalImage) {
        return "{\n"
                + "  \"jobId\": \"" + json(request.jobId()) + "\",\n"
                + "  \"createdAt\": \"" + Instant.now() + "\",\n"
                + "  \"provider\": \"" + json(request.providerId()) + "\",\n"
                + "  \"workflow\": \"" + json(request.workflowId()) + "\",\n"
                + "  \"sourceImage\": \"" + json(request.sourceImage().toString()) + "\",\n"
                + "  \"inputCopy\": \"" + json(inputCopy.toString()) + "\",\n"
                + "  \"intermediateImage\": \"" + json(intermediate.toString()) + "\",\n"
                + "  \"finalImage\": \"" + json(finalImage.toString()) + "\",\n"
                + "  \"outputProfile\": \"" + request.outputProfile().name() + "\",\n"
                + "  \"targetWidth\": " + request.outputProfile().width() + ",\n"
                + "  \"targetHeight\": " + request.outputProfile().height() + ",\n"
                + "  \"aspectStrategy\": \"" + request.aspectStrategy().name() + "\",\n"
                + "  \"pipelineProfile\": \"" + request.pipelineProfile().name() + "\",\n"
                + "  \"tileSize\": " + request.tileSize() + ",\n"
                + "  \"tileOverlap\": " + request.tileOverlap() + ",\n"
                + "  \"steps\": " + request.steps() + ",\n"
                + "  \"denoise\": " + String.format(Locale.ROOT, "%.3f", request.denoise()) + ",\n"
                + "  \"useLora\": " + request.useLora() + ",\n"
                + "  \"loraName\": \"" + json(request.loraName()) + "\",\n"
                + "  \"professionalWorkflowRequired\": " + request.outputProfile().professional() + ",\n"
                + "  \"note\": \"LoRA y adaptadores reales mantienen rostro, vestuario y estilo cuando estan instalados; este baseline no cuenta como workflow profesional.\"\n"
                + "}\n";
    }

    private static String sourceFileName(Path path) {
        String name = path.getFileName() == null ? "input.png" : path.getFileName().toString();
        return name.isBlank() ? "input.png" : name;
    }

    private static String safe(String value) {
        String text = value == null || value.isBlank() ? "job" : value.toLowerCase(Locale.ROOT);
        return text.replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-");
    }

    private static Optional<Path> realWorkflow(ImageEnhancementRequest request) throws IOException {
        Path workflows = request.projectRoot().resolve("models").resolve("image").resolve("workflows");
        Path explicit = workflows.resolve(request.workflowId() + ".json");
        if (isRealWorkflow(explicit)) {
            return Optional.of(explicit);
        }
        Path profile = workflows.resolve(request.outputProfile().workflowId() + ".json");
        if (!profile.equals(explicit) && isRealWorkflow(profile)) {
            return Optional.of(profile);
        }
        return Optional.empty();
    }

    private static boolean isRealWorkflow(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path) || Files.size(path) < 128) {
            return false;
        }
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String lower = content.toLowerCase(Locale.ROOT);
        if (lower.contains("placeholder") || lower.contains("readme")) {
            return false;
        }
        return content.contains("\"class_type\"") && content.contains("\"inputs\"");
    }

    private static String json(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
