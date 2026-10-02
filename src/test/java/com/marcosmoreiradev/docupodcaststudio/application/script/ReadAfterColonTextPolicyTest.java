package com.marcosmoreiradev.docupodcaststudio.application.script;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ReadAfterColonTextPolicyTest {
    @Test
    void removesShortSpeakerPrefixWhenEnabled() {
        assertEquals("Voy a conquistar el viejo oeste",
                ReadAfterColonTextPolicy.narrationText("Vaquero: Voy a conquistar el viejo oeste", true));
    }

    @Test
    void keepsOriginalTextWhenDisabledOrAmbiguous() {
        assertEquals("Vaquero: Voy", ReadAfterColonTextPolicy.narrationText("Vaquero: Voy", false));
        assertEquals("Capítulo 1. Tema: explicación", ReadAfterColonTextPolicy.narrationText("Capítulo 1. Tema: explicación", true));
    }

    @Test
    void recognizesMultiwordCharacterAndPreservesNaturalColons() {
        assertEquals("¡Orden en la plaza!",
                ReadAfterColonTextPolicy.narrationText("Guardia Verde 1: ¡Orden en la plaza!", true));
        assertEquals("Ven acá.",
                ReadAfterColonTextPolicy.narrationText("BURRO_FLORINDO: Ven acá.", true));
        assertEquals("La patria no necesita patriotas de balcón: ¡que alguien haga algo!",
                ReadAfterColonTextPolicy.narrationText(
                        "La patria no necesita patriotas de balcón: ¡que alguien haga algo!", true));
        assertEquals("Sí: las robaron.",
                ReadAfterColonTextPolicy.narrationText("Sí: las robaron.", true));
        assertEquals("Porque una república empieza así: alguien vende pan.",
                ReadAfterColonTextPolicy.narrationText(
                        "Porque una república empieza así: alguien vende pan.", true));
    }
}
