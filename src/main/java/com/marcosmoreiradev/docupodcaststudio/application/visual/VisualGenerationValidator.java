package com.marcosmoreiradev.docupodcaststudio.application.visual;

@FunctionalInterface
public interface VisualGenerationValidator {
    VisualGenerationValidationReport validate(VisualGenerationResult result);
}
