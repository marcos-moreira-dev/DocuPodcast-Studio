package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OllamaJsonTest {
    @Test
    void extractsACompleteObjectFromAMarkdownWrappedResponse() {
        String response = """
                ```json
                {"narrationText":"Curva con {hueco}","confidence":0.9}
                ```
                """;

        assertEquals(
                "{\"narrationText\":\"Curva con {hueco}\",\"confidence\":0.9}",
                OllamaJson.firstObject(response));
    }

    @Test
    void rejectsATruncatedObjectInsteadOfParsingPartialFields() {
        assertEquals("", OllamaJson.firstObject(
                "{\"narrationText\":\"Descripción incompleta\""));
    }
}
