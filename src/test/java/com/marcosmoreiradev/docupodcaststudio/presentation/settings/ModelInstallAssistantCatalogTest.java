package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModelInstallAssistantCatalogTest {
    @Test
    void modelInstallAssistantPrioritizesVoiceKeepsManualImportAndAvoidsFixedUrls() {
        var plans = ModelInstallAssistantCatalog.recommendedPlans();
        assertEquals(3, plans.size());
        assertTrue(plans.get(0).powerfulVoicePlan(), "Voz IA avanzada debe seguir siendo el motor potente prioritario.");

        String titles = plans.stream().map(ModelInstallPlan::title).collect(Collectors.joining(" | "));
        assertTrue(titles.contains("Voz IA avanzada"));
        assertTrue(titles.contains("Voz local simple"));
        assertTrue(titles.contains("Imagen IA teatral local"));
        assertFalse(titles.contains("Whisper"));

        String all = plans.toString();
        assertTrue(all.contains("Descargar desde catálogo verificable"));
        assertTrue(all.contains("Importar modelo manualmente"));
        assertTrue(all.contains("Importar paquete local"));
        assertTrue(all.contains("checksum"));
        assertTrue(all.contains("Carpeta local del motor avanzado"));
        assertFalse(all.contains("models/stt/whisper"));
        assertTrue(all.contains("no depender de un enlace único")
                || all.contains("No depender de un enlace único")
                || all.contains("URL fija"));
        assertFalse(all.contains("https://"));
        assertFalse(all.contains("http://"));
    }
}
