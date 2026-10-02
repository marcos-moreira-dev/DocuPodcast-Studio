package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

/** Provider-specific administration for local image generation. */
final class ComfyUiImageEngineAdministration extends AbstractLocalEngineAdministration
        implements ManagedDownloadAdministration, AutoCloseable {
    private static final EngineActionId IMPORT_MODELS = new EngineActionId("import-models");
    private static final EngineActionId IMPORT_WORKFLOW = new EngineActionId("import-workflow");
    private static final EngineActionId IMPORT_RUNTIME = new EngineActionId("import-runtime");
    static final EngineActionId RIFE_SMOKE_TEST = new EngineActionId("rife-smoke-test");
    static final EngineActionId DOWNLOAD_IPADAPTER_NODE = new EngineActionId("download-ipadapter-node");
    static final EngineActionId DOWNLOAD_IPADAPTER_MODEL = new EngineActionId("download-ipadapter-model");
    static final EngineActionId DOWNLOAD_CLIP_VISION = new EngineActionId("download-clip-vision");
    static final EngineActionId DOWNLOAD_SCRIBBLE = new EngineActionId("download-controlnet-scribble");
    static final EngineActionId DOWNLOAD_DREAMSHAPER = new EngineActionId("download-dreamshaper-8");
    static final EngineActionId IMPORT_CONDITIONING = new EngineActionId("import-conditioning-package");
    private static final ResourceSpec IPADAPTER_NODE = new ResourceSpec(
            "comfyui-ipadapter-b188a6cb.zip",
            "https://github.com/comfyorg/comfyui-ipadapter/archive/b188a6cb39b512a9c6da7235b880af42c78ccd0d.zip",
            "tools/image/downloads/comfyui-ipadapter-b188a6cb.zip",
            307_760L,
            "190c6fda69de20119298a1754eb922333d92119c2dc44ea7303dc1b07ded9670",
            "GPL-3.0");
    private static final ResourceSpec IPADAPTER_MODEL = new ResourceSpec(
            "ip-adapter-plus_sd15.safetensors",
            "https://huggingface.co/h94/IP-Adapter/resolve/main/models/ip-adapter-plus_sd15.safetensors",
            "tools/image/ComfyUI/models/ipadapter/ip-adapter-plus_sd15.safetensors",
            98_183_288L,
            "a1c250be40455cc61a43da1201ec3f1edaea71214865fb47f57927e06cbe4996",
            "Apache-2.0");

    @Override
    public void close() {
        process.close();
    }
    private static final ResourceSpec CLIP_VISION = new ResourceSpec(
            "CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors",
            "https://huggingface.co/h94/IP-Adapter/resolve/main/models/image_encoder/model.safetensors",
            "tools/image/ComfyUI/models/clip_vision/CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors",
            2_528_373_448L,
            "6ca9667da1ca9e0b0f75e46bb030f7e011f44f86cbfb8d5a36590fcd7507b030",
            "Apache-2.0");
    private static final ResourceSpec SCRIBBLE = new ResourceSpec(
            "control_v11p_sd15_scribble_fp16.safetensors",
            "https://huggingface.co/ckpt/ControlNet-v1-1/resolve/main/control_v11p_sd15_scribble_fp16.safetensors",
            "tools/image/ComfyUI/models/controlnet/control_v11p_sd15_scribble_fp16.safetensors",
            722_601_100L,
            "99edfd25b54c18c0ab19fba8c5618f741aac1f8c3101e7fa62cce925ad87ae68",
            "CreativeML OpenRAIL-M");
    private static final ResourceSpec DREAMSHAPER = new ResourceSpec(
            "DreamShaper_8_pruned.safetensors",
            "https://huggingface.co/Lykon/DreamShaper/resolve/main/DreamShaper_8_pruned.safetensors",
            "models/image/DreamShaper_8_pruned.safetensors",
            2_132_625_894L,
            "879db523c30d3b9017143d56705015e15a2cb5628762c11d086fed9538abd7fd",
            "CreativeML OpenRAIL-M");
    private final ImageGenerationEngine image;
    private final ComfyUiManagedProcess process;
    private final Function<String, ComfyUiLaunchProfile> launchProfiles;
    private final ThreadLocal<ManagedDownloadDecision> downloadDecision =
            ThreadLocal.withInitial(() -> ManagedDownloadDecision.USE_EXISTING);

    ComfyUiImageEngineAdministration(ImageGenerationEngine image, RuntimeAssetCatalog assets,
                                     ComfyUiManagedProcess process,
                                     Function<String, ComfyUiLaunchProfile> launchProfiles) {
        super(image, assets, List.of(
                action(IMPORT_RUNTIME, "Importar runtime ComfyUI",
                        "Importa un runtime local existente; no se presenta como una descarga o instalación.", true,
                        directory("runtimeDirectory", "Runtime ComfyUI", true)),
                action(IMPORT_MODELS, "Importar modelos de imagen",
                        "Copia modelos de imagen al catalogo administrado.", true,
                        directory("modelsDirectory", "Directorio de modelos", true)),
                action(IMPORT_WORKFLOW, "Importar workflow de imagen",
                        "Instala el workflow de imagen publicado por el adaptador.", true,
                        file("workflowFile", "Workflow JSON", true)),
                managedDownload(DOWNLOAD_IPADAPTER_NODE, "Descargar nodo IP-Adapter regional",
                        "Prepara el nodo fijado para identidad y vestuario por regiones.", IPADAPTER_NODE),
                managedDownload(DOWNLOAD_IPADAPTER_MODEL, "Descargar IP-Adapter Plus SD 1.5",
                        "Modelo visual de identidad y vestuario para SD 1.5.", IPADAPTER_MODEL),
                managedDownload(DOWNLOAD_CLIP_VISION, "Descargar CLIP Vision ViT-H",
                        "Codificador visual requerido por IP-Adapter Plus.", CLIP_VISION),
                managedDownload(DOWNLOAD_SCRIBBLE, "Descargar ControlNet Scribble",
                        "Guía estructural para la variante dibujada del storyboard.", SCRIBBLE),
                managedDownload(DOWNLOAD_DREAMSHAPER, "Descargar DreamShaper 8",
                        "Checkpoint SD 1.5 estético preferido para generación contextual de alta calidad.",
                        DREAMSHAPER),
                action(IMPORT_CONDITIONING, "Importar paquete de consistencia de personajes",
                        "Importa y valida modelos y nodo ya descargados.", true,
                        directory("conditioningDirectory", "Directorio del paquete", true)),
                action(EngineActionId.START, "Iniciar ComfyUI", "Inicia el runtime fijo y compartido.", false),
                action(EngineActionId.STOP, "Detener ComfyUI", "Detiene el proceso administrado compartido.", true),
                action(EngineActionId.REPAIR, "Limpiar preparación incompleta",
                        "Limpia exclusivamente staging incompleto de este adaptador.", true),
                action(EngineActionId.SMOKE_TEST, "Generar imagen de prueba",
                        "Produce una imagen real corta usando el preset publicado.", false),
                action(RIFE_SMOKE_TEST, "Probar interpolacion RIFE",
                        "Produce un frame central real con dos referencias sinteticas.", false)));
        this.image = image;
        this.process = process;
        this.launchProfiles = launchProfiles == null
                ? requested -> ComfyUiLaunchProfile.automatic(ComfyUiMemoryProfile.from(requested))
                : launchProfiles;
    }

    @Override public ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException {
        ResourceSpec resource = resourceFor(request == null ? null : request.actionId());
        if (DOWNLOAD_IPADAPTER_NODE.equals(request.actionId()) && nodeReady()) {
            return new ManagedDownloadPreflight(
                    resource.fileName(),
                    ManagedDownloadState.VALID,
                    assets.require(ComfyUiImageEngine.ID, "ipAdapterNode"),
                    resource.bytes(),
                    resource.bytes(),
                    resource.sha256(),
                    resource.sha256(),
                    resource.license(),
                    resource.url(),
                    "La revisión fijada del nodo IP-Adapter ya está instalada y es ejecutable.");
        }
        Path target = runtime.target(resource.relativeTarget());
        Path partial = runtime.staging(engineId().value()).resolve(target.getFileName() + ".partial");
        ManagedDownloadPreflight result = ManagedDownloadPreflightInspector.inspect(
                resource.fileName(), target, partial, resource.bytes(), resource.sha256(),
                resource.license(), resource.url());
        if (DOWNLOAD_IPADAPTER_NODE.equals(request.actionId()) && result.valid() && !nodeReady()) {
            return new ManagedDownloadPreflight(result.resourceId(), ManagedDownloadState.INVALID,
                    result.location(), result.expectedBytes(), result.actualBytes(),
                    result.expectedSha256(), result.actualSha256(), result.license(), result.source(),
                    "El ZIP es válido, pero el nodo IP-Adapter no está publicado en custom_nodes.");
        }
        return result;
    }

    @Override public EngineActionResult executeDownload(
            EngineActionRequest request,
            ManagedDownloadDecision decision,
            ExecutionContext context) throws IOException, InterruptedException {
        downloadDecision.set(decision == null ? ManagedDownloadDecision.CANCEL : decision);
        try {
            return execute(request, context);
        } finally {
            downloadDecision.remove();
        }
    }

    @Override protected List<GenerationArtifact> perform(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (IMPORT_RUNTIME.equals(request.actionId())) {
            runtime.importDirectory(inputPath(request, "runtimeDirectory"), "tools/image", context);
        } else if (IMPORT_MODELS.equals(request.actionId())) {
            runtime.importDirectory(inputPath(request, "modelsDirectory"), "models/image", context);
        } else if (IMPORT_WORKFLOW.equals(request.actionId())) {
            runtime.importFile(inputPath(request, "workflowFile"),
                    "models/image/workflows/workflow-sd15-reference.json", context);
        } else if (isManagedImageDownload(request.actionId())) {
            ResourceSpec resource = resourceFor(request.actionId());
            if (DOWNLOAD_IPADAPTER_NODE.equals(request.actionId())
                    && ManagedDownloadDecision.USE_EXISTING.equals(downloadDecision.get())
                    && nodeReady()) {
                context.progress().report("REUSING", 1.0,
                        "El nodo IP-Adapter administrado ya está instalado; se reutiliza sin acceder a la red.");
                return List.of();
            }
            runtime.downloadVerified(resource.url(), resource.relativeTarget(), engineId().value(),
                    context, downloadDecision.get(), path -> verify(path, resource));
            if (DOWNLOAD_IPADAPTER_NODE.equals(request.actionId())) {
                runtime.installZipDirectoryVerified(runtime.target(resource.relativeTarget()),
                        "tools/image/ComfyUI/custom_nodes/comfyui-ipadapter",
                        engineId().value(), context, ComfyUiImageEngineAdministration::verifyNodeDirectory);
            }
            process.reloadAfterManagedCatalogChange(context);
        } else if (IMPORT_CONDITIONING.equals(request.actionId())) {
            importConditioningPackage(inputPath(request, "conditioningDirectory"), context);
            process.reloadAfterManagedCatalogChange(context);
        } else if (EngineActionId.START.equals(request.actionId())) {
            process.start(assets, launchProfiles.apply(request.inputs().get("memoryProfile")), context);
        } else if (EngineActionId.STOP.equals(request.actionId())) {
            process.stop(context);
        } else if (EngineActionId.REPAIR.equals(request.actionId())) {
            runtime.repair(engineId().value(), context);
        } else if (EngineActionId.SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/comfyui-image");
            Files.createDirectories(output);
            Set<Path> previousRuntimeOutputs = smokeRuntimeOutputs();
            try {
                EnginePresetId preset = new EnginePresetId(request.inputs().getOrDefault("presetId", "draft"));
                if (image instanceof ComfyUiImageEngine comfy) {
                    EngineReadiness presetReadiness = comfy.inspectPresetReadiness(preset);
                    if (!presetReadiness.ready()) {
                        throw new IOException(presetReadiness.summary() + " "
                                + String.join(" ", presetReadiness.issues()) + " "
                                + String.join(" ", presetReadiness.recommendedActions()));
                    }
                }
                long seed = parseLong(request.inputs().get("seed"), 424242L);
                String prompt = request.inputs().getOrDefault("prompt",
                        "Ilustracion simple de un libro morado sobre fondo blanco").strip();
                String negativePrompt = request.inputs().getOrDefault("negativePrompt",
                        "texto, marca de agua").strip();
                int width = parseInt(request.inputs().get("width"), 512, 256, 2048);
                int height = parseInt(request.inputs().get("height"), 512, 256, 2048);
                int deliveryWidth = parseInt(request.inputs().get("deliveryWidth"), width, 64, 4096);
                int deliveryHeight = parseInt(request.inputs().get("deliveryHeight"), height, 64, 4096);
                String label = safeLabel(request.inputs().getOrDefault("label", "image-smoke"));
                ImageGenerationResult result = image.generate(new ImageGenerationRequest(
                        prompt, negativePrompt,
                        width, height, List.of(), output, label + "-" + preset.value(),
                        Map.of("deliveryWidth", Integer.toString(deliveryWidth),
                                "deliveryHeight", Integer.toString(deliveryHeight)),
                        preset, List.of(), seed, 1), context);
                return result.images().stream().map(path -> new GenerationArtifact("image", path.toUri(),
                        Map.of("purpose", "smoke", "presetId", preset.value(),
                                "seed", Long.toString(seed), "width", Integer.toString(width),
                                "height", Integer.toString(height), "label", label))).toList();
            } finally {
                cleanupNewSmokeRuntimeOutputs(previousRuntimeOutputs);
            }
        } else if (RIFE_SMOKE_TEST.equals(request.actionId())) {
            Path output = runtime.target("diagnostics/rife");
            Path frame = new RifeFrameInterpolator("http://127.0.0.1:8188",
                    assets.require(ComfyUiImageEngine.ID, "rife")).smoke(output, context);
            return List.of(new GenerationArtifact("image", frame.toUri(),
                    Map.of("purpose", "rife-smoke", "model", RifeFrameInterpolator.MODEL,
                            "frameRole", "middle")));
        }
        return List.of();
    }

    private void importConditioningPackage(Path directory, ExecutionContext context)
            throws IOException, InterruptedException {
        Path root = directory.toAbsolutePath().normalize();
        importNamed(root, IPADAPTER_MODEL, context);
        importNamed(root, CLIP_VISION, context);
        importNamed(root, SCRIBBLE, context);
        Path node = findDirectory(root, "IPAdapterPlus.py");
        if (node == null) {
            throw new IOException("El paquete no contiene el nodo IP-Adapter (IPAdapterPlus.py).");
        }
        runtime.importDirectory(node, "tools/image/ComfyUI/custom_nodes/comfyui-ipadapter", context);
        verifyNodeDirectory(runtime.target("tools/image/ComfyUI/custom_nodes/comfyui-ipadapter"));
    }

    private void importNamed(Path root, ResourceSpec resource, ExecutionContext context)
            throws IOException, InterruptedException {
        Path source;
        try (var paths = Files.walk(root)) {
            source = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(resource.fileName()))
                    .findFirst().orElseThrow(() -> new IOException(
                            "El paquete no contiene " + resource.fileName() + "."));
        }
        runtime.importFileVerified(source, resource.relativeTarget(), context,
                path -> verify(path, resource));
    }

    private static Path findDirectory(Path root, String marker) throws IOException {
        try (var paths = Files.walk(root, 5)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(marker))
                    .map(Path::getParent).findFirst().orElse(null);
        }
    }

    private boolean nodeReady() {
        try {
            verifyNodeDirectory(assets.require(ComfyUiImageEngine.ID, "ipAdapterNode"));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void verifyNodeDirectory(Path directory) throws IOException {
        if (directory == null || !Files.isRegularFile(directory.resolve("IPAdapterPlus.py"))
                || !Files.isRegularFile(directory.resolve("__init__.py"))) {
            throw new IOException("El nodo IP-Adapter no contiene sus archivos ejecutables esperados.");
        }
    }

    private static void verify(Path path, ResourceSpec resource) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != resource.bytes()
                || !resource.sha256().equalsIgnoreCase(ManagedDownloadPreflightInspector.sha256(path))) {
            throw new IOException("El recurso no coincide con el tamaño/SHA-256 oficial: "
                    + resource.fileName());
        }
    }

    private static boolean isManagedImageDownload(EngineActionId actionId) {
        return DOWNLOAD_IPADAPTER_NODE.equals(actionId)
                || DOWNLOAD_IPADAPTER_MODEL.equals(actionId)
                || DOWNLOAD_CLIP_VISION.equals(actionId)
                || DOWNLOAD_SCRIBBLE.equals(actionId)
                || DOWNLOAD_DREAMSHAPER.equals(actionId);
    }

    private static ResourceSpec resourceFor(EngineActionId actionId) {
        if (DOWNLOAD_IPADAPTER_NODE.equals(actionId)) return IPADAPTER_NODE;
        if (DOWNLOAD_IPADAPTER_MODEL.equals(actionId)) return IPADAPTER_MODEL;
        if (DOWNLOAD_CLIP_VISION.equals(actionId)) return CLIP_VISION;
        if (DOWNLOAD_SCRIBBLE.equals(actionId)) return SCRIBBLE;
        if (DOWNLOAD_DREAMSHAPER.equals(actionId)) return DREAMSHAPER;
        throw new IllegalArgumentException("La acción no descarga una dependencia condicionada.");
    }

    private static EngineActionDescriptor managedDownload(EngineActionId id,
                                                          String name,
                                                          String description,
                                                          ResourceSpec resource) {
        return new EngineActionDescriptor(id, name, description, List.of(), true,
                resource.bytes(), Map.of(
                "license", resource.license(),
                "sha256", resource.sha256(),
                "source", resource.url(),
                "managedDownload", "true",
                "component", DOWNLOAD_DREAMSHAPER.equals(id)
                        ? "Calidad contextual · DreamShaper 8"
                        : "Consistencia de personajes · IP-Adapter"));
    }

    private static long parseLong(String value, long fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback, int minimum, int maximum) {
        try {
            int parsed = value == null || value.isBlank() ? fallback : Integer.parseInt(value);
            return Math.max(minimum, Math.min(maximum, parsed));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String safeLabel(String value) {
        String normalized = value == null ? "" : value.strip().replaceAll("[^a-zA-Z0-9._-]", "-");
        return normalized.isBlank() ? "image-smoke" : normalized;
    }

    private record ResourceSpec(String fileName, String url, String relativeTarget,
                                long bytes, String sha256, String license) { }

    private Set<Path> smokeRuntimeOutputs() throws IOException {
        Path directory = assets.require(ComfyUiImageEngine.ID, "runtime").resolve("output").normalize();
        if (!Files.isDirectory(directory)) return Set.of();
        try (var paths = Files.list(directory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("image-smoke_"))
                    .map(Path::toAbsolutePath).map(Path::normalize).collect(Collectors.toUnmodifiableSet());
        }
    }

    private void cleanupNewSmokeRuntimeOutputs(Set<Path> previous) throws IOException {
        Set<Path> current = smokeRuntimeOutputs();
        for (Path path : current) if (!previous.contains(path)) Files.deleteIfExists(path);
    }
}
