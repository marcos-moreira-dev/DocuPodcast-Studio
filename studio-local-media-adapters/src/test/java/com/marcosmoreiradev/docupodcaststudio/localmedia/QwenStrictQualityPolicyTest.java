package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.AnalysisVisualInput;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QwenStrictQualityPolicyTest {
    @TempDir Path temp;

    @Test
    void configuredModelIsHonoredWithoutSilentQuantizationFallback() {
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                temp.resolve("ollama.exe"), temp.resolve("models"),
                temp.resolve("logs"));
        QwenVisualAnalysisEngine engine = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID,
                        Map.of("model", "qwen3-vl:4b-instruct-q4_K_M")),
                process);

        assertEquals("qwen3-vl:4b-instruct-q4_K_M", engine.selectedModel());
    }

    @Test
    void officialEightBQ8ProfileIsSelectableWithoutChangingRequestContract() {
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                temp.resolve("ollama.exe"), temp.resolve("models"),
                temp.resolve("logs"), "q8_0", true);
        QwenVisualAnalysisEngine engine = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of(
                        "model", "qwen3-vl:8b-instruct-q8_0",
                        "contextTokens", "8192",
                        "maxOutputTokens", "1800",
                        "kvCacheType", "q8_0")), process);

        assertEquals("qwen3-vl:8b-instruct-q8_0", engine.selectedModel());
        assertEquals("8192", engine.configurationSchema().fields().stream()
                .filter(field -> field.key().equals("contextTokens"))
                .findFirst().orElseThrow().defaultValue());
    }

    @Test
    void requestCannotSilentlySwitchAwayFromConfiguredProfile() {
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                temp.resolve("ollama.exe"), temp.resolve("models"),
                temp.resolve("logs"), "q8_0", true);
        QwenVisualAnalysisEngine engine = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of(
                        "model", "qwen3-vl:8b-instruct-q8_0")), process);
        ContentAnalysisRequest request = new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING, List.of(
                new AnalysisVisualInput(temp.resolve("page.png"),
                        "complete-page", "image/png")),
                "read", "", "es", "", Map.of(
                "model", "qwen3-vl:4b-instruct-q8_0"));

        IllegalArgumentException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> engine.analyzeShared(request, null));
        assertTrue(failure.getMessage().contains("no puede degradarse"));
    }

    @Test
    void visualEvidenceReceivesAGenerousDynamicContextWindow() {
        ContentAnalysisRequest oneRoi = new ContentAnalysisRequest(
                ContentAnalysisOperation.IMAGE_DESCRIPTION,
                List.of(new AnalysisVisualInput(
                        temp.resolve("roi.png"), "component-roi", "")),
                "Describe", "", "es", "", Map.of("maxOutputTokens", "100"));
        ContentAnalysisRequest roiAndContext = new ContentAnalysisRequest(
                ContentAnalysisOperation.IMAGE_DESCRIPTION,
                List.of(
                        new AnalysisVisualInput(temp.resolve("roi.png"),
                                "component-roi", ""),
                        new AnalysisVisualInput(temp.resolve("page.png"),
                                "context-page", "")),
                "Describe", "", "es", "", Map.of("maxOutputTokens", "100"));
        ContentAnalysisRequest complexWordImage = new ContentAnalysisRequest(
                ContentAnalysisOperation.IMAGE_DESCRIPTION,
                List.of(new AnalysisVisualInput(
                        temp.resolve("word-image.png"), "high-resolution-word-image", "")),
                "Describe", "", "es", "", Map.of(
                        "maxOutputTokens", "384", "contextWindowTokens", "16384"));

        assertTrue(QwenVisualAnalysisEngine.analysisOptions(oneRoi)
                .contains("\"num_ctx\":8192"));
        assertTrue(QwenVisualAnalysisEngine.analysisOptions(roiAndContext)
                .contains("\"num_ctx\":16384"));
        assertTrue(QwenVisualAnalysisEngine.analysisOptions(complexWordImage)
                .contains("\"num_ctx\":16384"));
    }

    @Test
    void completePageReadingUsesTheMeasuredEightKBlockProfile() {
        ContentAnalysisRequest page = new ContentAnalysisRequest(
                ContentAnalysisOperation.PAGE_SEMANTIC_READING,
                List.of(new AnalysisVisualInput(
                        temp.resolve("page.png"), "complete-page", "image/png")),
                "Lee la pagina", "", "es", "",
                Map.of("maxOutputTokens", "1800",
                        "contextWindowTokens", "8192"));

        String options = QwenVisualAnalysisEngine.analysisOptions(page);

        assertTrue(options.contains("\"num_ctx\":8192"));
        assertTrue(options.contains("\"num_predict\":1800"));
        assertTrue(options.contains("\"num_batch\":512"));
    }

    @Test
    void chatPayloadUsesVersionCompatibleKeepAliveTypesAndNoExecutionObjects() {
        String options = "{\"temperature\":0,\"seed\":42,\"num_ctx\":8192,"
                + "\"num_predict\":1800,\"num_batch\":512}";

        String batch = QwenVisualAnalysisEngine.chatPayload(
                QwenVisualAnalysisEngine.Q8_MODEL, true, true, "", options,
                "Lee", "\"YWJj\"");
        String standalone = QwenVisualAnalysisEngine.chatPayload(
                QwenVisualAnalysisEngine.Q8_MODEL, false, false, "", options,
                "Lee", "\"YWJj\"");

        assertTrue(batch.contains("\"keep_alive\":-1"));
        assertFalse(batch.contains("\"keep_alive\":\"-1\""));
        assertTrue(standalone.contains("\"keep_alive\":\"10m\""));
        assertTrue(batch.contains("\"num_ctx\":8192"));
        assertTrue(batch.contains("\"num_predict\":1800"));
        assertTrue(batch.contains("\"num_batch\":512"));
        assertFalse(batch.contains("OperationDeadline"));
        assertFalse(batch.contains("ExecutionContext"));
        assertFalse(batch.contains("deadline"));
    }

    @Test
    void deterministicHttp400IsRequestRejectedAndNeverRetryable() {
        EngineDiagnosticCode code = QwenVisualAnalysisEngine.diagnosticCodeForHttp(
                400, "{\"error\":\"invalid duration\"}", false);

        assertEquals(EngineDiagnosticCode.REQUEST_REJECTED, code);
        assertFalse(QwenVisualAnalysisEngine.retryable(code));
        assertFalse(code == EngineDiagnosticCode.PROTOCOL_INVALID);
        assertEquals("{\"error\":\"time: missing unit in duration \\\"-1\\\"\"}",
                QwenVisualAnalysisEngine.safeErrorBody(
                        "{\"error\":\"time: missing unit in duration \\\"-1\\\"\"}"));
    }

    @Test
    void retryPolicyNeverRepeatsAnIdenticalTruncatedOrInvalidRequest() {
        assertTrue(QwenVisualAnalysisEngine.retryable(
                EngineDiagnosticCode.TRANSPORT_TRANSIENT));
        for (EngineDiagnosticCode code : List.of(
                EngineDiagnosticCode.OUTPUT_TRUNCATED,
                EngineDiagnosticCode.PROTOCOL_INVALID,
                EngineDiagnosticCode.REQUEST_REJECTED,
                EngineDiagnosticCode.INVALID_OUTPUT,
                EngineDiagnosticCode.REQUEST_TIMEOUT,
                EngineDiagnosticCode.REQUEST_STALL,
                EngineDiagnosticCode.OOM,
                EngineDiagnosticCode.CANCELLED)) {
            assertEquals(false, QwenVisualAnalysisEngine.retryable(code), code.name());
        }
    }

    @Test
    void sumsActualVramReportedByOllama() {
        String runtime = """
                {"models":[
                  {"name":"qwen-a","size_vram":1200000000},
                  {"name":"qwen-b","size_vram":600000000}
                ]}
                """;

        assertEquals(1_800_000_000L,
                QwenVisualAnalysisEngine.runtimeVramBytes(runtime));
    }

    @Test
    void missingVramEvidenceIsNotTreatedAsGpuExecution() {
        assertEquals(0L,
                QwenVisualAnalysisEngine.runtimeVramBytes(
                        "{\"models\":[{\"name\":\"qwen\"}]}"));
        assertEquals(0L,
                QwenVisualAnalysisEngine.runtimeVramBytes(""));
    }
}
