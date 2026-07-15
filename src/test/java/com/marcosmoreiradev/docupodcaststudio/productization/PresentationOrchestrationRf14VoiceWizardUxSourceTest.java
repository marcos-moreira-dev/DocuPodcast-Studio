package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RF14: Nueva voz debe sentirse como flujo de grabación, no como formulario comprimido. */
final class PresentationOrchestrationRf14VoiceWizardUxSourceTest {
    @Test
    void newVoiceEditorKeepsSidebarStableAndShowsLargeInterpretationPhrase() throws Exception {
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleId.java");
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String editorPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertFalse(ids.contains("NEW_VOICE"));
        assertTrue(view.contains("VoiceManageMode.NEW_VOICE"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(false)"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(true)"));
        assertTrue(editorPanel.contains("Emoción de referencia"));
        assertTrue(editorPanel.contains("Frase para interpretar"));
        assertTrue(editorPanel.contains("Estado de muestra"));
        assertTrue(editorPanel.contains("Emociones listas"));
        assertTrue(editorPanel.contains("Escuchar reproduce la muestra grabada o importada"));
        assertTrue(editorPanel.contains("voice-interpretation-phrase"));
        assertFalse(editorPanel.contains("wizardSteps"));
        assertFalse(editorPanel.contains("Catálogo teatral"));
        assertFalse(editorPanel.contains("Detalle de voz seleccionada"));
        assertTrue(css.contains(".voice-interpretation-phrase"));
        assertTrue(css.contains("-fx-font-size: 25px"));
    }

    @Test
    void voiceActionsWrapAndUseReadableRecordingSymbols() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String testPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java");
        String strip = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceActionStrip.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertTrue(view.contains("VoiceActionStrip.of"));
        assertTrue(actions.contains("VoiceActionStrip.of"));
        assertTrue(testPanel.contains("VoiceActionStrip.of"));
        assertTrue(strip.contains("extends FlowPane"));
        assertTrue(actions.contains("● Grabar"));
        assertTrue(actions.contains("■ Detener"));
        assertTrue(actions.contains("▶ Escuchar"));
        assertTrue(actions.contains("Reproduciendo muestra"));
        assertTrue(actions.contains("↺ Repetir"));
        assertTrue(actions.contains("✕ Cancelar"));
        assertTrue(actions.contains("\"Grabar muestra\""));
        assertTrue(actions.contains("\"Reproducir muestra\""));
        assertFalse(view.contains("new ActionBar"));
        assertFalse(actions.contains("new ActionBar"));
        assertTrue(css.contains(".voice-action-strip"));
        assertTrue(css.contains("-fx-min-width: 138"));
    }

    @Test
    void voicesWorkspaceUsesWhiteCanvasAndSeparatorStyle() throws Exception {
        String css = read("src/main/resources/css/voice-library.css");
        String bitacora = read("DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md");

        assertTrue(css.contains(".voice-workspace-shell"));
        assertTrue(css.contains("-fx-background-color: #FFFFFF"));
        assertTrue(css.contains(".voice-master-pane"));
        assertTrue(css.contains("-fx-border-width: 0 1 0 0"));
        assertTrue(css.contains(".voice-generated-test-panel"));
        assertTrue(css.contains("-fx-border-width: 1 0 0 0"));
        assertTrue(bitacora.contains("Paso RF14-01"));
    }

    @Test
    void manageVoicesSummaryUsesRegisteredEmotionComboAndKeepsFullEditorAsSubflow() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String overview = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java");
        String selectionCoordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManagedSelectionCoordinator.java");
        String registeredTonePrompts = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceRegisteredTonePrompts.java");
        String manageModule = view.substring(view.indexOf("private VBox manageModule"), view.indexOf("private VBox manageOverviewSection"));

        assertTrue(view.contains("VoiceManageMode.EDIT_VOICE"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(false)"));
        assertTrue(view.contains("voiceProfileSampleEditorModule(true)"));
        assertTrue(view.contains("manageOverviewSection"));
        assertTrue(overview.contains("Emoción registrada para la prueba"));
        assertFalse(overview.contains("Detalle de voz seleccionada"));
        assertTrue(overview.contains("La voz simple no usa muestras ni emociones."));
        assertTrue(overview.contains("Las voces prediseñadas no se editan"));
        assertTrue(registeredTonePrompts.contains("VoiceReferenceSampleSet::registeredTones"));
        assertTrue(view.contains("Generar voz"));
        assertTrue(overview.contains("Uso responsable: registra únicamente"));
        assertTrue(view.contains("Gestionar voz seleccionada"));
        assertTrue(view.contains("VoiceProfilePresentationPolicy.predefinedVoice"));
        assertTrue(view.contains("voiceManagementHeader()"));
        assertTrue(selectionCoordinator.contains("selectDocumentAudioSource(\"Voz local simple\")"));
        assertTrue(selectionCoordinator.contains("selectDocumentAudioSource(\"Voz IA avanzada\")"));
        assertTrue(selectionCoordinator.contains("resetGeneratedVoiceTestForSelection"));
        assertTrue(manageModule.contains("manageOverviewSection()"));
        assertFalse(manageModule.contains("toneSampleEditorSection()"));
        assertFalse(manageModule.contains("toneCatalogStatusSection()"));
        assertFalse(manageModule.contains("generatedVoiceTestSection()"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
