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
        boolean flux = workflow != null && workflow.kind() == ComfyUiWorkflowKind.FLUX1_DEV_COMPONENTS;
        Path template = root.resolve(flux
                ? "models/image/workflows/workflow-flux-pulid-reference-api.json"
                : "models/image/workflows/workflow-sd15-ipadapter-reference-api.json");
        if (!Files.isRegularFile(template)) {
            throw new IOException("La generacion solicita referencias reales, pero falta el workflow API gestionado: "
                    + template + ". Importa el workflow desde Configuracion; no se generara una imagen solo por prompt.");
        }
        Path customNodes = root.resolve("tools/image/ComfyUI/custom_nodes");
        if (request.requiresIdentityConditioning()) {
            String token = flux ? "pulid" : "ipadapter";
            if (!containsEntry(customNodes, token)) {
                throw new IOException("Falta el nodo ComfyUI de identidad " + (flux ? "PuLID-FLUX" : "IP-Adapter Plus")
                        + " en " + customNodes + ". La identidad de personajes no se degradara silenciosamente a texto.");
            }
            if (!flux && !containsFile(root.resolve("models/image"), "clip_vision")) {
                throw new IOException("Falta el modelo CLIP Vision requerido por IP-Adapter. Importalo desde Configuracion.");
            }
        }
        if (request.hasStructureGuide()
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
