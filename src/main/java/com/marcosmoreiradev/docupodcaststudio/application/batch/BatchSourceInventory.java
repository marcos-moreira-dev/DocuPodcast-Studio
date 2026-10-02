package com.marcosmoreiradev.docupodcaststudio.application.batch;

import java.nio.file.Path;
import java.util.List;

public record BatchSourceInventory(Path sourceRoot, List<BatchSourceCandidate> documents,
                                   int ignoredFileCount, long totalBytes, int embeddedMediaCount) {
    public BatchSourceInventory {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
