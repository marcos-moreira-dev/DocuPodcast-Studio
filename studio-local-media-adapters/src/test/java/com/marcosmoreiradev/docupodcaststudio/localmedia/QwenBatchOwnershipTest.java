package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class QwenBatchOwnershipTest {
    @TempDir Path root;

    @Test
    void batchOwnershipIsExplicitAndCanCloseOnAnotherThread() throws Exception {
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                root.resolve("ollama.exe"), root.resolve("models"), root.resolve("logs"));
        QwenVisualAnalysisEngine engine = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of()), process);
        ExecutionContext context = ExecutionContext.defaults("document-batch-1");

        engine.beginContentAnalysisBatch(context);
        engine.beginContentAnalysisBatch(context);
        assertEquals(1, process.lifecycleSnapshot().residencyOwners());
        assertEquals("-1", QwenVisualAnalysisEngine.keepAliveFor(true),
                "an active logical batch must keep the model resident");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                engine.endContentAnalysisBatch(context);
                engine.endContentAnalysisBatch(context);
                return null;
            }).get(2, TimeUnit.SECONDS);
        }
        assertEquals(0, process.lifecycleSnapshot().residencyOwners());
        assertEquals("10m", QwenVisualAnalysisEngine.keepAliveFor(false),
                "outside a batch the runtime may apply its bounded idle policy");
    }
}
