package com.marcosmoreiradev.docupodcaststudio.domain.assets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectAssetReferenceTest {
    @Test
    void acceptsPortableRelativePathsAndNormalizesBackslashes() {
        ProjectAssetReference reference = new ProjectAssetReference(
                "SRC-001",
                ProjectAssetKind.SOURCE_DOCUMENT,
                "Notas Word",
                "source\\notas.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Documento fuente",
                "",
                ""
        );

        assertEquals("source/notas.docx", reference.relativePath());
    }

    @Test
    void rejectsAbsoluteWindowsPath() {
        assertThrows(IllegalArgumentException.class, () -> new ProjectAssetReference(
                "SRC-001",
                ProjectAssetKind.SOURCE_DOCUMENT,
                "Notas Word",
                "C:/Users/Omar/notas.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Documento fuente",
                "",
                ""
        ));
    }

    @Test
    void rejectsUrlsAndParentTraversal() {
        assertThrows(IllegalArgumentException.class, () -> asset("https://example.com/file.png"));
        assertThrows(IllegalArgumentException.class, () -> asset("media/../secret.png"));
    }

    @Test
    void exposesSemanticHelpers() {
        ProjectAssetReference audio = new ProjectAssetReference("AUD-001", ProjectAssetKind.AUDIO_CLIP,
                "Segmento 1", "jobs/JOB-001/audio/SEG-001.wav", "audio/wav", "Clip", "", "");
        ProjectAssetReference image = new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE,
                "Escena", "media/images/escena.png", "image/png", "Imagen", "", "");

        assertTrue(audio.isAudio());
        assertTrue(image.isImage());
    }

    private static ProjectAssetReference asset(String path) {
        return new ProjectAssetReference("IMG-001", ProjectAssetKind.IMAGE, "Imagen", path,
                "image/png", "Storyboard", "", "");
    }
}
