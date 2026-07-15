package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-CHUNKS-HF2: dependency repair, compact status actions and buffered resume from the exact selected cue. */
final class VoiceChunksGpuStatusHf2SourceTest {
    @Test
    void advancedVoiceRuntimePinsTransformersAndExplainsBeamSearchRepair() throws Exception {
        String requirements = read("tools/xtts-wrapper/requirements-xtts.txt");
        String check = read("tools/xtts-wrapper/check_xtts_runtime.py");
        String synthesize = read("tools/xtts-wrapper/synthesize_xtts.py");

        assertTrue(requirements.contains("TTS==0.22.0"));
        assertTrue(requirements.contains("transformers==4.44.2"));
        assertTrue(check.contains("from transformers import BeamSearchScorer"));
        assertTrue(check.contains("scripts\\\\20-preparar-python-portable-coqui.bat"));
        assertTrue(synthesize.contains("Runtime de Voz IA avanzada incompatible"));
        assertTrue(synthesize.contains("BeamSearchScorer"));
        assertFalse(synthesize.contains("from exc"));
    }

    @Test
    void statusBarUsesShortButtonsAndSubtlePressedStyles() throws Exception {
        String status = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/status/StatusBarView.java");
        String css = read("src/main/resources/css/statusbar.css");

        assertTrue(status.contains("Renderizar desde aquí"));
        assertTrue(status.contains("Reconstruir fragmentos de audio"));
        assertTrue(status.contains("Detalles ·"));
        assertTrue(status.contains("setMinWidth(Region.USE_PREF_SIZE)"));
        assertTrue(status.contains("statusScroller.setMinWidth(180)"));
        assertTrue(css.contains("status-generation-button:pressed"));
        assertTrue(css.contains("-fx-background-color: -docu-accent-soft"));
        assertTrue(css.contains("-fx-text-fill: -docu-accent-ink"));
    }

    @Test
    void selectedTitlePlaybackDoesNotFallBackToFirstAvailableCue() throws Exception {
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackWorkflowCoordinator.java");
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(workflow.contains("return manifest.cueForSegment(normalized.get().id())"));
        assertTrue(workflow.contains("do not fall back to the"));
        assertTrue(vm.contains("!pendingPlaybackStartSegmentId.isBlank() && manifest.cueForSegment(pendingPlaybackStartSegmentId).isPresent()"));
        assertTrue(vm.contains("status.completed()"));
        assertTrue(vm.contains("PlaybackManifest completedManifest = rebuildPlaybackManifestFromLatestJob()"));
    }

    @Test
    void bufferedGapCanResumeWhenNewSuffixManifestStartsAfterMissingPreviousCue() throws Exception {
        String vm = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");

        assertTrue(vm.contains("manifest.cueForUnit(waitingForBufferedSegmentAfter).isEmpty()"));
        assertTrue(vm.contains("nextCue = manifest.firstCue()"));
        assertTrue(vm.contains("do not wait for another manual Play click"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
