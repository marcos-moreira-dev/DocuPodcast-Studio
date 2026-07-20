package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.List;

@FunctionalInterface
public interface GenerationJobOperation {
    List<GenerationArtifact> execute(ExecutionContext context) throws Exception;
}
