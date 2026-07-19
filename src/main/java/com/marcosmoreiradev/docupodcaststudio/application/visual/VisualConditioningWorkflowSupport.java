package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/** Honest preflight for reference-conditioned ComfyUI workflows. */
public final class VisualConditioningWorkflowSupport {
    public Path requireTemplate(ComfyUiWorkflowSpec workflow, VisualEngineRequest request) throws IOException {
        if (request == null || request.conditioningReferences().isEmpty()) return null;
        Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        ComfyUiWorkflowKind kind = workflow == null
                ? ComfyUiWorkflowKind.SD15_CHECKPOINT
                : workflow.kind();
        boolean flux = kind.flux();
        String relativeTemplate = switch (kind) {
            case FLUX_KONTEXT_COMPONENTS -> "models/image/workflows/workflow-flux-kontext-reference-api.json";
            case FLUX1_DEV_COMPONENTS -> "models/image/workflows/workflow-flux-pulid-reference-api.json";
            case SDXL_REFERENCE_COMPONENTS -> "models/image/workflows/workflow-sdxl-ipadapter-reference-api.json";
            case SD15_CHECKPOINT -> "models/image/workflows/workflow-sd15-ipadapter-reference-api.json";
        };
        Path template = root.resolve(relativeTemplate);
        if (!Files.isRegularFile(template)) {
            throw new IOException("La generacion solicita referencias reales, pero falta el workflow API gestionado: "
                    + template + ". Importa el workflow desde Configuracion; no se generara una imagen solo por prompt.");
        }
        Path customNodes = root.resolve("tools/image/ComfyUI/custom_nodes");
        if (request.requiresIdentityConditioning()) {
            String token = flux ? "pulid" : "ipadapter";
            if (kind != ComfyUiWorkflowKind.FLUX_KONTEXT_COMPONENTS
                    && !containsEntry(customNodes, token)) {
                String component = flux ? "PuLID-FLUX" : "IP-Adapter Plus";
                throw new IOException("Falta el nodo ComfyUI de identidad " + component
                        + " en " + customNodes + ". La identidad de personajes no se degradara silenciosamente a texto.");
            }
            if (!flux && !containsFile(root.resolve("models/image"), "clip_vision")) {
                throw new IOException("Falta el modelo CLIP Vision requerido por IP-Adapter. Importalo desde Configuracion.");
            }
        }
        if (request.hasDrawnGuide()
                && !containsEntry(customNodes, "controlnet")
                && !containsFile(root.resolve("models/image"), "controlnet")) {
            throw new IOException("El frame dibujado requiere condicion estructural ControlNet scribble/canny. "
                    + "Faltan nodos o modelos ControlNet; el boceto no se usara como estilo ni se ignorara.");
        }
        return template;
    }

    private static boolean containsEntry(Path directory, String token) {
        if (!Files.isDirectory(directory)) return false;
        String expected = token.toLowerCase(Locale.ROOT);
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.anyMatch(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).contains(expected));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean containsFile(Path root, String token) {
        if (!Files.isDirectory(root)) return false;
        String expected = token.toLowerCase(Locale.ROOT);
        try (Stream<Path> entries = Files.walk(root, 4)) {
            return entries.filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().toLowerCase(Locale.ROOT).contains(expected));
        } catch (IOException ignored) {
            return false;
        }
    }
}
