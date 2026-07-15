package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF13: Vista Voces se ordena por tareas sin cambiar motores ni schema. */
final class PresentationOrchestrationRf13VoiceUxSourceTest {
    @Test
    void voicesSidebarKeepsThreeModulesAndUsesTaskLayout() throws Exception {
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleId.java");
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String layout = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceLayout.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertTrue(ids.contains("HOME"));
        assertTrue(ids.contains("ENGINE"));
        assertTrue(ids.contains("MANAGE"));
        assertTrue(ids.contains("Inicio"));
        assertTrue(ids.contains("Configurar motor"));
        assertTrue(ids.contains("Gestionar voces"));
        assertFalse(ids.contains("NEW_VOICE"));
        assertTrue(view.contains("VoiceWorkspaceLayout"));
        assertTrue(view.contains("masterDetail("));
        assertTrue(view.contains("balancedMasterDetail(list, detail)"));
        assertTrue(view.contains("voiceBrowser.setMaxHeight(Double.MAX_VALUE)"));
        assertTrue(layout.contains("masterDetail"));
        assertTrue(layout.contains("balancedMasterDetail"));
        assertTrue(css.contains(".voice-master-detail"));
        assertTrue(css.contains(".voice-master-detail-balanced"));
        assertTrue(css.contains(".voice-master-pane"));
    }

    @Test
    void newVoiceIsFocusedEditorSubflowInsideManageVoices() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertTrue(view.contains("VoiceManageMode.NEW_VOICE"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(false)"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(true)"));
        assertTrue(editorPanel.contains("Nueva voz"));
        assertTrue(editorPanel.contains("Gestionar voz"));
        assertTrue(editorPanel.contains("Nombre de la voz"));
        assertTrue(editorPanel.contains("Emoción de referencia"));
        assertTrue(editorPanel.contains("Frase para interpretar"));
        assertTrue(editorPanel.contains("Guardar exige nombre y muestra Neutral"));
        assertTrue(editorPanel.contains("voice-interpretation-phrase"));
        assertFalse(view.contains("newVoiceWizardModule"));
        assertFalse(editorPanel.contains("wizardSteps"));
        assertTrue(css.contains(".voice-interpretation-phrase"));
    }

    @Test
    void voiceWorkspaceLineBudgetAndDocumentationRecordRf13() throws Exception {
        long lines = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java")
                .lines()
                .count();
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");
        String registro = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");

        assertTrue(lines <= 1000, "VoiceLibraryWorkspaceView debe quedar en <= 1000 líneas tras RF13; actual=" + lines);
        assertTrue(bitacora.contains("Paso RF13-01"));
        assertTrue(registro.contains("PRESENTATION-ORCHESTRATION-RF13"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
