package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocPerfHf10CScrollAndDocumentationSourceTest {
    @Test
    void glassPlaybarKeepsLegacyContractWhileAvoidingScrollbarInterception() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));
        String floating = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java"));
        assertTrue(workspace.contains("new Insets(6, 24, 0, 24)"));
        assertTrue(floating.contains("setMaxWidth(820)"));
        assertTrue(floating.contains("setPickOnBounds(false)"));
    }

    @Test
    void largeDocumentCssIsSplitFromMainDocumentPage() throws Exception {
        String imports = Files.readString(Path.of("src/main/resources/css/docupodcast-light.css"));
        String largeCss = Files.readString(Path.of("src/main/resources/css/document/large-document.css"));
        assertTrue(imports.contains("document/large-document.css"));
        assertTrue(largeCss.contains("document-large-preview-notice"));
        assertTrue(largeCss.contains("document-large-preview-button"));
    }

    @Test
    void downloadUrlFieldsShowDefaultValuesWhenSettingsAreBlank() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        assertTrue(settings.contains("field.getPromptText().startsWith(\"http\")"));
        assertTrue(settings.contains("field.setText(field.getPromptText())"));
    }

    @Test
    void currentDocumentationMentionsHf10CAndFutureIndex() throws Exception {
        String registro = Files.readString(Path.of("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md"));
        String roadmap = Files.readString(Path.of("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md"));
        assertTrue(registro.contains("DOC-PERF-HF10C"));
        assertTrue(roadmap.contains("DOC-INDEX"));
    }
}
