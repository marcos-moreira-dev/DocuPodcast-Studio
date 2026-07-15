package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-INDEX-PLAYBACK-HF1 keeps index navigation as the audio generation pivot. */
class DocumentIndexPlaybackHf1SourceTest {
    @Test
    void listenToDocumentGeneratesFromIndexedBlockInsteadOfWholeDocument() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        int pivot = source.indexOf("Optional<NarrationSegment> selectedStart = selectedDocumentBlockId.get().isBlank()");
        int selectedGeneration = source.indexOf("submitAudioGenerationFromSegment(selectedStart.get(), true)", pivot);
        int globalGeneration = source.indexOf("submitAudioGeneration();", pivot);

        assertTrue(pivot > 0, "listenToDocument debe resolver el bloque elegido en el índice como pivote narrable.");
        assertTrue(selectedGeneration > pivot, "Si hay pivote de índice, debe generar desde ese segmento.");
        assertTrue(globalGeneration > selectedGeneration, "La generación global solo debe quedar como fallback posterior al pivote.");
    }

    @Test
    void selectionGenerationStillUsesSuffixScript() throws Exception {
        String source = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java");

        assertTrue(source.contains("scriptStartingAt(script, startSegment.id())"));
        assertTrue(coordinator.contains("renderUnitPlanStartingAt(fullPlan, suffixScript, startSegmentId)"));
        assertTrue(source.contains("pendingPlaybackStartSegmentId = startSegment.id()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
