package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class TransversalVisualPdfTreatmentEngineTest {
    @TempDir Path temp;

    @Test
    void sendsRoiFirstWithoutContextByDefaultAndPersistsGroundingDiagnostics() throws Exception {
        CapturingEngine adapter = new CapturingEngine("""
                {"status":"OK","objectId":"OBJ-1","narrationText":"El flujo Q conecta dos bloques.",
                "claims":[{"text":"flujo","evidenceIds":["roi"]}],"recognizedLabels":["Q"],
                "uncertainties":[],"confidence":0.9}
                """, "El flujo Q conecta dos bloques.");
        Path roi = image("roi.png", "roi");
        Path context = image("context.png", "page");

        PdfDerivedTreatment treatment = engine(adapter).generate(page(), List.of(region()),
                request(roi, context, Map.of("recognizedLabels", "Q")));

        assertEquals(PdfDerivedTreatmentState.DRAFT, treatment.state());
        assertEquals("OK", treatment.metadata().get("groundingStatus"));
        assertEquals(1, adapter.lastRequest.visualInputs().size());
        assertEquals("high-resolution-roi", adapter.lastRequest.visualInputs().getFirst().role());
        assertEquals("qwen3-vl:4b-instruct-q8_0",
                adapter.lastRequest.options().get("model"));
        assertEquals("320", adapter.lastRequest.options().get("maxOutputTokens"));
        assertTrue(adapter.lastRequest.instruction().contains("45 y 90 palabras"));
        assertTrue(adapter.lastRequest.responseSchema().contains("INSUFFICIENT_EVIDENCE"));
        assertFalse(treatment.metadata().get("roiSha256").isBlank());
        assertEquals("18", treatment.metadata().get("outputTokens"));
    }

    @Test
    void rejectsGlobalOrUnsupportedNumericResponseAsInsufficientEvidence() throws Exception {
        CapturingEngine adapter = new CapturingEngine("""
                {"status":"OK","objectId":"OBJ-1","narrationText":"Esta página presenta 99 resultados.",
                "claims":[],"recognizedLabels":[],"uncertainties":[],"confidence":0.8}
                """, "Esta página presenta 99 resultados.");
        Path roi = image("roi.png", "roi");

        PdfDerivedTreatment treatment = engine(adapter).generate(page(), List.of(region()),
                request(roi, null, Map.of()));

        assertEquals("INSUFFICIENT_EVIDENCE", treatment.metadata().get("groundingStatus"));
        assertTrue(treatment.metadata().get("groundingFailures").contains("unsupported-number"));
        assertTrue(treatment.metadata().get("groundingFailures").contains("global-page-summary"));
        assertEquals(PdfDerivedTreatmentState.DRAFT, treatment.state());
    }

    @Test
    void requiresObjectIdAndNeverLetsContextReplaceRoi() throws Exception {
        CapturingEngine adapter = new CapturingEngine("{}", "");
        Path context = image("context.png", "page");
        var request = new PdfDerivedTreatmentGenerationRequest(temp, 1,
                PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, List.of("IMAGE"), "", true,
                Map.of("contextImage", context.toString(), "includeContextPage", "true"));

        assertThrows(java.io.IOException.class,
                () -> engine(adapter).generate(page(), List.of(region()), request));
        assertNull(adapter.lastRequest);
    }

    private TransversalVisualPdfTreatmentEngine engine(CapturingEngine adapter) {
        ContentAnalysisEngineRegistry registry = new ContentAnalysisEngineRegistry().register(adapter);
        MediaCapabilityService media = new MediaCapabilityService(
                new MediaEnginePlatform(null, null, null, null, null, null, registry, null),
                LocalResourceScheduler.safeDefaults());
        return new TransversalVisualPdfTreatmentEngine(media);
    }

    private PdfDerivedTreatmentGenerationRequest request(Path roi, Path context, Map<String,String> extra) {
        java.util.LinkedHashMap<String,String> options = new java.util.LinkedHashMap<>(extra);
        options.put("objectId", "OBJ-1");
        options.put("roiImage", roi.toString());
        options.put("nearbyContext", "Etiqueta Q junto al flujo.");
        if (context != null) options.put("contextImage", context.toString());
        return new PdfDerivedTreatmentGenerationRequest(temp, 1, PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of("IMAGE"), "", true, options);
    }

    private Path image(String name, String content) throws Exception {
        Path path = temp.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private static PreparedPdfPage page() {
        return new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(region()), List.of(), "");
    }

    private static PdfRegion region() {
        return new PdfRegion("IMAGE", 1, 100, 100, 400, 300, 0, 0, "Etiqueta Q",
                PdfRegionType.IMAGE, PdfNarratability.UNCERTAIN, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.9, "e", "p", "g", "c"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    private static final class CapturingEngine implements ContentAnalysisEngine {
        private final String json;
        private final String text;
        private ContentAnalysisRequest lastRequest;
        private CapturingEngine(String json, String text) { this.json = json; this.text = text; }
        @Override public EngineDescriptor descriptor() { return new EngineDescriptor(new EngineId("grounded-test"),
                CapabilityId.VISUAL_CONTENT_DESCRIPTION, "test", "1", "test", Set.of(), true); }
        @Override public Set<ContentAnalysisOperation> operations() { return Set.of(ContentAnalysisOperation.IMAGE_DESCRIPTION); }
        @Override public EngineConfigurationSchema configurationSchema() { return new EngineConfigurationSchema(descriptor().id(), List.of()); }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) { return EngineReadiness.ready(descriptor().id(), "ready"); }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request, ExecutionContext context) {
            lastRequest = request;
            return new ContentAnalysisResult(text, json, 0.9, List.of(), Map.of(
                    "engineId", "grounded-test", "model", "qwen-test", "durationMs", "12",
                    "doneReason", "stop", "promptTokens", "42", "outputTokens", "18"));
        }
    }
}
