package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds temporary source crop previews for study problems. */
public final class PrepareStudySourceCropsUseCase {
    private final SourceCropRenderer renderer;

    public PrepareStudySourceCropsUseCase(SourceCropRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    public Map<String, Path> prepare(ReadableDocument document, List<DocumentBlock> blocks) {
        if (document == null || document.format() != SourceDocumentFormat.PDF || blocks == null || blocks.isEmpty()) {
            return Map.of();
        }
        try {
            Path tempDir = Files.createTempDirectory("docupodcast-study-source-crops");
            LinkedHashMap<String, Path> crops = new LinkedHashMap<>();
            for (DocumentBlock block : blocks) {
                Path target = tempDir.resolve(block.id() + ".png");
                renderer.renderBlockCrop(document.sourcePath(), block, target).ifPresent(path -> crops.put(block.id(), path));
            }
            return crops.isEmpty() ? Map.of() : Map.copyOf(crops);
        } catch (IOException ex) {
            return Map.of();
        }
    }
}
