package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MathCatSpeechEngineTest {
    @TempDir Path temporaryRuntime;

    @Test
    void speaksValidatedMathMlInSpanishWithoutNetwork() throws Exception {
        MathCatSpeechEngine engine = new MathCatSpeechEngine(temporaryRuntime);

        ContentAnalysisResult result = engine.analyze(new ContentAnalysisRequest(
                ContentAnalysisOperation.MATH_SPEECH,
                List.of(),
                "Convierte la fórmula validada a habla.",
                "",
                "es",
                "",
                Map.of("mathMl", """
                        <math xmlns="http://www.w3.org/1998/Math/MathML">
                          <mrow><mi>x</mi><mo>=</mo><mfrac><mn>1</mn><mn>2</mn></mfrac></mrow>
                        </math>
                        """)),
                ExecutionContext.defaults("mathcat-test"));

        assertFalse(result.text().isBlank());
        assertTrue(result.diagnostics().get("local").equals("true"));
        assertTrue(java.nio.file.Files.isRegularFile(temporaryRuntime.resolve(
                "tools/document-ai/mathcat/MathCAT/Rules/Languages/es/SimpleSpeak_Rules.yaml")));
    }
}
