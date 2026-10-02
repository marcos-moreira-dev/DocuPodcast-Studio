package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in preparation/certification gate for the explicitly configured PDF VLM. */
@EnabledIfSystemProperty(named = "docupodcast.pdfVlm.preparePhysical", matches = "true")
final class PdfVlmModelPreparationPhysicalTest {
    @Test
    void installsExactConfiguredModelAndCertifiesARealVisualRequest() throws Exception {
        Path root = Path.of(System.getProperty("docupodcast.pdfVlm.root", "."))
                .toAbsolutePath().normalize();
        Path image = Path.of(System.getProperty("docupodcast.pdfVlm.smokeImage"))
                .toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(image), "Falta imagen real: " + image);
        PdfVlmRuntimeProfile profile = PdfVlmRuntimeProfile.fromSystem();
        assertFalse(profile.model().toLowerCase().contains("q4"),
                "El gate de calidad no permite candidato Q4");

        try (MediaEnginePlatform platform = LocalMediaAdapters.create(
                LocalMediaLayout.development(root))) {
            EngineAdministration administration = platform.administration()
                    .require(QwenVisualAnalysisEngine.ID);
            assertInstanceOf(ManagedDownloadAdministration.class, administration);
            ManagedDownloadAdministration download =
                    (ManagedDownloadAdministration) administration;
            ExecutionContext context = ExecutionContext.defaults("pdf-vlm-prepare")
                    .withDeadline(OperationDeadline.after(Duration.ofMinutes(90)));
            download.executeDownload(new EngineActionRequest(QwenVisualAnalysisEngine.ID,
                            EngineActionId.INSTALL, Map.of()),
                    ManagedDownloadDecision.USE_EXISTING, context);
            administration.execute(new EngineActionRequest(QwenVisualAnalysisEngine.ID,
                    EngineActionId.SMOKE_TEST, Map.of("testImage", image.toString())), context);

            ContentAnalysisEngine engine = platform.contentAnalysisEngines()
                    .supporting(ContentAnalysisOperation.PAGE_SEMANTIC_READING).getFirst();
            assertEquals(profile.model(), engine.descriptor().version());
            assertTrue(engine.inspectReadiness(new EngineConfiguration(
                    engine.descriptor().id(), Map.of())).ready());
        }
    }
}
