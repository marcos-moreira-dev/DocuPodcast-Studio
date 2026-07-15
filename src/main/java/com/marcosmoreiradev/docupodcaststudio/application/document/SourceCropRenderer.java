package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Port for rendering faithful source-document crops without exposing infrastructure adapters to presentation. */
public interface SourceCropRenderer {
    Optional<Path> renderBlockCrop(Path sourceDocument, DocumentBlock block, Path target) throws IOException;
}
