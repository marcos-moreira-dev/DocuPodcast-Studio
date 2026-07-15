package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BrainAudioWorkflowCoordinatorSourceTest {
    @Test
    void audioWorkflowMovesJobBrainBehindTheDocumentReader() throws Exception {
        String coordinator = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String roadmap = Files.readString(Path.of("docs/productizacion/ROADMAP_POST_T64_AUDIO_WORKFLOW.md"));

        assertTrue(coordinator.contains("class AudioWorkflowCoordinator"));
        assertTrue(coordinator.contains("engineDescriptor"));
        assertTrue(coordinator.contains("persistedJobs"));
        assertTrue(coordinator.contains("recoverableSnapshot"));
        assertTrue(coordinator.contains("selectedSnapshot"));
        assertTrue(coordinator.contains("jobDetailLines"));
        assertTrue(coordinator.contains("diagnosticLabels"));
        assertTrue(shell.contains("private final AudioWorkflowCoordinator audioWorkflow"));
        assertTrue(shell.contains("audioWorkflow.replaceAndSubmitAsync"));
        assertTrue(shell.contains("audioWorkflow.resume"));
        assertTrue(shell.contains("audioWorkflow.cancel"));
        assertTrue(shell.contains("audioWorkflow.persistedJobs"));
        assertTrue(shell.contains("audioWorkflow.jobDetailLines"));
        assertTrue(roadmap.contains("T65"));
        assertTrue(roadmap.contains("NarrativeLayerCoordinator"));
    }
}
