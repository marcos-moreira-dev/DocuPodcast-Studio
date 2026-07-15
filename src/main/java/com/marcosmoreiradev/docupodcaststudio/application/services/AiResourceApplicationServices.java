package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.resources.AiResourceCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.resources.ExportAiResourcesUseCase;

/** AI resource export use cases. */
public record AiResourceApplicationServices(
        AiResourceCatalog catalog,
        ExportAiResourcesUseCase exportAiResources
) {
}
