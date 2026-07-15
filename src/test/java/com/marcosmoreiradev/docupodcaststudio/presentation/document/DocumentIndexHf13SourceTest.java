package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** DOC-INDEX-HF13 keeps document navigation available without bringing back the old technical structure panel. */
class DocumentIndexHf13SourceTest {
    @Test
    void documentWorkspaceRegistersIndexSideDockModule() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockModuleId.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/SideDockStatePolicy.java");

        assertTrue(workspace.contains("SideDockModuleId.DOCUMENT_INDEX"));
        assertTrue(workspace.contains("new DocumentIndexPanel("));
        assertTrue(workspace.contains("viewModel.applicationServices().document().buildDocumentOutline()"));
        assertTrue(ids.contains("DOCUMENT_INDEX("));
        assertTrue(policy.contains("SideDockModuleId.DOCUMENT_INDEX"));
        assertFalse(workspace.contains("Estructura documental"));
    }

    @Test
    void indexPanelRendersApplicationOutlineProjection() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java");

        assertTrue(panel.contains("TreeView<IndexEntry>"));
        assertTrue(panel.contains("DocumentOutlineProjection"));
        assertTrue(panel.contains("buildOutline.build(document)"));
        assertTrue(panel.contains("outlineRoot"));
        assertTrue(panel.contains("Origen del indice"));
        assertTrue(panel.contains("onBlockSelected.accept"));
        assertFalse(panel.contains("hasStructuralHeadings()"));
        assertFalse(panel.contains("flatRoot"));
    }

    @Test
    void indexStylesArePresentWithoutDashboardClasses() throws Exception {
        String css = read("src/main/resources/css/document-reader.css");

        assertTrue(css.contains("DOC-INDEX-HF13"));
        assertTrue(css.contains(".document-index-panel"));
        assertTrue(css.contains(".document-index-tree"));
        assertFalse(css.contains("document-index-dashboard"));
        assertFalse(css.contains("document-index-card-grid"));
    }

    @Test
    void indexUsesTreeIconInsteadOfVisualRailIcon() throws Exception {
        String icons = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppIcon.java");
        String sideDock = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java");

        assertTrue(icons.contains("INDEX_TREE("));
        assertTrue(sideDock.contains("return AppIcon.INDEX_TREE"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
