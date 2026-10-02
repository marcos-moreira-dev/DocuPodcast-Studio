package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Formula capability sharing the PP-StructureV3 runtime and model inventory. */
final class PpFormulaRecognitionEngine implements ContentAnalysisEngine {
    static final EngineId ID = new EngineId("pp-formulanet-local");
    private final PpStructureV3Engine delegate;

    PpFormulaRecognitionEngine(PpStructureV3Engine delegate) { this.delegate = delegate; }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.CONTENT_MATH_RECOGNITION,
                "Reconocimiento matemático local", "PP-FormulaNet Plus-M", "shared-managed-python",
                Set.of(EngineFeature.HARDWARE_ACCELERATION), false);
    }
    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of());
    }
    @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
        EngineReadiness current = delegate.inspectReadiness(configuration);
        return new EngineReadiness(ID, current.state(), current.summary(), current.issues(),
                current.recommendedActions(), current.diagnostics());
    }
    @Override public Set<ContentAnalysisOperation> operations() {
        return Set.of(ContentAnalysisOperation.MATH_RECOGNITION);
    }
    @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request, ExecutionContext context)
            throws IOException, InterruptedException {
        if (!operations().contains(request.operation())) {
            throw new IllegalArgumentException(
                    "PP-FormulaNet does not implement " + request.operation());
        }
        return delegate.analyzeShared(request, context);
    }
}
