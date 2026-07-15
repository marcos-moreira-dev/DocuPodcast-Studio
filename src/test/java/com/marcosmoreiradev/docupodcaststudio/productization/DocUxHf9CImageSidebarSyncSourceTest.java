package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-UX-HF9C: left Image inspector and right Visual rail use the same sentence-level image resolver. */
final class DocUxHf9CImageSidebarSyncSourceTest {
    @Test
    void selectedImagePreviewUsesTheSameFragmentProjectionAsTheRightRail() throws Exception {
        String vm = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentImageContextPanel.java"));
        String rail = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java"));

        assertTrue(vm.contains("documentFragmentRailPresentations()"));
        assertTrue(vm.contains("DocumentVisualFragmentKey::from"));
        assertTrue(vm.contains("DocumentFragmentRailPresentation::imageFileUri"));
        assertTrue(vm.contains("filter(uri -> uri != null && !uri.isBlank())"));
        assertTrue(vm.contains("selectedVisualFragmentImageUri"));
        assertTrue(vm.contains("selectedVisualFragmentSegmentId"));
        assertTrue(vm.contains("selectedVisualFragmentSegmentIdProperty()"));
        assertTrue(vm.contains("selectedVisualFragmentKeyProperty()"));
        assertTrue(vm.contains("return explicitSelectedImage"));
        assertTrue(vm.indexOf("String explicitSelectedImage = selectedVisualFragmentImageUri.get()")
                < vm.indexOf("DocumentTextRange selectedRange = selectedDocumentTextRange.get()"));
        assertTrue(vm.contains("selectDocumentFragmentRailItem(DocumentFragmentRailPresentation fragment)"));
        assertTrue(vm.contains("fragment.segmentId()"));
        assertTrue(panel.contains("updatePreview(viewModel.selectedDocumentImageUri())"));
        assertTrue(panel.contains("selectedVisualFragmentImageUriProperty()"));
        assertTrue(panel.contains("selectedVisualFragmentKeyProperty()"));
        assertTrue(panel.contains("schedulePreviewRefresh()"));
        assertTrue(panel.contains("StableImageLoader.shared().load"));
        assertTrue(panel.contains("bindToSelectedImage"));
        assertTrue(rail.contains("selectDocumentFragmentRailItem"));
        assertTrue(rail.contains("MouseButton.PRIMARY"));
        assertTrue(rail.contains("event.getClickCount() == 1"));
    }
}
