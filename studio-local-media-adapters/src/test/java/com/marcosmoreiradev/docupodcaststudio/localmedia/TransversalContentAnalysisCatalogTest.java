package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.CapabilityId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisEngineRegistry;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TransversalContentAnalysisCatalogTest {
    @Test
    void sharedRuntimesPublishOneNeutralEnginePerCapability() {
        ManagedOllamaProcess ollama = new ManagedOllamaProcess(
                Path.of("missing-ollama.exe"), Path.of("missing-models"), Path.of("missing-logs"));
        QwenVisualAnalysisEngine visual = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of()), ollama);
        QwenContextCorrectionEngine correction = new QwenContextCorrectionEngine(visual);
        QwenNarratabilityAnalysisEngine narratability =
                new QwenNarratabilityAnalysisEngine(visual);
        PpStructureV3Engine layout = new PpStructureV3Engine(
                new EngineConfiguration(PpStructureV3Engine.ID, Map.of()));
        PpFormulaRecognitionEngine formula = new PpFormulaRecognitionEngine(layout);

        assertEquals(CapabilityId.VISUAL_CONTENT_DESCRIPTION,
                visual.descriptor().capability());
        assertEquals(java.util.Set.of(
                        ContentAnalysisOperation.IMAGE_DESCRIPTION,
                        ContentAnalysisOperation.IMAGE_PROMPT_PLANNING,
                        ContentAnalysisOperation.IMAGE_QUALITY_REVIEW,
                        ContentAnalysisOperation.PAGE_SEMANTIC_READING),
                visual.operations());
        assertEquals(CapabilityId.CONTENT_CONTEXT_CORRECTION,
                correction.descriptor().capability());
        assertEquals(java.util.Set.of(ContentAnalysisOperation.CONTEXT_CORRECTION),
                correction.operations());
        assertEquals(CapabilityId.CONTENT_NARRATABILITY_ANALYSIS,
                narratability.descriptor().capability());
        assertEquals(java.util.Set.of(
                        ContentAnalysisOperation.NARRATABILITY_CLASSIFICATION),
                narratability.operations());
        ContentAnalysisEngineRegistry registry = new ContentAnalysisEngineRegistry()
                .register(visual)
                .register(correction)
                .register(narratability);
        assertEquals(java.util.List.of(narratability),
                registry.supporting(
                        ContentAnalysisOperation.NARRATABILITY_CLASSIFICATION));
        assertEquals(CapabilityId.CONTENT_LAYOUT_ANALYSIS,
                layout.descriptor().capability());
        assertEquals(java.util.Set.of(
                        ContentAnalysisOperation.LAYOUT_ANALYSIS,
                        ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION),
                layout.operations());
        assertEquals(CapabilityId.CONTENT_MATH_RECOGNITION,
                formula.descriptor().capability());
        assertEquals(java.util.Set.of(ContentAnalysisOperation.MATH_RECOGNITION),
                formula.operations());
    }
}
