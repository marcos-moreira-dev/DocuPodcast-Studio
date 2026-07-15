package com.marcosmoreiradev.docupodcaststudio.presentation.t111;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IconographyCssAndPlaybarT111SourceTest {
    @Test
    void ribbonUsesSemanticIconCatalogInsteadOfTemporaryMarkers() throws Exception {
        String ribbon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java");
        String catalog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonIconCatalog.java");
        String icon = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/IconView.java");
        String css = read("src/main/resources/css/components/ribbon.css");

        assertTrue(ribbon.contains("RibbonIconCatalog.iconFor(command.commandId())"));
        assertFalse(ribbon.contains("cmd(\"DOC\""));
        assertFalse(ribbon.contains("cmd(\"PLAY\""));
        assertFalse(ribbon.contains("cmd(\"GEN\""));
        assertFalse(ribbon.contains("cmd(\"STOP\""));
        assertFalse(ribbon.contains("cmd(\"VOICE\""));
        assertTrue(catalog.contains("OPEN_SOURCE_DOCUMENT"));
        assertTrue(catalog.contains("EXPORT_PROJECT_BUNDLE"));
        assertTrue(catalog.contains("AppIcon.OPEN_SOURCE"));
        assertTrue(icon.contains("/icons/ui/"));
        assertTrue(css.contains("T111-HF2 — PNG icon polish"));
    }

    @Test
    void documentImagesUseReusableSourceVisualComponent() throws Exception {
        String document = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");
        String component = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java");
        String styles = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java");
        String icons = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppIcon.java");
        String light = read("src/main/resources/css/docupodcast-light.css");

        assertTrue(document.contains("SourceVisualBlockView.embeddedImage"));
        assertFalse(document.contains("new ImageView"), "DocumentWorkspaceView debe delegar visuales fuente al componente transversal.");
        assertTrue(component.contains("UI_SOURCE_VISUAL_BLOCK"));
        assertTrue(component.contains("No se asigna a la secuencia visual automáticamente"));
        assertTrue(styles.contains("UI_SOURCE_VISUAL_BLOCK"));
        assertTrue(icons.contains("IMAGE(\"image.png\""));
        assertTrue(light.contains("components/source-visual.css"));
    }

    @Test
    void playbarPrimaryActionGuidesProjectSaveBeforeAudioPreparation() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java");
        String document = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java");

        assertTrue(shell.contains("ensureProjectSavedForDocumentAudio(\"preparar o reproducir la lectura desde la barra flotante\")"));
        assertTrue(document.contains("saveProjectBeforeAudioRequest"));
        assertTrue(document.contains("runPrimaryActionFromPlaybar"));
        assertTrue(document.contains("viewModel.currentProjectFile().isEmpty()"));
        assertTrue(document.contains("saveProjectBeforeAudioRequest.getAsBoolean()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
