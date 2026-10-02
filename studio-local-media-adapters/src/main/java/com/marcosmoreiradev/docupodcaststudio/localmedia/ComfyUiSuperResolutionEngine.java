package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Real-ESRGAN adapter over ComfyUI's built-in tiled upscale nodes. */
public final class ComfyUiSuperResolutionEngine implements ImageSuperResolutionEngine {
    public static final EngineId ID = new EngineId("comfyui-real-esrgan");
    public static final String DEFAULT_MODEL = ImageSuperResolutionRequest.DEFAULT_MODEL;
    private final EngineConfiguration configuration;
    private final ComfyUiTransport transport;

    public ComfyUiSuperResolutionEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.transport = new ComfyUiTransport(this.configuration.value("baseUrl"));
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.IMAGE_SUPER_RESOLUTION,
                "Superresolución IA Real-ESRGAN", "x4plus", "comfyui",
                Set.of(EngineFeature.HARDWARE_ACCELERATION, EngineFeature.TILED_PROCESSING), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Dirección local",
                        "Endpoint HTTP del runtime ComfyUI compartido.", ConfigurationFieldType.URL,
                        true, "http://127.0.0.1:8188"),
                new EngineConfigurationField("modelPath", "Ubicación administrada de Real-ESRGAN",
                        "El peso se selecciona únicamente al usar Importar modelo.",
                        ConfigurationFieldType.READ_ONLY_PATH,
                        true, configuredModelPath().toString())));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        Path model = configuredModelPath();
        if (!Files.isRegularFile(model)) {
            return EngineReadiness.unavailable(ID, "Falta el modelo RealESRGAN_x4plus.",
                    "Instálalo o impórtalo desde Ajustes > Motores y dependencias.");
        }
        try {
            if (!ComfyUiSuperResolutionAdministration.isOfficial(model)) {
                return EngineReadiness.unavailable(
                        ID,
                        "El modelo RealESRGAN_x4plus no supera la validación.",
                        "Reintenta la instalación o importa el peso oficial.");
            }
            if (!transport.isReady()) {
                return EngineReadiness.unavailable(ID,
                        "Real-ESRGAN x4plus está instalado y validado; ComfyUI no está iniciado.",
                        "Inicia el motor local de imagen.");
            }
            String loader = transport.objectInfo("UpscaleModelLoader");
            String upscaler = transport.objectInfo("ImageUpscaleWithModel");
            if (!loader.contains(model.getFileName().toString()) || upscaler.isBlank()) {
                return EngineReadiness.unavailable(ID, "ComfyUI no publicó el modelo de superresolución.",
                        "Reinicia ComfyUI después de instalar el modelo.");
            }
            return EngineReadiness.ready(ID, "Real-ESRGAN x4plus disponible con procesamiento por mosaicos.");
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "No se pudo verificar la superresolución en ComfyUI.",
                    "Inicia o reinicia el motor local de imagen.");
        }
    }

    @Override public ImageSuperResolutionResult upscale(
            ImageSuperResolutionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        Path source = request.source().toAbsolutePath().normalize();
        BufferedImage sourceImage = ImageIO.read(source.toFile());
        if (sourceImage == null) throw new IOException("La imagen de origen no es PNG/JPEG compatible.");
        if (request.targetWidth() <= sourceImage.getWidth() || request.targetHeight() <= sourceImage.getHeight()) {
            throw new IOException("La resolución de destino debe ser superior al origen en ambos ejes.");
        }
        Path model = configuredModelPath();
        if (!Files.isRegularFile(model)) {
            throw new IOException("Falta " + model.getFileName() + "; instálalo desde Ajustes > Motores.");
        }
        ExecutionContext current = context == null ? ExecutionContext.defaults("real-esrgan") : context;
        current.progress().report("UPLOADING", 0.03, "Enviando la imagen existente a Real-ESRGAN.");
        String uploaded = transport.uploadImage(source, 0, Duration.ofMinutes(5));
        String workflow = ComfyWorkflowTemplate.superResolution(
                uploaded, request.modelName(), request.filenamePrefix() + "-ai-x4");
        ComfyUiTransport.DownloadedArtifact artifact = transport.execute(workflow, current, "superresolución IA");
        BufferedImage aiImage = ImageIO.read(new ByteArrayInputStream(artifact.bytes()));
        if (aiImage == null) throw new IOException("Real-ESRGAN devolvió un artefacto inválido.");

        Files.createDirectories(request.outputDirectory());
        Path aiTemporary = request.outputDirectory().resolve(request.filenamePrefix() + ".ai-x4.partial.png");
        Path normalizedTemporary = request.outputDirectory().resolve(request.filenamePrefix() + ".delivery.partial.png");
        Path target = request.outputDirectory().resolve(request.filenamePrefix() + ".png");
        try {
            Files.write(aiTemporary, artifact.bytes());
            current.progress().report("NORMALIZING", 0.92,
                    "Ajustando el resultado IA a " + request.targetWidth() + "x" + request.targetHeight() + ".");
            ImageDeliveryNormalizer.normalize(aiTemporary, normalizedTemporary,
                    request.targetWidth(), request.targetHeight(), request.containWithoutCrop());
            BufferedImage verified = ImageIO.read(normalizedTemporary.toFile());
            if (verified == null || verified.getWidth() != request.targetWidth()
                    || verified.getHeight() != request.targetHeight()) {
                throw new IOException("El PNG normalizado no coincide con las dimensiones solicitadas.");
            }
            promote(normalizedTemporary, target);
        } finally {
            Files.deleteIfExists(aiTemporary);
            Files.deleteIfExists(normalizedTemporary);
        }
        return new ImageSuperResolutionResult(source, target, request.targetWidth(), request.targetHeight(),
                request.modelName(), Map.of(
                "engineId", ID.value(),
                "promptId", artifact.promptId(),
                "workflow", "existing-image-upscale-only",
                "aiNativeWidth", Integer.toString(aiImage.getWidth()),
                "aiNativeHeight", Integer.toString(aiImage.getHeight())));
    }

    private Path configuredModelPath() {
        String value = configuration.value("modelPath");
        return value.isBlank() ? Path.of(DEFAULT_MODEL).toAbsolutePath().normalize()
                : Path.of(value).toAbsolutePath().normalize();
    }

    private static void promote(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
