package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentMediaRailIntermediateFrameSourceTest {
    @Test
    void visualRailShowsPersistedIntermediateThumbnailWithFullscreenClick() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/media-rail.css"));

        assertTrue(source.contains("theatreIntermediateFrameUri(fragment.segmentId(), next.segmentId())"));
        assertTrue(source.contains("document-media-intermediate-thumb"));
        assertTrue(source.contains("ImageFullscreenViewer.show"));
        assertTrue(source.contains("\"Frame inferido\""));
        assertTrue(css.contains(".document-media-intermediate-thumb"));
    }
}
