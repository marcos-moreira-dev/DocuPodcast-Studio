package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOICE-REGISTRATION-WIZARD1: nueva voz usa un editor operativo en el workspace. */
final class VoiceRegistrationWizard1SourceTest {
    @Test
    void voiceWorkspaceExposesOneEditorForNewAndSelectedAdvancedVoice() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String editor = view + editorPanel;

        assertTrue(view.contains("VoiceManageMode.NEW_VOICE"));
        assertTrue(view.contains("VoiceManageMode.EDIT_VOICE"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(false)"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(true)"));
        assertTrue(editor.contains("Nueva voz"));
        assertTrue(editor.contains("Gestionar voz"));
        assertTrue(editor.contains("Volver a gestionar voces"));
        assertTrue(editor.contains("Nombre de la voz"));
        assertTrue(editor.contains("Emoción de referencia"));
        assertTrue(editor.contains("Guardar / actualizar voz"));
        assertTrue(editor.contains("Eliminar voz"));
        assertTrue(editor.contains("Exportar muestras"));
        assertFalse(editorPanel.contains("Crear voz"));
        assertFalse(editorPanel.contains("Catálogo teatral de tonos"));
        assertFalse(editorPanel.contains("Detalle de voz seleccionada"));
        assertFalse(view.contains("toneDashboard"));
    }

    @Test
    void editorUsesJavaRecordingActionsAndNeutralGate() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String gateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/recording/JavaSoundAudioRecordingGateway.java");
        String voices = view + editorPanel + actions;

        assertTrue(voices.contains("Grabar muestra"));
        assertTrue(voices.contains("Detener y guardar"));
        assertTrue(voices.contains("Reproducir muestra"));
        assertTrue(voices.contains("Eliminar muestra"));
        assertTrue(view.contains("Para registrar una voz avanzada necesitas al menos una muestra Neutral"));
        assertTrue(voices.contains("Guardar exige nombre y muestra Neutral"));
        assertTrue(voices.contains("Emociones listas"));
        assertTrue(voices.contains("Escuchar reproduce la muestra grabada o importada"));
        assertTrue(gateway.contains("javax.sound.sampled"));
        assertFalse(gateway.toLowerCase().contains("python"));
    }

    @Test
    void editorDocumentsReferenceSamplesNotFixedClips() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String overviewPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java");
        String doc = read("DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md");
        String editor = view + editorPanel + overviewPanel;

        assertTrue(editor.contains("no son clips fijos"));
        assertTrue(editor.contains("texto nuevo"));
        assertTrue(doc.contains("referencias para Coqui/XTTS"));
        assertTrue(doc.contains("no son clips fijos"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
