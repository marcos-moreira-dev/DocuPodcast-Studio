package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

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

    void releaseModels() throws IOException, InterruptedException {
        HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/free"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"unload_models\":true,\"free_memory\":true}"))
                .build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("ComfyUI rechazó la solicitud de liberar modelos: HTTP " + response.statusCode());
    }

    String systemStats() throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/system_stats"))
                        .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ComfyUI no publicó /system_stats: HTTP " + response.statusCode()
                    + technicalBody(response.body()));
        }
        return response.body();
    }

    String objectInfo(String node) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                        URI.create(baseUrl + "/object_info/" + encode(node)))
                .timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ComfyUI no publicó el nodo " + node + ": HTTP " + response.statusCode()
                    + technicalBody(response.body()));
        }
        return response.body();
    }

    String uploadImage(Path source, int index, Duration timeout) throws IOException, InterruptedException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IOException("La referencia visual no existe: " + source);
        }
        String boundary = "----DocuPodcast" + UUID.randomUUID().toString().replace("-", "");
        String filename = "certification-ref-" + index + "-" + source.getFileName();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(Files.readAllBytes(source));
        body.write(("\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"type\"\r\n\r\ninput\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"overwrite\"\r\n\r\ntrue\r\n"
                + "--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/upload/image"))
                        .timeout(timeout == null ? Duration.ofMinutes(2) : timeout)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("ComfyUI rechazo la referencia visual: HTTP " + response.statusCode());
        }
        Pattern name = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = name.matcher(response.body());
        return matcher.find() ? matcher.group(1) : filename;
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
        String clientId = UUID.randomUUID().toString();
        String body = "{\"client_id\":\"" + clientId + "\",\"prompt\":" + workflow + "}";
        HttpResponse<String> queued = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/prompt"))
                        .timeout(current.policy().timeout()).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (queued.statusCode() < 200 || queued.statusCode() >= 300) {
            throw new IOException(promptError(queued.statusCode(), queued.body()));
        }
        Matcher id = PROMPT_ID.matcher(queued.body());
        if (!id.find()) throw new IOException("ComfyUI no devolvió prompt_id."
                + technicalBody(queued.body()));
        String promptId = id.group(1);
        try (var live = ComfyUiProgressStream.open(client, baseUrl, clientId, promptId)) {
        long deadline = System.nanoTime() + current.policy().timeout().toNanos();
        while (System.nanoTime() < deadline) {
            current.cancellation().throwIfCancellationRequested();
            HttpResponse<String> history;
            try {
                history = client.send(HttpRequest.newBuilder(
                                URI.create(baseUrl + "/history/" + encode(promptId)))
                        .timeout(historyPollTimeout(deadline)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (HttpTimeoutException busyLoadingModel) {
                current.progress().report("LOADING_MODEL", 0.35,
                        "ComfyUI sigue cargando el modelo en GPU y memoria de apoyo.");
                continue;
            } catch (ConnectException stopped) {
                throw new IOException("ComfyUI dejó de responder durante el trabajo. "
                        + "El proceso pudo cerrarse por memoria insuficiente o por un fallo del runtime.", stopped);
            }
            if (history.statusCode() < 200 || history.statusCode() >= 300) {
                throw new IOException("ComfyUI no pudo consultar el estado del trabajo: HTTP "
                        + history.statusCode() + technicalBody(history.body()));
            }
            if (Pattern.compile("\"status_str\"\\s*:\\s*\"error\"").matcher(history.body()).find())
                throw new IOException("ComfyUI informó un fallo de generación." + technicalBody(history.body()));
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
            current.progress().report("GENERATING", 0.5, live.detail());
            Thread.sleep(500);
        }
        throw new IOException("ComfyUI no produjo un artefacto antes del timeout.");
        }
    }

    private static Duration historyPollTimeout(long deadline) {
        long remainingNanos = Math.max(Duration.ofSeconds(1).toNanos(), deadline - System.nanoTime());
        return Duration.ofNanos(Math.min(Duration.ofSeconds(10).toNanos(), remainingNanos));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String promptError(int status, String body) {
        String normalized = body == null ? "" : body.replaceAll("\\s+", " ").strip();
        String human;
        if (normalized.contains("DualCLIPLoader") || normalized.contains("text_encoders")) {
            human = "ComfyUI no puede cargar los codificadores de texto requeridos por FLUX.";
        } else if (normalized.contains("CheckpointLoader") || normalized.contains("ckpt_name")) {
            human = "ComfyUI no encuentra el checkpoint solicitado.";
        } else if (normalized.contains("UpscaleModelLoader") || normalized.contains("model_name")) {
            human = "ComfyUI no encuentra el modelo de superresolución solicitado.";
        } else if (normalized.contains("class_type") || normalized.contains("does not exist")) {
            human = "El workflow solicita un nodo que esta instalación de ComfyUI no tiene.";
        } else {
            human = "ComfyUI rechazó el trabajo.";
        }
        return human + " HTTP " + status + technicalBody(body);
    }

    private static String technicalBody(String body) {
        if (body == null || body.isBlank()) return "";
        String normalized = body.replaceAll("\\s+", " ").strip();
        int limit = Math.min(normalized.length(), 8_000);
        return " Detalle técnico: " + normalized.substring(0, limit)
                + (normalized.length() > limit ? "…" : "");
    }

    record DownloadedArtifact(String filename, byte[] bytes, String promptId) { }
}
