package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** VOZ-UX4R-2B: Configurar motor inside Voices uses real settings and detected devices. */
final class VoiceUx4R2BEngineSelectorSourceTest {
    @Test
    void voiceEngineModuleHasRealEngineAndDeviceSelectors() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String controls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java");
        assertTrue(view.contains("VoiceEngineSettingsControls"));
        assertTrue(controls.contains("ComboBox<EngineModeChoice> engineModeSelector"));
        assertTrue(controls.contains("ComboBox<ComputeDeviceChoice> computeDeviceSelector"));
        assertTrue(controls.contains("Voz IA avanzada"));
        assertTrue(controls.contains("Voz local simple"));
        assertTrue(controls.contains("Modo de prueba"));
        assertTrue(controls.contains("inspectComputeEnvironment().inspect(settings)"));
        assertTrue(controls.contains("saveOperationalSettings().save(settings)"));
        assertFalse(view.contains("selector de CPU/GPU real se implementa en VOZ-UX4R-2B"));
    }

    @Test
    void voiceEngineModuleDocumentsAllEngineDevicePolicy() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_2B_CONFIGURAR_MOTOR_VOCES.md");
        assertTrue(doc.contains("selector de motor activo"));
        assertTrue(doc.contains("selector de dispositivo de renderizado"));
        assertTrue(doc.contains("El selector de dispositivo aplica a todos los motores"));
        assertTrue(doc.contains("GPU NVIDIA detectada"));
        assertTrue(doc.contains("OperationalSettings"));
        assertTrue(doc.contains("MOTOR-SMOKE4R / COQUI-DL1"));
    }

    @Test
    void compileHotfixDoesNotCallNonExistingVoiceCapabilityMethods() throws IOException {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        assertFalse(view.contains("voice.description()"));
        assertFalse(view.contains("capability.reason()"));
        assertFalse(view.contains("capability.documentUsageHint()"));
        assertTrue(view.contains("voice.consentNote()"));
        assertTrue(view.contains("capability.message()"));
        assertTrue(view.contains("documentUsageHint(capability)"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
