package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentNarratedExperienceSourceTest {
    @Test
    void documentWorkspaceIsAlignedAsNarratedDocumentWithoutMutatingWord() throws Exception {
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String sideDock = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java"));
        String docs = Files.readString(Path.of("docs/52_TANDA_35A_DOCUMENTO_NARRADO_EXPERIENCIA_PRINCIPAL.md"));

        assertTrue(welcome.contains("DocuPodcast Studio"));
        assertTrue(welcome.contains("Escucha rápido"));
        assertTrue(document.contains("Documento narrado"));
        assertTrue(document.contains("no dentro del Word"));
        assertTrue(document.contains("blockMarker"));
        assertTrue(sideDock.contains("Ocultar"));
        assertTrue(docs.contains("Reproducir documento / Reproducir desde aquí"));
        assertTrue(docs.contains("PerformanceSpan"));
    }
}
