package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Downloads an explicit local theatre image package after user confirmation. */
public final class DownloadLocalTheatreImagePackageUseCase {
    private final HttpClient httpClient;
    private final InspectLocalModelFolderUseCase inspector;
    private final InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector;

    public DownloadLocalTheatreImagePackageUseCase() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), new InspectLocalModelFolderUseCase(), new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public DownloadLocalTheatreImagePackageUseCase(HttpClient httpClient, InspectLocalModelFolderUseCase inspector) {
        this(httpClient, inspector, new InspectLocalTheatreImageEngineArtifactsUseCase());
    }

    public DownloadLocalTheatreImagePackageUseCase(HttpClient httpClient,
                                                   InspectLocalModelFolderUseCase inspector,
                                                   InspectLocalTheatreImageEngineArtifactsUseCase artifactInspector) {
        this.httpClient = httpClient;
        this.inspector = inspector;
        this.artifactInspector = artifactInspector;
    }

    public LocalTheatreImagePackageDownloadReport download(OperationalSettings settings, Path applicationRoot) {
        return download(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public LocalTheatreImagePackageDownloadReport download(OperationalSettings settings, Path applicationRoot,
                                                          ModelSetupProgressListener progressListener) {
        return download(settings, applicationRoot, progressListener, false);
    }

    public LocalTheatreImagePackageDownloadReport download(OperationalSettings settings, Path applicationRoot,
                                                          ModelSetupProgressListener progressListener,
                                                          boolean forceReinstall) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(current.imageGeneration().preset());
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path target = root.resolve("models/image").normalize();
        ArrayList<String> downloaded = new ArrayList<>();
        try {
            new PrepareLocalTheatreImageRuntimeUseCase().prepare(current, root, progress);
            Files.createDirectories(target);
            ImagePackageDownloadPreflight preflight = preflight(current, root);
            if (!preflight.downloadable()) {
                return new LocalTheatreImagePackageDownloadReport(false, target, null, downloaded,
                        preflight.userMessage());
            }
            Path model = target.resolve(profile.checkpointName()).normalize();
            if (forceReinstall || !Files.isRegularFile(model)) {
                progress.onProgress((forceReinstall ? "Reinstalando paquete " : "Descargando paquete ")
                        + profile.displayName() + " (" + humanBytes(preflight.expectedBytes()) + ")...");
                download(preflight.uri(), model);
                downloaded.add(target.relativize(model).toString().replace('\\', '/'));
            }
            writeReferenceFiles(target, profile);
            ImageEngineArtifactInspectionReport artifacts = artifactInspector.inspect(current, root);
            ModelInspectionResult inspection = inspector.inspect(ModelFolderContract.localTheatreImage(), target);
            boolean success = artifacts.modelReady();
            String message = success
                    ? "Paquete de Imagen IA teatral descargado: " + profile.displayName()
                    + ". Modelo y workflow reales instalados; el runtime se valida aparte."
                    : "La descarga termino, pero el paquete aun no esta completo: "
                    + String.join(" - ", modelMissingRequirements(artifacts));
            return new LocalTheatreImagePackageDownloadReport(success, target, inspection, downloaded, message);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new LocalTheatreImagePackageDownloadReport(false, target, null, downloaded,
                    "No se pudo descargar Imagen IA teatral: " + ex.getMessage());
        }
    }

    public ImagePackageDownloadPreflight preflight(OperationalSettings settings, Path applicationRoot) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        ImageModelPackageProfile profile = ImageModelPackageProfile.fromPreset(current.imageGeneration().preset());
        if (profile.downloadUrl().isBlank() || profile.accessPolicy() == ImageModelPackageAccessPolicy.MANUAL_IMPORT) {
            return new ImagePackageDownloadPreflight(profile, null, profile.approximateBytes(), 0, false, false,
                    profile.displayName() + " se prepara importando un paquete local compatible.");
        }
        URI uri = URI.create(profile.downloadUrl());
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(25))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long bytes = resolveExpectedBytes(response, profile);
            int status = response.statusCode();
            boolean gated = status == 401 || status == 403 || isGated(response);
            if (gated) {
                return new ImagePackageDownloadPreflight(profile, uri, bytes, status, false, true,
                        profile.displayName() + " requiere acceso del proveedor o token. Importa un paquete local compatible si ya tienes licencia/acceso.");
            }
            if (status >= 300 && status < 400) {
                return new ImagePackageDownloadPreflight(profile, uri, bytes, status, false, profile.requiresAuthentication(),
                        "El proveedor devolvio una redireccion no resuelta para " + profile.displayName()
                                + ". Reintenta o importa el paquete local.");
            }
            if (status < 200 || status >= 300) {
                return new ImagePackageDownloadPreflight(profile, uri, bytes, status, false, profile.requiresAuthentication(),
                        "El proveedor devolvio HTTP " + status + " para " + profile.displayName()
                                + ". Reintenta, revisa acceso o importa el paquete local.");
            }
            return new ImagePackageDownloadPreflight(profile, uri, bytes, status, true, profile.requiresAuthentication(),
                    "Paquete disponible: " + profile.displayName() + " (" + humanBytes(bytes) + ").");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            boolean canAttempt = profile.accessPolicy() == ImageModelPackageAccessPolicy.PUBLIC;
            String message = canAttempt
                    ? "No se pudo verificar el tamano de " + profile.displayName()
                    + ". Puedes reintentar la descarga o importar el paquete local. Detalle: " + ex.getMessage()
                    : profile.displayName() + " requiere acceso del proveedor. Importa el paquete local si ya tienes licencia/acceso.";
            return new ImagePackageDownloadPreflight(profile, uri, profile.approximateBytes(), 0, canAttempt,
                    profile.requiresAuthentication(), message);
        }
    }

    private void download(URI uri, Path target) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(40))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() == 401 || response.statusCode() == 403 || isGated(response)) {
            throw new IOException("el proveedor requiere acceso, token o licencia; importa el paquete local si ya tienes permiso");
        }
        if (response.statusCode() >= 300 && response.statusCode() < 400) {
            throw new IOException("HTTP " + response.statusCode() + " al seguir la redireccion del proveedor");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " al descargar " + uri);
        }
        Files.createDirectories(target.getParent());
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        try (InputStream body = response.body()) {
            Files.copy(body, partial, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean isGated(HttpResponse<?> response) {
        String code = response.headers().firstValue("X-Error-Code").orElse("");
        String message = response.headers().firstValue("X-Error-Message").orElse("");
        return code.toLowerCase(Locale.ROOT).contains("gated")
                || message.toLowerCase(Locale.ROOT).contains("restricted")
                || message.toLowerCase(Locale.ROOT).contains("authenticated");
    }

    private static long resolveExpectedBytes(HttpResponse<?> response, ImageModelPackageProfile profile) {
        return response.headers().firstValue("content-length")
                .or(() -> response.headers().firstValue("X-Linked-Size"))
                .flatMap(DownloadLocalTheatreImagePackageUseCase::parseLong)
                .orElse(profile.approximateBytes());
    }

    private static java.util.Optional<Long> parseLong(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value.strip()));
        } catch (RuntimeException ex) {
            return java.util.Optional.empty();
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes <= 0) {
            return "tamano por verificar";
        }
        double gb = bytes / 1024.0 / 1024.0 / 1024.0;
        if (gb >= 1.0) {
            return String.format(Locale.ROOT, "%.2f GB", gb);
        }
        double mb = bytes / 1024.0 / 1024.0;
        return String.format(Locale.ROOT, "%.0f MB", mb);
    }

    private static void writeReferenceFiles(Path target, ImageModelPackageProfile profile) throws IOException {
        ImageEnginePresetSupport presetSupport = ImageEnginePresetSupportPolicy.forPresetId(profile.presetId());
        Files.createDirectories(target.resolve("workflows"));
        Files.createDirectories(target.resolve("adapters"));
        Files.createDirectories(target.resolve("loras"));
        if (presetSupport.builtInWorkflowAvailable()) {
            Files.writeString(target.resolve(ImageEnginePresetSupportPolicy.SD15_REFERENCE_WORKFLOW),
                    minimalWorkflow(presetSupport.checkpointName()),
                    StandardCharsets.UTF_8);
        } else {
            Files.writeString(target.resolve("workflows/README.txt"),
                    "Workflow pendiente para " + profile.displayName() + ".\n"
                            + presetSupport.userMessage() + "\n"
                            + "Archivo esperado: models/image/" + presetSupport.workflowName() + "\n",
                    StandardCharsets.UTF_8);
        }
        Files.deleteIfExists(target.resolve("adapters/reference-adapter-placeholder.bin"));
        Files.writeString(target.resolve("loras/README.txt"),
                "LoRA opcional avanzado para personajes, vestuario o estilo entrenado.\n"
                        + "El flujo base de DocuPodcast usa referencias/adaptadores antes de requerir entrenamiento LoRA.\n",
                StandardCharsets.UTF_8);
        String manifest = "{\n"
                + "  \"package\": \"docupodcast-theatre-image-" + profile.presetId().toLowerCase(Locale.ROOT) + "\",\n"
                + "  \"profile\": \"" + profile.presetId() + "\",\n"
                + "  \"provider\": \"" + profile.providerUrl() + "\",\n"
                + "  \"download\": \"" + profile.downloadUrl() + "\",\n"
                + "  \"model\": \"" + presetSupport.checkpointName() + "\",\n"
                + "  \"workflow\": \"" + presetSupport.workflowName() + "\",\n"
                + "  \"builtInWorkflow\": " + presetSupport.builtInWorkflowAvailable() + "\n"
                + "}\n";
        Files.writeString(target.resolve("model-manifest.json"), manifest, StandardCharsets.UTF_8);
        Files.writeString(target.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);
    }

    private static List<String> modelMissingRequirements(ImageEngineArtifactInspectionReport artifacts) {
        if (artifacts == null) {
            return List.of("No se pudo inspeccionar el paquete descargado.");
        }
        ArrayList<String> missing = new ArrayList<>();
        if (!artifacts.checkpointStatus().ready()) {
            missing.add("checkpoint real");
        }
        if (!artifacts.workflowStatus().ready()) {
            missing.add("workflow ComfyUI real");
        }
        if (missing.isEmpty()) {
            missing.addAll(artifacts.missingRequirements());
        }
        return missing;
    }

    private static String minimalWorkflow(String checkpointName) {
        String checkpoint = checkpointName == null ? "" : checkpointName.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\n"
                + "  \"1\": {\"class_type\": \"CheckpointLoaderSimple\", \"inputs\": {\"ckpt_name\": \"" + checkpoint + "\"}},\n"
                + "  \"2\": {\"class_type\": \"CLIPTextEncode\", \"inputs\": {\"text\": \"theatre stage smoke test\", \"clip\": [\"1\", 1]}},\n"
                + "  \"3\": {\"class_type\": \"CLIPTextEncode\", \"inputs\": {\"text\": \"text watermark\", \"clip\": [\"1\", 1]}},\n"
                + "  \"4\": {\"class_type\": \"EmptyLatentImage\", \"inputs\": {\"width\": 512, \"height\": 512, \"batch_size\": 1}},\n"
                + "  \"5\": {\"class_type\": \"KSampler\", \"inputs\": {\"seed\": 123456, \"steps\": 8, \"cfg\": 7.0, \"sampler_name\": \"euler\", \"scheduler\": \"normal\", \"denoise\": 1.0, \"model\": [\"1\", 0], \"positive\": [\"2\", 0], \"negative\": [\"3\", 0], \"latent_image\": [\"4\", 0]}},\n"
                + "  \"6\": {\"class_type\": \"VAEDecode\", \"inputs\": {\"samples\": [\"5\", 0], \"vae\": [\"1\", 2]}},\n"
                + "  \"7\": {\"class_type\": \"SaveImage\", \"inputs\": {\"filename_prefix\": \"docupodcast-smoke\", \"images\": [\"6\", 0]}}\n"
                + "}\n";
    }
}
