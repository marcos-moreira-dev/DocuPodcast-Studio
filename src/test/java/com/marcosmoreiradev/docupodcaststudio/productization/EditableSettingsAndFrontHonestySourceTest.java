package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditableSettingsAndFrontHonestySourceTest {
    @Test
    void settingsDialogIsEditableAndMainFrontOffersOnlyImplementedSourceFormats() throws Exception {
        String dialog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String actionBar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsActionBar.java"));
        String formModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String settingsSurface = dialog + actionBar;
        String shell = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String toolbar = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java"));
        String welcome = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java"));
        String factory = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/InfrastructureServicesFactory.java"));

        assertTrue(formModel.contains("TextField"));
        assertTrue(formModel.contains("CheckBox"));
        assertTrue(formModel.contains("ComboBox"));
        assertTrue(settingsSurface.contains("Guardar cambios"));
        assertTrue(dialog.contains("saveOperationalSettings().save"));
        assertTrue(dialog.contains("Configuración guardada"));
        assertTrue(toolbar.contains("Abrir fuente"));
        assertTrue(shell.contains("*.md"));
        assertTrue(shell.contains("*.txt"));
        assertTrue(factory.contains("new MarkdownDocumentImporter()"));
        assertTrue(factory.contains("new PlainTextDocumentImporter()"));
        assertTrue(factory.contains("new PdfDocumentImporter(pdfRenderEngine, pdfOcrTextLayer)"));
        assertTrue(shell.contains("*.pdf"));
        assertTrue(shell.contains("PDF texto u OCR local"));
        assertTrue(welcome.contains("PDF"));
    }
}
