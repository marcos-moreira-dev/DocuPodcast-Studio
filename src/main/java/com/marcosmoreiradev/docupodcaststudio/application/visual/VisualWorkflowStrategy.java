package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.io.IOException;

/** Resolves an executable local workflow without knowledge of the consuming category. */
@FunctionalInterface
public interface VisualWorkflowStrategy {
    ComfyUiWorkflowSpec resolve(VisualGenerationRequest request, VisualModelManifest manifest) throws IOException;
}
