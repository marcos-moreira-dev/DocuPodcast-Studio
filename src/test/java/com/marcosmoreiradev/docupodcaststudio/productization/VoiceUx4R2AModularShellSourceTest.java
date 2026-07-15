package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-2A: Vista Voces becomes a sober modular administration workspace. */
final class VoiceUx4R2AModularShellSourceTest {
    @Test
    void voicesWorkspaceUsesThreeSoberModulesInsteadOfSplitPane() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String ids = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleId.java");
        assertTrue(view.contains("VoiceWorkspaceShell"));
        assertTrue(view.contains("VoiceModuleNavigation"));
        assertTrue(ids.contains("HOME"));
        assertTrue(ids.contains("ENGINE"));
        assertTrue(ids.contains("MANAGE"));
        assertTrue(ids.contains("Inicio"));
        assertTrue(ids.contains("Configurar motor"));
        assertTrue(ids.contains("Gestionar voces"));
        assertFalse(view.contains("SplitPane"));
    }

    @Test
    void manageModuleKeepsEmotionReplacementAndNeutralContractVisible() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String actions = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java");
        String editor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String registeredPrompts = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceRegisteredTonePrompts.java");
        String voices = view + actions + editor + registeredPrompts;
        assertTrue(view.contains("Muestras de voz por emoción"));
        assertTrue(voices.contains("Importar audio"));
        assertTrue(voices.contains("Grabar muestra"));
        assertTrue(editor.contains("reemplaza la muestra de la emoción seleccionada"));
        assertTrue(editor.contains("Guardar exige nombre y muestra Neutral"));
        assertTrue(registeredPrompts.contains("registeredTones"));
    }

    @Test
    void documentationTracksTheModularShellRoadmap() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_2A_SHELL_MODULAR_VOCES.md");
        assertTrue(doc.contains("Inicio"));
        assertTrue(doc.contains("Configurar motor"));
        assertTrue(doc.contains("Gestionar voces"));
        assertTrue(doc.contains("VOZ-UX4R-2B"));
        assertTrue(doc.contains("VOZ-UX4R-3"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
