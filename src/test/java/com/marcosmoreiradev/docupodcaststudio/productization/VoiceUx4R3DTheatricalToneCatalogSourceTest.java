package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-3D: Gestionar voces expone el catálogo teatral desde el ComboBox de emoción. */
final class VoiceUx4R3DTheatricalToneCatalogSourceTest {
    @Test
    void voiceWorkflowBuildsFullTheatricalCatalogForTheVoiceWorkspace() throws Exception {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/VoiceSampleWorkflowCoordinator.java");
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");

        assertTrue(coordinator.contains("build(target == null ? \"Voz avanzada\" : target.displayName(), true)"));
        assertTrue(view.contains("tonePromptSelector.setVisibleRowCount(16)"));
        assertTrue(view.contains("tonePromptSelector.setPromptText(\"Elegir tono o emoción\")"));
        assertTrue(view.contains("prompts.addAll(wizard.theatricalPrompts())"));
        assertTrue(view.contains("updateTonePromptControls(voice)"));
        assertFalse(view.contains("Catálogo teatral extendido"));
        assertFalse(view.contains("toneCatalogStatusSection"));
        assertFalse(view.contains("toneCatalogRow"));
        assertFalse(view.contains("VoiceReferenceTone.values()"));
    }

    @Test
    void theatricalToneCatalogLivesInTheComboAndAvoidsDashboardCards() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertTrue(editor.contains("Referencia sonora por emoción"));
        assertTrue(editor.contains("Emoción de referencia"));
        assertTrue(editor.contains("Escuchar reproduce la muestra grabada o importada"));
        assertFalse(view.contains("new InfoBadge(value, \"voice-tone-catalog-badge\")"));
        assertFalse(css.contains("voice-tone-dashboard"));
        assertFalse(view.contains("toneDashboard"));
    }

    @Test
    void documentationRegistersTheTheatricalToneCatalogPass() throws Exception {
        String current = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String roadmap = read("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md");
        String validation = read("VALIDATION.md");
        String doc = read("docs/productizacion/VOZ_UX4R_3D_CATALOGO_TEATRAL_TONOS.md");

        assertTrue(current.contains("VOZ-UX4R-3D"));
        assertTrue(roadmap.contains("Roadmap actualizado tras VOZ-UX4R-3D"));
        assertTrue(validation.contains("VoiceUx4R3DTheatricalToneCatalogSourceTest"));
        assertTrue(doc.contains("ComboBox"));
        assertTrue(doc.contains("catálogo teatral extendido"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
