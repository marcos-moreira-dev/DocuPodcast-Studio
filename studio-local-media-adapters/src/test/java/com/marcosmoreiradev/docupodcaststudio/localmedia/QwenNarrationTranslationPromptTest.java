package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QwenNarrationTranslationPromptTest {
    @Test
    void englishTargetIsSpelledOutAndSourceIsClearlyDelimited() {
        var request = new ContentAnalysisRequest(
                ContentAnalysisOperation.NARRATION_TRANSLATION, List.of(),
                "Preserve equations.", "La derivada de seno es coseno.", "en", "",
                Map.of("sourceLanguage", "es", "targetLanguage", "en"));

        String prompt = QwenVisualAnalysisEngine.groundedPrompt(request);

        assertTrue(prompt.contains("from Spanish (es) to English (en)"));
        assertTrue(prompt.contains("MUST be written entirely in English (en)"));
        assertTrue(prompt.contains("SOURCE NARRATION (Spanish (es))"));
        assertTrue(prompt.contains("La derivada de seno es coseno."));
        assertFalse(prompt.contains("Eres un traductor"),
                "English targets must not be framed by a Spanish meta-instruction");
    }
}
