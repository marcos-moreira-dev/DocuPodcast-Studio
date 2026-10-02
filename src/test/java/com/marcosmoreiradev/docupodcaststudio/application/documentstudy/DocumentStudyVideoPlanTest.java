package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.video.SimpleVideoResolutionPreset;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentKind;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializeWordDocumentContentAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.WordContentAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.SecondarySlideInclusionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

final class DocumentStudyVideoPlanTest {
    @TempDir
    Path projectDirectory;

    @Test
    void excludesLogicalOnlySegmentsFromSpokenVideoWithoutRequiringNewAudio()
            throws Exception {
        DocumentContentItem spoken = textItem("SPOKEN", "SEG-1", "Texto narrable");
        DocumentContentItem logical = textItem("LOGICAL", "SEG-2", "Título lógico");
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"),
                List.of(spoken, logical));
        NarrationSegment spokenSegment = segment("SEG-1", "SPOKEN", "Texto narrable");
        NarrationSegment logicalOnly = new NarrationSegment("SEG-2",
                NarrationSegmentType.PARAGRAPH, "Título lógico", "Título lógico",
                List.of("LOGICAL"), "CHR-NARRATOR", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of("logicalOnly", "true"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx", List.of(spokenSegment, logicalOnly));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO),
                projection, script, List.of(), projectDirectory,
                DocumentTextVideoOptions.defaults());

        assertEquals(List.of("SEG-1"), plan.frames().stream()
                .filter(SimpleVideoFrame::audioRequired)
                .map(SimpleVideoFrame::segmentId)
                .toList());
    }

    @Test
    void explicitPrimarySourceBlockIdWinsOverSourceBlockIdsOrder() throws Exception {
        DocumentContentItem right = new DocumentContentItem("RIGHT",
                DocumentContentKind.PROSE, "Texto correcto", "Texto correcto",
                List.of("SEG-1"), List.of("RIGHT"), List.of(), "fp-right", 1L,
                DocumentPresentationMode.TEXT_RENDER, new WordContentAnchor("RIGHT"));
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), List.of(right));
        NarrationSegment segment = new NarrationSegment("SEG-1", NarrationSegmentType.PARAGRAPH,
                "Correcto", "Texto correcto", List.of("WRONG-FIRST", "RIGHT"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceBlockId", "RIGHT", "pdfCoveredRegionIds", "COVERED-WRONG",
                        "pdfReadingOrder", "999"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx", List.of(segment));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO),
                projection, script, List.of(), projectDirectory, DocumentTextVideoOptions.defaults());

        assertEquals(1, plan.frameCount());
        assertEquals("RIGHT", plan.frames().getFirst().visualBinding().sourceBlockId());
        assertEquals("RIGHT", plan.frames().getFirst().visualBinding().regionId());
        assertEquals(DocumentPresentationMode.TEXT_RENDER,
                plan.frames().getFirst().visualBinding().presentationMode());
        assertEquals(999, plan.frames().getFirst().visualBinding().readingOrder());
        assertFalse(plan.frames().getFirst().visualBinding().renderedTextHash().isBlank());
        assertFalse(plan.frames().getFirst().visualBinding().expectedSourceTextHash().isBlank());
        Path manifest = projectDirectory.resolve(NarratedFrameBindingManifestWriter.TSV_PATH);
        assertTrue(Files.isRegularFile(manifest));
        String manifestText = Files.readString(manifest);
        assertTrue(manifestText.contains("\tSEG-1\tRIGHT\t"));
        assertTrue(manifestText.lines().findFirst().orElseThrow().contains("timelineIndex"));
        assertTrue(manifestText.lines().findFirst().orElseThrow().contains("renderedTextHash"));
    }

    @Test
    void insertingSilentExtraDoesNotShiftFollowingNarratedBindings() throws Exception {
        DocumentContentItem extra = new DocumentContentItem("EXTRA-TABLE",
                DocumentContentKind.TABLE, "Tabla adicional", "", List.of(),
                List.of("EXTRA-TABLE"), "fp-extra", 1L,
                new WordContentAnchor("EXTRA-TABLE", Map.of(
                        "table.rowCount", "1", "table.columnCount", "1",
                        "table.cell.0.0", "Extra")));
        DocumentContentItem first = textItem("A", "SEG-A", "Primero");
        DocumentContentItem second = textItem("B", "SEG-B", "Segundo");
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"),
                List.of(extra, first, second));
        NarrationScriptDocument script = NarrationScriptDocument.create("Word", "es", "source.docx",
                List.of(segment("SEG-A", "A", "Primero"), segment("SEG-B", "B", "Segundo")));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO),
                projection, script, List.of(), projectDirectory, DocumentTextVideoOptions.defaults());

        List<SimpleVideoFrame> spoken = plan.frames().stream().filter(SimpleVideoFrame::audioRequired).toList();
        assertEquals(List.of("A", "B"), spoken.stream()
                .map(frame -> frame.visualBinding().sourceBlockId()).toList());
        assertTrue(plan.frames().getFirst().silentVisual());
    }

    @Test
    void consecutiveImageNarrationsShareOnlyTheirExplicitImageAndThenTransitionToText()
            throws Exception {
        Map<String, String> imageMetadata = pngMetadata();
        DocumentContentItem image = new DocumentContentItem("IMG-SHARED",
                DocumentContentKind.IMAGE, "Figura", "Descripción uno. Descripción dos.",
                List.of("SEG-I1", "SEG-I2"), List.of("IMG-SHARED"), "fp-image", 1L,
                new WordContentAnchor("IMG-SHARED", imageMetadata));
        DocumentContentItem text = textItem("TEXT-NEXT", "SEG-TEXT", "Después de la figura");
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), List.of(image, text));
        NarrationScriptDocument script = NarrationScriptDocument.create("Word", "es", "source.docx",
                List.of(segment("SEG-I1", "IMG-SHARED", "Descripción uno"),
                        segment("SEG-I2", "IMG-SHARED", "Descripción dos"),
                        segment("SEG-TEXT", "TEXT-NEXT", "Después de la figura")));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(), new DocumentStudySlideCompositor(),
                null, new MaterializeWordDocumentContentAssetUseCase()).build(
                DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO),
                projection, script, List.of(), projectDirectory, DocumentTextVideoOptions.defaults());

        assertEquals(List.of("IMG-SHARED", "IMG-SHARED", "TEXT-NEXT"), plan.frames().stream()
                .map(frame -> frame.visualBinding().sourceBlockId()).toList());
        assertEquals(plan.frames().get(0).imageRelativePath(), plan.frames().get(1).imageRelativePath());
        assertEquals(DocumentPresentationMode.SOURCE_CAPTURE,
                plan.frames().get(1).visualBinding().presentationMode());
        assertEquals(DocumentPresentationMode.TEXT_RENDER,
                plan.frames().get(2).visualBinding().presentationMode());
        assertFalse(plan.frames().get(1).imageRelativePath()
                .equals(plan.frames().get(2).imageRelativePath()));
    }

    @Test
    void refusesItemWhoseVisualIdentityDoesNotContainPrimarySource() {
        DocumentContentItem wrong = new DocumentContentItem("WRONG",
                DocumentContentKind.PROSE, "Texto ajeno", "Texto ajeno",
                List.of("SEG-1"), List.of("WRONG"), List.of(), "fp-wrong", 1L,
                DocumentPresentationMode.TEXT_RENDER, new WordContentAnchor("WRONG"));
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), List.of(wrong));
        NarrationSegment segment = new NarrationSegment("SEG-1", NarrationSegmentType.PARAGRAPH,
                "Correcto", "Texto correcto", List.of("WRONG"), "CHR-NARRATOR",
                "VOC-NARRATOR", "STY-NEUTRAL", Map.of("sourceBlockId", "RIGHT"));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx", List.of(segment));

        assertThrows(java.io.IOException.class, () -> new BuildDocumentStudyVideoPlanUseCase().build(
                DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO),
                projection, script, List.of(), projectDirectory, DocumentTextVideoOptions.defaults()));
    }

    @Test
    void wordImageUsesItsNarrationAndEmbeddedSourceInTheTransversalVideoFlow()
            throws Exception {
        BufferedImage source = new BufferedImage(240, 120, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(new Color(30, 110, 210));
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(source, "png", encoded);
        Map<String, String> metadata = Map.of(
                "embeddedImageBase64", Base64.getEncoder().encodeToString(encoded.toByteArray()),
                "embeddedImageMimeType", "image/png",
                "description", "El esquema relaciona las etapas principales del proceso.");
        DocumentContentItem image = new DocumentContentItem("IMG-1",
                DocumentContentKind.IMAGE, "Figura", "El esquema relaciona las etapas principales del proceso.",
                List.of("SEG-IMG"), List.of("IMG-1"), "content-fingerprint", 1L,
                new WordContentAnchor("IMG-1", metadata));
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), List.of(image));
        NarrationScriptDocument script = NarrationScriptDocument.create("Word", "es", "Word", List.of(
                NarrationSegment.of("SEG-IMG", NarrationSegmentType.IMAGE_NOTICE,
                        "Figura", image.narrationText(), List.of("IMG-1"))));
        DocuPodcastProject project = DocuPodcastProject.createNew(
                "Documental", ProjectMode.DOCUMENTARY_STUDIO);

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(), new DocumentStudySlideCompositor(),
                null, new MaterializeWordDocumentContentAssetUseCase())
                .build(project, projection, script, List.of(), projectDirectory,
                        DocumentTextVideoOptions.defaults());

        assertEquals(1, plan.frameCount());
        assertEquals("SEG-IMG", plan.frames().getFirst().segmentId());
        assertEquals(image.narrationText(), plan.frames().getFirst().narrationPreview());
        assertTrue(Files.isRegularFile(projectDirectory.resolve(
                plan.frames().getFirst().imageRelativePath())));
        try (var assets = Files.walk(projectDirectory.resolve(
                "media/images/document-study/word-source"))) {
            assertTrue(assets.anyMatch(Files::isRegularFile));
        }
    }

    @Test
    void resolvesOnlyParagraphsTablesAndHeadingsInDocxOrder() {
        ReadableDocument document = document();
        List<DocumentStudyVideoContentResolver.Item> items = new DocumentStudyVideoContentResolver()
                .resolve(document, DocumentStudyVideoConfiguration.empty());

        assertEquals(List.of(
                        DocumentStudyVideoContentResolver.Kind.COVER,
                        DocumentStudyVideoContentResolver.Kind.PARAGRAPH,
                        DocumentStudyVideoContentResolver.Kind.TABLE,
                        DocumentStudyVideoContentResolver.Kind.PARAGRAPH),
                items.stream().map(DocumentStudyVideoContentResolver.Item::kind).toList());
        assertFalse(items.stream().anyMatch(item -> item.block().type() == DocumentBlockType.IMAGE_NOTICE));
    }

    @Test
    void readingPolicyDoesNotRemoveSourceVisualsAndVideoOmitRemovesThemAll() {
        List<DocumentContentItem> content = List.of(
                semantic("T", DocumentContentKind.TABLE, Map.of()),
                semantic("E", DocumentContentKind.EQUATION,
                        Map.of("embeddedImageBase64", "AQID")),
                semantic("I", DocumentContentKind.IMAGE,
                        Map.of("embeddedImageBase64", "AQID")),
                semantic("X", DocumentContentKind.EXTRA,
                        Map.of("embeddedImageBase64", "AQID")));
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), content);
        DocumentStudyVideoContentResolver resolver =
                new DocumentStudyVideoContentResolver();

        assertEquals(List.of(DocumentStudyVideoContentResolver.Kind.TABLE,
                        DocumentStudyVideoContentResolver.Kind.EQUATION,
                        DocumentStudyVideoContentResolver.Kind.IMAGE,
                        DocumentStudyVideoContentResolver.Kind.EXTRA),
                resolver.resolve(projection, DocumentStudyVideoConfiguration.empty(),
                                SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS)
                        .stream().map(DocumentStudyVideoContentResolver.Item::kind).toList());
        assertEquals(List.of(DocumentStudyVideoContentResolver.Kind.TABLE,
                        DocumentStudyVideoContentResolver.Kind.EQUATION,
                        DocumentStudyVideoContentResolver.Kind.IMAGE,
                        DocumentStudyVideoContentResolver.Kind.EXTRA),
                resolver.resolve(projection, DocumentStudyVideoConfiguration.empty(),
                                SecondarySemanticReadingPolicy.IMAGES_AND_EXTRAS)
                        .stream().map(DocumentStudyVideoContentResolver.Item::kind).toList());
        assertEquals(List.of(DocumentStudyVideoContentResolver.Kind.TABLE,
                        DocumentStudyVideoContentResolver.Kind.EQUATION,
                        DocumentStudyVideoContentResolver.Kind.IMAGE,
                        DocumentStudyVideoContentResolver.Kind.EXTRA),
                resolver.resolve(projection, DocumentStudyVideoConfiguration.empty(),
                                SecondarySemanticReadingPolicy.OMIT_ALL)
                        .stream().map(DocumentStudyVideoContentResolver.Item::kind).toList());
        assertTrue(resolver.resolve(projection,
                DocumentStudyVideoConfiguration.empty()
                        .withSecondarySlideInclusionMode(SecondarySlideInclusionMode.OMIT),
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF).isEmpty());
    }

    @Test
    void authorizedSecondaryWithoutNarrationKeepsStableSilentSlideDuration() {
        DocumentContentItem image = semantic("IMG-SILENT", DocumentContentKind.IMAGE,
                Map.of("embeddedImageBase64", "AQID"));
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"), List.of(image));
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withDefaultSecondarySemanticDuration(12.0);

        DocumentStudyVideoContentResolver.Item item =
                new DocumentStudyVideoContentResolver().resolve(projection, configuration,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF).getFirst();

        assertEquals("IMG-SILENT", item.content().contentId());
        assertTrue(item.content().narrationText().isBlank());
        assertEquals(12.0, item.durationSeconds());
    }

    @Test
    void resolvesLargeDocumentAgainstLargeConfigurationWithoutQuadraticLookup() {
        ArrayList<DocumentContentItem> content = new ArrayList<>();
        ArrayList<DocumentVideoSlideConfiguration> slides = new ArrayList<>();
        for (int index = 0; index < 1_744; index++) {
            String id = "B" + index;
            content.add(textItem(id, "SEG-" + index, "Texto narrable " + index));
            slides.add(DocumentVideoSlideConfiguration.empty(id));
        }
        DocumentStudyVideoConfiguration configuration = new DocumentStudyVideoConfiguration(
                "", 6.0, List.of(), List.of(), List.of(), List.of(), List.of(),
                slides, SecondarySlideInclusionMode.FOLLOW_READING_POLICY);
        DocumentContentProjection projection = new DocumentContentProjection(
                "Documento grande", SourceDocumentFormat.DOCX,
                projectDirectory.resolve("source.docx"), content);
        DocumentStudyVideoConfiguration preparedConfiguration = configuration;

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertEquals(1_744,
                new DocumentStudyVideoContentResolver().resolve(
                        projection, preparedConfiguration,
                        SecondarySemanticReadingPolicy.OMIT_ALL).size()));
    }

    @Test
    void explicitVideoInclusionKeepsWordImageSilentWhenSemanticReadingIsOmitted()
            throws Exception {
        DocumentContentItem image = new DocumentContentItem("IMG-SILENT-VIDEO",
                DocumentContentKind.IMAGE, "Figura sin descripción", "Descripción antigua",
                List.of("SEG-STALE-IMAGE"),
                List.of("IMG-SILENT-VIDEO"), "fp-silent-video", 1L,
                new WordContentAnchor("IMG-SILENT-VIDEO", pngMetadata()));
        DocumentContentItem text = textItem("TEXT-AFTER", "SEG-AFTER", "Texto posterior");
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"),
                List.of(image, text));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx",
                List.of(segment("SEG-STALE-IMAGE", "IMG-SILENT-VIDEO",
                                "Descripción antigua"),
                        segment("SEG-AFTER", "TEXT-AFTER", "Texto posterior")));
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withDefaultSecondarySemanticDuration(6.0)
                .withSecondarySlideInclusionMode(SecondarySlideInclusionMode.INCLUDE_ALLOWED);
        DocuPodcastProject project = DocuPodcastProject.createNew(
                        "Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration))
                .withDocumentListeningPreferences(
                        DocuPodcastProject.createNew("base").documentListeningPreferences()
                                .withSecondarySemanticPolicy(SecondarySemanticReadingPolicy.OMIT_ALL));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase(
                new DocumentStudyVideoContentResolver(), new DocumentStudySlideCompositor(),
                null, new MaterializeWordDocumentContentAssetUseCase())
                .build(project, projection, script, List.of(), projectDirectory,
                        DocumentTextVideoOptions.defaults());

        assertEquals(2, plan.frameCount());
        SimpleVideoFrame silentImage = plan.frames().getFirst();
        assertTrue(silentImage.silentVisual());
        assertFalse(silentImage.audioRequired());
        assertEquals(6.0, silentImage.frameDurationSeconds());
        assertTrue(Files.isRegularFile(projectDirectory.resolve(
                silentImage.imageRelativePath())));
    }

    @Test
    void reusesParagraphSlideForAllAudioUnitsAndKeepsTableSilent() throws Exception {
        ReadableDocument document = document();
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-HEAD", NarrationSegmentType.HEADING, "Titulo", "Titulo", List.of("B000")),
                NarrationSegment.of("SEG-P1-A", NarrationSegmentType.PARAGRAPH, "P1", "Primera parte", List.of("B001")),
                NarrationSegment.of("SEG-P1-B", NarrationSegmentType.PARAGRAPH, "P1", "Segunda parte", List.of("B001")),
                new NarrationSegment("SEG-TABLE", NarrationSegmentType.TABLE_NOTICE, "Tabla", "No debe usarse",
                        List.of("B002"), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                        Map.of("secondaryReadUnit", "true")),
                NarrationSegment.of("SEG-P2", NarrationSegmentType.PARAGRAPH, "P2", "Parrafo final", List.of("B004"))));
        List<String> segmentIds = List.of("SEG-HEAD", "SEG-P1-A", "SEG-P1-B", "SEG-P2");
        for (String id : segmentIds) {
            Path file = projectDirectory.resolve("jobs/JOB/" + id + ".wav");
            Files.createDirectories(file.getParent());
            writeWav(file, 1.0);
        }
        List<AudioSegmentSnapshot> audio = segmentIds.stream()
                .map(id -> {
                    NarrationSegment segment = script.segmentById(id).orElseThrow();
                    return AudioSegmentSnapshot.pending(id, id,
                                    AudioGenerationUnit.fromSegment(segment).sourceFingerprint())
                            .completed("jobs/JOB/" + id + ".wav", 1.0);
                })
                .toList();
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withTitle("Documental")
                .withTable(new DocumentTableSlideConfiguration("B002", 9.0))
                .withClosingSlide(new DocumentStudyClosingSlide("DOC-CLOSING-1", "Gracias", 7.0, ""));
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration));

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                project, document, script, List.of(audioJob(audio)), projectDirectory,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(6, plan.frameCount());
        assertEquals("SEG-HEAD", plan.frames().get(0).segmentId());
        assertEquals("SEG-P1-A", plan.frames().get(1).segmentId());
        assertEquals("SEG-P1-B", plan.frames().get(2).segmentId());
        assertNotEquals(plan.frames().get(1).imageRelativePath(), plan.frames().get(2).imageRelativePath());
        assertTrue(plan.frames().get(1).visualBinding().fragmentCovered());
        assertTrue(plan.frames().get(2).visualBinding().fragmentCovered());
        SimpleVideoFrame table = plan.frames().get(3);
        assertTrue(table.silentVisual());
        assertEquals(9.0, table.frameDurationSeconds());
        assertTrue(table.audioRelativePath().isBlank());
        assertEquals("SEG-P2", plan.frames().get(4).segmentId());
        SimpleVideoFrame closing = plan.frames().get(5);
        assertTrue(closing.silentVisual());
        assertEquals("CLOSING-DOC-CLOSING-1", closing.segmentId());
        assertEquals(7.0, closing.frameDurationSeconds());
        assertTrue(closing.audioRelativePath().isBlank());
        assertEquals(21.4, plan.totalDurationSeconds(), 0.001);
        assertTrue(plan.exportableAsRenderedVideo());
    }

    @Test
    void disabledContentStaysResolvableButSkipsItsVisualAndNarration() throws Exception {
        ReadableDocument document = document();
        NarrationScriptDocument script = NarrationScriptDocument.create("Documento", "es", "source.docx", List.of(
                NarrationSegment.of("SEG-HEAD", NarrationSegmentType.HEADING, "Titulo", "Titulo", List.of("B000")),
                NarrationSegment.of("SEG-P1", NarrationSegmentType.PARAGRAPH, "P1", "No debe usarse", List.of("B001")),
                NarrationSegment.of("SEG-P2", NarrationSegmentType.PARAGRAPH, "P2", "Debe continuar", List.of("B004"))));
        List<String> segmentIds = List.of("SEG-HEAD", "SEG-P1", "SEG-P2");
        for (String id : segmentIds) {
            Path file = projectDirectory.resolve("jobs/JOB-DISABLED/" + id + ".wav");
            Files.createDirectories(file.getParent());
            writeWav(file, 1.0);
        }
        List<AudioSegmentSnapshot> audio = segmentIds.stream()
                .map(id -> {
                    NarrationSegment segment = script.segmentById(id).orElseThrow();
                    return AudioSegmentSnapshot.pending(id, id,
                                    AudioGenerationUnit.fromSegment(segment).sourceFingerprint())
                            .completed("jobs/JOB-DISABLED/" + id + ".wav", 1.0);
                })
                .toList();
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty()
                .withBlockEnabled("B001", false)
                .withBlockEnabled("B002", false);
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration));

        List<DocumentStudyVideoContentResolver.Item> content = new DocumentStudyVideoContentResolver()
                .resolve(document, configuration);
        assertFalse(content.stream().filter(item -> item.block().id().equals("B001")).findFirst().orElseThrow().enabled());

        SimpleVideoPlan plan = new BuildDocumentStudyVideoPlanUseCase().build(
                project, document, script, List.of(audioJob(audio)), projectDirectory,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        assertEquals(List.of("SEG-HEAD", "SEG-P2"),
                plan.frames().stream().map(SimpleVideoFrame::segmentId).toList());
        assertFalse(plan.frames().stream().anyMatch(SimpleVideoFrame::silentVisual));
    }

    @Test
    void exportNarrationRequiresOnlyEnabledConfiguredContent() {
        DocumentContentItem first = textItem("B001", "SEG-1", "Primero");
        DocumentContentItem second = textItem("B002", "SEG-2", "Segundo");
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"),
                List.of(first, second));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx", List.of(
                        segment("SEG-1", "B001", "Primero"),
                        segment("SEG-2", "B002", "Segundo")));
        DocumentStudyVideoConfiguration configuration =
                DocumentStudyVideoConfiguration.empty().withBlockEnabled("B002", false);

        NarrationScriptDocument exportNarration =
                new ResolveDocumentStudyVideoNarrationUseCase().execute(
                        projection, configuration,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, script);

        assertEquals(List.of("SEG-1"), exportNarration.segments().stream()
                .map(NarrationSegment::id).toList());
    }

    @Test
    void silentSecondaryVisualDoesNotBecomeAnAudioRequirement() throws Exception {
        DocumentContentItem text = textItem("B001", "SEG-1", "Texto hablado");
        DocumentContentItem silentImage = semantic("B002", DocumentContentKind.IMAGE,
                pngMetadata());
        DocumentContentProjection projection = new DocumentContentProjection("Word",
                SourceDocumentFormat.DOCX, projectDirectory.resolve("source.docx"),
                List.of(text, silentImage));
        NarrationScriptDocument script = NarrationScriptDocument.create(
                "Word", "es", "source.docx",
                List.of(segment("SEG-1", "B001", "Texto hablado")));

        NarrationScriptDocument exportNarration =
                new ResolveDocumentStudyVideoNarrationUseCase().execute(
                        projection, DocumentStudyVideoConfiguration.empty(),
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF, script);

        assertEquals(List.of("SEG-1"), exportNarration.segments().stream()
                .map(NarrationSegment::id).toList());
        assertTrue(new DocumentStudyVideoContentResolver()
                .resolve(projection, DocumentStudyVideoConfiguration.empty())
                .stream().anyMatch(item -> item.content().contentId().equals("B002")));
    }

    @Test
    void musicPlaylistLoopsAndTrimsAtExactVideoEnd() throws Exception {
        Files.createDirectories(projectDirectory.resolve("media/audio"));
        Files.write(projectDirectory.resolve("media/audio/one.wav"), new byte[]{1});
        Files.write(projectDirectory.resolve("media/audio/two.wav"), new byte[]{1});
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withAsset(audioAsset("AUD-1", "media/audio/one.wav"))
                .withAsset(audioAsset("AUD-2", "media/audio/two.wav"));
        DocumentStudyVideoConfiguration configuration = DocumentStudyVideoConfiguration.empty().withMusicTracks(List.of(
                new DocumentStudyMusicTrack("MUSIC-1", "AUD-1", 4.0, 0.20),
                new DocumentStudyMusicTrack("MUSIC-2", "AUD-2", 3.0, 0.35)));
        project = project.withStudy(project.study().withDocumentaryVideoConfiguration(configuration));
        SimpleVideoFrame frame = new SimpleVideoFrame(
                "FRAME-1", "TABLE-1", "Tabla", "", "", "slide.png", "",
                0.0, 12.0, true, false, true);
        SimpleVideoPlan video = new SimpleVideoPlan("Video", List.of(frame), 0.0, Instant.EPOCH);

        var overlays = new BuildDocumentStudyVideoAudioOverlayPlanUseCase()
                .build(project, projectDirectory, video);

        assertEquals(4, overlays.inputs().size());
        assertEquals(List.of(0.0, 4.0, 7.0, 11.0),
                overlays.inputs().stream().map(input -> input.timelineStartSeconds()).toList());
        assertEquals(12.0, overlays.inputs().getLast().timelineEndSeconds());
        assertEquals(1.0, overlays.inputs().getLast().sourceEndSeconds());
        assertEquals(0.35, overlays.inputs().get(1).volume());
    }

    @Test
    void illustrationOnlySkipsParagraphTextButKeepsTitleAndCenteredImage() throws Exception {
        Path source = projectDirectory.resolve("media/images/illustration.png");
        Files.createDirectories(source.getParent());
        BufferedImage illustration = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        var graphics = illustration.createGraphics();
        graphics.setColor(new Color(20, 100, 220));
        graphics.fillRect(0, 0, illustration.getWidth(), illustration.getHeight());
        graphics.dispose();
        ImageIO.write(illustration, "png", source.toFile());

        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withAsset(new ProjectAssetReference("IMG-1", ProjectAssetKind.IMAGE, "Ilustracion",
                        "media/images/illustration.png", "image/png", "Prueba", "", ""));
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "B-LONG", "fingerprint", "IMG-1", "", "", DocumentVisualSource.IMPORTED,
                "", null, "Subtitulo que no debe mostrarse", true);
        DocumentBlock block = DocumentBlock.of("B-LONG", DocumentBlockType.PARAGRAPH,
                "Texto demasiado largo ".repeat(2500), "Normal");
        Path output = projectDirectory.resolve("generated/slide.png");

        new DocumentStudySlideCompositor().composeParagraph(
                output, block, visual, DocumentStudyVideoConfiguration.empty().withTitle("Titulo global"),
                project, projectDirectory,
                DocumentTextVideoOptions.defaults().withResolution(SimpleVideoResolutionPreset.HD_720));

        BufferedImage rendered = ImageIO.read(output.toFile());
        assertEquals(1280, rendered.getWidth());
        assertEquals(720, rendered.getHeight());
        Color center = new Color(rendered.getRGB(rendered.getWidth() / 2, rendered.getHeight() / 2));
        assertEquals(new Color(20, 100, 220), center);
    }

    @Test
    void narratedTextFrameUsesConfiguredUnderlineButOrdinaryVisualDoesNot() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO);
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "B-UNDERLINE", "fingerprint", "", "", "", DocumentVisualSource.IMPORTED,
                "", null, "", false);
        DocumentBlock block = DocumentBlock.of("B-UNDERLINE", DocumentBlockType.PARAGRAPH,
                "Este fragmento se narra y ocupa más de una línea para verificar el énfasis visual.", "Normal");
        DocumentTextVideoOptions options = new DocumentTextVideoOptions(
                SimpleVideoResolutionPreset.LOW_540,
                DocumentTextVideoBackgroundMode.SOLID_COLOR, "#FFFFFF", "",
                "#20232A", "#4F46E5", "SansSerif", 48,
                true, "#FF0000", 8);
        Path narrated = projectDirectory.resolve("generated/narrated.png");
        Path ordinary = projectDirectory.resolve("generated/ordinary.png");
        DocumentStudySlideCompositor compositor = new DocumentStudySlideCompositor();

        compositor.composeNarratedParagraph(narrated, block, visual,
                DocumentStudyVideoConfiguration.empty(), project, projectDirectory, options);
        compositor.composeParagraph(ordinary, block, visual,
                DocumentStudyVideoConfiguration.empty(), project, projectDirectory, options);

        assertTrue(countPixels(ImageIO.read(narrated.toFile()), Color.RED) > 100);
        assertEquals(0, countPixels(ImageIO.read(ordinary.toFile()), Color.RED));
    }

    @Test
    void wordSentenceHighlightKeepsTheCompleteParagraphAsVisualContext() throws Exception {
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO);
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "B-CONTEXT", "fingerprint", "", "", "", DocumentVisualSource.IMPORTED,
                "", null, "", false);
        DocumentBlock block = DocumentBlock.of("B-CONTEXT", DocumentBlockType.PARAGRAPH,
                "Primera oración que se está narrando. Segunda oración que debe permanecer visible.", "Normal");
        DocumentTextVideoOptions options = new DocumentTextVideoOptions(
                SimpleVideoResolutionPreset.LOW_540,
                DocumentTextVideoBackgroundMode.SOLID_COLOR, "#FFFFFF", "",
                "#20232A", "#4F46E5", "SansSerif", 48,
                true, "#FF0000", 8);
        Path narrated = projectDirectory.resolve("generated/context-narrated.png");
        Path ordinary = projectDirectory.resolve("generated/context-ordinary.png");
        DocumentStudySlideCompositor compositor = new DocumentStudySlideCompositor();

        compositor.composeNarratedParagraph(narrated, block,
                "Primera oración que se está narrando.", visual,
                DocumentStudyVideoConfiguration.empty(), project, projectDirectory, options);
        compositor.composeParagraph(ordinary, block, visual,
                DocumentStudyVideoConfiguration.empty(), project, projectDirectory, options);

        BufferedImage narratedImage = ImageIO.read(narrated.toFile());
        BufferedImage ordinaryImage = ImageIO.read(ordinary.toFile());
        assertTrue(countPixels(narratedImage, Color.RED) > 50);
        long ordinaryTextPixels = countPixels(ordinaryImage, new Color(0x20, 0x23, 0x2A));
        long narratedTextPixels = countPixels(narratedImage, new Color(0x20, 0x23, 0x2A));
        // The rounded underline may antialias over a few descender pixels, but
        // both complete sentences must retain the same rendered text body.
        assertTrue(Math.abs(ordinaryTextPixels - narratedTextPixels) < 100);
    }

    @Test
    void narratedUnderlineSettingsAreClampedAndPreservedAcrossResolutionChanges() {
        DocumentTextVideoOptions options = new DocumentTextVideoOptions(
                SimpleVideoResolutionPreset.LOW_540,
                DocumentTextVideoBackgroundMode.SOLID_COLOR, "#FFFFFF", "",
                "#20232A", "#4F46E5", "SansSerif", 48,
                true, "#123ABC", 99).withResolution(SimpleVideoResolutionPreset.HD_720);

        assertTrue(options.underlineNarratedText());
        assertEquals("#123ABC", options.narratedUnderlineColor());
        assertEquals(15, options.narratedUnderlineThicknessPx());
        assertEquals(SimpleVideoResolutionPreset.HD_720, options.resolution());
    }

    private static long countPixels(BufferedImage image, Color expected) {
        long count = 0L;
        int rgb = expected.getRGB();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == rgb) count++;
            }
        }
        return count;
    }

    private static ReadableDocument document() {
        return new ReadableDocument("Documento", SourceDocumentFormat.DOCX, Path.of("source.docx"), List.of(
                DocumentBlock.of("B000", DocumentBlockType.HEADING, "Introduccion", "Heading 1"),
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto de prueba para el primer parrafo.", "Normal"),
                DocumentBlock.of("B002", DocumentBlockType.TABLE_NOTICE, "Tabla", "table", Map.of(
                        "table.rowCount", "2", "table.columnCount", "2",
                        "table.header.0", "Nombre", "table.header.1", "Valor",
                        "table.cell.1.0", "A", "table.cell.1.1", "1")),
                DocumentBlock.of("B003", DocumentBlockType.IMAGE_NOTICE, "Imagen incrustada", "image"),
                DocumentBlock.of("B004", DocumentBlockType.PARAGRAPH, "Texto de cierre.", "Normal")));
    }

    private static DocumentContentItem semantic(String id, DocumentContentKind kind,
                                                Map<String, String> metadata) {
        return new DocumentContentItem(id, kind, id, "", List.of(), List.of(id),
                "fingerprint-" + id, 1L, new WordContentAnchor(id, metadata));
    }

    private static DocumentContentItem textItem(String id, String segmentId, String text) {
        return new DocumentContentItem(id, DocumentContentKind.PROSE, text, text,
                List.of(segmentId), List.of(id), List.of(), "fp-" + id, 1L,
                DocumentPresentationMode.TEXT_RENDER, new WordContentAnchor(id));
    }

    private static NarrationSegment segment(String id, String sourceId, String text) {
        return new NarrationSegment(id, NarrationSegmentType.PARAGRAPH, text, text,
                List.of(sourceId), "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceBlockId", sourceId));
    }

    private static Map<String, String> pngMetadata() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return Map.of("embeddedImageBase64", Base64.getEncoder().encodeToString(output.toByteArray()),
                "embeddedImageMimeType", "image/png");
    }

    private static AudioJobSnapshot audioJob(List<AudioSegmentSnapshot> segments) {
        return new AudioJobSnapshot(
                "JOB-1", "Documento", AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                segments.size(), segments.size(), 0, 1.0, "", "", 0, "OK", "jobs/JOB", "",
                "jobs/JOB/manifest.json", segments, Instant.EPOCH, Instant.EPOCH);
    }

    private static ProjectAssetReference audioAsset(String id, String path) {
        return new ProjectAssetReference(id, ProjectAssetKind.AUDIO_CLIP, id, path,
                "audio/wav", "Musica documental", "", "");
    }

    private static void writeWav(Path target, double seconds) throws Exception {
        float sampleRate = 8_000f;
        byte[] pcm = new byte[(int) (sampleRate * seconds) * 2];
        var format = new javax.sound.sampled.AudioFormat(sampleRate, 16, 1, true, false);
        try (var input = new java.io.ByteArrayInputStream(pcm);
             var stream = new javax.sound.sampled.AudioInputStream(
                     input, format, pcm.length / format.getFrameSize())) {
            javax.sound.sampled.AudioSystem.write(stream,
                    javax.sound.sampled.AudioFileFormat.Type.WAVE, target.toFile());
        }
    }
}
