package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves installed API-format ComfyUI video workflows.
 *
 * <p>This service never downloads models or workflows. Missing packages produce
 * an actionable preflight error so the UI can request explicit installation.</p>
 */
public final class VisualClipWorkflowPackageService {
    public WorkflowPackage require(VisualClipGenerationProfile profile,
                                   String customWorkflowPath) throws IOException {
        VisualClipGenerationProfile selected = profile == null
                ? VisualClipGenerationProfile.WAN22_TI2V_5B_BALANCED
                : profile;
        Path applicationRoot = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        Path workflow = selected == VisualClipGenerationProfile.CUSTOM_COMFY_VIDEO
                ? configured(applicationRoot, customWorkflowPath)
                : applicationRoot.resolve(relativeWorkflow(selected)).normalize();
        if (workflow == null || !Files.isRegularFile(workflow)) {
            throw new IOException("Falta el workflow API de video para " + selected.displayName()
                    + ". Preparalo desde Configuracion > Generacion visual. No se descargara automaticamente.\n"
                    + "Ruta esperada: " + (workflow == null ? "sin configurar" : workflow));
        }
        String template = Files.readString(workflow, StandardCharsets.UTF_8);
        if (!template.contains("\"class_type\"") || !template.contains("{{START_IMAGE}}")) {
            throw new IOException("El workflow " + workflow.getFileName()
                    + " no es un workflow API compatible. Debe incluir class_type y {{START_IMAGE}}.");
        }
        return new WorkflowPackage(selected, workflow, template, modelId(selected));
    }

    private static Path configured(Path applicationRoot, String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return null;
        }
        Path path = Path.of(normalized);
        return path.isAbsolute() ? path.normalize() : applicationRoot.resolve(path).normalize();
    }

    private static String relativeWorkflow(VisualClipGenerationProfile profile) {
        return switch (profile) {
            case WAN22_TI2V_5B_BALANCED ->
                    "models/video/workflows/workflow-wan22-ti2v-5b-api.json";
            case WAN22_I2V_14B_QUALITY ->
                    "models/video/workflows/workflow-wan22-i2v-14b-api.json";
            case LTX23_I2V_PORTRAIT ->
                    "models/video/workflows/workflow-ltx23-i2v-portrait-api.json";
            case CUSTOM_COMFY_VIDEO -> "";
        };
    }

    private static String modelId(VisualClipGenerationProfile profile) {
        return switch (profile) {
            case WAN22_TI2V_5B_BALANCED -> "wan2.2-ti2v-5b";
            case WAN22_I2V_14B_QUALITY -> "wan2.2-i2v-14b";
            case LTX23_I2V_PORTRAIT -> "ltx-2.3-i2v";
            case CUSTOM_COMFY_VIDEO -> "custom-comfy-video";
        };
    }

    public record WorkflowPackage(
            VisualClipGenerationProfile profile,
            Path workflowPath,
            String template,
            String modelId
    ) {
    }
}
