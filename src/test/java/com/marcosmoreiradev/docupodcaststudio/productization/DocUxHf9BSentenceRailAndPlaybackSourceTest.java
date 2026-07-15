package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF9B makes document operations sentence-first instead of paragraph-first. */
final class DocUxHf9BSentenceRailAndPlaybackSourceTest {
    @Test
    void rightRailListsEverySentenceFragmentEvenWithoutImage() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));
        String projection = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRailProjectionFactory.java"));
        assertTrue(rail.contains("documentFragmentRailPresentations"));
        assertTrue(rail.contains("Every narrable sentence/fragment receives a card"));
        assertTrue(projection.contains("DocumentSentenceSplitter.split(block.get())"));
        assertTrue(projection.contains("new DocumentFragmentRailPresentation"));
        assertFalse(rail.contains("viewModel.storyboardScenePresentations()"), "El rail derecho de Documento no debe depender del storyboard por párrafo.");
    }

    @Test
    void selectedSentencePlaybackStartsAtMatchingCueUnit() throws Exception {
        String resolver = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentPlaybackSelectionResolver.java"));
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        assertTrue(resolver.contains("cueForSelection"));
        assertTrue(resolver.contains("manifest.cueForUnit(unitId)"));
        assertTrue(resolver.contains("segment.sourceBlockIds().stream().anyMatch"));
        assertTrue(shell.contains("playbackSelectionResolver.cueForSelection"));
    }

    @Test
    void sourceVisualBlocksSurviveReadingProfileAsNonNarratableVisuals() throws Exception {
        String profile = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/reading/ApplyReadingProfileUseCase.java"));
        assertTrue(profile.contains("DocumentBlockType.IMAGE_NOTICE"));
        assertTrue(profile.contains("DocumentBlockType.TABLE_NOTICE"));
        assertFalse(profile.contains("ImageNarrationPolicy.IGNORE_IMAGES"));
        assertFalse(profile.contains("TableNarrationPolicy.IGNORE_TABLES"));
    }
}
