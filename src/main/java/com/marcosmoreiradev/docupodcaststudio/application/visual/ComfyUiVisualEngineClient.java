package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationAttemptPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.process.GenerationTaskKind;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Common ComfyUI HTTP client for visual generation, smoke tests and theatre adapters. */
public final class ComfyUiVisualEngineClient {
    private static final Pattern PROMPT_ID = Pattern.compile("\"prompt_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FILE = Pattern.compile("\"filename\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SUBFOLDER = Pattern.compile("\"subfolder\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern TYPE = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern UPLOADED_NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final String RIFE_MODEL = "rife_v4.25_lite.safetensors";
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final HttpClient httpClient;
    private final ComfyUiWorkflowPayloadFactory workflowPayloadFactory = new ComfyUiWorkflowPayloadFactory();

    public ComfyUiVisualEngineClient() {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8))
                .build());
    }

    public ComfyUiVisualEngineClient(HttpClient httpClient) {
        this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
    }

    public ConnectionResult test(String baseUrl, Duration timeout) {
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/system_stats").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            return new ConnectionResult(ok, ok
                    ? "Motor de generacion visual local disponible."
                    : "El motor de generacion visual local respondio, pero rechazo la prueba.",
                    "baseUrl=" + normalizedBaseUrl(baseUrl) + "\nhttpStatus=" + response.statusCode());
        } catch (IOException | InterruptedException | IllegalArgumentException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ConnectionResult(false,
                    "El motor de generacion visual local no responde. Verifica que este preparado e iniciado desde Configuracion.",
                    "baseUrl=" + normalizedBaseUrl(baseUrl) + "\nerror=" + ex.getMessage());
        }
    }

    public ComfyUiSystemStats systemStats(String baseUrl, Duration timeout) throws IOException {
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/system_stats").GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("ComfyUI rechazo /system_stats: HTTP " + response.statusCode() + ".");
            }
            return ComfyUiSystemStats.parse(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Verificacion del dispositivo visual interrumpida.", ex);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Endpoint visual no valido: " + ex.getMessage(), ex);
        }
    }

    public VisualEngineResult generate(String baseUrl,
                                       Duration timeout,
                                       VisualEngineRequest request,
                                       GenerationAttemptPolicy attemptPolicy,
                                       GenerationTaskKind taskKind,
                                       Consumer<String> progress) throws IOException {
        return generate(baseUrl, timeout, request, ComfyUiWorkflowSpec.sd15(), attemptPolicy, taskKind, progress);
    }

    public VisualEngineResult generate(String baseUrl,
                                       Duration timeout,
                                       VisualEngineRequest request,
                                       ComfyUiWorkflowSpec workflow,
                                       GenerationAttemptPolicy attemptPolicy,
                                       GenerationTaskKind taskKind,
                                       Consumer<String> progress) throws IOException {
        GenerationAttemptPolicy policy = attemptPolicy == null ? GenerationAttemptPolicy.defaults() : attemptPolicy;
        GenerationTaskKind kind = taskKind == null ? GenerationTaskKind.IMAGE_CANDIDATE : taskKind;
        int maxAttempts = policy.maxAttempts(kind);
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return generateOnce(baseUrl, timeout, request, workflow, attempt, maxAttempts, progress);
            } catch (IOException ex) {
                lastFailure = ex;
                if (attempt >= maxAttempts) {
                    break;
                }
                progress(progress, "Reintentando " + (attempt + 1) + "/" + maxAttempts + ": " + ex.getMessage());
            }
        }
        throw lastFailure == null ? new IOException("No se pudo generar la imagen.") : lastFailure;
    }

    public String queuePrompt(String baseUrl, Duration timeout, VisualEngineRequest request) throws IOException {
        return queuePrompt(baseUrl, timeout, request, ComfyUiWorkflowSpec.sd15());
    }

    public String queuePrompt(String baseUrl, Duration timeout, VisualEngineRequest request,
                              ComfyUiWorkflowSpec workflow) throws IOException {
        VisualEngineRequest preparedRequest = uploadConditioningReferences(baseUrl, timeout, request);
        String body = workflowPayloadFactory.create(preparedRequest, workflow);
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/prompt")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("El motor local rechazo el prompt: HTTP " + response.statusCode()
                        + ". " + response.body());
            }
            Matcher matcher = PROMPT_ID.matcher(response.body());
            if (!matcher.find()) {
                throw new IOException("El motor local no devolvio prompt_id. " + response.body());
            }
            return matcher.group(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Generacion del motor local interrumpida.", ex);
        }
    }

    private VisualEngineRequest uploadConditioningReferences(String baseUrl,
                                                              Duration timeout,
                                                              VisualEngineRequest request) throws IOException {
        if (request == null || request.conditioningReferences().isEmpty()) return request;
        ArrayList<VisualConditioningReference> uploaded = new ArrayList<>();
        int index = 0;
        for (VisualConditioningReference reference : request.conditioningReferences()) {
            if (reference == null || !reference.usable()) {
                throw new IOException("Referencia visual no disponible: "
                        + (reference == null ? "sin referencia" : reference.label()));
            }
            String name = uploadImage(baseUrl, timeout, reference.imagePath(), ++index);
            uploaded.add(reference.withEngineImageName(name));
        }
        return request.withConditioningReferences(List.copyOf(uploaded));
    }

    public VisualEngineResult interpolateMiddleFrame(String baseUrl,
                                                     Duration timeout,
                                                     Path previousFrame,
                                                     Path nextFrame,
                                                     Path outputDirectory,
                                                     String filenamePrefix,
                                                     Consumer<String> progress) throws IOException {
        Path previous = previousFrame == null ? null : previousFrame.toAbsolutePath().normalize();
        Path next = nextFrame == null ? null : nextFrame.toAbsolutePath().normalize();
        if (previous == null || !Files.isRegularFile(previous)
                || next == null || !Files.isRegularFile(next)) {
            throw new IOException("RIFE requiere dos imagenes existentes para interpolar.");
        }
        if (outputDirectory == null) {
            throw new IOException("Selecciona una carpeta de salida para frames RIFE.");
        }
        ensureRifeReady(baseUrl, timeout);
        BufferedImage reference = ImageIO.read(previous.toFile());
        if (reference == null) {
            throw new IOException("No se pudo leer el frame anterior para validar dimensiones.");
        }
        Files.createDirectories(outputDirectory);
        progress(progress, "Subiendo frames a ComfyUI.");
        String previousName = uploadImage(baseUrl, timeout, previous, 1);
        String nextName = uploadImage(baseUrl, timeout, next, 2);
        String safePrefix = safePrefix(filenamePrefix == null || filenamePrefix.isBlank()
                ? "rife-intermedio"
                : filenamePrefix);
        progress(progress, "Enviando workflow RIFE.");
        String promptId = queueRawPrompt(baseUrl, timeout, rifePrompt(previousName, nextName, safePrefix));
        progress(progress, "Esperando frame central RIFE.");
        OutputImage output = waitForOutput(baseUrl, timeout, promptId, Duration.ofSeconds(
                Math.min(1800, Math.max(90, normalizeTimeout(timeout).toSeconds()))));
        Path target = outputDirectory.resolve(safePrefix + ".png");
        progress(progress, "Descargando frame central RIFE.");
        downloadPng(baseUrl, timeout, output, target, reference.getWidth(), reference.getHeight());
        return new VisualEngineResult(target, reference.getWidth(), reference.getHeight(), promptId,
                "prompt_id=" + promptId
                        + "\nbaseUrl=" + normalizedBaseUrl(baseUrl)
                        + "\nworkflow=RIFE"
                        + "\nmodel=" + RIFE_MODEL
                        + "\ntarget=" + reference.getWidth() + "x" + reference.getHeight());
    }

    private void ensureRifeReady(String baseUrl, Duration timeout) throws IOException {
        ConnectionResult status = test(baseUrl, timeout);
        if (!status.available()) {
            throw new IOException(status.message() + "\n" + status.diagnostic());
        }
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout,
                            "/object_info/FrameInterpolationModelLoader")
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body() == null ? "" : response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300
                    || !body.contains("FrameInterpolationModelLoader")) {
                throw new IOException("RIFE no esta disponible en ComfyUI. Falta el nodo FrameInterpolationModelLoader.");
            }
            if (!body.contains(RIFE_MODEL)) {
                throw new IOException("Falta el modelo RIFE " + RIFE_MODEL
                        + " en ComfyUI/models/frame_interpolation. Preparalo localmente y vuelve a intentar.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Preflight RIFE interrumpido.", ex);
        }
    }

    public String queueRawPrompt(String baseUrl, Duration timeout, String promptJson) throws IOException {
        String body = "{\"prompt\":" + promptJson + "}";
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/prompt")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("El motor local rechazo el workflow visual: HTTP " + response.statusCode()
                        + ". " + response.body());
            }
            Matcher matcher = PROMPT_ID.matcher(response.body());
            if (!matcher.find()) {
                throw new IOException("El motor local no devolvio prompt_id para el workflow visual. "
                        + response.body());
            }
            return matcher.group(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Envio del workflow visual interrumpido.", ex);
        }
    }

    private static String rifePrompt(String previousName, String nextName, String filenamePrefix) {
        return "{"
                + "\"1\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + esc(previousName) + "\"}},"
                + "\"2\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + esc(nextName) + "\"}},"
                + "\"3\":{\"class_type\":\"ImageBatch\",\"inputs\":{\"image1\":[\"1\",0],\"image2\":[\"2\",0]}},"
                + "\"4\":{\"class_type\":\"FrameInterpolationModelLoader\",\"inputs\":{\"model_name\":\"" + RIFE_MODEL + "\"}},"
                + "\"5\":{\"class_type\":\"FrameInterpolate\",\"inputs\":{\"interp_model\":[\"4\",0],\"images\":[\"3\",0],\"multiplier\":2}},"
                + "\"6\":{\"class_type\":\"ImageFromBatch\",\"inputs\":{\"image\":[\"5\",0],\"batch_index\":1,\"length\":1}},"
                + "\"7\":{\"class_type\":\"SaveImage\",\"inputs\":{\"images\":[\"6\",0],\"filename_prefix\":\"" + esc(filenamePrefix) + "\"}}"
                + "}";
    }

    public String uploadImage(String baseUrl, Duration timeout, Path source, int index) throws IOException {
        Path normalizedSource = source == null ? null : source.toAbsolutePath().normalize();
        if (normalizedSource == null || !Files.isRegularFile(normalizedSource)) {
            throw new IOException("La imagen que se intentara subir a ComfyUI no existe.");
        }
        String boundary = "----DocuPodcast" + UUID.randomUUID().toString().replace("-", "");
        String extension = extension(normalizedSource.getFileName().toString());
        String filename = "docupodcast-ref-" + index + "-" + UUID.randomUUID().toString().substring(0, 8) + extension;
        byte[] body = multipart(boundary, filename, Files.readAllBytes(normalizedSource));
        try {
            HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/upload/image")
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("ComfyUI rechazo una referencia visual: HTTP " + response.statusCode()
                        + ". " + response.body());
            }
            Matcher matcher = UPLOADED_NAME.matcher(response.body());
            return matcher.find() ? matcher.group(1) : filename;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Carga de referencia visual interrumpida.", ex);
        }
    }

    private static byte[] multipart(String boundary, String filename, byte[] content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writePart(output, boundary, "image", filename, "application/octet-stream", content);
        writeField(output, boundary, "type", "input");
        writeField(output, boundary, "overwrite", "true");
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private static void writePart(ByteArrayOutputStream output,
                                  String boundary,
                                  String field,
                                  String filename,
                                  String contentType,
                                  byte[] content) throws IOException {
        output.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(content);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeField(ByteArrayOutputStream output,
                                   String boundary,
                                   String field,
                                   String value) throws IOException {
        output.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + field + "\"\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static String extension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        return dot < 0 ? ".png" : filename.substring(dot);
    }

    public OutputImage waitForOutput(String baseUrl, Duration timeout, String promptId, Duration maxWait) throws IOException {
        long deadline = System.nanoTime() + (maxWait == null ? Duration.ofMinutes(5) : maxWait).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/history/" + enc(promptId))
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    Optional<OutputImage> parsed = parseOutput(response.body());
                    if (parsed.isPresent()) {
                        return parsed.get();
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Espera del motor local interrumpida.", ex);
            }
        }
        throw new IOException("El motor local no termino la imagen antes del timeout. prompt_id=" + promptId);
    }

    public List<OutputArtifact> waitForArtifacts(String baseUrl,
                                                 Duration timeout,
                                                 String promptId,
                                                 Duration maxWait) throws IOException {
        long deadline = System.nanoTime() + (maxWait == null ? Duration.ofMinutes(20) : maxWait).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = httpClient.send(request(baseUrl, timeout, "/history/" + enc(promptId))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    List<OutputArtifact> artifacts = parseArtifacts(response.body());
                    if (!artifacts.isEmpty()) {
                        return artifacts;
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IOException("Espera del workflow visual interrumpida.", ex);
            }
        }
        throw new IOException("El motor local no termino el workflow antes del timeout. prompt_id=" + promptId);
    }

    public void downloadPng(String baseUrl,
                            Duration timeout,
                            OutputImage output,
                            Path target,
                            int expectedWidth,
                            int expectedHeight) throws IOException {
        if (output == null) {
            throw new IOException("El motor local no entrego referencia de imagen.");
        }
        String path = "/view?filename=" + enc(output.filename())
                + "&subfolder=" + enc(output.subfolder())
                + "&type=" + enc(output.type());
        try {
            HttpResponse<byte[]> response = httpClient.send(request(baseUrl, timeout, path).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("El motor local no permitio descargar la imagen: HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (!isPng(body)) {
                throw new IOException("El motor local devolvio una salida invalida. No se genero un PNG real.");
            }
            writeFinalPng(body, target, expectedWidth, expectedHeight);
            validatePngDimensions(target, expectedWidth, expectedHeight);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Descarga de imagen del motor local interrumpida.", ex);
        }
    }

    public void downloadArtifact(String baseUrl,
                                 Duration timeout,
                                 OutputArtifact output,
                                 Path target) throws IOException {
        if (output == null || output.filename().isBlank()) {
            throw new IOException("El motor local no entrego una referencia de archivo valida.");
        }
        String path = "/view?filename=" + enc(output.filename())
                + "&subfolder=" + enc(output.subfolder())
                + "&type=" + enc(output.type());
        try {
            HttpResponse<byte[]> response = httpClient.send(request(baseUrl, timeout, path).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("El motor local no permitio descargar " + output.filename()
                        + ": HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new IOException("El motor local devolvio un archivo vacio: " + output.filename());
            }
            Path normalizedTarget = target.toAbsolutePath().normalize();
            Files.createDirectories(normalizedTarget.getParent());
            Files.write(normalizedTarget, body);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Descarga del workflow visual interrumpida.", ex);
        }
    }

    private VisualEngineResult generateOnce(String baseUrl,
                                            Duration timeout,
                                            VisualEngineRequest request,
                                            ComfyUiWorkflowSpec workflow,
                                            int attempt,
                                            int maxAttempts,
                                            Consumer<String> progress) throws IOException {
        if (request == null) {
            throw new IOException("Solicitud visual invalida.");
        }
        ConnectionResult status = test(baseUrl, timeout);
        if (!status.available()) {
            throw new IOException(status.message() + "\n" + status.diagnostic());
        }
        Path outputDirectory = request.outputDirectory();
        if (outputDirectory == null) {
            throw new IOException("Selecciona una carpeta de salida para imagenes.");
        }
        Files.createDirectories(outputDirectory);
        progress(progress, "Enviando prompt (intento " + attempt + "/" + maxAttempts + ").");
        ComfyUiWorkflowSpec currentWorkflow = workflow == null ? ComfyUiWorkflowSpec.sd15() : workflow;
        String promptId = queuePrompt(baseUrl, timeout, request, currentWorkflow);
        progress(progress, "Esperando motor local (intento " + attempt + "/" + maxAttempts + ").");
        OutputImage output = waitForOutput(baseUrl, timeout, promptId, waitDuration(timeout, currentWorkflow));
        progress(progress, "Descargando PNG (intento " + attempt + "/" + maxAttempts + ").");
        Path target = outputDirectory.resolve(request.filenamePrefix() + ".png");
        int generationWidth = currentWorkflow.generationWidth(request);
        int generationHeight = currentWorkflow.generationHeight(request);
        if (generationWidth != request.targetWidth() || generationHeight != request.targetHeight()) {
            progress(progress, "Redimensionando a " + request.targetWidth() + "x" + request.targetHeight() + ".");
        }
        downloadPng(baseUrl, timeout, output, target, request.targetWidth(), request.targetHeight());
        return new VisualEngineResult(target, request.targetWidth(), request.targetHeight(), promptId,
                "prompt_id=" + promptId
                        + "\nbaseUrl=" + normalizedBaseUrl(baseUrl)
                        + "\nworkflow=" + currentWorkflow.kind()
                        + "\nbaseGeneration=" + generationWidth + "x" + generationHeight
                        + "\ntarget=" + request.targetWidth() + "x" + request.targetHeight()
                        + "\nresize=java2d-bicubic");
    }

    private static Optional<OutputImage> parseOutput(String body) {
        Matcher file = FILE.matcher(body == null ? "" : body);
        if (!file.find()) {
            return Optional.empty();
        }
        Matcher subfolder = SUBFOLDER.matcher(body);
        Matcher type = TYPE.matcher(body);
        return Optional.of(new OutputImage(file.group(1),
                subfolder.find() ? subfolder.group(1) : "",
                type.find() ? type.group(1) : "output"));
    }

    private static List<OutputArtifact> parseArtifacts(String body) {
        String source = body == null ? "" : body;
        ArrayList<OutputArtifact> artifacts = new ArrayList<>();
        Matcher files = FILE.matcher(source);
        ArrayList<Integer> starts = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        while (files.find()) {
            starts.add(files.start());
            names.add(files.group(1));
        }
        for (int index = 0; index < names.size(); index++) {
            int start = starts.get(index);
            int end = index + 1 < starts.size() ? starts.get(index + 1) : source.length();
            String section = source.substring(start, end);
            Matcher subfolder = SUBFOLDER.matcher(section);
            Matcher type = TYPE.matcher(section);
            artifacts.add(new OutputArtifact(
                    names.get(index),
                    subfolder.find() ? subfolder.group(1) : "",
                    type.find() ? type.group(1) : "output"));
        }
        return List.copyOf(artifacts);
    }

    private static HttpRequest.Builder request(String baseUrl, Duration timeout, String path) {
        return HttpRequest.newBuilder(URI.create(normalizedBaseUrl(baseUrl) + path))
                .timeout(normalizeTimeout(timeout));
    }

    private static Duration normalizeTimeout(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return Duration.ofSeconds(300);
        }
        return timeout;
    }

    static Duration waitDuration(Duration timeout, ComfyUiWorkflowSpec workflow) {
        long seconds = normalizeTimeout(timeout).toSeconds();
        return Duration.ofSeconds(Math.max(90, seconds));
    }

    private static String normalizedBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8188" : baseUrl.strip();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.toLowerCase(java.util.Locale.ROOT).startsWith("http") ? value : "http://" + value;
    }

    private static void validatePngDimensions(Path target, int expectedWidth, int expectedHeight) throws IOException {
        BufferedImage image = ImageIO.read(target.toFile());
        if (image == null) {
            throw new IOException("No se pudo validar el PNG generado por el motor local.");
        }
        if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
            throw new IOException("El motor genero " + image.getWidth() + "x" + image.getHeight()
                    + "; se esperaba " + expectedWidth + "x" + expectedHeight
                    + ". Revisa el workflow, perfil de salida o relacion de aspecto.");
        }
    }

    private static void writeFinalPng(byte[] png, Path target, int targetWidth, int targetHeight) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
        if (source == null) {
            throw new IOException("No se pudo leer el PNG generado por el motor local.");
        }
        Files.createDirectories(target.toAbsolutePath().normalize().getParent());
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            Files.write(target, png);
            return;
        }
        BufferedImage finalImage = fitToTarget(source, targetWidth, targetHeight);
        ImageIO.write(finalImage, "png", target.toFile());
    }

    private static BufferedImage fitToTarget(BufferedImage source, int targetWidth, int targetHeight) {
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle cover = coverRect(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
        g.drawImage(source, cover.x, cover.y, cover.width, cover.height, null);
        g.dispose();
        return canvas;
    }

    private static Rectangle coverRect(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        double scale = Math.max(targetWidth / (double) sourceWidth, targetHeight / (double) sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        return new Rectangle((targetWidth - width) / 2, (targetHeight - height) / 2, width, height);
    }

    private static boolean isPng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static String esc(String value) {
        return (value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String safePrefix(String value) {
        String normalized = (value == null ? "" : value).toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._/-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized.isBlank() ? "rife-intermedio" : normalized;
    }

    private static void progress(Consumer<String> progress, String message) {
        if (progress != null) {
            progress.accept(message);
        }
    }

    public record ConnectionResult(boolean available, String message, String diagnostic) {
        public ConnectionResult {
            message = message == null ? "" : message.strip();
            diagnostic = diagnostic == null ? "" : diagnostic.strip();
        }
    }

    public record OutputImage(String filename, String subfolder, String type) {
        public OutputImage {
            filename = filename == null ? "" : filename.strip();
            subfolder = subfolder == null ? "" : subfolder.strip();
            type = type == null || type.isBlank() ? "output" : type.strip();
        }
    }

    public record OutputArtifact(String filename, String subfolder, String type) {
        public OutputArtifact {
            filename = filename == null ? "" : filename.strip();
            subfolder = subfolder == null ? "" : subfolder.strip();
            type = type == null || type.isBlank() ? "output" : type.strip();
        }

        public String extension() {
            int dot = filename.lastIndexOf('.');
            return dot < 0 ? "" : filename.substring(dot).toLowerCase(java.util.Locale.ROOT);
        }

        public boolean video() {
            return switch (extension()) {
                case ".mp4", ".webm", ".mov", ".mkv" -> true;
                default -> false;
            };
        }

        public boolean image() {
            return switch (extension()) {
                case ".png", ".jpg", ".jpeg", ".webp" -> true;
                default -> false;
            };
        }
    }
}
