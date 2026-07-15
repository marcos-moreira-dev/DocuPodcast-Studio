package com.marcosmoreiradev.docupodcaststudio.application.narrative;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeVideoGrammarMarkdownParserTest {
    @Test
    void parsesTemplateFragmentsVisualPromptBridgePromptHardCutAndNotes() {
        NarrativeVideoImportPlan plan = NarrativeVideoGrammarMarkdownParser.parse(NarrativeVideoGrammarTemplate.markdown());

        assertEquals("Mi video narrativo", plan.title());
        assertEquals(2, plan.fragmentCount());
        assertEquals("es", plan.metadata().get("idioma"));
        assertEquals("16:9", plan.metadata().get("formatosugerido"));
        assertEquals("Fragmento 1", plan.fragments().getFirst().title());
        assertEquals("MYSTERIOUS", plan.fragments().getFirst().tone());
        assertTrue(plan.fragments().getFirst().visualPrompt().contains("Imagen principal"));
        assertTrue(plan.fragments().getFirst().bridgePrompt().contains("conectar"));
        assertTrue(plan.fragments().get(1).hardCut());
        assertTrue(plan.fragments().getFirst().notes().contains("produccion"));
        assertTrue(plan.fragments().getFirst().text().contains("texto narrado"));
    }

    @Test
    void keepsCompatibilityWithPreviousKeyValueTemplate() {
        NarrativeVideoImportPlan plan = NarrativeVideoGrammarMarkdownParser.parse("""
                # DocuPodcast Video Narrativo Grammar v1

                titulo: Mi video narrativo

                ## Fragmentos

                ### Fragmento 1
                titulo: Apertura
                corte: suave
                prompt_visual: Imagen principal del fragmento.
                prompt_puente: Imagen opcional para conectar.
                notas: Notas de produccion.
                texto:
                Texto narrado anterior.
                """);

        assertEquals("Mi video narrativo", plan.title());
        assertEquals("Apertura", plan.fragments().getFirst().title());
        assertEquals("Texto narrado anterior.", plan.fragments().getFirst().text());
        assertTrue(plan.fragments().getFirst().bridgePrompt().contains("conectar"));
    }
}
