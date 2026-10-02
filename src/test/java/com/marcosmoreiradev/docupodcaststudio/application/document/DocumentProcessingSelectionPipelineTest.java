package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.audio.ReusableAudioCoverage;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.BuildDocumentStudyVideoPlanUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioGenerationStage;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningLanguage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class DocumentProcessingSelectionPipelineTest {
    @TempDir Path projectRoot;

    @Test
    void intervalTranslationAndAudioCoverageRemainTheSameUniverseAtExport()
            throws Exception {
        NarrationScriptDocument canonical = script();
        ResolveDocumentProcessingSelectionUseCase resolver =
                new ResolveDocumentProcessingSelectionUseCase();
        ResolvedDocumentProcessingSelection selection = resolver.execute(
                canonical, DocumentProcessingScope.INTERVAL,
                DocumentProcessingInterval.pages(2, 3, 3), "",
                "PDF-1", "SHA-1");
        TranslationEngine engine = new TranslationEngine();
        AdaptNarrationLanguageUseCase adaptation = adaptation(engine);
        DocumentTranslationPreferences preferences = new DocumentTranslationPreferences(
                true, DocumentListeningLanguage.ENGLISH);

        var generated = adaptation.execute(selection.narration(), preferences,
                projectRoot, CancellationToken.NONE, ProgressSink.NONE);
        var listeningPrepared = adaptation.inspectCache(
                selection.narration(), preferences, projectRoot);
        assertTrue(listeningPrepared.complete());
        assertTrue(listeningPrepared.missingSegmentIds().isEmpty());
        assertTrue(listeningPrepared.invalidSegmentIds().isEmpty());
        adaptation.execute(selection.narration(), preferences, projectRoot,
                CancellationToken.NONE, ProgressSink.NONE);
        List<AudioGenerationUnit> units = generated.script().segments().stream()
                .filter(NarrationSegment::narratable)
                .map(AudioGenerationUnit::fromSegment).toList();
        AudioJobSnapshot persisted = completedJob(units);
        ReusableAudioCoverage.Report generatedCoverage = new ReusableAudioCoverage()
                .resolve(units, List.of(persisted), projectRoot);

        int callsAfterGeneration = engine.calls.get();
        NarrationScriptDocument exportEffective = adaptation.fromCache(
                selection.narration(), preferences, projectRoot).orElseThrow();
        List<AudioGenerationUnit> exportUnits = exportEffective.segments().stream()
                .filter(NarrationSegment::narratable)
                .map(AudioGenerationUnit::fromSegment).toList();
        ReusableAudioCoverage.Report exportCoverage = new ReusableAudioCoverage()
                .resolve(exportUnits, List.of(persisted), projectRoot);

        assertEquals(List.of(2, 3), selection.resolvedPageNumbers());
        assertEquals(List.of("SEG-2", "SEG-3"), selection.resolvedSegmentIds());
        assertEquals(List.of("SEG-2", "SEG-3"), exportEffective.segments().stream()
                .map(NarrationSegment::id).toList());
        assertEquals(Set.of("Texto pagina 2", "Texto pagina 3"), Set.copyOf(engine.inputs));
        assertEquals(2, callsAfterGeneration);
        assertEquals(callsAfterGeneration, engine.calls.get(),
                "second processing and export must reuse translation cache");
        assertTrue(generatedCoverage.complete());
        assertTrue(exportCoverage.complete());
        assertEquals(units.stream().map(AudioGenerationUnit::sourceFingerprint).toList(),
                exportUnits.stream().map(AudioGenerationUnit::sourceFingerprint).toList());
        assertEquals(0, exportCoverage.missingOrStale().size());

        DocumentContentProjection visualSource = new DocumentContentProjection(
                "PDF", SourceDocumentFormat.PDF, projectRoot.resolve("source.pdf"),
                List.of(content(1), content(2), content(3)));
        DocumentContentProjection visualSelection =
                new FilterDocumentContentProjectionBySelectionUseCase()
                        .execute(visualSource, selection);
        assertEquals(List.of(2, 3), visualSelection.items().stream()
                .map(item -> item.pdfAnchor().orElseThrow().pageNumber()).toList());
        var videoPlan = new BuildDocumentStudyVideoPlanUseCase().build(
                DocuPodcastProject.createNew("Intervalo", ProjectMode.DOCUMENTARY_STUDIO),
                visualSelection, exportEffective, List.of(persisted), projectRoot,
                DocumentTextVideoOptions.defaults());
        assertEquals(Set.of("SEG-2", "SEG-3"), videoPlan.frames().stream()
                .filter(frame -> !frame.silentVisual())
                .map(frame -> frame.segmentId()).collect(java.util.stream.Collectors.toSet()));
        assertEquals(0, videoPlan.framesMissingAudio());
    }

    @Test
    void fullDocumentAddsPageOneWhileOtherScopesKeepTheirExactBoundary() {
        NarrationScriptDocument canonical = script();
        ResolveDocumentProcessingSelectionUseCase resolver =
                new ResolveDocumentProcessingSelectionUseCase();

        var full = resolver.execute(canonical, DocumentProcessingScope.FULL_DOCUMENT,
                null, "", "PDF-1", "SHA-1");
        var from = resolver.execute(canonical, DocumentProcessingScope.FROM_SELECTION,
                null, "SEG-2", "PDF-1", "SHA-1");
        var single = resolver.execute(canonical, DocumentProcessingScope.SINGLE_FRAGMENT,
                null, "SEG-2", "PDF-1", "SHA-1");

        assertEquals(List.of("SEG-1", "SEG-2", "SEG-3"), full.resolvedSegmentIds());
        assertEquals(List.of("SEG-2", "SEG-3"), from.resolvedSegmentIds());
        assertEquals(List.of("SEG-2"), single.resolvedSegmentIds());
        assertEquals(List.of(1, 2, 3), full.resolvedPageNumbers());
    }

    @Test
    void wordIntervalSelectsInclusiveSemanticBlocksWithoutPdfMetadata() {
        NarrationScriptDocument word = new NarrationScriptDocument(
                "SCRIPT-WORD", "Word", "es", "source.docx",
                List.of(wordSegment("SEG-A", "B0001"),
                        wordSegment("SEG-B", "B0002"),
                        wordSegment("SEG-C", "B0003")),
                Instant.EPOCH, Instant.EPOCH, "");

        ResolvedDocumentProcessingSelection selected =
                new ResolveDocumentProcessingSelectionUseCase().execute(
                        word, DocumentProcessingScope.INTERVAL,
                        DocumentProcessingInterval.blocks(2, 3, 3), "",
                        "SCRIPT-WORD", "");

        assertEquals(List.of("SEG-B", "SEG-C"),
                selected.resolvedSegmentIds());
        assertEquals(List.of(), selected.resolvedPageNumbers());
        assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.document
                .DocumentProcessingIntervalUnit.BLOCK, selected.unit());
    }

    private AudioJobSnapshot completedJob(List<AudioGenerationUnit> units) throws Exception {
        ArrayList<AudioSegmentSnapshot> audio = new ArrayList<>();
        for (AudioGenerationUnit unit : units) {
            Path wav = projectRoot.resolve("jobs/JOB-INTERVAL/" + unit.id() + ".wav");
            Files.createDirectories(wav.getParent());
            writeWav(wav);
            audio.add(AudioSegmentSnapshot.pending(unit.id(), unit.sourceSegmentId(),
                    unit.sourceFingerprint()).completed(
                    projectRoot.relativize(wav).toString().replace('\\', '/'), 0.25));
        }
        return new AudioJobSnapshot("JOB-INTERVAL", "Intervalo",
                AudioJobState.COMPLETED, AudioGenerationStage.EXPORT_READY,
                audio.size(), audio.size(), 0, 1.0, "", "", 0L, "OK",
                "jobs/JOB-INTERVAL", "", "", audio, Instant.EPOCH, Instant.EPOCH);
    }

    private static NarrationScriptDocument script() {
        return new NarrationScriptDocument("SCRIPT", "PDF", "es", "source.pdf",
                List.of(segment(1), segment(2), segment(3)),
                Instant.EPOCH, Instant.EPOCH, "");
    }

    private static NarrationSegment segment(int page) {
        return new NarrationSegment("SEG-" + page, NarrationSegmentType.PARAGRAPH,
                "", "Texto pagina " + page, List.of("REGION-" + page),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("pdfSourcePage", Integer.toString(page),
                        "sourceLanguage", "es", "sourceBlockId", "REGION-" + page));
    }

    private static NarrationSegment wordSegment(String segmentId, String blockId) {
        return new NarrationSegment(segmentId, NarrationSegmentType.PARAGRAPH,
                "", "Texto " + blockId, List.of(blockId),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceBlockId", blockId));
    }

    private static DocumentContentItem content(int page) {
        String id = "REGION-" + page;
        return new DocumentContentItem(id, DocumentContentKind.PROSE,
                "Pagina " + page, "Texto pagina " + page,
                List.of("SEG-" + page), List.of(id), List.of(), "FP-" + page, 1L,
                DocumentPresentationMode.TEXT_RENDER,
                new PdfContentAnchor(page, 600, 800,
                        new DocumentContentRectangle(10, 10, 590, 790),
                        List.of(id), 1L, "VISUAL-" + page));
    }

    private static AdaptNarrationLanguageUseCase adaptation(TranslationEngine engine) {
        ContentAnalysisEngineRegistry registry =
                new ContentAnalysisEngineRegistry().register(engine);
        MediaEnginePlatform platform = new MediaEnginePlatform(
                null, null, null, null, null, null, registry, null);
        return new AdaptNarrationLanguageUseCase(new MediaCapabilityService(
                platform, request -> ResourceLease.NONE));
    }

    private static void writeWav(Path target) throws Exception {
        AudioFormat format = new AudioFormat(8_000f, 16, 1, true, false);
        byte[] pcm = new byte[4_000];
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(pcm), format,
                pcm.length / format.getFrameSize())) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, target.toFile());
        }
    }

    private static final class TranslationEngine implements ContentAnalysisEngine,
            ContentAnalysisBatchLifecycle {
        final AtomicInteger calls = new AtomicInteger();
        final List<String> inputs = new ArrayList<>();
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("scope-translation-test"),
                    CapabilityId.NARRATION_TRANSLATION, "Translation", "1", "test",
                    Set.of(), true);
        }
        @Override public Set<ContentAnalysisOperation> operations() {
            return Set.of(ContentAnalysisOperation.NARRATION_TRANSLATION);
        }
        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(descriptor().id(), List.of());
        }
        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(descriptor().id(), "ready");
        }
        @Override public ContentAnalysisResult analyze(ContentAnalysisRequest request,
                                                       ExecutionContext context) {
            calls.incrementAndGet();
            inputs.add(request.nearbyContext());
            String page = request.nearbyContext().replaceAll("\\D+", "");
            return new ContentAnalysisResult("The English text for page " + page
                    + " is ready for narration.", "", 1.0,
                    List.of(), Map.of());
        }
        @Override public void beginContentAnalysisBatch(ExecutionContext context) { }
        @Override public void endContentAnalysisBatch(ExecutionContext context) { }
    }
}
