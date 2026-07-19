package com.marcosmoreiradev.docupodcaststudio.application.visual;

/** Built-in ComfyUI workflows supported by the shared visual engine client. */
public enum ComfyUiWorkflowKind {
    SD15_CHECKPOINT,
    SDXL_REFERENCE_COMPONENTS,
    FLUX1_DEV_COMPONENTS,
    FLUX_KONTEXT_COMPONENTS;

    public boolean flux() {
        return this == FLUX1_DEV_COMPONENTS || this == FLUX_KONTEXT_COMPONENTS;
    }
}
