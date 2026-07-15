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
}
