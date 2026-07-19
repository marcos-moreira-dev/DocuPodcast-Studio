package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxLicenseAcceptanceStore;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxMemoryPreflight;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.FluxModelBundle;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;

import java.io.IOException;
import java.nio.file.Path;

/** Resolves a category-neutral image profile to its installed ComfyUI workflow. */
public final class VisualImageWorkflowResolver {
    public ComfyUiWorkflowSpec resolve(VisualGenerationProfile profile,
                                       VisualEngineRequest request) throws IOException {
        VisualGenerationProfile selected = profile == null
                ? VisualGenerationProfile.DIAGNOSTIC_SD15
                : profile;
        if (selected == VisualGenerationProfile.PRODUCTION_SDXL_REFERENCE) {
            return ComfyUiWorkflowSpec.sdxlReference(request.checkpointName());
        }
        if (selected == VisualGenerationProfile.DIAGNOSTIC_SD15) {
            return ComfyUiWorkflowSpec.sd15();
        }
        if (selected == VisualGenerationProfile.CUSTOM_COMFY_WORKFLOW) {
            throw new IOException("El workflow ComfyUI personalizado de imagen debe configurarse antes de generar.");
        }
        Path root = RuntimePathResolver.defaultResolver().resolve().applicationRoot();
        if (!new FluxLicenseAcceptanceStore().accepted(root)) {
            throw new IOException("Confirma la licencia FLUX.1-Kontext-dev en Configuracion antes de generar.");
        }
        FluxModelBundle bundle = FluxModelBundle.inspectKontext(root);
        if (!bundle.ready()) {
            throw new IOException("Faltan componentes FLUX Kontext: "
                    + String.join(", ", bundle.missingComponents()));
        }
        FluxMemoryPreflight.Report memory = new FluxMemoryPreflight().inspect(root);
        if (!memory.ready()) {
            throw new IOException(memory.userMessage() + "\n" + memory.diagnostic());
        }
        return ComfyUiWorkflowSpec.fluxKontextForTarget(
                bundle.modelName(),
                bundle.vaeName(),
                bundle.clipLName(),
                bundle.t5Name(),
                request.targetWidth(),
                request.targetHeight());
    }
}
