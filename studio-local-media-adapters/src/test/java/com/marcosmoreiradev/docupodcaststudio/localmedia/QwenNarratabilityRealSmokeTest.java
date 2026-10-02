package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Measures the text-only page filter through the same managed Qwen runtime. */
@EnabledIfSystemProperty(
        named = "docupodcast.qwen.narratabilitySmoke", matches = "true")
final class QwenNarratabilityRealSmokeTest {
    @Test
    void q8ClassifiesEveryStableRegionWithoutImages() throws Exception {
        Path root = repositoryRoot();
        String model = QwenVisualAnalysisEngine.Q8_MODEL;
        MediaEnginePlatform platform =
                LocalMediaAdapters.create(LocalMediaLayout.development(root));
        ExecutionContext base =
                ExecutionContext.defaults("qwen-q8-narratability-real-smoke");
        ExecutionContext gpuFirst = new ExecutionContext(
                base.operationId(), base.cancellation(), base.progress(),
                base.policy(), base.resourceLease(), base.staging(),
                ComputePreference.preferGpu(true));
        String page = """
                ID=R-001
                TYPE=HEADING
                AUTO=NARRATABLE
                TEXT=Error Analysis and Computer Arithmetic
                ---
                ID=R-002
                TYPE=PARAGRAPH
                AUTO=NARRATABLE
                TEXT=This section introduces the sources of error in numerical computation.
                ---
                ID=R-003
                TYPE=PARAGRAPH
                AUTO=UNCERTAIN
                TEXT=lI | 0 O ; ; 1l ???
                ---
                ID=R-004
                TYPE=PAGE_NUMBER
                AUTO=NON_NARRATABLE
                TEXT=895
                ---
                ID=R-005
                TYPE=PARAGRAPH
                AUTO=NARRATABLE
                TEXT=Round-off error is produced because a computer represents only finitely many digits.
                ---
                ID=R-006
                TYPE=FOOTER
                AUTO=NON_NARRATABLE
                TEXT=Copyright 2016 Cengage Learning
                ---
                """;
        String schema = """
                {"type":"object","properties":{
                  "decisions":{"type":"array","items":{"type":"object","properties":{
                    "regionId":{"type":"string"},
                    "narratability":{"type":"string","enum":["NARRATABLE","NON_NARRATABLE","UNCERTAIN"]},
                    "reason":{"type":"string","enum":["CLEAR_PROSE","STRUCTURAL_METADATA",
                      "OCR_GIBBERISH","TABLE_FRAGMENT","FORMULA_FRAGMENT","AMBIGUOUS","OTHER"]},
                    "confidence":{"type":"number","minimum":0,"maximum":1}
                  },"required":["regionId","narratability","reason","confidence"]}},
                  "uncertainties":{"type":"array","items":{"type":"string"}},
                  "confidence":{"type":"number","minimum":0,"maximum":1}
                },"required":["decisions","uncertainties","confidence"]}
                """;
        Instant started = Instant.now();
        try {
            ContentAnalysisEngine engine = platform.contentAnalysisEngines()
                    .find(QwenNarratabilityAnalysisEngine.ID).orElseThrow();
            ContentAnalysisRequest request = new ContentAnalysisRequest(
                            ContentAnalysisOperation.NARRATABILITY_CLASSIFICATION,
                            List.of(),
                            "Clasifica todas las regiones por ID. No reescribas el texto. "
                                    + "Usa CLEAR_PROSE para NARRATABLE, AMBIGUOUS para UNCERTAIN, "
                                    + "STRUCTURAL_METADATA para cabeceras, pies o números de página, "
                                    + "OCR_GIBBERISH para texto ilegible y TABLE_FRAGMENT solo para tablas.",
                            page, "es", schema,
                            Map.of("model", model,
                                    "maxOutputTokens", "384"));
            ContentAnalysisResult result = engine.analyze(request, gpuFirst);
            long coldMillis = Duration.between(started, Instant.now()).toMillis();
            for (int index = 1; index <= 6; index++) {
                assertTrue(result.structuredJson().contains(
                        "R-%03d".formatted(index)));
            }
            assertDecision(result.structuredJson(), "R-001", "NARRATABLE");
            assertDecision(result.structuredJson(), "R-002", "NARRATABLE");
            assertDecision(result.structuredJson(), "R-003", "UNCERTAIN");
            assertDecision(result.structuredJson(), "R-004", "NON_NARRATABLE");
            assertDecision(result.structuredJson(), "R-005", "NARRATABLE");
            assertDecision(result.structuredJson(), "R-006", "NON_NARRATABLE");
            Instant warmStarted = Instant.now();
            ContentAnalysisResult warm = engine.analyze(request, gpuFirst);
            long warmMillis =
                    Duration.between(warmStarted, Instant.now()).toMillis();
            assertDecision(warm.structuredJson(), "R-003", "UNCERTAIN");
            assertDecision(warm.structuredJson(), "R-004", "NON_NARRATABLE");
            Path report = root.resolve(
                    "target/document-ai/q8-narratability-smoke.json");
            Files.createDirectories(report.getParent());
            Files.writeString(report, """
                    {"coldElapsedMillis":%d,"warmElapsedMillis":%d,
                     "model":"%s","result":%s}
                    """.formatted(
                    coldMillis, warmMillis,
                    model,
                    result.structuredJson()), StandardCharsets.UTF_8);
        } finally {
            platform.administration().require(QwenVisualAnalysisEngine.ID).execute(
                    new EngineActionRequest(QwenVisualAnalysisEngine.ID,
                            EngineActionId.STOP, Map.of()), gpuFirst);
        }
    }

    private static void assertDecision(String json, String regionId, String decision) {
        assertTrue(java.util.regex.Pattern.compile(
                        "\"regionId\"\\s*:\\s*\"" + regionId
                                + "\"[^{}]*\"narratability\"\\s*:\\s*\""
                                + decision + "\"",
                        java.util.regex.Pattern.DOTALL)
                .matcher(json).find());
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
