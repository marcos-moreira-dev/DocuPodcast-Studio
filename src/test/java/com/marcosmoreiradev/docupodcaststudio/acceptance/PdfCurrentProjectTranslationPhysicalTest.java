package com.marcosmoreiradev.docupodcaststudio.acceptance;

import com.marcosmoreiradev.docupodcaststudio.application.document.FilterPdfNarrationByPageIntervalUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningLanguage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentTranslationPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.reading.FileNarrationTranslationCache;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputePreference;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeResourceBudget;
import com.marcosmoreiradev.docupodcaststudio.media.api.PriorityResourceScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opt-in real-project translation gate using the production adapters and cache. */
@EnabledIfSystemProperty(named = "docupodcast.pdf.currentProjectTranslation",
        matches = "true")
final class PdfCurrentProjectTranslationPhysicalTest {
    @Test
    void preparesEffectiveEnglishNarrationForRequestedPage() throws Exception {
        Path repositoryRoot = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath().normalize();
        Path projectRoot = Path.of(System.getProperty(
                "docupodcast.pdf.currentProjectRoot",
                "D:/Proyectos/Demostracion_limite_notable"));
        int page = Integer.getInteger("docupodcast.pdf.translationPage", 3);
        var repository = new JsonPreparedPdfDocumentRepository();
        var manifest = repository.loadManifest(projectRoot).orElseThrow();
        Path source = projectRoot.resolve("source/Demostracion_limite_notable.pdf");
        var workspace = new PreparedPdfWorkspaceRef(projectRoot, source,
                manifest.sourceSha256());
        var canonical = new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, manifest.title(), "es", false, null,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        var scoped = page <= 0 ? canonical
                : new FilterPdfNarrationByPageIntervalUseCase().execute(
                canonical, DocumentProcessingInterval.pages(page, page,
                        manifest.pageCount()));
        var preferences = new DocumentTranslationPreferences(true,
                DocumentListeningLanguage.ENGLISH);
        var cache = new FileNarrationTranslationCache();
        String correlationId = "PHYSICAL-GATE-TRANSLATION-"
                + (page <= 0 ? "FULL" : "P" + page) + "-"
                + Instant.now().toEpochMilli();
        long started = System.nanoTime();
        try (var platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(repositoryRoot))) {
            var media = new MediaCapabilityService(platform,
                    new PriorityResourceScheduler(
                            ComputeResourceBudget.incrementalReaderDefaults()),
                    ComputePreference::automatic);
            var useCase = new AdaptNarrationLanguageUseCase(media, cache);
            var before = useCase.inspectCache(scoped, preferences, projectRoot);
            var result = useCase.execute(scoped, preferences, projectRoot,
                    () -> false,
                    (stage, progress, message) -> System.out.printf(
                            "gate=%s stage=%s progress=%.3f message=%s%n",
                            correlationId, stage, progress, message), correlationId);
            var after = useCase.inspectCache(scoped, preferences, projectRoot);
            assertTrue(after.complete(), () -> "missing=" + after.missingSegmentIds()
                    + " invalid=" + after.invalidSegmentIds());
            Path report = Path.of("target/pdf-current-translation-gate.txt");
            Files.writeString(report,
                    "correlationId=" + correlationId
                            + "\npage=" + page
                            + "\nsegments=" + scoped.segments().size()
                            + "\nbeforeMissing=" + before.missingSegmentIds()
                            + "\nbeforeInvalid=" + before.invalidSegmentIds()
                            + "\ntranslationCalls=" + result.translationCalls()
                            + "\ncacheHits=" + result.cacheHits()
                            + "\nafterMissing=" + after.missingSegmentIds()
                            + "\nafterInvalid=" + after.invalidSegmentIds()
                            + "\nelapsedMs=" + ((System.nanoTime() - started) / 1_000_000L)
                            + "\n", StandardCharsets.UTF_8);
        }
    }
}
