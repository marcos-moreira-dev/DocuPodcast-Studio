package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QwenRequestIsolationDiagnosticsTest {
    @Test
    void sequentialPagesUseDifferentContextsButTheSameResidencyKey() {
        var key1 = QwenVisualAnalysisEngine.modelKey(ExecutionContext.defaults("page-1"));
        var key2 = QwenVisualAnalysisEngine.modelKey(ExecutionContext.defaults("page-2"));
        var first = QwenVisualAnalysisEngine.requestContextDiagnostics("page-1:qwen:1", key1);
        var second = QwenVisualAnalysisEngine.requestContextDiagnostics("page-2:qwen:2", key2);

        assertNotEquals(first.get("contextId"), second.get("contextId"));
        assertEquals("0", first.get("historyPages"));
        assertEquals("0", second.get("historyPages"));
        assertEquals("true", first.get("freshContext"));
        assertEquals(first.get("modelResidencyKey"), second.get("modelResidencyKey"));
    }

    @Test
    void heartbeatDoesNotSpamBeforeItsInterval() {
        long start = 1_000_000L;
        assertFalse(QwenVisualAnalysisEngine.heartbeatDue(
                start + Duration.ofSeconds(44).toNanos(), start, Duration.ofSeconds(45)));
        assertTrue(QwenVisualAnalysisEngine.heartbeatDue(
                start + Duration.ofSeconds(45).toNanos(), start, Duration.ofSeconds(45)));
    }
}
