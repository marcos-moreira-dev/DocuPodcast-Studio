package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** ComfyUI wire protocol shared by the image and video adapters only. */
final class ComfyUiTransport {
    private static final Pattern PROMPT_ID = Pattern.compile("\"prompt_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FILE = Pattern.compile("\"filename\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SUBFOLDER = Pattern.compile("\"subfolder\"\\s*:\\s*\"([^\"]*)\"");
    private final String baseUrl;
    private final HttpClient client;

    ComfyUiTransport(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8188" : baseUrl.strip();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        this.baseUrl = value;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    boolean isReady() throws IOException, InterruptedException {
        HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/system_stats"))
                .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.discarding());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    DownloadedArtifact execute(String workflow, ExecutionContext context, String progressLabel)
            throws IOException, InterruptedException {
        return executeAll(workflow, context, progressLabel).getFirst();
    }

    List<DownloadedArtifact> executeAll(String workflow, ExecutionContext context, String progressLabel)
            throws IOException, InterruptedException {
        ExecutionContext current = context == null ? ExecutionContext.defaults("comfyui") : context;
        current.cancellation().throwIfCancellationRequested();
        current.progress().report("QUEUED", 0.05, "Enviando trabajo de " + progressLabel + ".");
        String body = "{\"prompt\":" + workflow + "}";
        HttpResponse<String> queued = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/prompt"))
                        .timeout(current.policy().timeout()).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (queued.statusCode() < 200 || queued.statusCode() >= 300) {
            throw new IOException("ComfyUI rechazó el trabajo: HTTP " + queued.statusCode());
        }
        Matcher id = PROMPT_ID.matcher(queued.body());
        if (!id.find()) throw new IOException("ComfyUI no devolvió prompt_id.");
        String promptId = id.group(1);
        long deadline = System.nanoTime() + current.policy().timeout().toNanos();
        while (System.nanoTime() < deadline) {
            current.cancellation().throwIfCancellationRequested();
            HttpResponse<String> history = client.send(HttpRequest.newBuilder(
                            URI.create(baseUrl + "/history/" + encode(promptId)))
                    .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Matcher file = FILE.matcher(history.body());
            ArrayList<String> filenames = new ArrayList<>();
            while (file.find()) filenames.add(file.group(1));
            if (!filenames.isEmpty()) {
                Matcher subfolder = SUBFOLDER.matcher(history.body());
                String sub = subfolder.find() ? subfolder.group(1) : "";
                ArrayList<DownloadedArtifact> artifacts = new ArrayList<>();
                for (String filename : filenames.stream().distinct().toList()) {
                    URI view = URI.create(baseUrl + "/view?filename=" + encode(filename)
                            + "&subfolder=" + encode(sub) + "&type=output");
                    HttpResponse<byte[]> artifact = client.send(HttpRequest.newBuilder(view)
                                    .timeout(current.policy().timeout()).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
                    if (artifact.statusCode() < 200 || artifact.statusCode() >= 300) {
                        throw new IOException("No se pudo descargar el artefacto: HTTP " + artifact.statusCode());
                    }
                    artifacts.add(new DownloadedArtifact(filename, artifact.body(), promptId));
                }
                current.progress().report("COMPLETED", 1.0, "Trabajo de " + progressLabel + " completado.");
                return List.copyOf(artifacts);
            }
            current.progress().report("GENERATING", 0.5, "Esperando resultado de " + progressLabel + ".");
            Thread.sleep(500);
        }
        throw new IOException("ComfyUI no produjo un artefacto antes del timeout.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    record DownloadedArtifact(String filename, byte[] bytes, String promptId) { }
}
