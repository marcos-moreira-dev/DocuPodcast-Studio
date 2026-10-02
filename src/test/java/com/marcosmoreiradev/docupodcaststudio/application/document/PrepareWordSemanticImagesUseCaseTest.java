package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class PrepareWordSemanticImagesUseCaseTest {
    @TempDir Path project;

    @Test
    void suppliesThreePreviousParagraphsAndOnlyOneFollowingAsAdvisoryContext() {
        List<DocumentBlock> blocks = List.of(
                DocumentBlock.of("P-0", DocumentBlockType.PARAGRAPH, "Fuera de ventana.", ""),
                DocumentBlock.of("P-1", DocumentBlockType.PARAGRAPH, "Primer anterior.", ""),
                DocumentBlock.of("P-2", DocumentBlockType.PARAGRAPH, "Segundo anterior.", ""),
                DocumentBlock.of("P-3", DocumentBlockType.PARAGRAPH, "Tercer anterior.", ""),
                DocumentBlock.of("IMG", DocumentBlockType.IMAGE_NOTICE, "Imagen", ""),
                DocumentBlock.of("P-4", DocumentBlockType.PARAGRAPH, "Primer posterior.", ""),
                DocumentBlock.of("P-5", DocumentBlockType.PARAGRAPH, "Posterior excluido.", ""));

        String context = PrepareWordSemanticImagesUseCase.nearbyContext(blocks, 4);

        assertTrue(context.startsWith("CONTEXTO ORIENTATIVO; NO ES EVIDENCIA VISUAL."));
        assertFalse(context.contains("Fuera de ventana."));
        assertTrue(context.contains("ANTERIOR P-1: Primer anterior."));
        assertTrue(context.contains("ANTERIOR P-2: Segundo anterior."));
        assertTrue(context.contains("ANTERIOR P-3: Tercer anterior."));
        assertTrue(context.contains("POSTERIOR P-4: Primer posterior."));
        assertFalse(context.contains("Posterior excluido."));
    }

    @Test
    void describesAuthorizedEmbeddedImageWithExactQ8AndPageSizedCleanContext() throws Exception {
        CapturingEngine engine = new CapturingEngine();
        ReadableDocument document = document(Map.of(
                "embeddedImageBase64", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                "embeddedImageMimeType", "image/png"));

        var result = useCase(engine).execute(document, project,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, "es");

        assertEquals(1, result.generated());
        assertEquals(PrepareWordSemanticImagesUseCase.MODEL,
                engine.lastRequest.options().get("model"));
        assertEquals("8192", engine.lastRequest.options().get("contextWindowTokens"));
        assertEquals(1, engine.lastRequest.visualInputs().size());
        assertEquals("high-resolution-word-image",
                engine.lastRequest.visualInputs().getFirst().role());
        DocumentBlock image = result.document().blockById("IMG-1").orElseThrow();
        assertEquals("DRAFT", image.metadata().get("descriptionState"));
        assertEquals("DRAFT_POLICY_VALIDATED", image.metadata().get("automaticAdmission"));
        assertTrue(image.metadata().get("description").split("\\s+").length >= 12);
    }

    @Test
    void preservesManualDescriptionAndOmittedPolicyNeverInvokesQwen() throws Exception {
        CapturingEngine engine = new CapturingEngine();
        ReadableDocument manual = document(Map.of(
                "description", "Descripcion manual que conserva el autor.",
                "embeddedImageBase64", Base64.getEncoder().encodeToString(new byte[]{1}),
                "embeddedImageMimeType", "image/png"));

        var manualResult = useCase(engine).execute(manual, project,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, "es");
        var omittedResult = useCase(engine).execute(document(Map.of(
                        "embeddedImageBase64", Base64.getEncoder().encodeToString(new byte[]{2}),
                        "embeddedImageMimeType", "image/png")), project,
                SecondarySemanticReadingPolicy.OMIT_ALL, "es");

        assertEquals(0, manualResult.generated());
        assertEquals("Descripcion manual que conserva el autor.", manualResult.document()
                .blockById("IMG-1").orElseThrow().metadata().get("description"));
        assertEquals(0, omittedResult.generated());
        assertNull(engine.lastRequest);
    }

    @Test
    void javaKeepsCanonicalBlockIdentityWhenTheModelEchoesAnotherObjectId() throws Exception {
        CapturingEngine engine = new CapturingEngine("MODEL-INVENTED-ID");
        ReadableDocument document = document(Map.of(
                "embeddedImageBase64", Base64.getEncoder().encodeToString(new byte[]{4, 5, 6}),
                "embeddedImageMimeType", "image/png"));

        var result = useCase(engine).execute(document, project,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, "es");

        assertEquals(1, result.generated());
        DocumentBlock image = result.document().blockById("IMG-1").orElseThrow();
        assertEquals("MODEL-INVENTED-ID",
                image.metadata().get("descriptionReturnedObjectId"));
        assertFalse(image.metadata().get("description").isBlank());
    }

    @Test
    void retriesInsufficientContextualReadingOnceWithAVisualOnlyStrategy() throws Exception {
        RecoveringEngine engine = new RecoveringEngine();
        ReadableDocument document = document(Map.of(
                "embeddedImageBase64", Base64.getEncoder().encodeToString(new byte[]{7, 8, 9}),
                "embeddedImageMimeType", "image/png"));

        var result = useCase(engine).execute(document, project,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, "es");

        assertEquals(1, result.generated());
        assertEquals(2, engine.calls);
        assertFalse(engine.firstRequest.nearbyContext().isBlank());
        assertTrue(engine.secondRequest.nearbyContext().isBlank());
        assertEquals("VISUAL_ONLY_NO_CONTEXT",
                engine.secondRequest.options().get("recoveryStrategy"));
    }

    @Test
    void cancellationReturnsAlreadyAcceptedDescriptionsInsteadOfDiscardingThem()
            throws Exception {
        InterruptingAfterFirstEngine engine = new InterruptingAfterFirstEngine();
        String embedded = Base64.getEncoder().encodeToString(new byte[]{11, 12, 13});
        ReadableDocument document = new ReadableDocument("Word",
                SourceDocumentFormat.DOCX, project.resolve("source.docx"), List.of(
                DocumentBlock.of("IMG-1", DocumentBlockType.IMAGE_NOTICE,
                        "Primera imagen", "", Map.of(
                        "embeddedImageBase64", embedded,
                        "embeddedImageMimeType", "image/png")),
                DocumentBlock.of("IMG-2", DocumentBlockType.IMAGE_NOTICE,
                        "Segunda imagen", "", Map.of(
                        "embeddedImageBase64", embedded,
                        "embeddedImageMimeType", "image/png"))));

        PrepareWordSemanticImagesUseCase.Result result;
        try {
            result = useCase(engine).execute(document, project,
                    SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, "es");
        } finally {
            Thread.interrupted();
        }

        assertTrue(result.cancelled());
        assertEquals(1, result.generated());
        assertFalse(result.document().blockById("IMG-1").orElseThrow()
                .metadata().getOrDefault("description", "").isBlank());
        assertTrue(result.document().blockById("IMG-2").orElseThrow()
                .metadata().getOrDefault("description", "").isBlank());
    }

    private PrepareWordSemanticImagesUseCase useCase(ContentAnalysisEngine engine) {
        var registry = new ContentAnalysisEngineRegistry().register(engine);
        var media = new MediaCapabilityService(new MediaEnginePlatform(
                null, null, null, null, null, null, registry, null),
                LocalResourceScheduler.safeDefaults());
        return new PrepareWordSemanticImagesUseCase(media,
                new MaterializeWordDocumentContentAssetUseCase());
    }

    private ReadableDocument document(Map<String, String> metadata) {
        return new ReadableDocument("Word", SourceDocumentFormat.DOCX,
                project.resolve("source.docx"), List.of(
                DocumentBlock.of("P-1", DocumentBlockType.PARAGRAPH,
                        "El diagrama explica el proceso experimental.", ""),
                DocumentBlock.of("IMG-1", DocumentBlockType.IMAGE_NOTICE,
                        "Imagen detectada", "", metadata),
                DocumentBlock.of("P-2", DocumentBlockType.PARAGRAPH,
                        "Los resultados confirman la relacion mostrada.", "")));
    }

    private static final class CapturingEngine implements ContentAnalysisEngine {
        private ContentAnalysisRequest lastRequest;
        private final String returnedObjectId;

        private CapturingEngine() {
            this("WORD-IMAGE-IMG-1");
        }

        private CapturingEngine(String returnedObjectId) {
            this.returnedObjectId = returnedObjectId;
        }
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("word-image-test"),
                    CapabilityId.VISUAL_CONTENT_DESCRIPTION, "test", "1", "test", Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.IMAGE_DESCRIPTION);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(descriptor().id(), List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(descriptor().id(), "ready");
        }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                                       ExecutionContext context) {
            lastRequest = request;
            String text = "El diagrama muestra tres etapas conectadas y destaca la relacion central del proceso experimental descrito en el texto cercano.";
            return new ContentAnalysisResult(text,
                    "{\"status\":\"OK\",\"objectId\":\"" + returnedObjectId + "\","
                            + "\"narrationText\":\"" + text + "\",\"claims\":[],"
                            + "\"recognizedLabels\":[],\"uncertainties\":[],\"confidence\":0.9}",
                    0.9, List.of(), Map.of("model", PrepareWordSemanticImagesUseCase.MODEL));
        }
    }

    private static final class RecoveringEngine implements ContentAnalysisEngine {
        private int calls;
        private ContentAnalysisRequest firstRequest;
        private ContentAnalysisRequest secondRequest;

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("word-image-recovery-test"),
                    CapabilityId.VISUAL_CONTENT_DESCRIPTION, "test", "1", "test",
                    Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.IMAGE_DESCRIPTION);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(descriptor().id(), List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(descriptor().id(), "ready");
        }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                                       ExecutionContext context) {
            calls++;
            if (calls == 1) {
                firstRequest = request;
                return new ContentAnalysisResult("",
                        "{\"status\":\"INSUFFICIENT_EVIDENCE\",\"objectId\":\"IMG-1\","
                                + "\"narrationText\":\"\",\"claims\":[],"
                                + "\"recognizedLabels\":[],\"uncertainties\":[],"
                                + "\"confidence\":0.1}",
                        0.1, List.of(), Map.of());
            }
            secondRequest = request;
            String text = "La ilustracion muestra dos personajes en escenas contrastantes y varios objetos claramente visibles alrededor de ellos.";
            return new ContentAnalysisResult(text,
                    "{\"status\":\"OK\",\"objectId\":\"IMG-1\","
                            + "\"narrationText\":\"" + text + "\",\"claims\":[],"
                            + "\"recognizedLabels\":[],\"uncertainties\":[],"
                            + "\"confidence\":0.8}",
                    0.8, List.of(), Map.of());
        }
    }

    private static final class InterruptingAfterFirstEngine
            implements ContentAnalysisEngine {
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("word-image-cancel-test"),
                    CapabilityId.VISUAL_CONTENT_DESCRIPTION, "test", "1", "test",
                    Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.IMAGE_DESCRIPTION);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(descriptor().id(), List.of());
        }
        @Override public EngineReadiness inspectReadiness(
                EngineConfiguration configuration) {
            return EngineReadiness.ready(descriptor().id(), "ready");
        }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                                       ExecutionContext context) {
            Thread.currentThread().interrupt();
            String text = "La primera ilustracion muestra tres etapas conectadas y una relacion visual claramente identificable entre todos sus elementos.";
            return new ContentAnalysisResult(text,
                    "{\"status\":\"OK\",\"objectId\":\"WORD-IMAGE-IMG-1\","+
                            "\"narrationText\":\"" + text + "\",\"claims\":[],"+
                            "\"recognizedLabels\":[],\"uncertainties\":[],"+
                            "\"confidence\":0.9}", 0.9, List.of(), Map.of());
        }
    }
}
