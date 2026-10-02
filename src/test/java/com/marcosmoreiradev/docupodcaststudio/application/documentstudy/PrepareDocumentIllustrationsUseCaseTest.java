package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.*;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.settings.*;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.*;
import com.marcosmoreiradev.docupodcaststudio.domain.script.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PrepareDocumentIllustrationsUseCaseTest {
    @TempDir Path root;
    private final List<String> calls = new ArrayList<>();
    private String verdict = "ACCEPT";

    @Test void disabledDoesNotContactEnginesOrCreateAssets() throws Exception {
        var result = service().prepare(project(false), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertTrue(result.images().isEmpty());
        assertTrue(calls.isEmpty());
        assertFalse(Files.exists(root.resolve("media")));
    }

    @Test void preparesReviewsAndReusesAcceptedSmallIllustrationWithoutIllustratingHeading() throws Exception {
        var useCase = service();
        var result = useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertEquals(Set.of("SEG-1"), result.images().keySet());
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        assertEquals(1, result.eligibleCount());
        assertEquals(0, result.missingCount());
        assertEquals(List.of("IMAGE_PROMPT_PLANNING", "generate", "release", "IMAGE_QUALITY_REVIEW"), calls);
        var image = ImageIO.read(result.images().get("SEG-1").toFile());
        assertEquals(600, image.getWidth());
        assertEquals(300, image.getHeight());
        useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertEquals(4, calls.size(), "Accepted assets must be reused without invoking either model");
    }

    @Test void rejectionHasOnlyOneCorrectiveRetryAndDoesNotPublishBadImage() throws Exception {
        verdict = "REJECT: use one recognizable motif";
        var result = service().prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertTrue(result.images().isEmpty());
        assertEquals(2, Collections.frequency(calls, "generate"));
        assertEquals(2, Collections.frequency(calls, "IMAGE_QUALITY_REVIEW"));
        assertEquals(1, result.warnings().size());
        assertEquals(1, result.missingCount());
        assertTrue(Files.readString(root.resolve("media/images/document-illustrations/last-report.txt"))
                .contains("0 de 1 listas; 1 pendientes"));
    }

    @Test void cancellationDoesNotStartModels() {
        assertThrows(java.io.IOException.class, () -> service().prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> true, ignored -> {}));
        assertTrue(calls.isEmpty());
    }

    @Test void explicitImportedVisualIsPreservedWithoutCallingModels() throws Exception {
        var initial = project(true);
        var configured = initial.withStudy(initial.study().withDocumentaryVideoConfiguration(
                initial.study().documentaryVideoConfiguration().withParagraph(
                        com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment
                                .empty("BODY").withImportedImage("MY-IMAGE"))));
        var result = service().prepare(configured, document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertTrue(result.images().isEmpty());
        assertTrue(calls.isEmpty());
    }

    @Test void acceptedPropertiesWithoutImageDoNotCountAsComplete() throws Exception {
        var useCase = service();
        var first = useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        Files.delete(first.images().get("SEG-1"));
        calls.clear();
        var resumed = useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertEquals(1, Collections.frequency(calls, "generate"));
        assertEquals(0, resumed.missingCount());
    }

    @Test void matchingHashForUndecodablePngStillRequiresRegeneration() throws Exception {
        var useCase = service();
        var first = useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        Path image = first.images().get("SEG-1");
        Files.writeString(image, "status=accepted");
        Path manifest = image.resolveSibling("generation.properties");
        Properties state = new Properties();
        try (var input = Files.newInputStream(manifest)) { state.load(input); }
        state.setProperty("imageSha256", HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image))));
        try (var output = Files.newOutputStream(manifest)) { state.store(output, "test"); }
        calls.clear();
        useCase.prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertEquals(1, Collections.frequency(calls, "generate"));
        assertNotNull(ImageIO.read(image.toFile()));
    }

    @Test void bareRejectionRetriesWithoutPublishingCanonicalImage() throws Exception {
        verdict = "REJECT";
        var result = service().prepare(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, ignored -> {});
        assertEquals(1, result.missingCount());
        assertEquals(2, Collections.frequency(calls, "generate"));
        try (var files = Files.walk(root)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().equals("illustration.png")));
        }
    }

    @Test void communicatesPromptGenerationReviewAndReuseWithEligibleOrdinal() throws Exception {
        var useCase = service();
        var events = new ArrayList<PrepareDocumentIllustrationsUseCase.Progress>();
        useCase.prepareWithProgress(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, events::add);
        assertTrue(events.stream().anyMatch(e -> e.phase() == PrepareDocumentIllustrationsUseCase.Phase.PROMPT));
        assertTrue(events.stream().anyMatch(e -> e.phase() == PrepareDocumentIllustrationsUseCase.Phase.GENERATION));
        assertTrue(events.stream().anyMatch(e -> e.phase() == PrepareDocumentIllustrationsUseCase.Phase.REVIEW));
        assertTrue(events.stream().filter(e -> e.phase() != PrepareDocumentIllustrationsUseCase.Phase.FINISHED)
                .allMatch(e -> e.detail().startsWith("Ilustración 1/1 (SEG-1)")));
        events.clear();
        useCase.prepareWithProgress(project(true), document(), script(), root,
                OperationalSettings.defaults(), () -> false, events::add);
        assertEquals(List.of(PrepareDocumentIllustrationsUseCase.Phase.CACHE,
                PrepareDocumentIllustrationsUseCase.Phase.FINISHED), events.stream().map(e -> e.phase()).toList());
    }

    private PrepareDocumentIllustrationsUseCase service() {
        var platform = MediaEnginePlatform.empty();
        platform.imageEngines().register(new ImageGenerationEngine() {
            public EngineDescriptor descriptor() { return descriptorFor(SelectedMediaEngines.from(OperationalSettings.defaults()).image(), CapabilityId.IMAGE_GENERATION); }
            public EngineConfigurationSchema configurationSchema() { return null; }
            public EngineReadiness inspectReadiness(EngineConfiguration configuration) { return null; }
            public ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context) throws java.io.IOException {
                calls.add("generate");
                assertEquals(512, request.width());
                assertEquals(512, request.height());
                assertEquals(ComputePreference.Mode.PREFER_GPU, context.computePreference().mode());
                assertTrue(request.prompt().contains("hand-drawn"));
                var file = request.outputDirectory().resolve(request.filenamePrefix()+".png");
                ImageIO.write(new BufferedImage(800,400,BufferedImage.TYPE_INT_RGB), "png", file.toFile());
                return new ImageGenerationResult(List.of(file), Map.of("device", "test"));
            }
            public void releaseIdleResources(ExecutionContext context) { calls.add("release"); }
        });
        platform.contentAnalysisEngines().register(new ContentAnalysisEngine() {
            public EngineDescriptor descriptor() { return descriptorFor(new EngineId("fake-analysis"), CapabilityId.VISUAL_CONTENT_DESCRIPTION); }
            public EngineConfigurationSchema configurationSchema() { return null; }
            public EngineReadiness inspectReadiness(EngineConfiguration configuration) { return null; }
            public Set<ContentAnalysisOperation> operations() { return Set.of(ContentAnalysisOperation.IMAGE_PROMPT_PLANNING, ContentAnalysisOperation.IMAGE_QUALITY_REVIEW); }
            public ContentAnalysisResult analyze(ContentAnalysisRequest request, ExecutionContext context) {
                calls.add(request.operation().name());
                boolean review = request.operation() == ContentAnalysisOperation.IMAGE_QUALITY_REVIEW;
                if (review) assertEquals(1, request.visualInputs().size());
                return new ContentAnalysisResult(review ? verdict : "A simple tree", "", 1, List.of(), Map.of());
            }
        });
        return new PrepareDocumentIllustrationsUseCase(new MediaCapabilityService(platform, request -> ResourceLease.NONE));
    }

    private static EngineDescriptor descriptorFor(EngineId id, CapabilityId capability) {
        return new EngineDescriptor(id, capability, "test", "1", "test", Set.of(), false);
    }
    private DocuPodcastProject project(boolean enabled) {
        var project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO);
        return project.withStudy(project.study().withDocumentaryVideoConfiguration(
                project.study().documentaryVideoConfiguration().withAiIllustrationsEnabled(enabled)));
    }
    private DocumentContentProjection document() {
        return new DocumentContentProjection("Bosques", SourceDocumentFormat.DOCX, root.resolve("source.docx"), List.of(
                item("TITLE", "SEG-T", DocumentContentKind.COVER, "Bosques"),
                item("BODY", "SEG-1", DocumentContentKind.PROSE, "Los árboles absorben carbono.")));
    }
    private DocumentContentItem item(String id, String segment, DocumentContentKind kind, String text) {
        return new DocumentContentItem(id, kind, text, text, List.of(segment), List.of(id), List.of(), "fp", 1L,
                DocumentPresentationMode.TEXT_RENDER, new WordContentAnchor(id));
    }
    private NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Bosques", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-T", NarrationSegmentType.TITLE, "Bosques", "Bosques", List.of("TITLE")),
                NarrationSegment.of("SEG-1", NarrationSegmentType.PARAGRAPH, "", "Los árboles absorben carbono.", List.of("BODY"))));
    }
}
