package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF9D: right Visual rail can copy an assigned image to adjacent sentence fragments. */
final class DocUxHf9DVisualRailContextMenuSourceTest {
    @Test
    void visualRailOffersContextMenuForPreviousAndNextFragmentImageCopy() throws Exception {
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));

        assertTrue(rail.contains("ContextMenu"));
        assertTrue(rail.contains("Asignar imagen al fragmento anterior"));
        assertTrue(rail.contains("Asignar imagen al fragmento posterior"));
        assertTrue(rail.contains("previousItem.setDisable(previous == null || !fragment.imageReady())"));
        assertTrue(rail.contains("nextItem.setDisable(next == null || !fragment.imageReady())"));
        assertTrue(rail.contains("copyFragmentImageToAdjacentFragment(fragment, previous)"));
        assertTrue(rail.contains("copyFragmentImageToAdjacentFragment(fragment, next)"));
    }

    @Test
    void copiedImageSelectsDestinationSoLeftInspectorRefreshes() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(vm.contains("copyFragmentImageToAdjacentFragment"));
        assertTrue(vm.contains("NarrativeLayerKind.IMAGE, scriptRange.get(), targetRange"));
        assertTrue(vm.contains("selectDocumentTextRange(targetRange, target.preview())"));
        assertTrue(vm.contains("El panel Imagen se actualizó con la misma miniatura"));
        assertTrue(vm.contains("bumpDocumentMediaRevision()"));
    }
}
