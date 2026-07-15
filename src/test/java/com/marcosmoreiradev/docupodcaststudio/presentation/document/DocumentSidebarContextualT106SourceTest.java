package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentSidebarContextualT106SourceTest {
    @Test
    void leftSidebarIsContextualAndActionableWithoutTechnicalCabin() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String details = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java");
        String audio = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");
        String image = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java");
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java");
        String css = read("src/main/resources/css/document-reader.css")
                + read("src/main/resources/css/components/actions.css");

        assertTrue(ids.contains("DOCUMENT_CONTEXT_DETAILS(\"Fragmento\")"));
        assertTrue(workspace.contains("\"Fragmento\""));
        assertTrue(workspace.contains("\"Texto\""));
        assertTrue(details.contains("Reproducir desde aquí"));
        assertTrue(details.contains("DocuPodcast lo preparará con la voz base"));
        assertFalse(details.contains("prepareDocumentLayerAssignment(NarrativeLayerKind.VOICE)"));
        assertFalse(details.contains("prepareDocumentLayerAssignment(NarrativeLayerKind.EMOTION)"));
        assertFalse(details.contains("prepareDocumentLayerAssignment(NarrativeLayerKind.IMAGE)"));
        assertTrue(details.contains("La fuente original no se edita"));
        assertTrue(audio.contains("ComboBox<VoiceProfile>"));
        assertTrue(audio.contains("ComboBox<VoiceReferenceTone>"));
        assertTrue(audio.contains("Audio del computador"));
        assertFalse(audio.contains("Preparar audio"));
        assertTrue(image.contains("Vincula una imagen"));
        assertTrue(image.contains("ImageView"));
        assertFalse(image.contains("Reemplazar imagen"));
        assertTrue(css.contains("T106"));
        assertTrue(css.contains("rgba(31, 41, 55, 0.42)"));
        assertFalse(details.contains("metadatos"));
        assertFalse(details.contains("cabina"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
