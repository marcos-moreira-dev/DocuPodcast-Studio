package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MaterializeWordDocumentContentAssetUseCaseTest {
    @TempDir Path project;

    @Test
    void materializesEmbeddedImageIdempotentlyInsideProject() throws Exception {
        byte[] image = new byte[]{1, 2, 3, 4, 5};
        DocumentContentItem content = new DocumentContentItem("IMG-1",
                DocumentContentKind.IMAGE, "Imagen", "Descripcion", List.of(),
                List.of("IMG-1"), "fingerprint", 1L,
                new WordContentAnchor("IMG-1", Map.of(
                        "embeddedImageBase64", Base64.getEncoder().encodeToString(image),
                        "embeddedImageMimeType", "image/png")));
        var useCase = new MaterializeWordDocumentContentAssetUseCase();

        var first = useCase.materialize(content, project);
        var second = useCase.materialize(content, project);

        assertFalse(first.reused());
        assertTrue(second.reused());
        assertTrue(first.path().startsWith(project));
        assertTrue(first.projectRelativePath().contains("word-source"));
        assertEquals(ProjectAssetKind.STUDY_SOURCE_CROP, first.asset().kind());
        assertEquals(first.projectRelativePath(), first.asset().relativePath());
        assertTrue(!first.asset().checksum().isBlank());
        assertArrayEquals(image, Files.readAllBytes(first.path()));
    }
}
