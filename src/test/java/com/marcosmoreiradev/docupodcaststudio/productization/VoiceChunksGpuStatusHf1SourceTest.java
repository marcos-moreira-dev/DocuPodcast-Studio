package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-CHUNKS-HF1: status bar chunk actions, honest GPU messaging and index synchronization. */
final class VoiceChunksGpuStatusHf1SourceTest {
    @Test
    void statusBarSeparatesSelectedChunkResumeAndRedoGeneration() throws Exception {
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String viewModel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(status.contains("Renderizar desde aquí"));
        assertTrue(status.contains("Reconstruir fragmentos de audio"));
        assertTrue(status.contains("Seguir generando"));
        assertTrue(status.contains("documentProgressLabel"));
        assertTrue(status.contains("blocks.size() <= 1 ? 100"));
        assertTrue(shell.contains("handleGenerateSelectedChunkFromStatusBar"));
        assertTrue(shell.contains("viewModel.selectedDocumentBlockIdProperty()"));
        assertTrue(viewModel.contains("generateAudioChunksFromSelectedFragment"));
        assertTrue(viewModel.contains("submitAudioGenerationFromSegment(segment.get(), false)"));
        assertTrue(viewModel.contains("Seguir generando enviado"));
    }

    @Test
    void documentIndexTracksNearestRegionForCurrentSelection() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java");

        assertTrue(panel.contains("selectNearestIndexEntry"));
        assertTrue(panel.contains("nearestIndexedBlockId"));
        assertTrue(panel.contains("queueSelectionRefresh"));
        assertTrue(panel.contains("Platform.runLater"));
        assertTrue(panel.contains("syncingSelection"));
        assertTrue(panel.contains("tree.scrollTo"));
        assertTrue(panel.contains("indexedBlocks"));
        assertTrue(panel.contains("blockSourceIndexById"));
        assertTrue(panel.contains("indexedItemsByBlockId"));
        assertTrue(panel.contains("targetChanged"));
    }

    @Test
    void gpuMessagingPropagatesSelectedDeviceWithoutPromisingUnsupportedCuda() throws Exception {
        String assessment = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/compute/AssessComputeAccelerationUseCase.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String progress = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java");
        String voice = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String voiceEngineControls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java");
        String voiceSurface = voice + voiceEngineControls;

        assertTrue(assessment.contains("Voz IA avanzada intentará el dispositivo solicitado en modo manual"));
        assertTrue(settings.contains("SettingsOperationProgressCoordinator"));
        assertTrue(progress.contains("CUDA no quedó disponible para Voz IA avanzada"));
        assertTrue(voiceSurface.contains("Dispositivo solicitado:"));
        assertTrue(voiceSurface.contains("Voz local simple recibirá ese dispositivo si el runtime lo soporta."));
        assertFalse(voiceSurface.contains("Coqui/XTTS"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
