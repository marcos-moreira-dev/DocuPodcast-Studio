package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Text-only semantic classification sharing the managed Qwen runtime and model. */
final class QwenNarratabilityAnalysisEngine implements ContentAnalysisEngine,
        ContentAnalysisBatchLifecycle {
    static final EngineId ID = new EngineId("qwen3-vl-narratability-local");
    private final QwenVisualAnalysisEngine delegate;

    QwenNarratabilityAnalysisEngine(QwenVisualAnalysisEngine delegate) {
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
    }

    @Override public EngineDescriptor descriptor() {
        return new EngineDescriptor(ID, CapabilityId.CONTENT_NARRATABILITY_ANALYSIS,
                "Filtro semántico local", "Qwen3-VL 4B", "shared-managed-ollama",
                Set.of(EngineFeature.HARDWARE_ACCELERATION), false);
    }

    @Override public EngineConfigurationSchema configurationSchema() {
        return new EngineConfigurationSchema(ID, List.of());
    }

    @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
        EngineReadiness readiness = delegate.inspectReadiness(configuration);
        return new EngineReadiness(ID, readiness.state(), readiness.summary(),
                readiness.issues(), readiness.recommendedActions(), readiness.diagnostics());
    }

    @Override public Set<ContentAnalysisOperation> operations() {
        return Set.of(ContentAnalysisOperation.NARRATABILITY_CLASSIFICATION);
    }

    @Override
    public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                         ExecutionContext context)
            throws IOException, InterruptedException {
        if (!operations().contains(request.operation())) {
            throw new IllegalArgumentException(
                    "Qwen narratability does not implement " + request.operation());
        }
        return delegate.analyzeShared(request, context);
    }

    @Override public Object contentAnalysisBatchIdentity() { return delegate; }
    @Override public void beginContentAnalysisBatch(ExecutionContext context) {
        delegate.beginContentAnalysisBatch(context);
    }
    @Override public void endContentAnalysisBatch(ExecutionContext context)
            throws IOException, InterruptedException {
        delegate.endContentAnalysisBatch(context);
    }
}
