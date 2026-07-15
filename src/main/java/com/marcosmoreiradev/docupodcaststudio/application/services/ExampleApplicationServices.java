package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.examples.CreateExampleProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.examples.ExampleProjectCatalog;
import com.marcosmoreiradev.docupodcaststudio.application.examples.InspectExampleProjectReadinessUseCase;

/** Application services for bundled demo examples. */
public record ExampleApplicationServices(
        ExampleProjectCatalog catalog,
        CreateExampleProjectUseCase createExampleProject,
        InspectExampleProjectReadinessUseCase inspectReadiness
) {
}
