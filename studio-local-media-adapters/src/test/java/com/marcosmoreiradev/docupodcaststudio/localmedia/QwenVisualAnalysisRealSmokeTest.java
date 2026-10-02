package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Explicit physical gate; never runs during ordinary CI or page opening. */
@EnabledIfSystemProperty(named = "docupodcast.qwen.realSmoke", matches = "true")
final class QwenVisualAnalysisRealSmokeTest {
    @Test
    void selectedModelDescribesARealCorpusPageGpuFirstAndLeavesNoProcess()
            throws Exception {
        Path root = repositoryRoot();
        String model = QwenVisualAnalysisEngine.Q8_MODEL;
        Path image = root.resolve(
                "target/pdf-real-corpus/burden/page-0895-overlay.png");
        assertTrue(Files.isRegularFile(image), "real corpus overlay is required");
        MediaEnginePlatform platform =
                LocalMediaAdapters.create(LocalMediaLayout.development(root));
        ExecutionContext base = ExecutionContext.defaults("qwen-q8-real-smoke");
        ExecutionContext gpuFirst = new ExecutionContext(
                base.operationId(), base.cancellation(), base.progress(),
                base.policy(), base.resourceLease(), base.staging(),
                ComputePreference.preferGpu(true));
        try {
            platform.administration().require(QwenVisualAnalysisEngine.ID).execute(
                    new EngineActionRequest(QwenVisualAnalysisEngine.ID,
                            EngineActionId.SMOKE_TEST,
                            Map.of("model", model,
                                    "testImage", image.toString())),
                    gpuFirst);
            EngineCertificationRecord certification =
                    new FileEngineCertificationStore(root.resolve(
                            "state/document-ai-certifications"))
                            .find(QwenVisualAnalysisEngine.ID, model).orElseThrow();
            assertTrue(certification.visualInputVerified());
            assertEquals(model, certification.modelId());
        } finally {
            platform.administration().require(QwenVisualAnalysisEngine.ID).execute(
                    new EngineActionRequest(QwenVisualAnalysisEngine.ID,
                            EngineActionId.STOP, Map.of()), gpuFirst);
        }
    }

    private static Path repositoryRoot() {
        Path current = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("tools/document-ai"))) return current;
        Path parent = current.getParent();
        if (parent != null
                && Files.isDirectory(parent.resolve("tools/document-ai"))) return parent;
        throw new IllegalStateException("No se encontró la raíz del repositorio.");
    }
}
