package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ComfyUiImageEngine implements ImageGenerationEngine {
    public static final EngineId ID = new EngineId("comfyui");
    private static final Pattern PROMPT_ID = Pattern.compile("\"prompt_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FILE = Pattern.compile("\"filename\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SUBFOLDER = Pattern.compile("\"subfolder\"\\s*:\\s*\"([^\"]*)\"");
    private final EngineConfiguration configuration;
    private final HttpClient client;

    public ComfyUiImageEngine(EngineConfiguration configuration) {
        this.configuration = configuration == null ? new EngineConfiguration(ID, Map.of()) : configuration;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.IMAGE_GENERATION, "Imagen local", "local", "http",
                Set.of(EngineFeature.CONDITIONING_IMAGE, EngineFeature.FRAME_INTERPOLATION), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of(
                new EngineConfigurationField("baseUrl", "Dirección local", "Endpoint HTTP de ComfyUI.",
                        ConfigurationFieldType.URL, true, "http://127.0.0.1:8188")));
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration ignored) {
        String base = baseUrl();
        try {
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(base + "/system_stats"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? EngineReadiness.ready(ID, "Motor local de imagen disponible.")
                    : EngineReadiness.unavailable(ID, "El motor de imagen rechazó la prueba HTTP.", "Inicia o revisa ComfyUI.");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            return EngineReadiness.unavailable(ID, "El motor local de imagen no responde.", "Inicia ComfyUI y revisa su dirección local.");
        }
    }

    @Override public ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("image-generation") : context;
        String workflow = request.options().getOrDefault("workflowJson", "");
        if (workflow.isBlank()) throw new IOException("La petición no incluye workflowJson para el adaptador ComfyUI.");
        String payload = workflow
                .replace("{{prompt}}", json(request.prompt()))
                .replace("{{negativePrompt}}", json(request.negativePrompt()))
                .replace("{{width}}", Integer.toString(request.width()))
                .replace("{{height}}", Integer.toString(request.height()))
                .replace("{{prefix}}", json(request.filenamePrefix()));
        current.progress().report("QUEUED", 0.05, "Enviando trabajo al motor local de imagen.");
        HttpResponse<String> queued = client.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/prompt"))
                        .timeout(current.policy().timeout()).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (queued.statusCode() < 200 || queued.statusCode() >= 300) {
            throw new IOException("ComfyUI rechazó el trabajo: HTTP " + queued.statusCode());
        }
        Matcher idMatcher = PROMPT_ID.matcher(queued.body());
        if (!idMatcher.find()) throw new IOException("ComfyUI no devolvió prompt_id.");
        String promptId = idMatcher.group(1);
        long deadline = System.nanoTime() + current.policy().timeout().toNanos();
        while (System.nanoTime() < deadline) {
            current.cancellation().throwIfCancellationRequested();
            HttpResponse<String> history = client.send(HttpRequest.newBuilder(
                            URI.create(baseUrl() + "/history/" + encode(promptId)))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Matcher file = FILE.matcher(history.body());
            if (file.find()) {
                String filename = file.group(1);
                Matcher sub = SUBFOLDER.matcher(history.body());
                String subfolder = sub.find() ? sub.group(1) : "";
                URI view = URI.create(baseUrl() + "/view?filename=" + encode(filename)
                        + "&subfolder=" + encode(subfolder) + "&type=output");
                HttpResponse<byte[]> image = client.send(HttpRequest.newBuilder(view).GET().build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                if (image.statusCode() < 200 || image.statusCode() >= 300) {
                    throw new IOException("No se pudo descargar la imagen generada: HTTP " + image.statusCode());
                }
                Files.createDirectories(request.outputDirectory());
                Path target = request.outputDirectory().resolve(request.filenamePrefix() + ".png");
                Files.write(target, image.body());
                current.progress().report("COMPLETED", 1.0, "Imagen generada.");
                return new ImageGenerationResult(List.of(target), Map.of("engineId", ID.value(), "promptId", promptId));
            }
            current.progress().report("GENERATING", 0.5, "Esperando resultado del motor local de imagen.");
            Thread.sleep(500);
        }
        throw new IOException("ComfyUI no produjo una imagen antes del timeout.");
    }

    private String baseUrl() {
        String value = configuration.value("baseUrl");
        if (value.isBlank()) value = "http://127.0.0.1:8188";
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String json(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
