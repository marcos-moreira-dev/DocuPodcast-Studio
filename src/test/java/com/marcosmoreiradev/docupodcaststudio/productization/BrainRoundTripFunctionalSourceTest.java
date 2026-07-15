package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrainRoundTripFunctionalSourceTest {
    private static final Path ROOT = Path.of("");

    @Test
    void roundTripUseCasePersistsDocumentProjectionLayersStoryboardAndAudioJobs() throws IOException {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectRoundTripUseCase.java");
        assertTrue(useCase.contains("ProjectRoundTripUseCase"));
        assertTrue(useCase.contains("importedDocumentRepository.materialize"));
        assertTrue(useCase.contains("narrationScriptRepository.materialize"));
        assertTrue(useCase.contains("storyboardRepository.materialize"));
        assertTrue(useCase.contains("audioJobRepository.save"));
        assertTrue(useCase.contains("projectRepository.open"));
        assertTrue(useCase.contains("source Word/PDF/Markdown/TXT"));

        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/ProjectApplicationServices.java");
        assertTrue(services.contains("ProjectRoundTripUseCase projectRoundTrip"));

        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        assertTrue(factory.contains("new ProjectRoundTripUseCase"));
        assertTrue(factory.contains("infrastructure.audioJobRepository()"));
    }

    @Test
    void documentationKeepsNarratedDocumentAsRootAndListsRemainingTandas() throws IOException {
        String doc = read("docs/productizacion/ROUNDTRIP_FUNCIONAL_REAL_T66.md");
        assertTrue(doc.contains("Documento narrable"));
        assertTrue(doc.contains("proyección interna de narración"));
        assertTrue(doc.contains("nunca edita ni sobrescribe Word/DOCX, PDF, Markdown/MD o TXT"));
        assertTrue(doc.contains("ProjectRoundTripUseCaseTest"));

        String pending = read("docs/productizacion/TANDAS_PENDIENTES_POST_T66.md");
        assertTrue(pending.contains("T67"));
        assertTrue(pending.contains("T68"));
        assertTrue(pending.contains("T69"));
        assertTrue(pending.contains("T70"));
        assertTrue(pending.contains("T71"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8);
    }
}
