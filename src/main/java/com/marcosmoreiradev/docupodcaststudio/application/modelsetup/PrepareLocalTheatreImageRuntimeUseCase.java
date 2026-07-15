package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates app-local folders for the image engine without pretending that runtime or workflows are installed. */
public final class PrepareLocalTheatreImageRuntimeUseCase {
    public LocalTheatreImagePreparationReport prepare(OperationalSettings settings, Path applicationRoot) {
        return prepare(settings, applicationRoot, ModelSetupProgressListener.noop());
    }

    public LocalTheatreImagePreparationReport prepare(OperationalSettings settings, Path applicationRoot,
                                                     ModelSetupProgressListener progressListener) {
        ModelSetupProgressListener progress = progressListener == null ? ModelSetupProgressListener.noop() : progressListener;
        Path root = applicationRoot == null ? Path.of(".").toAbsolutePath().normalize() : applicationRoot.toAbsolutePath().normalize();
        Path runtime = root.resolve("tools/image").normalize();
        Path models = root.resolve("models/image").normalize();
        try {
            progress.onProgress("Preparando carpetas locales para Imagen IA teatral...");
            Files.createDirectories(runtime);
            Files.createDirectories(models.resolve("workflows"));
            Files.createDirectories(models.resolve("adapters"));
            Files.createDirectories(models.resolve("loras"));
            Files.createDirectories(models.resolve("vae"));
            Files.createDirectories(models.resolve("text_encoders"));
            Files.createDirectories(root.resolve("generated/teatro-ia"));
            writeIfMissing(runtime.resolve("README.txt"),
                    "Runtime local de imagen IA teatral.\n"
                            + "Coloca aqui un runtime compatible con lanzador local: start-image-engine.bat, run.bat, ComfyUI.bat o main.py con Python local.\n"
                            + "La implementacion tecnica puede usar un backend local; no uses Python global de forma implicita.\n");
            writeIfMissing(models.resolve("adapters/README.txt"),
                    "Coloca aqui adaptadores de referencia para cara, vestuario y consistencia.\n"
                            + "El flujo base usa referencias visuales; no requiere entrenar LoRA para cada obra.\n");
            writeIfMissing(models.resolve("workflows/README.txt"),
                    "Workflows ComfyUI esperados por DocuPodcast Studio.\n"
                            + "- SD 1.5 integrado: workflows/workflow-sd15-reference.json\n"
                            + "- Modelo grande Flux: workflows/workflow-flux-reference.json, flux1-dev.safetensors, vae/ae.safetensors y text_encoders/clip_l.safetensors + T5XXL\n"
                            + "- Workflow personalizado: workflows/workflow-custom-comfy.json\n"
                            + "No coloques placeholders: cada JSON debe ser un workflow real con nodos de carga, sampler y salida PNG.\n");
            writeIfMissing(models.resolve("vae/README.txt"),
                    "VAE de modelos de imagen. Para FLUX.1-dev coloca aqui ae.safetensors.\n");
            writeIfMissing(models.resolve("text_encoders/README.txt"),
                    "Text encoders de modelos grandes. Para FLUX.1-dev coloca aqui clip_l.safetensors y t5xxl_fp16.safetensors o t5xxl_fp8_e4m3fn.safetensors.\n");
            writeIfMissing(models.resolve("loras/README.txt"),
                    "LoRA es opcional avanzado para personajes, estilos o vestuario entrenado.\n"
                            + "Usalo cuando las referencias y adaptadores no basten para mantener identidad visual.\n");
            writeFluxWorkflow(models.resolve("workflows/workflow-flux-reference.json"));
            writeExtraModelPaths(runtime.resolve("extra_model_paths.yaml"), models);
            progress.onProgress("Carpetas base preparadas. Falta instalar o importar un runtime compatible para arrancar el motor.");
            return new LocalTheatreImagePreparationReport(true, runtime, models,
                    "Carpetas base de Imagen IA teatral preparadas. Esto no instala runtime ni workflow. Instala o importa un runtime compatible en tools/image y descarga o importa un paquete real de modelos/workflow para generar.");
        } catch (IOException ex) {
            return new LocalTheatreImagePreparationReport(false, runtime, models,
                    "No se pudo preparar Imagen IA teatral local: " + ex.getMessage());
        }
    }

    private static void writeIfMissing(Path path, String content) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void writeFluxWorkflow(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        String diagnostic = "{\n"
                + "  \"format\": \"docupodcast-comfy-api-reference\",\n"
                + "  \"preset\": \"HIGH_QUALITY_FLUX\",\n"
                + "  \"builtIn\": true,\n"
                + "  \"nodes\": [\"UNETLoader\", \"DualCLIPLoader\", \"VAELoader\", \"CLIPTextEncodeFlux\", \"ModelSamplingFlux\", \"SamplerCustomAdvanced\", \"SaveImage\"]\n"
                + "}\n";
        Files.writeString(path, diagnostic, StandardCharsets.UTF_8);
    }

    private static void writeExtraModelPaths(Path path, Path models) throws IOException {
        Files.createDirectories(path.getParent());
        String base = models.toAbsolutePath().normalize().toString().replace('\\', '/');
        String yaml = "docupodcast_studio:\n"
                + "  base_path: \"" + base.replace("\"", "\\\"") + "\"\n"
                + "  checkpoints: .\n"
                + "  diffusion_models: .\n"
                + "  vae: vae\n"
                + "  text_encoders: text_encoders\n"
                + "  loras: loras\n";
        Files.writeString(path, yaml, StandardCharsets.UTF_8);
    }
}
