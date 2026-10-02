package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionId;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineActionRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ManagedOllamaProcessFailureTest {
    @TempDir Path root;

    @Test
    void retriesThreePrivatePortsAndReportsConflictWithoutAdoptingExternalServer()
            throws Exception {
        Path executable = root.resolve("ollama.exe");
        Files.writeString(executable, "fixture", StandardCharsets.UTF_8);
        AtomicInteger ports = new AtomicInteger(19000);
        AtomicInteger launches = new AtomicInteger();
        AtomicReference<Map<String, String>> environment = new AtomicReference<>();
        ManagedOllamaProcess process = new ManagedOllamaProcess(
                executable, root.resolve("models"), root.resolve("logs"),
                ports::incrementAndGet,
                builder -> {
                    launches.incrementAndGet();
                    environment.set(Map.copyOf(builder.environment()));
                    return new ProcessBuilder("cmd", "/c",
                            "echo bind: address already in use & exit /b 1")
                            .redirectErrorStream(true)
                            .redirectOutput(ProcessBuilder.Redirect.appendTo(
                                    root.resolve("logs/visual-analysis.log").toFile()))
                            .start();
                },
                new ManagedOllamaProcess.EndpointClient() {
                    @Override public boolean ready(java.net.URI endpoint) { return false; }
                    @Override public ManagedOllamaProcess.EndpointResponse post(
                            java.net.URI endpoint, String body, java.time.Duration timeout) {
                        return new ManagedOllamaProcess.EndpointResponse(500, "");
                    }
                    @Override public ManagedOllamaProcess.EndpointResponse get(
                            java.net.URI endpoint, java.time.Duration timeout) {
                        return new ManagedOllamaProcess.EndpointResponse(500, "");
                    }
                }, System::nanoTime);

        EngineExecutionException failure = assertThrows(EngineExecutionException.class,
                () -> process.ensureReady(ExecutionContext.defaults("port-conflict")));

        assertEquals(EngineDiagnosticCode.PORT_CONFLICT, failure.code());
        assertEquals(3, launches.get());
        assertEquals("true", environment.get().get("OLLAMA_FLASH_ATTENTION"));
        assertEquals("q8_0", environment.get().get("OLLAMA_KV_CACHE_TYPE"));
        assertEquals("1", environment.get().get("OLLAMA_NUM_PARALLEL"));
        assertEquals("1", environment.get().get("OLLAMA_MAX_LOADED_MODELS"));
    }

    @Test
    void refusesLargePreparationWhenControlledDiskProbeReportsNoSpace() {
        Path executable = root.resolve("tools/document-ai/ollama/ollama.exe");
        Path models = root.resolve("tools/document-ai/ollama-models");
        Path logs = root.resolve("logs/document-ai");
        RuntimeAssetCatalog assets = new RuntimeAssetCatalog(root, Map.of(
                QwenVisualAnalysisEngine.ID, Map.of(
                        "executable", executable, "models", models, "logs", logs)));
        ManagedOllamaProcess process = new ManagedOllamaProcess(executable, models, logs);
        QwenVisualAnalysisEngine engine = new QwenVisualAnalysisEngine(
                new EngineConfiguration(QwenVisualAnalysisEngine.ID, Map.of()), process);
        QwenVisualAnalysisAdministration administration =
                new QwenVisualAnalysisAdministration(engine, assets, process, ignored -> 1L);

        EngineExecutionException failure = assertThrows(EngineExecutionException.class,
                () -> administration.execute(new EngineActionRequest(
                                QwenVisualAnalysisEngine.ID, EngineActionId.INSTALL,
                                Map.of("model", "Q4")),
                        ExecutionContext.defaults("no-space")));

        assertEquals(EngineDiagnosticCode.NO_SPACE, failure.code());
    }
}
