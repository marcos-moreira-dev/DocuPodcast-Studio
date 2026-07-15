package com.marcosmoreiradev.docupodcaststudio.domain.assets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectAssetCatalogTest {
    @Test
    void indexesByIdAndKind() {
        ProjectAssetReference source = new ProjectAssetReference("SRC-001", ProjectAssetKind.SOURCE_DOCUMENT,
                "Notas", "source/notas.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Fuente", "", "");
        ProjectAssetReference image = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "Imagen", "media/images/escena.png", "image/png", "Storyboard", "", "");

        ProjectAssetCatalog catalog = new ProjectAssetCatalog(List.of(source, image));

        assertEquals(2, catalog.size());
        assertTrue(catalog.byId("SRC-001").isPresent());
        assertEquals(1, catalog.byKind(ProjectAssetKind.IMAGE).size());
    }

    @Test
    void rejectsDuplicatedIds() {
        ProjectAssetReference one = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "A", "media/images/a.png", "image/png", "", "", "");
        ProjectAssetReference two = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "B", "media/images/b.png", "image/png", "", "", "");

        assertThrows(IllegalArgumentException.class, () -> new ProjectAssetCatalog(List.of(one, two)));
    }

    @Test
    void withReferenceReplacesExistingId() {
        ProjectAssetReference one = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "A", "media/images/a.png", "image/png", "", "", "");
        ProjectAssetReference two = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "B", "media/images/b.png", "image/png", "", "", "");

        ProjectAssetCatalog catalog = ProjectAssetCatalog.empty().withReference(one).withReference(two);

        assertEquals(1, catalog.size());
        assertEquals("B", catalog.byId("IMG-001").orElseThrow().displayName());
    }
}
