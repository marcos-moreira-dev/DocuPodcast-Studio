package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T121-V10: docs and smoke checklist for the final Voice Library pass. */
final class VoiceLibraryT121V10DocumentationSourceTest {
    @Test
    void finalVoiceSmokeChecklistExistsAndUsesFriendlyProductLanguage() throws Exception {
        String smoke = read("docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md");
        String lower = smoke.toLowerCase(Locale.ROOT);

        assertTrue(smoke.contains("Vista > Voces"));
        assertTrue(smoke.contains("Voz IA avanzada"));
        assertTrue(smoke.contains("Voz local simple"));
        assertTrue(smoke.contains("Modo de prueba"));
        assertTrue(lower.contains("fallback neutral"));
        assertTrue(lower.contains("descargar/exportar"));
        assertTrue(lower.contains("no se muestran muestras humanas"));
        assertFalse(lower.contains("coqui"));
        assertFalse(lower.contains("kogi"));
        assertFalse(lower.contains("xtts"));
        assertFalse(lower.contains("piper"));
    }

    @Test
    void rootDocsAndRegistryPointToT121V10AsLatestVoiceClosure() throws Exception {
        String readme = read("README.md");
        String handoff = read("AI_HANDOFF.md");
        String validation = read("VALIDATION.md");
        String registry = read("docs/productizacion/REGISTRO_TANDAS_DOCUPODCAST.md");
        String doc = read("docs/productizacion/T121_V10_TESTS_DOCUMENTACION_SMOKE_VOCES.md");

        assertTrue(readme.contains("T121-V10"));
        assertTrue(handoff.contains("T121-V10"));
        assertTrue(validation.contains("T121-V10"));
        assertTrue(registry.contains("T121-V10"));
        assertTrue(doc.contains("VoiceLibraryFinalSmokeT121V10SourceTest"));
        assertTrue(doc.contains("SMOKE_VISUAL_VOCES_FINAL.md"));
    }

    @Test
    void voicePlanningIndexMarksV10ImplementedAndDoesNotUseTechnicalEngineAsVisibleLabel() throws Exception {
        String index = read("docs/productizacion/voces_ux_final/00_INDICE_TANDAS_VISTA_VOCES.md");
        String lower = index.toLowerCase(Locale.ROOT);

        assertTrue(index.contains("T121-V10 — implementada"));
        assertTrue(index.contains("Voz local simple"));
        assertFalse(index.contains("Modo Piper mínimo"));
        assertFalse(lower.contains("coqui en la ux/ui"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
