package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-UX4R-DOC1: final documented contract for the sober modular Voice workspace. */
final class VoiceUx4RFinalContractDocumentationSourceTest {
    @Test
    void finalVoiceWorkspaceContractDocumentsThreeSoberModules() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md");
        assertTrue(doc.contains("Inicio"));
        assertTrue(doc.contains("Configurar motor"));
        assertTrue(doc.contains("Gestionar voces"));
        assertTrue(doc.contains("La UI debe preferir filas limpias"));
        assertTrue(doc.contains("sin decoración innecesaria"));
    }

    @Test
    void finalVoiceWorkspaceContractDocumentsEmotionReplacementAndDeletionWarning() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md");
        assertTrue(doc.contains("Reemplazar una emoción"));
        assertTrue(doc.contains("Importar audio"));
        assertTrue(doc.contains("Grabar nuevamente"));
        assertTrue(doc.contains("Eliminar voz \"Pepito\""));
        assertTrue(doc.contains("muestras de audio asociadas"));
    }

    @Test
    void finalVoiceWorkspaceContractDocumentsManyEmotionsAndDocumentFiltering() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md");
        assertTrue(doc.contains("Neutral"));
        assertTrue(doc.contains("Misterioso"));
        assertTrue(doc.contains("Melancólico"));
        assertTrue(doc.contains("Épico"));
        assertTrue(doc.contains("el combo Voz solo muestra voces con Neutral registrada"));
        assertTrue(doc.contains("el combo Emoción solo muestra emociones registradas para esa voz"));
    }

    @Test
    void finalVoiceWorkspaceContractDocumentsDeviceSelectorForAllEngines() throws IOException {
        String doc = read("docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md");
        assertTrue(doc.contains("El selector de dispositivo aplica a todos los motores"));
        assertTrue(doc.contains("GPU NVIDIA detectada"));
        assertTrue(doc.contains("GPU AMD detectada"));
        assertTrue(doc.contains("GPU Intel detectada"));
        assertTrue(doc.contains("CPU (sin GPU compatible detectada)"));
    }

    @Test
    void currentDocsPointToFinalVoiceContract() throws IOException {
        assertTrue(read("README.md").contains("VOZ-UX4R-DOC1"));
        assertTrue(read("AI_HANDOFF.md").contains("VOZ-UX4R-DOC1"));
        assertTrue(read("VALIDATION.md").contains("VoiceUx4RFinalContractDocumentationSourceTest"));
        assertTrue(read("DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md").contains("VOZ-UX4R-DOC1"));
        assertTrue(read("DOCUMENTACION_ACTUAL/06_DESCARGAS_MOTORES.md").contains("selector de dispositivo aplica a todos los motores"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
