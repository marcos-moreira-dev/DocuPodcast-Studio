package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModelInstallAssistantSourceTest {
    @Test
    void settingsExposeModelAssistantWithoutMakingMainReaderTechnical() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantCatalog.java"));
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ModelInstallAssistantView.java"));
        String appStyles = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java"));
        String css = Files.readString(Path.of("src/main/resources/css/components/settings-shell.css"));
        String document = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java"));

        assertFalse(settings.contains("Asistente / modelos"));
        assertFalse(settings.contains("modelAssistantPage"));
        assertFalse(settings.contains("ModelInstallAssistantView"));
        assertTrue(catalog.contains("Voz IA avanzada"));
        assertTrue(catalog.contains("Voz local simple"));
        assertFalse(catalog.contains("Whisper local"));
        assertTrue(catalog.contains("Descargar desde catálogo verificable"));
        assertTrue(catalog.contains("Importar modelo manualmente"));
        assertTrue(catalog.contains("checksum"));
        assertFalse(catalog.contains("https://"));
        assertFalse(catalog.contains("http://"));
        assertTrue(view.contains("AppStyles.UI_MODEL_CARD"));
        assertFalse(view.contains("new Button"));
        assertTrue(appStyles.contains("ui-model-card"));
        assertTrue(css.contains("ui-model-card"));
        assertFalse(document.contains("Descargar desde catálogo verificable"));
        assertFalse(document.contains("checksum"));
    }
}
