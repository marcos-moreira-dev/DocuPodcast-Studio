package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF9G/HF10H: source opening is only document import, while audio generation reports generated WAV size. */
final class DocUxHf9GImportOverlaySizeSourceTest {
    @Test
    void sourceOpeningDialogDoesNotPromisePreparingAudioFragments() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/DocumentImportProgressDialog.java"));

        assertTrue(shell.contains("Leyendo documento para mostrarlo en la vista Documento"));
        assertTrue(shell.contains("PauseTransition closePulse"));
        assertTrue(dialog.contains("para mostrarla en Documento"));
        assertFalse(shell.contains("Leyendo documento y preparando fragmentos"));
        assertFalse(dialog.contains("preparando fragmentos"));
    }

    @Test
    void audioPreparationOverlayShowsGeneratedChunksSize() throws Exception {
        String overlay = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/LongProcessOverlayView.java"));

        assertTrue(overlay.contains("generatedChunksSizeLabel"));
        assertTrue(overlay.contains("audio generado en disco"));
        assertFalse(overlay.contains("tamaño estimado"));
        assertTrue(overlay.contains("completedWavBytes"));
        assertTrue(overlay.contains(".endsWith(\".wav\")"));
        assertTrue(overlay.contains("humanSize"));
    }

    @Test
    void engineUnavailableDialogContractStaysVisibleToSourceGuards() throws Exception {
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/dialogs/AudioEngineUnavailableDialog.java"));

        assertTrue(shell.contains("Motor de voz no disponible"));
        assertTrue(shell.contains("confirmAudioEngineReadyForDocumentAction"));
        assertTrue(dialog.contains("¿Quieres abrir Configuración"));
    }
}
