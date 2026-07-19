package com.marcosmoreiradev.docupodcaststudio.application.visual;

/** Category adapter that translates domain context into the shared visual request. */
@FunctionalInterface
public interface VisualGenerationContextProvider<T> {
    VisualGenerationRequest build(T context);
}
