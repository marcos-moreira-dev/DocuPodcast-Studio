package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.document.FilterPdfNarrationByPageIntervalUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningLanguage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class AdaptNarrationLanguageUseCaseTest {
    @Test
    void cancellationStopsActiveInferenceEndsBatchAndReleasesLease() throws Exception {
        CountDownLatch inferenceStarted = new CountDownLatch(1);
        CountDownLatch inferenceStopped = new CountDownLatch(1);
        AtomicInteger batchEnds = new AtomicInteger();
        AtomicInteger leaseCloses = new AtomicInteger();
        ContentAnalysisEngine engine = new TranslationEngine() {
            @Override public ContentAnalysisResult analyze(
                    ContentAnalysisRequest request, ExecutionContext context)
                    throws InterruptedException {
                calls.incrementAndGet();
                inferenceStarted.countDown();
                try {
                    while (true) {
                        context.cancellation().throwIfCancellationRequested();
                        Thread.sleep(25L);
                    }
                } finally {
                    inferenceStopped.countDown();
                }
            }

            @Override public void endContentAnalysisBatch(ExecutionContext context) {
                batchEnds.incrementAndGet();
            }
        };
        ResourceScheduler scheduler = request -> () -> leaseCloses.incrementAndGet();
        MediaEnginePlatform platform = new MediaEnginePlatform(null, null, null,
                null, null, null,
                new ContentAnalysisEngineRegistry().register(engine), null);
        AdaptNarrationLanguageUseCase useCase = new AdaptNarrationLanguageUseCase(
                new MediaCapabilityService(platform, scheduler));
        AtomicBoolean cancellation = new AtomicBoolean();
        AtomicReference<Throwable> terminal = new AtomicReference<>();
        AtomicBoolean continued = new AtomicBoolean();
        Thread worker = Thread.ofVirtual().start(() -> {
            try {
                useCase.execute(script(spanishSegment("SEG-CANCEL",
                                "La demostración usa dos cotas.")),
                        new DocumentTranslationPreferences(true,
                                DocumentListeningLanguage.ENGLISH),
                        cancellation::get, ProgressSink.NONE);
                continued.set(true);
            } catch (Throwable stopped) {
                terminal.set(stopped);
            }
        });

        assertTrue(inferenceStarted.await(2, TimeUnit.SECONDS));
        cancellation.set(true);
        worker.interrupt();
        worker.join(2_000L);

        assertFalse(worker.isAlive(), "translation worker must terminate");
        assertTrue(inferenceStopped.await(1, TimeUnit.SECONDS));
        assertInstanceOf(InterruptedException.class, terminal.get());
        assertFalse(continued.get(), "cancelled work must not run its continuation");
        assertEquals(1, batchEnds.get(), "batch lifecycle must close exactly once");
        assertEquals(1, leaseCloses.get(), "resource lease must be released exactly once");
    }

    @Test
    void translationOffIsExactPassthroughAndMakesNoModelRequest() throws Exception {
        Fixture fixture = new Fixture();
        NarrationScriptDocument source = script(english(), spanish());

        var result = fixture.useCase.execute(source,
                DocumentTranslationPreferences.defaults(),
                CancellationToken.NONE, ProgressSink.NONE);

        assertEquals(source.segments().stream().map(NarrationSegment::narrationText).toList(),
                result.script().segments().stream().map(NarrationSegment::narrationText).toList());
        assertEquals("en", result.script().language());
        assertEquals(0, result.translationCalls());
        assertEquals(0, fixture.engine.calls.get());
    }

    @Test
    void translatesOnlyMismatchedSegmentsAndKeepsStableSourceBindings() throws Exception {
        Fixture fixture = new Fixture();
        NarrationScriptDocument source = script(english(), spanish());
        DocumentTranslationPreferences preferences = new DocumentTranslationPreferences(
                true, DocumentListeningLanguage.SPANISH);

        var first = fixture.useCase.execute(source, preferences,
                CancellationToken.NONE, ProgressSink.NONE);
        var second = fixture.useCase.execute(source, preferences,
                CancellationToken.NONE, ProgressSink.NONE);

        assertEquals(1, first.translationCalls());
        assertEquals(1, first.passthroughSegments());
        assertEquals(1, fixture.engine.calls.get(), "second pass must use translation cache");
        assertEquals(1, second.cacheHits());
        NarrationSegment translated = first.script().segmentById("SEG-EN").orElseThrow();
        assertEquals("La derivada de seno de x es coseno de x.", translated.narrationText());
        assertEquals(List.of("PDF-R1"), translated.sourceBlockIds());
        assertEquals("PDF-R1", translated.metadata().get("sourceRegionId"));
        assertEquals("en", translated.metadata().get("sourceLanguage"));
        assertEquals("es", translated.metadata().get("effectiveLanguage"));
        assertEquals("La derivada es continua.",
                first.script().segmentById("SEG-ES").orElseThrow().narrationText());
        assertEquals("The derivative of sin x is cos x.",
                source.segmentById("SEG-EN").orElseThrow().narrationText(),
                "canonical source narration must remain untouched");
    }

    @Test
    void fingerprintIgnoresVisualGeometry() {
        String first = AdaptNarrationLanguageUseCase.fingerprint(
                "The derivative", "en", "es");
        String second = AdaptNarrationLanguageUseCase.fingerprint(
                "The derivative", "en", "es");
        assertEquals(first, second);
        assertNotEquals(first, AdaptNarrationLanguageUseCase.fingerprint(
                "The derivative", "en", "en"));
    }

    @Test
    void intervalTranslatesOnlySegmentsInsideRequestedPages() throws Exception {
        Fixture fixture = new Fixture();
        NarrationSegment pageOne = withPage(english(), 1);
        NarrationSegment pageTwo = withPage(new NarrationSegment(
                "SEG-EN-2", NarrationSegmentType.PARAGRAPH, "",
                "The function is continuous on the interval.", List.of("PDF-R3"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceLanguage", "en")), 2);
        NarrationScriptDocument interval = new FilterPdfNarrationByPageIntervalUseCase()
                .execute(script(pageOne, pageTwo), DocumentProcessingInterval.pages(2, 2, 2));

        var result = fixture.useCase.execute(interval,
                new DocumentTranslationPreferences(true, DocumentListeningLanguage.SPANISH),
                CancellationToken.NONE, ProgressSink.NONE);

        assertEquals(List.of("SEG-EN-2"), result.script().segments().stream()
                .map(NarrationSegment::id).toList());
        assertEquals(1, fixture.engine.calls.get());
    }

    @Test
    void seventeenSegmentRepairCallsTranslationOnlyForTheSingleMissingEntry()
            throws Exception {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        TranslationEngine engine = new TranslationEngine(
                "The final conclusion is valid.", false);
        Fixture fixture = new Fixture(engine, cache);
        List<NarrationSegment> segments = java.util.stream.IntStream.rangeClosed(1, 17)
                .mapToObj(index -> new NarrationSegment("SEG-" + index,
                        NarrationSegmentType.PARAGRAPH, "",
                        "La conclusión matemática número " + index + " es válida.",
                        List.of("PDF-R" + index), "CHR-NARRATOR", "VOC-NARRATOR",
                        "STY-NEUTRAL", Map.of("sourceLanguage", "es",
                                "pdfSourcePage", index == 1 ? "2" : "3")))
                .toList();
        Path project = Path.of("project");
        for (int index = 0; index < 16; index++) {
            NarrationSegment segment = segments.get(index);
            cache.values.put(AdaptNarrationLanguageUseCase.fingerprint(
                    segment.narrationText(), "es", "en"),
                    "The mathematical conclusion number " + (index + 1) + " is valid.");
        }
        NarrationScriptDocument source = script(segments.toArray(NarrationSegment[]::new));
        DocumentTranslationPreferences preferences = new DocumentTranslationPreferences(
                true, DocumentListeningLanguage.ENGLISH);

        var first = fixture.useCase.execute(source, preferences, project,
                CancellationToken.NONE, ProgressSink.NONE);
        var second = fixture.useCase.execute(source, preferences, project,
                CancellationToken.NONE, ProgressSink.NONE);

        assertEquals(1, first.translationCalls());
        assertEquals(16, first.cacheHits());
        assertEquals(1, engine.calls.get());
        assertEquals(0, second.translationCalls());
        assertEquals(17, second.cacheHits());
    }

    @Test
    void passiveInspectionWithSeventeenMissingTranslationsNeverInvokesEngine()
            throws Exception {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        TranslationEngine engine = new TranslationEngine(
                "The translated narration is ready.", false);
        Fixture fixture = new Fixture(engine, cache);
        List<NarrationSegment> segments = java.util.stream.IntStream.rangeClosed(1, 17)
                .mapToObj(index -> new NarrationSegment("SEG-" + index,
                        NarrationSegmentType.PARAGRAPH, "",
                        "La conclusión matemática número " + index + " es válida.",
                        List.of("PDF-R" + index), "CHR-NARRATOR", "VOC-NARRATOR",
                        "STY-NEUTRAL", Map.of("sourceLanguage", "es",
                                "pdfSourcePage", index == 1 ? "2" : "3")))
                .toList();

        var inspection = fixture.useCase.inspectCache(
                script(segments.toArray(NarrationSegment[]::new)),
                new DocumentTranslationPreferences(true,
                        DocumentListeningLanguage.ENGLISH), Path.of("project"));

        assertFalse(inspection.complete());
        assertEquals(17, inspection.missingSegmentIds().size());
        assertTrue(inspection.invalidSegmentIds().isEmpty());
        assertTrue(inspection.effectiveScript().isEmpty());
        assertEquals(0, engine.calls.get(), "readiness must never invoke Qwen");
        assertTrue(cache.values.isEmpty(), "readiness must not repair or write cache");
    }

    @Test
    void passiveInspectionReportsWrongLanguageCacheAsInvalidWithoutRepairingIt()
            throws Exception {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        TranslationEngine engine = new TranslationEngine(
                "The translated narration is ready.", false);
        Fixture fixture = new Fixture(engine, cache);
        NarrationSegment source = new NarrationSegment("SEG-INVALID",
                NarrationSegmentType.PARAGRAPH, "", "La conclusión es válida.",
                List.of("PDF-R1"), "CHR-NARRATOR", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of("sourceLanguage", "es"));
        String fingerprint = AdaptNarrationLanguageUseCase.fingerprint(
                source.narrationText(), "es", "en");
        cache.values.put(fingerprint,
                "Tema principal: Cuando una fracción indeterminada revela una certeza.");

        var inspection = fixture.useCase.inspectCache(script(source),
                new DocumentTranslationPreferences(true,
                        DocumentListeningLanguage.ENGLISH), Path.of("project"));

        assertEquals(List.of("SEG-INVALID"), inspection.invalidSegmentIds());
        assertTrue(inspection.missingSegmentIds().isEmpty());
        assertEquals(0, engine.calls.get());
        assertEquals("Tema principal: Cuando una fracción indeterminada revela una certeza.",
                cache.values.get(fingerprint), "readiness must not mutate persisted state");
    }

    @Test
    void failedTranslationIsTerminalAndNamesTheMissingSegment() {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        TranslationEngine engine = new TranslationEngine("", true);
        Fixture fixture = new Fixture(engine, cache);
        NarrationSegment missing = new NarrationSegment("SEG-FAILED",
                NarrationSegmentType.PARAGRAPH, "", "La conclusión es válida.",
                List.of("PDF-R17"), "CHR-NARRATOR", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of("sourceLanguage", "es"));

        NarrationTranslationException failure = assertThrows(
                NarrationTranslationException.class, () -> fixture.useCase.execute(
                        script(missing), new DocumentTranslationPreferences(true,
                                DocumentListeningLanguage.ENGLISH), Path.of("project"),
                        CancellationToken.NONE, ProgressSink.NONE));

        assertEquals("SEG-FAILED", failure.segmentId());
        assertEquals("INVALID_OUTPUT", failure.reason());
        assertEquals(1, engine.calls.get());
    }

    @Test
    void wrongLanguageOutputIsPersistedAsInvalidDiagnosticNotAsTranslation() {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        String raw = "Tema principal: La demostración encierra lo desconocido entre certezas.";
        TranslationEngine engine = new TranslationEngine(raw, false);
        Fixture fixture = new Fixture(engine, cache);
        NarrationSegment source = spanishSegment("SEG-WRONG-LANGUAGE",
                "La demostración usa dos cotas.");

        NarrationTranslationException failure = assertThrows(
                NarrationTranslationException.class, () -> fixture.useCase.execute(
                        script(source), new DocumentTranslationPreferences(true,
                                DocumentListeningLanguage.ENGLISH), Path.of("project"),
                        CancellationToken.NONE, ProgressSink.NONE));

        assertEquals("INVALID_OUTPUT", failure.reason());
        assertTrue(cache.values.isEmpty());
        NarrationTranslationCache.Failure diagnostic = cache.failures.values().stream()
                .findFirst().orElseThrow();
        assertEquals(raw, diagnostic.rawOutput());
        assertEquals("SEG-WRONG-LANGUAGE", diagnostic.segmentId());
    }

    @Test
    void invalidSegmentDoesNotDiscardValidSiblingsAndRetryTouchesOnlyInvalid()
            throws Exception {
        MemoryTranslationCache cache = new MemoryTranslationCache();
        String invalidSource = "La segunda conclusión es válida.";
        TranslationEngine firstEngine = new TranslationEngine(
                "The translated conclusion is valid.", false, Set.of(invalidSource));
        Fixture first = new Fixture(firstEngine, cache);
        NarrationScriptDocument source = script(
                spanishSegment("SEG-1", "La primera conclusión es válida."),
                spanishSegment("SEG-2", invalidSource),
                spanishSegment("SEG-3", "La tercera conclusión es válida."));
        DocumentTranslationPreferences preferences = new DocumentTranslationPreferences(
                true, DocumentListeningLanguage.ENGLISH);

        assertThrows(NarrationTranslationException.class, () -> first.useCase.execute(
                source, preferences, Path.of("project"),
                CancellationToken.NONE, ProgressSink.NONE));

        assertEquals(3, firstEngine.calls.get(), "failure must not abort sibling segments");
        var partial = first.useCase.inspectCache(source, preferences, Path.of("project"));
        assertEquals(List.of("SEG-2"), partial.invalidSegmentIds());
        assertTrue(partial.missingSegmentIds().isEmpty());
        assertEquals(2, cache.values.size());

        TranslationEngine retryEngine = new TranslationEngine(
                "The second conclusion is valid.", false);
        Fixture retry = new Fixture(retryEngine, cache);
        retry.useCase.execute(source, preferences, Path.of("project"),
                CancellationToken.NONE, ProgressSink.NONE);

        assertEquals(1, retryEngine.calls.get(), "retry must translate only the invalid entry");
        assertTrue(retry.useCase.inspectCache(source, preferences,
                Path.of("project")).complete());
        assertTrue(cache.failures.isEmpty());
    }

    private static NarrationSegment spanishSegment(String id, String text) {
        return new NarrationSegment(id, NarrationSegmentType.PARAGRAPH, "", text,
                List.of("PDF-" + id), "CHR-NARRATOR", "VOC-NARRATOR",
                "STY-NEUTRAL", Map.of("sourceLanguage", "es", "pdfSourcePage", "2"));
    }

    private static NarrationSegment withPage(NarrationSegment source, int page) {
        java.util.LinkedHashMap<String, String> metadata =
                new java.util.LinkedHashMap<>(source.metadata());
        metadata.put("pdfSourcePage", Integer.toString(page));
        return new NarrationSegment(source.id(), source.type(), source.title(),
                source.narrationText(), source.sourceBlockIds(), source.characterId(),
                source.voiceProfileId(), source.performanceStyleId(), metadata);
    }

    private static NarrationScriptDocument script(NarrationSegment... segments) {
        return new NarrationScriptDocument("SCRIPT-1", "Document", "en", "source.pdf",
                List.of(segments), Instant.EPOCH, Instant.EPOCH, "");
    }

    private static NarrationSegment english() {
        return new NarrationSegment("SEG-EN", NarrationSegmentType.PARAGRAPH, "",
                "The derivative of sin x is cos x.", List.of("PDF-R1"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceRegionId", "PDF-R1", "sourceLanguage", "en",
                        "pdfBbox", "1,2,3,4"));
    }

    private static NarrationSegment spanish() {
        return new NarrationSegment("SEG-ES", NarrationSegmentType.PARAGRAPH, "",
                "La derivada es continua.", List.of("PDF-R2"),
                "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL",
                Map.of("sourceRegionId", "PDF-R2", "sourceLanguage", "es"));
    }

    private static final class Fixture {
        final TranslationEngine engine;
        final AdaptNarrationLanguageUseCase useCase;

        Fixture() {
            this(new TranslationEngine(), NarrationTranslationCache.NONE);
        }

        Fixture(TranslationEngine engine, NarrationTranslationCache cache) {
            this.engine = engine;
            ContentAnalysisEngineRegistry registry =
                    new ContentAnalysisEngineRegistry().register(engine);
            MediaEnginePlatform platform = new MediaEnginePlatform(
                    null, null, null, null, null, null, registry, null);
            useCase = new AdaptNarrationLanguageUseCase(
                    new MediaCapabilityService(platform, request -> ResourceLease.NONE), cache);
        }
    }

    private static class TranslationEngine implements ContentAnalysisEngine,
            ContentAnalysisBatchLifecycle {
        final AtomicInteger calls = new AtomicInteger();
        final String response;
        final boolean fail;
        final Set<String> invalidInputs;

        TranslationEngine() {
            this("La derivada de seno de x es coseno de x.", false);
        }

        TranslationEngine(String response, boolean fail) {
            this(response, fail, Set.of());
        }

        TranslationEngine(String response, boolean fail, Set<String> invalidInputs) {
            this.response = response;
            this.fail = fail;
            this.invalidInputs = Set.copyOf(invalidInputs);
        }

        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(new EngineId("translation-test"),
                    CapabilityId.NARRATION_TRANSLATION, "Translation test", "1",
                    "test", Set.of(), true);
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
                                                       ExecutionContext context)
                throws java.io.IOException, InterruptedException {
            calls.incrementAndGet();
            if (fail || invalidInputs.contains(request.nearbyContext())) {
                throw new EngineExecutionException(EngineDiagnosticCode.INVALID_OUTPUT,
                        "invalid translation envelope", Map.of("rawOutput", "not-json"));
            }
            return new ContentAnalysisResult(
                    response, "", 1.0,
                    List.of(), Map.of());
        }
        @Override public void beginContentAnalysisBatch(ExecutionContext context) { }
        @Override public void endContentAnalysisBatch(ExecutionContext context) { }
    }

    private static final class MemoryTranslationCache implements NarrationTranslationCache {
        final Map<String, String> values = new ConcurrentHashMap<>();
        final Map<String, Failure> failures = new ConcurrentHashMap<>();
        @Override public java.util.Optional<String> find(Path projectRoot, String fingerprint) {
            return java.util.Optional.ofNullable(values.get(fingerprint));
        }
        @Override public void store(Path projectRoot, String fingerprint, String translatedText) {
            values.put(fingerprint, translatedText);
        }
        @Override public java.util.Optional<Failure> findFailure(
                Path projectRoot, String fingerprint) {
            return java.util.Optional.ofNullable(failures.get(fingerprint));
        }
        @Override public void storeFailure(
                Path projectRoot, String fingerprint, Failure failure) {
            failures.put(fingerprint, failure);
        }
        @Override public void clearFailure(Path projectRoot, String fingerprint) {
            failures.remove(fingerprint);
        }
    }
}
