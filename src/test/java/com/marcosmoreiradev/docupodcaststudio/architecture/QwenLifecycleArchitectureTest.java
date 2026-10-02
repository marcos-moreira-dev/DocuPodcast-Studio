package com.marcosmoreiradev.docupodcaststudio.architecture;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QwenLifecycleArchitectureTest {
    @Test
    void qwenUsesExplicitGlobalOwnershipAndProductionCapacityRemainsOne()
            throws Exception {
        String qwen = Files.readString(Path.of(
                "studio-local-media-adapters/src/main/java/com/marcosmoreiradev/"
                        + "docupodcaststudio/localmedia/QwenVisualAnalysisEngine.java"),
                StandardCharsets.UTF_8);
        String budget = Files.readString(Path.of(
                "studio-media-api/src/main/java/com/marcosmoreiradev/"
                        + "docupodcaststudio/media/api/ComputeResourceBudget.java"),
                StandardCharsets.UTF_8);
        String runtime = Files.readString(Path.of(
                "studio-local-media-adapters/src/main/java/com/marcosmoreiradev/"
                        + "docupodcaststudio/localmedia/ManagedOllamaProcess.java"),
                StandardCharsets.UTF_8);

        assertFalse(qwen.contains("ThreadLocal"),
                "model/batch ownership must not be inferred from a carrier thread");
        assertTrue(qwen.contains("retainModelResidency"));
        assertTrue(qwen.contains("openModelRequest"));
        assertTrue(budget.contains("ResourceId.QWEN_INFERENCE, 1"),
                "thread-safe tests must not enable productive Qwen concurrency");
        assertTrue(runtime.contains("this.runtimeParallel = 1;"));
        assertFalse(runtime.contains("experimentalConcurrency"));
    }
}
