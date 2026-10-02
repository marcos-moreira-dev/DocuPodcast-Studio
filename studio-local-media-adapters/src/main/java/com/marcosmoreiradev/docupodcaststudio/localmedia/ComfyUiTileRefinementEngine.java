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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GPU-first composition-preserving img2img adapter using SD 1.5 and ControlNet Tile. */
public final class ComfyUiTileRefinementEngine implements ImageRefinementEngine {
    public static final EngineId ID = new EngineId("comfyui-controlnet-tile");
    public static final String DEFAULT_CONTROL_NET = "control_v11f1e_sd15_tile.pth";
    private static final String DEFAULT_PROMPT =
            "high quality image, clean coherent details, natural texture, preserve composition and identity";
    private static final String NEGATIVE =
            "changed composition, duplicated subject, altered identity, deformed object, text artifacts, watermark, blur";
    private static final Pattern VRAM_TOTAL = Pattern.compile(
            "\"(?:vram_total|vram_total_bytes)\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final EngineConfiguration configuration;
    private final ComfyUiTransport transport;

    public ComfyUiTileRefinementEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.transport = new ComfyUiTransport(this.configuration.value("baseUrl"));
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.IMAGE_REFINEMENT,
                "Mejora de composición · ControlNet Tile", "sd15-tile-v1.1", "comfyui",
                Set.of(EngineFeature.HARDWARE_ACCELERATION, EngineFeature.TILED_PROCESSING,
                        EngineFeature.CONDITIONING_IMAGE, EngineFeature.COMPOSITION_PRESERVING), false);
    }

    @Override public List<EnginePresetDescriptor> presets() {
        return List.of(
                new EnginePresetDescriptor(ImageRefinementRequest.CONSERVATIVE, "Conservadora",
                        "Refinado de baja intensidad que prioriza conservar la composición.",
                        Set.of(EngineFeature.COMPOSITION_PRESERVING),
                        Map.of("denoise", "0.18",
                                "resource.runtime", "ComfyUI",
                                "resource.checkpoint", "SD 1.5",
                                "resource.model", "ControlNet Tile"), true),
                new EnginePresetDescriptor(ImageRefinementRequest.BALANCED, "Equilibrada",
                        "Refinado moderado con más reconstrucción de detalle.",
                        Set.of(EngineFeature.COMPOSITION_PRESERVING),
                        Map.of("denoise", "0.28",
                                "resource.runtime", "ComfyUI",
                                "resource.checkpoint", "SD 1.5",
                                "resource.model", "ControlNet Tile"), false));
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Dirección local",
                        "Endpoint HTTP del runtime ComfyUI compartido.", ConfigurationFieldType.URL,
                        true, "http://127.0.0.1:8188"),
                new EngineConfigurationField("controlNetPath", "Ubicación administrada de ControlNet Tile",
                        "El peso se selecciona únicamente al usar Importar modelo.",
                        ConfigurationFieldType.READ_ONLY_PATH, true, controlNetPath().toString()),
                new EngineConfigurationField("checkpointPath", "Checkpoint SD 1.5 compartido",
                        "Modelo base reutilizado por el refinado conservador.",
                        ConfigurationFieldType.READ_ONLY_PATH, true, checkpointPath().toString())));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        Path controlNet = controlNetPath();
        Path checkpoint = checkpointPath();
        if (!Files.isRegularFile(controlNet)) {
            return EngineReadiness.unavailable(ID, "Falta ControlNet Tile para la mejora de composición.",
                    "Instálalo o impórtalo desde Motores y dependencias.");
        }
        if (!Files.isRegularFile(checkpoint)) {
            return EngineReadiness.unavailable(ID, "Falta el checkpoint SD 1.5 compartido.",
                    "Prepara el perfil Prueba 4 GB desde Motores y dependencias.");
        }
        try {
            if (!ComfyUiTileRefinementAdministration.isOfficial(controlNet)) {
                return EngineReadiness.unavailable(ID, "ControlNet Tile no supera la validación.",
                        "Repara o importa el peso oficial.");
            }
            if (!transport.isReady()) {
                return EngineReadiness.unavailable(ID,
                        "ControlNet Tile está instalado; ComfyUI todavía no está iniciado.",
                        "Inicia el motor local de imagen.");
            }
            String controls = transport.objectInfo("ControlNetLoader");
            String checkpoints = transport.objectInfo("CheckpointLoaderSimple");
            for (String node : List.of("ControlNetApplyAdvanced", "VAEEncode", "KSampler", "VAEDecode")) {
                if (transport.objectInfo(node).isBlank()) {
                    return EngineReadiness.unavailable(ID, "ComfyUI no publicó el nodo " + node + ".",
                            "Repara o actualiza el runtime administrado.");
                }
            }
            if (!controls.contains(controlNet.getFileName().toString())) {
                return EngineReadiness.unavailable(ID, "ComfyUI no enumera ControlNet Tile.",
                        "Recarga el catálogo del runtime.");
            }
            if (!checkpoints.contains(checkpoint.getFileName().toString())) {
                return EngineReadiness.unavailable(ID, "ComfyUI no enumera el checkpoint SD 1.5.",
                        "Repara las rutas compartidas de modelos.");
            }
            return EngineReadiness.ready(ID, "ControlNet Tile disponible para refinado conservador por mosaicos.");
        } catch (Exception failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "No se pudo verificar ControlNet Tile en ComfyUI.",
                    "Inicia o reinicia el motor local de imagen.");
        }
    }

    @Override public ImageRefinementResult refine(ImageRefinementRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        Path source = request.source().toAbsolutePath().normalize();
        BufferedImage input = ImageIO.read(source.toFile());
        if (input == null) throw new IOException("La imagen reescalada no es un PNG/JPEG compatible.");
        if (!Files.isRegularFile(controlNetPath()) || !Files.isRegularFile(checkpointPath())) {
            throw new IOException("Faltan ControlNet Tile o el checkpoint SD 1.5 compartido.");
        }
        ExecutionContext current = context == null
                ? ExecutionContext.defaults("controlnet-tile-refinement") : context;
        Parameters parameters = Parameters.forPreset(request.presetId());
        int initialTile = requestedTile(request.options().get("tileSize"), detectedTileSize());
        try {
            return refineWithTiles(request, current, input, initialTile, overlap(initialTile), parameters);
        } catch (IOException failure) {
            int fallbackTile = smallerTile(initialTile);
            if (fallbackTile >= 384 && isOutOfMemory(failure)) {
                current.progress().report("RETRYING", 0.02,
                        "Memoria GPU insuficiente; reintentando con mosaicos de " + fallbackTile + " px.");
                return refineWithTiles(request, current, input, fallbackTile, overlap(fallbackTile), parameters);
            }
            throw failure;
        }
    }

    private ImageRefinementResult refineWithTiles(
            ImageRefinementRequest request,
            ExecutionContext context,
            BufferedImage input,
            int tileSize,
            int overlap,
            Parameters parameters) throws IOException, InterruptedException {
        List<Integer> xs = positions(input.getWidth(), tileSize, overlap);
        List<Integer> ys = positions(input.getHeight(), tileSize, overlap);
        int total = xs.size() * ys.size();
        float[] red = new float[input.getWidth() * input.getHeight()];
        float[] green = new float[red.length];
        float[] blue = new float[red.length];
        float[] weights = new float[red.length];
        Path staging = request.outputDirectory().resolve(
                "." + request.filenamePrefix() + "-refinement-staging");
        Files.createDirectories(staging);
        ArrayList<String> promptIds = new ArrayList<>();
        int index = 0;
        try {
            for (int row = 0; row < ys.size(); row++) {
                for (int column = 0; column < xs.size(); column++) {
                    context.cancellation().throwIfCancellationRequested();
                    int x = xs.get(column);
                    int y = ys.get(row);
                    int width = Math.min(tileSize, input.getWidth() - x);
                    int height = Math.min(tileSize, input.getHeight() - y);
                    BufferedImage tile = input.getSubimage(x, y, width, height);
                    BufferedImage aiTile = padToMultipleOfEight(tile);
                    Path tileInput = staging.resolve("input-" + index + ".png");
                    ImageIO.write(aiTile, "png", tileInput.toFile());
                    context.progress().report("REFINING_TILE", index / (double) Math.max(1, total),
                            "Mejorando mosaico " + (index + 1) + " de " + total + ".");
                    String uploaded = transport.uploadImage(tileInput, index, Duration.ofMinutes(5));
                    String workflow = ComfyWorkflowTemplate.refinement(
                            uploaded,
                            checkpointPath().getFileName().toString(),
                            controlNetPath().getFileName().toString(),
                            request.prompt().isBlank() ? DEFAULT_PROMPT : request.prompt(),
                            NEGATIVE,
                            request.filenamePrefix() + "-tile-" + index,
                            request.seed() + index,
                            parameters.steps(),
                            parameters.denoise(),
                            parameters.controlStrength(),
                            parameters.cfg());
                    ComfyUiTransport.DownloadedArtifact artifact =
                            transport.execute(workflow, context, "refinado de mosaico");
                    BufferedImage refined = ImageIO.read(new ByteArrayInputStream(artifact.bytes()));
                    if (refined == null) throw new IOException("ComfyUI devolvió un mosaico inválido.");
                    if (refined.getWidth() != aiTile.getWidth()
                            || refined.getHeight() != aiTile.getHeight()) {
                        throw new IOException("El mosaico refinado cambió de dimensiones.");
                    }
                    BufferedImage cropped = refined.getSubimage(0, 0, width, height);
                    blend(cropped, x, y, input.getWidth(), input.getHeight(), overlap,
                            column > 0, column + 1 < xs.size(), row > 0, row + 1 < ys.size(),
                            red, green, blue, weights);
                    promptIds.add(artifact.promptId());
                    cleanupComfyArtifact(uploaded, artifact.filename());
                    Files.deleteIfExists(tileInput);
                    index++;
                }
            }
            BufferedImage merged = merge(input, red, green, blue, weights);
            Files.createDirectories(request.outputDirectory());
            Path partial = request.outputDirectory().resolve(request.filenamePrefix() + ".partial.png");
            Path target = request.outputDirectory().resolve(request.filenamePrefix() + ".png");
            ImageIO.write(merged, "png", partial.toFile());
            BufferedImage verified = ImageIO.read(partial.toFile());
            if (verified == null || verified.getWidth() != input.getWidth()
                    || verified.getHeight() != input.getHeight()) {
                throw new IOException("El refinado final no conserva las dimensiones de la imagen reescalada.");
            }
            promote(partial, target);
            return new ImageRefinementResult(request.source(), target, input.getWidth(), input.getHeight(),
                    true, "", request.presetId(), Map.of(
                    "engineId", ID.value(),
                    "workflow", "existing-image-controlnet-tile-img2img",
                    "tileSize", Integer.toString(tileSize),
                    "tileOverlap", Integer.toString(overlap),
                    "tileCount", Integer.toString(total),
                    "promptIds", String.join(",", promptIds),
                    "denoise", Double.toString(parameters.denoise()),
                    "controlStrength", Double.toString(parameters.controlStrength())));
        } finally {
            cleanupStaging(staging);
        }
    }

    private int detectedTileSize() {
        try {
            Matcher matcher = VRAM_TOTAL.matcher(transport.systemStats());
            if (!matcher.find()) return 512;
            long bytes = Long.parseLong(matcher.group(1));
            long gib = bytes / (1024L * 1024L * 1024L);
            if (gib >= 10) return 1024;
            if (gib >= 6) return 768;
        } catch (Exception ignored) {
            // Conservative 4 GB profile remains the safe default.
        }
        return 512;
    }

    private Path controlNetPath() {
        return configuredPath("controlNetPath", DEFAULT_CONTROL_NET);
    }

    private Path checkpointPath() {
        return configuredPath("checkpointPath", "v1-5-pruned-emaonly-fp16.safetensors");
    }

    private void cleanupComfyArtifact(String uploaded, String generated) {
        Path modelDirectory = controlNetPath().getParent();
        if (modelDirectory == null || modelDirectory.getParent() == null
                || modelDirectory.getParent().getParent() == null) return;
        Path runtimeRoot = modelDirectory.getParent().getParent();
        deleteConfined(runtimeRoot.resolve("input"), uploaded);
        deleteConfined(runtimeRoot.resolve("output"), generated);
    }

    private static void deleteConfined(Path directory, String relativeName) {
        if (directory == null || relativeName == null || relativeName.isBlank()) return;
        try {
            Path root = directory.toAbsolutePath().normalize();
            Path target = root.resolve(relativeName).normalize();
            if (target.startsWith(root)) Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Runtime output is disposable; inability to clean it must not invalidate a verified result.
        }
    }

    private Path configuredPath(String key, String fallback) {
        String value = configuration.value(key);
        return Path.of(value.isBlank() ? fallback : value).toAbsolutePath().normalize();
    }

    static List<Integer> positions(int length, int tile, int overlap) {
        if (length <= tile) return List.of(0);
        int stride = Math.max(1, tile - overlap);
        ArrayList<Integer> positions = new ArrayList<>();
        for (int value = 0; value + tile < length; value += stride) positions.add(value);
        int last = length - tile;
        if (positions.isEmpty() || positions.getLast() != last) positions.add(last);
        return List.copyOf(positions);
    }

    private static void blend(
            BufferedImage tile, int offsetX, int offsetY, int fullWidth, int fullHeight, int overlap,
            boolean left, boolean right, boolean top, boolean bottom,
            float[] red, float[] green, float[] blue, float[] weights) {
        for (int y = 0; y < tile.getHeight(); y++) {
            float wy = edgeWeight(y, tile.getHeight(), overlap, top, bottom);
            for (int x = 0; x < tile.getWidth(); x++) {
                float weight = wy * edgeWeight(x, tile.getWidth(), overlap, left, right);
                int argb = tile.getRGB(x, y);
                int target = (offsetY + y) * fullWidth + offsetX + x;
                red[target] += ((argb >>> 16) & 0xff) * weight;
                green[target] += ((argb >>> 8) & 0xff) * weight;
                blue[target] += (argb & 0xff) * weight;
                weights[target] += weight;
            }
        }
    }

    private static float edgeWeight(int value, int length, int overlap, boolean before, boolean after) {
        if (overlap <= 0) return 1f;
        if (before && value < overlap) return cosine(value / (float) overlap);
        int fromEnd = length - 1 - value;
        if (after && fromEnd < overlap) return cosine(fromEnd / (float) overlap);
        return 1f;
    }

    private static float cosine(float ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * clamped));
    }

    static BufferedImage merge(
            BufferedImage source, float[] red, float[] green, float[] blue, float[] weights) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < weights.length; i++) {
            float weight = weights[i] <= 0f ? 1f : weights[i];
            int a = (source.getRGB(i % width, i / width) >>> 24) & 0xff;
            int r = channel(red[i] / weight);
            int g = channel(green[i] / weight);
            int b = channel(blue[i] / weight);
            output.setRGB(i % width, i / width, (a << 24) | (r << 16) | (g << 8) | b);
        }
        return output;
    }

    static BufferedImage padToMultipleOfEight(BufferedImage source) {
        int width = ((source.getWidth() + 7) / 8) * 8;
        int height = ((source.getHeight() + 7) / 8) * 8;
        if (width == source.getWidth() && height == source.getHeight()) return source;
        BufferedImage padded = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int sourceY = Math.min(y, source.getHeight() - 1);
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(x, source.getWidth() - 1);
                padded.setRGB(x, y, source.getRGB(sourceX, sourceY));
            }
        }
        return padded;
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static int requestedTile(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.strip());
            return parsed >= 384 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int overlap(int tile) {
        if (tile >= 1024) return 128;
        if (tile >= 768) return 96;
        return tile >= 512 ? 64 : 48;
    }

    private static int smallerTile(int tile) {
        if (tile > 768) return 768;
        if (tile > 512) return 512;
        return 384;
    }

    private static boolean isOutOfMemory(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("out of memory") || message.contains("cuda oom")
                    || message.contains("memory insufficient")) return true;
            current = current.getCause();
        }
        return false;
    }

    private static void promote(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cleanupStaging(Path staging) {
        if (staging == null || !Files.exists(staging)) return;
        try (var paths = Files.walk(staging)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Staging is recoverable and never replaces a user image.
        }
    }

    private record Parameters(int steps, double denoise, double controlStrength, double cfg) {
        static Parameters forPreset(EnginePresetId preset) {
            return ImageRefinementRequest.BALANCED.equals(preset)
                    ? new Parameters(16, 0.28, 0.85, 6.0)
                    : new Parameters(12, 0.18, 0.95, 5.5);
        }
    }
}
