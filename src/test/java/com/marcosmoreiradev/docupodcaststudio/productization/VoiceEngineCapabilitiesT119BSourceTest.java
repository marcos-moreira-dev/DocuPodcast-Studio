package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T119B guards that voice/document UI is driven by active engine capabilities. */
final class VoiceEngineCapabilitiesT119BSourceTest {
    @Test
    void policyDefinesExplicitCoquiPiperAndMockCapabilities() throws Exception {
        String profile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceEngineCapabilityProfile.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceCapabilityPolicy.java");

        assertTrue(profile.contains("coquiXttsMode"));
        assertTrue(profile.contains("piperMode"));
        assertTrue(profile.contains("mockMode"));
        assertTrue(profile.contains("supportsCustomVoiceSample"));
        assertTrue(profile.contains("supportsPiperModelVoice"));
        assertTrue(profile.contains("supportsEmotion"));
        assertTrue(profile.contains("supportsExpressiveStyle"));
        assertTrue(profile.contains("supportsVoiceCloning"));
        assertTrue(policy.contains("activeEngineProfile"));
        assertTrue(policy.contains("La Voz local simple usa modelos .onnx"));
        assertTrue(policy.contains("Voz IA avanzada puede usar voces importadas"));
    }

    @Test
    void documentSidebarHidesImpossiblePiperExpressiveControls() throws Exception {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(panel.contains("VoiceEngineCapabilityProfile"));
        assertTrue(panel.contains("profile.piperMode()"));
        assertTrue(panel.contains("La Voz local simple usa la voz local disponible"));
        assertTrue(panel.contains("tonos por muestra humana"));
        assertTrue(panel.contains("profile.supportsEmotion() || profile.supportsExpressiveStyle()"));
        assertTrue(panel.contains("El modo de prueba solo valida el flujo"));
        assertFalse(panel.contains("Asignar al segmento seleccionado"));
    }

    @Test
    void documentationRecordsEngineDrivenUiRule() throws Exception {
        String docs = read("docs/productizacion/T119B_UX_CAPACIDADES_MOTOR_VOZ.md");
        assertTrue(docs.contains("capability-driven"));
        assertTrue(docs.contains("Piper"));
        assertTrue(docs.contains("Coqui/XTTS"));
        assertTrue(docs.contains("Mock"));
        assertTrue(docs.contains("no debe mostrar emociones"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
